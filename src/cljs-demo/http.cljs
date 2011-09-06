(ns cljs-demo.http
  (:require [cljs.nodejs :as node]))

(def http
  (node/require "http"))

(defn handler [_ res]
  (.writeHead res 200 (.strobj {"Content-Type" "text/plain"}))
  (.end res "Hello World!\n"))

(defn start [& _]
  (let [server (.createServer http handler)]
    (.listen server 1337 "127.0.0.1")
    (println "Server running at http://127.0.0.1:1337/")))

; (set! *main-cli-fn* start)
