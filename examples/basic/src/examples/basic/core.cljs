(ns examples.basic.core
  (:require [goog.dom :as gdom]
            [om.core :as om :include-macros true]
            [om.dom :as dom :include-macros true]
            [worlds.core :as worlds]))

(enable-console-print!)

;; =============================================================================
;; App State

(def app-state
  (worlds/world
    {:title "Animals!"
     :animals [{:name "Lion"} {:name "Zebra"} {:name "Alligator"}]}))

;; =============================================================================
;; Editable

(defn display [show]
  (if show
    #js {}
    #js {:display "none"}))

(defn handle-change [e data edit-key owner]
  (om/transact! data edit-key (fn [_] (.. e -target -value))))

(defn end-edit [data edit-key text owner cb]
  (om/set-state! owner :editing false)
  (om/transact! data edit-key (fn [_] text) :update)
  (when cb
    (cb text)))

(defn editable [data owner {:keys [edit-key] :as opts}]
  (reify
    om/IInitState
    (init-state [_]
      {:editing false})
    om/IWillUpdate
    (will-update [_ next-props next-state]
      (when (and (not (om/get-render-state owner :editing))
                      (:editing next-state))
        (worlds/sprout! owner)))
    om/IRenderState
    (render-state [_ {:keys [edit-text editing on-edit]}]
      (let [text (get data edit-key)]
        (dom/li nil
          (dom/span #js {:style (display (not editing))} text)
          (dom/input
            #js {:style (display editing)
                 :value text
                 :onChange #(handle-change % data edit-key owner)
                 :onKeyPress #(when (and (om/get-state owner :editing)
                                         (== (.-keyCode %) 13))
                                (end-edit data edit-key text owner on-edit))
                 :onBlur (fn [e]
                           (om/set-state! owner :editing false)
                           (worlds/destroy! owner))})
          (dom/button
            #js {:style (display (not editing))
                 :onClick #(om/set-state! owner :editing true)}
            "Edit"))))))

;; =============================================================================
;; Application

(defn app-view [app owner]
  (reify
    om/IRender
    (render [_]
      (dom/div nil
        (dom/h2 nil (:title app))
        (apply dom/ul nil
          (om/build-all editable (:animals app)
            {:opts {:edit-key :name}}))))))

(om/root app-view app-state {:target (gdom/getElement "app")})
