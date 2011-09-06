(ns cljs-demo.hello
  (:require [cljs-demo.util :as util]))

(defn start [& _]
  (prn (util/clj->js 3))
  (prn (util/clj->js "foo"))
  (prn (util/clj->js :bar))
  (prn (util/clj->js {:foo "bar" :biz #{1 2 3}}))
  (prn (util/clj->js [1 2 3]))
  (println "Hello World!"))

; (set! *main-cli-fn* start)

