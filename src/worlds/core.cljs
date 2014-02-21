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

(deftype World [^:mutable state worlds meta validator ^:mutable watches]
  IAtom

  IDeref
  (-deref [_] state)

  IWatchable
  (-notify-watches [this oldval newval]
    )
  (-add-watch [this key f]
    )
  (-remove-watch [this key f]
    )

  IReset
  (-reset! [this old new]
    )

  ISwap
  (-swap! [this f]
    )
  (-swap! [this f a]
    )
  (-swap! [this f a b]
    )
  (-swap! [this f a b xs]
    )

  ISprout
  (-sprout! [_]
    (let [worlds' (if (= (peek worlds) state)
                    worlds
                    (conj worlds state))]
      (reset! worlds worlds')))

  IDestroy
  (-destroy! [this world]
    (swap! worlds pop)
    (reset! this (peek world))))

(defn world
  ([state & {:as options}] (world state (atom []) options))
  ([state worlds {:keys [meta validator]}]
     (World. state worlds meta validator nil)))

;; =============================================================================
;; API

(defn sprout! [owner]
  (-sprout! (om/state (om/get-props owner))))

(defn destroy! [owner]
  (-destroy! (om/state (om/get-props owner))))
