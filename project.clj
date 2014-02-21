(defproject worlds "0.1.0-SNAPSHOT"
  :description "A Worlds implementation for Om"
  :url "http://github.com/swannodette/worlds"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.5.1"]
                 [org.clojure/clojurescript "0.0-2173"]
                 [om "0.5.0"]]
  :cljsbuild {
    :builds [{:id "basic"
              :source-paths ["src" "test"]
              :compiler {
                :optimizations :none
                :output-to "examples/basic/main.js"
                :output-dir "examples/basic/out"
                :source-map true}}]})
