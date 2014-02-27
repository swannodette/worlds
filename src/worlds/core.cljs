(ns worlds.core
  (:require [om.core :as om :include-macros true]))

;; =============================================================================
;; Protocols

(defprotocol ISprout
  (-sprout! [world owner cursor]))

(defprotocol ICommit
  (-commit! [world cursor]))

(defprotocol IDestroy
  (-destroy! [world cursor]))

;; =============================================================================
;; World

(defn ^:private apply-fs [c fs]
  (reduce (fn [x [korks f tag]]
            (if (empty? korks)
              (f x)
              (update-in x korks f)))
    c fs))

(deftype World [^:mutable state worlds max sprouts meta validator
                ^:mutable watches]
  IAtom

  IDeref
  (-deref [_] state)

  IWatchable
  (-notify-watches [this oldval newval]
    (doseq [[key f] watches]
      (f key this oldval newval)))
  (-add-watch [this key f]
    (set! watches (assoc watches key f)))
  (-remove-watch [this key f]
    (set! watches (dissoc watches key)))
  
  IReset
  (-reset! [this new-value]
    (when-not (nil? validator)
      (assert (validator new-value) "Validator rejected reference state"))
    (let [old-value state]
      (set! state new-value)
      (when-not (nil? watches)
        (-notify-watches this old-value new-value))))

  ISwap
  (-swap! [this f]
    (-reset! this (f state)))
  (-swap! [this f a]
    (-reset! this (f state a)))
  (-swap! [this f a b]
    (-reset! this (f state a b)))
  (-swap! [this f a b xs]
    (-reset! this (apply f state a b xs)))

  om/IOmSwap
  (-om-swap! [this cursor korks f tag]
    (let [id (-> (om/value cursor) cljs.core/meta ::id)]
      (if (contains? @sprouts id)
        (let [cpath (om/path cursor)]
          (swap! sprouts update-in [id] conj [korks f tag])
          (if (empty? cpath)
            (swap! this clone)
            (swap! this update-in cpath clone))
          ::om/defer)
        (let [path (into (om/path cursor) korks)]
          (if (empty? path)
            (swap! this f)
            (swap! this update-in path f))))))

  ISprout
  (-sprout! [_ owner cursor]
    (let [id (om/id owner)]
      (when-not (contains? @sprouts id)
        (swap! sprouts assoc id []))
      (let [fs (get @sprouts id)
            ret (vary-meta (apply-fs cursor fs) assoc ::id id)]
        (specify! ret
          IDeref
          (-deref [_]
            (apply-fs (get-in @(om/state cursor) (om/path cursor)) fs))))))

  ICommit
  (-commit! [_ cursor]
    (let [id (-> cursor om/value cljs.core/meta ::id)
          sprouts' @sprouts]
      (when (contains? sprouts' id)
        (let [cmds (get sprouts' id)
              _    (swap! sprouts dissoc id)]
          (doseq [cmd cmds]
            (apply om/transact! (cons cursor cmd))))
        (swap! worlds conj state))))

  IDestroy
  (-destroy! [this cursor]
    (swap! sprouts dissoc (-> cursor om/value cljs.core/meta ::id))
    (swap! this identity)))

(defn world
  ([state] (world state [] nil))
  ([state worlds max] (world state worlds max nil))
  ([state worlds max & {:keys [meta validator]}]
     (World. state (atom worlds) 100 (atom {}) meta validator nil)))

;; =============================================================================
;; API

(defn sprout! [owner cursor]
  (-sprout! (om/state cursor) owner cursor))

(defn destroy! [cursor]
  (-destroy! (om/state cursor) cursor))

(defn commit! [cursor]
  (-commit! (om/state cursor) cursor))
