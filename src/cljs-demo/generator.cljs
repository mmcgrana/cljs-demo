(ns cljs-demo.generator
  (:require [cljs.nodejs :as node]
            [cljs-demo.util :as util]))

(def http (node/require "http"))

(defn log [data]
  (util/log (merge {:ns "cljs-demo.generator"} data)))

(defn start [& _]
  (log {:fn "start" :event "request"})
  (let [req-opts (-> (util/env "EVENTS_URL")
                   (util/url-parse)
                   (assoc :method "POST"))
        req (.request http (util/clj->js req-opts))]
    (util/set-interval 500 (fn []
      (doseq [name ["tick" "tock" "whiz" "bang"]]
        (log {:fn "start" :event "emit" :name name})
        (let [data {"name" name "value" (rand-int 5)}
              json (util/json-generate data)]
          (.write req json)))))))

(util/main "generator" start)
