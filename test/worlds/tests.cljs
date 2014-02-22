(ns worlds.tests
  (:require [worlds.core :as worlds]))

(enable-console-print!)

(defn run-tests []
  (assert
    (= (type (worlds/world {:text "Hello!"})) worlds/World))

  (let [w (worlds/world {:text "Hello!"})]
    (reset! w {:text "Goodbye!"})
    (assert (= (:text @w) "Goodbye!")))

  (let [w (worlds/world {:text "Hello!"})]
    (swap! w #(assoc % :text "Goodbye!"))
    (assert (= (:text @w) "Goodbye!")))

  (let [w (worlds/world {:text "Hello!"})]
    (swap! w assoc :text "Goodbye!")
    (assert (= (:text @w) "Goodbye!"))))

(run-tests)

(println "ok")
