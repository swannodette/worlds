(ns worlds.core
  (:require [om.core :as om :include-macros true]))

;; =============================================================================
;; Protocols

(defprotocol ISprout
  (-sprout! [world]))

(defprotocol IDestroy
  (-destroy! [world]))

;; =============================================================================
;; World

(deftype World [^:mutable state worlds max meta validator ^:mutable watches]
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

  ISprout
  (-sprout! [_]
    (let [worlds' (if (= (peek worlds) state)
                    worlds
                    (conj worlds state))]
      (when-not (identical? worlds worlds')
        (reset! worlds worlds'))))

  IDestroy
  (-destroy! [this world]
    (swap! worlds pop)
    (reset! this (peek world))))

(defn world
  ([state] (world state [] nil))
  ([state worlds max] (world state worlds max))
  ([state worlds max {:keys [meta validator]}]
     (World. state (atom worlds) 100 meta validator nil)))

;; =============================================================================
;; API

(defn sprout! [owner]
  (-sprout! (om/state (om/get-props owner))))

(defn destroy! [owner]
  (-destroy! (om/state (om/get-props owner))))
