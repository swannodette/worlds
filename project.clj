(defproject worlds "0.1.0-SNAPSHOT"
  :description "A Worlds implementation for Om"
  :url "http://github.com/swannodette/worlds"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}

  :dependencies [[org.clojure/clojure "1.5.1"]
                 [org.clojure/clojurescript "0.0-2173"]
                 [om "0.5.1-SNAPSHOT"]]

  :plugins [[lein-cljsbuild "1.0.2"]]

  :cljsbuild {
    :builds [{:id "test"
              :source-paths ["src" "test"]
              :compiler {
                :optimizations :none
                :output-to "script/test.js"
                :output-dir "script/out"
                :source-map true}}
             {:id "basic"
              :source-paths ["examples/basic/src"]
              :compiler {
                :optimizations :none
                :output-to "examples/basic/main.js"
                :output-dir "examples/basic/out"
                :source-map true}}]})
