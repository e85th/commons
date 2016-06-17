(ns e85th.commons.aws.sqs
  (:require [amazonica.aws.sqs :as sqs]
            [clojure.spec :as s]))


(s/fdef ls
  :ret string?)

(defn ls
  "Lists the queues"
  []
  (:queue-urls (sqs/list-queues)))


(s/fdef mk
  :args (s/cat :q-name string? :profile (s/? string?))
  :ret string?)

(defn mk
  "Creates a queue with name q-name and if specified a profile."
  ([q-name]
   (:queue-url (sqs/create-queue :queue-name q-name)))
  ([q-name profile]
   (:queue-url (sqs/create-queue profile :queue-name q-name))))



(s/fdef rm
  :args (s/cat :q-url string?)
  :ret boolean?)

(defn rm
  "Remove the queue specified by the q-url."
  [q-url]
  (sqs/delete-queue q-url))



(s/fdef enqueue
  :args (s/cat :q-url string? :msg some? :profile (s/? string?)))

(defn enqueue
  ([q-url msg]
   (sqs/send-message q-url msg))
  ([q-url msg profile]
   (sqs/send-message profile q-url msg)))
