(ns worlds.tests
  (:require [worlds.core :as worlds]))

(enable-console-print!)

(defn run-tests []
  (println "Test basic operations")

  (assert (= (type (worlds/world {:text "Hello!"})) worlds/World))

  (println "Test reset! and swap!")

  (let [w (worlds/world {:text "Hello!"})]
    (reset! w {:text "Goodbye!"})
    (assert (= (:text @w) "Goodbye!")))

  (let [w (worlds/world {:text "Hello!"})]
    (swap! w #(assoc % :text "Goodbye!"))
    (assert (= (:text @w) "Goodbye!")))

  (let [w (worlds/world {:text "Hello!"})]
    (swap! w assoc :text "Goodbye!")
    (assert (= (:text @w) "Goodbye!")))

  (let [w (worlds/world {:foo {:bar {:baz 1}}})]
    (swap! w update-in [:foo :bar :baz] + 2 3)
    (assert (= (get-in @w [:foo :bar :baz]) 6)))

  (println "Test add-watch")

  (let [w (worlds/world {:foo {:bar {:baz 1}}})
        x (atom nil)]
    (add-watch w :foo
      (fn [k r o n]
        (reset! x [k r o n])))
    (swap! w update-in [:foo :bar :baz] + 2 3)
    (assert (= @x [:foo w {:foo {:bar {:baz 1}}} {:foo {:bar {:baz 6}}}])))

  (println "Test -sprout! and -destroy!")

  (let [w (worlds/world {:text "Hello!"})]
    (worlds/-sprout! w)
    (assert (= (:text @w) "Hello!")))
  )

(run-tests)

(println "ok")
