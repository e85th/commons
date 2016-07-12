(ns e85th.commons.aws.mq
  (:require [e85th.commons.mq :as mq]
            [e85th.commons.util :as u]
            [e85th.commons.aws.sqs :as sqs]
            [com.stuartsierra.component :as component]
            [schema.core :as s]
            [taoensso.timbre :as log]
            [cheshire.core :as json]))

(defn with-error-handling
  [f error-msg]
  (try
    (f)
    (catch Exception ex
      (log/error ex error-msg))))

(s/defn process-message
  "Process one message, parses it to a clojure data structure and dispatches via on-message-fn."
  [q-url {:keys [body] :as msg} on-message-fn profile]
  (try
    (let [body (json/parse-string body true)]
      (log/infof "Message received: %s" body)
      (on-message-fn body)
      (sqs/delete-message q-url msg profile))
    (catch Exception ex
      (log/error ex)
      (with-error-handling #(sqs/return-to-queue q-url msg profile) "Error returning message to queue."))))

(s/defn run-sqs-loop*
  [q-url on-message-fn profile]
  (while true
    (let [{:keys [messages]} (sqs/dequeue q-url profile)] ;; blocking call
      (run! #(process-message q-url %1 on-message-fn profile) messages))))

(s/defn run-sqs-loop
  [quit q-url on-message-fn profile]
  (while (not @quit)
    (with-error-handling #(run-sqs-loop* q-url on-message-fn) "Exception encountered, continuing with sqs-loop..")))

(s/defn mk-dynamic-queue :- s/Str
  "Makes a queue dynamically with a redrive policy for failed messages.
   Answers with the q-url."
  [q-name profile]
  (let [dlq-name (str q-name "-failed")]
    (sqs/mk-queue-with-redrive-policy q-name dlq-name profile)))

(defrecord SqsMessageProcessor [thread-name q-name topic-names profile on-message-fn dynamic?]
  component/Lifecycle
  (start [this]
    (let [quit (volatile! false)
          q-mk-fn (if dynamic? mk-dynamic-queue sqs/name->url)
          q-url (q-mk-fn q-name profile)]

      (if (seq topic-names)
        (sqs/subscribe-to-topics q-url topic-names profile)
        (sqs/name->url q-name profile))

      (u/start-user-thread thread-name #(run-sqs-loop quit q-url on-message-fn profile))
      (assoc this :quit quit)))

  (stop [this]
    (vreset! (:quit this) true)
    this))

(s/defn new-message-processor
  "Creates a new SqsMessageProcessor component."
  [thread-name :- s/Str q-name :- s/Str topic-names :- [s/Str] profile :- s/Str on-message-fn dynamic? :- s/Bool]
  (map->SqsMessageProcessor {:thread-name thread-name
                             :q-name q-name
                             :topic-names topic-names
                             :on-message-fn on-message-fn
                             :dynamic? dynamic?
                             :profile profile}))
