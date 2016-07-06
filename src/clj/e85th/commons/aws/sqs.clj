(ns e85th.commons.aws.sqs
  (:require [amazonica.aws.sqs :as sqs]
            [cheshire.core :as json]))


(defn ls
  "Lists the queues"
  []
  (:queue-urls (sqs/list-queues)))


(defn mk
  "Creates a queue with name q-name and if specified a profile. If the queue already exists
   nothing is done but the queue url is still returned."
  ([q-name]
   (:queue-url (sqs/create-queue :queue-name q-name)))
  ([q-name profile]
   (:queue-url (sqs/create-queue profile :queue-name q-name))))

(defn rm
  "Remove the queue specified by the q-url."
  [q-url]
  (sqs/delete-queue q-url))


(defn name->url
  "Looks up a queuue url "
  ([q-name]
   (sqs/find-queue q-name))
  ([q-name profile]
   (sqs/find-queue q-name)))

(defn url->attrs
  "Answers with all attrs for a given queue url"
  ([q-url]
   (sqs/get-queue-attributes q-url ["All"]))
  ([q-url profile]
   (sqs/get-queue-attributes profile q-url ["All"])))

(defn url->arn
  ([q-url]
   (:QueueArn (url->attrs q-url)))
  ([q-url profile]
   (:QueueArn (url->attrs q-url profile))))

(defn enqueue
  ([q-url msg]
   (sqs/send-message q-url msg))
  ([q-url msg profile]
   (sqs/send-message profile q-url msg)))


(defn dequeue
  "Dequeues a message from the queue specified by q-url.  The message is not implicitly
   deleted from the queue.  wait-secs should generally be 20 (seconds). If there is
   a message, this method will return sooner than wait-secs seconds. max-messages is the max number of messages
   to dequeue at once."
  ([q-url max-messages wait-secs]
   (sqs/receive-message :queue-url q-url :delete false :wait-time-seconds wait-secs :max-number-of-messages max-messages))
  ([q-url max-messages wait-secs profile]
   (sqs/receive-message profile :queue-url q-url :delete false :wait-time-seconds wait-secs :max-number-of-messages max-messages)))


(defn delete-message
  ([q-url msg]
   (sqs/delete-message (assoc msg :queue-url q-url)))
  ([q-url msg profile]
   (sqs/delete-message profile (assoc msg :queue-url q-url))))


(defn subscribe-policy
  "Generates a policy which allows the queue identified by q-arn to subscribe to
   the topic-arns."
  [q-arn topic-arns]
  {:Version "2012-10-17"
   :Statement [{:Sid "sqs-sns-subscribe"
                :Effect "Allow"
                :Principal "*"
                :Action "sqs:SendMessage"
                :Resource q-arn
                :Condition {:ArnEquals {:aws:SourceArn topic-arns}}}]})
