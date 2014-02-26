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
    om/IRenderState
    (render-state [_ {:keys [edit-text editing
                             on-edit on-commit on-blur]}]
      (let [text (get data edit-key)]
        (dom/li nil
          (dom/span #js {:style (display (not editing))} text)
          (dom/input
            #js {:style (display editing)
                 :value text
                 :onChange #(do
                              (handle-change % data edit-key owner)
                              (when on-commit
                                (on-commit text)))
                 :onKeyPress #(when (and (om/get-state owner :editing)
                                         (== (.-keyCode %) 13))
                                (end-edit data edit-key text owner on-edit)
                                (when on-edit
                                  (on-edit text)))
                 :onBlur (fn [e]
                           (om/set-state! owner :editing false)
                           (when on-blur
                             (on-blur)))})
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
          {:opts (dissoc opts :view)
           :init-state {:on-commit (fn [& xs] (om/commit! data'))
                        :on-blur (fn [& xs] (om/destroy! data'))}})))))

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
