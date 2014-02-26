(ns worlds.core
  (:require [om.core :as om :include-macros true]))

;; =============================================================================
;; Protocols

(defprotocol ISprout
  (-sprout! [world]))

(defprotocol ICommit
  (-commit! [world id]))

(defprotocol IDestroy
  (-destroy! [world id]))

;; =============================================================================
;; World

(deftype World [^:mutable state worlds max sprouts meta validator
                ^:mutable watches]
  IAtom

  IDeref
  (-deref [_]
    (let [xs @sprouts]
      (if (pos? (count xs))
        (reduce (fn [st [cursor korks f tag]]
                  (let [path (into (om/path cursor) korks)]
                    (if (empty? path)
                      (f st)
                      (update-in st path f))))
          state (mapcat vals xs))
        state)))

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
    (let [id (-> cursor meta ::om/id)]
      (if (contains? @sprouts id)
        (do
          (swap! sprouts update-in [id] conj [cursor korks f tag])
          (swap! this identity)
          ::om/defer)
        (let [path (into (om/path cursor) korks)]
          (if (empty? path)
            (swap! this f)
            (swap! this update-in path f))))))

  ISprout
  (-sprout! [_ id]
    (when-not (contains? @sprouts id)
      (swap! sprouts assoc id [])))

  ICommit
  (-commit! [_ id]
    (when (contains? @sprouts id)
      (let [cmds (get @sprouts id)
            _    (swap! sprouts dissoc id)]
        (doseq [cmd cmds]
          (apply om/transact! cmd)))
      (swap! worlds conj state)))

  IDestroy
  (-destroy! [this id]
    (swap! sprouts dissoc id)))

(defn world
  ([state] (world state [] nil))
  ([state worlds max] (world state worlds max nil))
  ([state worlds max & {:keys [meta validator]}]
     (World. state (atom worlds) 100 (atom {}) meta validator nil)))

;; =============================================================================
;; API

(defn sprout! [owner cursor]
  (-sprout! (om/state cursor))
  (vary-meta cursor assoc ::id (om/id owner)))

(defn destroy! [cursor]
  (-destroy! (om/state cursor) (-> cursor meta ::id)))

(defn commit! [cursor id]
  (-commit! (om/state cursor) (-> cursor meta ::id)))
