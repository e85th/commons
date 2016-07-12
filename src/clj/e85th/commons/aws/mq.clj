(ns e85th.commons.aws.mq
  (:require [e85th.commons.mq :as mq]
            [e85th.commons.util :as u]
            [e85th.commons.aws.sqs :as sqs]
            [e85th.commons.aws.models :as m]
            [com.stuartsierra.component :as component]
            [schema.core :as s]
            [taoensso.timbre :as log]
            [cheshire.core :as json])
  (:import [clojure.lang IFn]))

(defn- with-error-handling
  [f error-msg]
  (try
    (f)
    (catch Exception ex
      (log/error ex error-msg)
      (Thread/sleep 2)
      (u/exit 1 (.getMessage ex)))))

(defn dispatchable
  [[msg-type data]]
  [(keyword msg-type) data])

(s/defn process-message
  "Process one message, parses it to a clojure data structure and dispatches via on-message-fn."
  [profile :- m/Profile q-url :- s/Str on-message-fn :- IFn {:keys [body] :as msg}]
  (try
    (let [dispatchable-msg (-> body (json/parse-string) dispatchable)]
      (log/debugf "Message received: %s" dispatchable-msg)
      (on-message-fn dispatchable-msg)
      (sqs/delete-message profile q-url msg))
    (catch Exception ex
      (log/error ex)
      (with-error-handling #(sqs/return-to-queue profile q-url msg) "Error returning message to queue."))))

(s/defn run-sqs-loop*
  [profile :- m/Profile q-url :- s/Str on-message-fn :- IFn]
  (while true
    (let [{:keys [messages]} (sqs/dequeue profile q-url 10 20)] ;; blocking call
      (run! (partial process-message profile q-url on-message-fn) messages))))

(s/defn run-sqs-loop
  [quit profile :- m/Profile q-url :- s/Str on-message-fn]
  (while (not @quit)
    (with-error-handling #(run-sqs-loop* profile q-url on-message-fn) "Exception encountered, continuing with sqs-loop..")))

(s/defn ^:private mk-dynamic-queue :- s/Str
  "Makes a queue dynamically with a redrive policy for failed messages.
   Answers with the q-url."
  ([profile :- m/Profile q-name :- s/Str]
   (mk-dynamic-queue profile q-name "-failed"))
  ([profile :- m/Profile q-name :- s/Str dlq-suffix :- s/Str]
   (let [dlq-name (str q-name dlq-suffix)]
     (sqs/mk-with-redrive-policy profile q-name dlq-name))))

(defrecord SqsMessageProcessor [thread-name q-name topic-names profile on-message-fn dynamic? resources]
  component/Lifecycle
  (start [this]
    (log/infof "SqsMessageProcessor starting for queue %s" q-name)
    (let [quit (volatile! false)
          q-url ((if dynamic? mk-dynamic-queue sqs/name->url) profile q-name)]

      (assert q-url "Unable to locate queue. If it is dynamic, specify :dynamic? true.")

      (if (seq topic-names)
        (sqs/subscribe-to-topics profile q-url topic-names)
        (sqs/name->url profile q-name))

      (u/start-user-thread thread-name #(run-sqs-loop quit profile q-url (partial on-message-fn resources)))
      (assoc this :quit quit)))

  (stop [this]
    (log/infof "SqsMessageProcessor stopping for queue %s" q-name)
    (some-> this :quit (vreset! true))
    this))

(s/defschema
  MessageProcessorParams
  {:q-name s/Str
   :on-message-fn IFn
   (s/optional-key :profile) m/Profile
   (s/optional-key :topic-names) [s/Str]
   (s/optional-key :thread-name) s/Str
   (s/optional-key :dynamic?) s/Bool
   (s/optional-key :resources) s/Any})

(s/defn new-message-processor
  "Creates a new SqsMessageProcessor component.
   :dynamic? true will create a queue with a redrive policy.
   :resources are components needed by the message processing function.
   :on-message-fn is a function that takes resources and msg (on-message-fn resources msg)
    The msg passed to on-message-fn will be of [msg-type data].  msg-type is a keyword and
    can be used to dispatch via a multi method."
  [{:keys [q-name] :as params} :- MessageProcessorParams]
  (map->SqsMessageProcessor
   (merge {:topic-names []
           :profile m/default-profile
           :thread-name (str "sqs-mq-processor-" q-name)
           :dynamic? false} params)))
