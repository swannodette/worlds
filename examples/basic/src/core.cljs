(ns examples.basic.core
  (:require [goog.dom :as gdom]
            [om.core :as om :include-macros true]
            [om.dom :as dom :include-macros true]
            [worlds.core :as worlds]))

(def app-state (worlds/world {:text "Hello worlds!"}))

(defn app-view [app owner]
  (reify
    IRender
    (-render [_]
      (dom/div nil
        (dom/h2 nil (:text app))))))

(om/root app-view app-state {:target (gdom/getElement "app")})
