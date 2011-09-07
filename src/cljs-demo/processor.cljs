(ns cljs-demo.processor
  (:require [cljs.nodejs :as node]
            [cljs-demo.util :as util]))

(def url  (node/require "url"))
(def http (node/require "http"))

(defn log [data]
  (prn (merge {:ns "processor"} data)))

(defn rand-id []
  (let [chars (apply vector "abcdefghijklmnopqrstuvwxyz0123456789")
         num-chars (count chars)]
     (apply str
       (take 8 (repeatedly #(get chars (rand-int num-chars)))))))

(def stats-a
  (atom {}))

(defn update-stats [{:strs [name value]}]
  (log {:fn "update-stats" :name name})
  (swap! stats-a update-in [name] #(+ value (or % 0))))

(def conns-a
  (atom {}))

(defn add-conn [id conn]
  (log {:fn "add-conn" :conn-id id})
  (swap! conns-a assoc id conn))

(defn remove-conn [id]
  (log {:fn "remove-conn" :conn-id id})
  (swap! conns-a dissoc id))

(defn close-conns []
  (log {:fn "close-conns" :event "start"})
  (doseq [{:keys [id res]} (vals (deref conns-a))]
    (log {:fn "close-conns" :conn-id id :event "close"})
    (.end res ""))
  (log {:fn "close-conns" :event "finish"}))

(defn stream-stats [{:keys [id req res] :as conn}]
  (log {:fn "stream-stats" :conn-id id :event "respond"})
  (.writeHead res 200 (util/clj->js {"Content-Type" "application/json"}))
  (log {:fn "stream-stats" :conn-id id :event "register"})
  (add-conn id conn)
  (let [interval-id
    (util/set-interval 1000 (fn []
      (log {:fn "stream-stats" :conn-id id :event "tick"})
      (doseq [[name sum] (deref stats-a)]
        (log {:fn "stream-stats" :conn-id id :name name :event "emit"})
        (.write res (util/json-generate {"name" name "sum" sum})))))]
    (.on req "close" (fn []
      (log {:fn "stream-stats" :conn-id id :event "close"})
      (util/clear-interval interval-id)
      (remove-conn id)))))

(defn stream-events [{:keys [id req res] :as conn}]
  (log {:fn "stream-events" :conn-id id :event "register"})
  (add-conn id conn)
  (.on req "data" (fn [line]
    (when line
      (log {:fn "stream-events" :conn-id id :event "data"})
      (let [data (util/json-parse line)]
        (update-stats data)))))
  (.on req "close" (fn []
    (log {:fn "stream-events" :conn-id id :event "close"})
    (remove-conn id))))

(defn not-found [{:keys [id res]}]
  (log {:fn "not-found" :conn-id id :event "respond"})
  (.writeHead res 404 (util/clj->js {"Content-Type" "application/json"}))
  (.write res (util/json-generate {"error" "not found"}))
  (. res (end)))

(defn parse-request [req]
  {:method (.method req)
   :path   (.pathname (.parse url (.url req)))})

(defn handle-conn [req res]
  (let [conn-id (rand-id)
        {:keys [method path]} (parse-request req)
        conn {:id conn-id :req req :res res}]
    (log {:fn "handle-conn" :conn-id conn-id :method method :path path})
    (condp = [method path]
      ["GET" "/stats"]   (stream-stats conn)
      ["POST" "/events"] (stream-events conn)
      (not-found conn))))

(defn listen [handler port callback]
  (let [server (.createServer http handler)]
    (.on server "clientError" (fn [e]
      (log {:fn "listen" :even "error" :message (. e (toString))})))
    (.listen server port "0.0.0.0" #(callback server))))

(defn stop [server]
  (log {:fn "stop" :event "close-server"})
  (.close server)
  (log {:fn "stop" :event "close-conns"})
  (close-conns)
  (log {:fn "stop" :event "exit" :status 0})
  (util/exit 0))

(defn start [& _]
  (let [port (js/parseInt (util/env "PORT"))]
    (log {:fn "start" :event "listen" :port port})
    (listen handle-conn port (fn [server]
      (log {:fn "start" :event "listening"})
      (doseq [signal ["TERM" "INT"]]
        (util/trap signal (fn []
          (log {:fn "start" :event "catch" :signal signal})
          (stop server)))
        (log {:fn "start" :event "trapping" :signal signal}))))))

(util/main "processor" start)
