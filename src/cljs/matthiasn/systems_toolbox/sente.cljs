(ns matthiasn.systems-toolbox.sente
  (:require [cljs.core.match :refer-macros [match]]
            [matthiasn.systems-toolbox.component :as comp]
            [taoensso.sente :as sente :refer (cb-success?)]))

(defn now [] (.getTime (js/Date.)))

(defn make-handler
  "Create handler function for messages from WebSocket connection. Calls put-fn with received
   messages."
  [put-fn]
  (fn [{:keys [event]}]
    (match event
           [:chsk/state {:first-open? true}] (put-fn [:first-open true])
           [:chsk/recv [cmd-type payload]] (put-fn [cmd-type (assoc payload :recv-timestamp (now))])
           [:chsk/handshake _] ()
           :else (println "Unmatched event in WS component:" event))))

(defn mk-state
  "Return clean initial component state atom."
  [put-fn]
  (let [ws (sente/make-channel-socket! "/chsk" {:type :auto})]
    (sente/start-chsk-router! (:ch-recv ws) (make-handler put-fn))
    ws))

(defn in-handler
  "Handle incoming messages: process / add to application state."
  [ws _ msg]
  (let [state (:state ws)
        send-fn (:send-fn ws)
        [cmd-type payload] msg
        msg-meta (-> (merge (meta msg) {})
                     (assoc-in , [:client-ws-cmp :uid] (:uid @state))
                     (assoc-in , [:client-ws-cmp :sent-timestamp] (now)))]
    (send-fn [cmd-type {:msg (assoc payload :uid (:uid @state) :sent-timestamp (now)) :msg-meta msg-meta}])))

(defn component [cmp-id] (comp/make-component cmp-id mk-state in-handler nil))
