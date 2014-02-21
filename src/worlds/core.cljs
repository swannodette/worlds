(ns worlds.core
  (:require [om.core :as om :include-macros true]))

;; =============================================================================
;; Protocols

(defprotocol ISpawn
  (-spawn [world]))

(defprotocol IDestroy
  (-destroy [world]))

(defprotocol ICommit
  (-commit [world]))

;; =============================================================================
;; World

(deftype World [value worlds]
  IAtom

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
  (-spawn [_ world]
    ()))

(defn world
  ([value] (world value (atom [value])))
  ([value worlds]
     (World. value worlds)))

;; =============================================================================
;; API

(defn spawn [owner]
  (-spawn (om/state (om/get-props owner))))

(defn destroy [owner]
  (-destroy (om/state (om/get-props owner))))

(defn commit [owner]
  (-commit (om/state (om/get-props owner))))
