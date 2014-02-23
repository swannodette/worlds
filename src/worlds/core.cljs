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
        (reduce (fn [st f] (f st)) state (mapcat vals xs))
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
  (-om-swap! [this cursor f]
    (let [id (-> cursor meta ::om/id)]
      (if (contains? @sprouts id)
        (swap! sprouts update-in [id] (fnil conj []) f)
        (swap! this cursor f))))
  (-om-swap! [this cursor f a]
    (let [id (-> cursor meta ::om/id)]
      (if (contains? @sprouts id)
        (swap! sprouts update-in [id] (fnil conj []) #(f % a))
        (swap! this cursor f a))))
  (-om-swap! [this cursor f a b]
    (let [id (-> cursor meta ::om/id)]
      (if (contains? @sprouts id)
        (swap! sprouts update-in [id] (fnil conj []) #(f % a b))
        (swap! this cursor f a b))))
  (-om-swap! [this cursor f a b xs]
    (let [id (-> cursor meta ::om/id)]
      (if (contains? @sprouts id)
        (swap! sprouts update-in [id] (fnil conj []) #(apply f % a b xs))
        (swap! this cursor f a b xs))))

  ISprout
  (-sprout! [_]
    (let [xs @worlds
          worlds' (cond
                    (= (peek xs) state) xs
                    (>= (count xs) max) (conj (subvec 1 xs) state)
                    :else (conj xs state))]
      (when-not (identical? worlds worlds')
        (reset! worlds worlds'))))

  ICommit
  (-commit! [_ id]
    (swap! worlds conj state)
    (let [fs (get @sprouts id)]
      (set! state (reduce (fn [s f] (f s)) state fs)))
    (swap! sprouts dissoc id))

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

(defn commit! [cursor]
  (-commit! (om/state cursor)))
