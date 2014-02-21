(ns worlds.core
  (:require [om.core :as om :include-macros true]))

;; =============================================================================
;; Protocols

(defprotocol ISpawn
  (-spawn! [world]))

(defprotocol IDestroy
  (-destroy! [world]))

(defprotocol ICommit
  (-commit! [world]))

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

  ISpawn
  (-spawn! [_ world]
    (swap! worlds conj state)))

(defn world
  ([state & {:as options}] (world state (atom []) options))
  ([state worlds {:keys [meta validator]}]
     (World. state worlds meta validator nil)))

;; =============================================================================
;; API

(defn spawn! [owner]
  (-spawn! (om/state (om/get-props owner))))

(defn destroy! [owner]
  (-destroy! (om/state (om/get-props owner))))

(defn commit [owner]
  (-commit! (om/state (om/get-props owner))))
