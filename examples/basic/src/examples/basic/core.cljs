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

(defn handle-change [e data edit-key owner cb]
  (let [text (.. e -target -value)]
    (om/transact! data edit-key (fn [_] text))
    (when cb
      (cb text))))

(defn handle-end-edit [e data edit-key owner cb]
  (when (and (om/get-state owner :editing)
             (== (.-keyCode e) 13))
    (let [text (get @data edit-key)]
      (om/set-state! owner :editing false)
      (om/transact! data edit-key (fn [_] text) :update)
      (when cb
        (cb text)))))

(defn handle-blur [owner cb]
  (om/set-state! owner :editing false)
  (when cb
    (cb)))

(defn editable [data owner {:keys [edit-key on-edit on-commit on-blur] :as opts}]
  (reify
    om/IInitState
    (init-state [_]
      {:editing false})
    om/IRenderState
    (render-state [_ {:keys [editing]}]
      (let [text (get data edit-key)]
        (dom/li nil
          (dom/span #js {:style (display (not editing))} text)
          (dom/input
            #js {:style (display editing)
                 :value text
                 :onChange #(handle-change % data edit-key owner on-edit)
                 :onKeyPress #(handle-end-edit % data edit-key owner on-commit)
                 :onBlur (fn [e] (handle-blur owner on-blur))})
          (dom/button
            #js {:style (display (not editing))
                 :onClick #(om/set-state! owner :editing true)}
            "Edit"))))))

(defn wrap-editable [data owner {:keys [view] :as opts}]
  (reify
    om/IRender
    (render [_]
      (let [data' (om/sprout! owner data)]
        (om/build view data'
          {:opts (assoc (dissoc opts :view)
                   :on-commit (fn [& xs] (om/commit! data'))
                   :on-blur (fn [& xs] (om/destroy! data')))})))))

;; =============================================================================
;; Application

(defn app-view [app owner]
  (reify
    om/IRender
    (render [_]
      (dom/div nil
        (dom/h2 nil (:title app))
        (apply dom/ul nil
          (om/build-all wrap-editable (:animals app)
            {:opts {:view editable
                    :edit-key :name}}))))))

(om/root app-view app-state {:target (gdom/getElement "app")})
