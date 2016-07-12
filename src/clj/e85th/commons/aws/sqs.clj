(ns e85th.commons.aws.sqs
  (:require [amazonica.aws.sqs :as sqs]
            [schema.core :as s]
            [e85th.commons.aws.sns :as sns]
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

(defn name->arn
  ([q-name]
   (-> q-name name->url url->arn))
  ([q-name profile]
   (-> q-name (name->url profile) (url->arn profile))))

(defn enqueue
  ([q-url msg]
   (sqs/send-message q-url msg))
  ([q-url msg profile]
   (sqs/send-message profile q-url msg)))


(defn dequeue
  "Dequeues a message from the queue specified by q-url.  The message is not implicitly
   deleted from the queue.  wait-secs should generally be 20 (seconds). If there is
   a message, this method will return sooner than wait-secs seconds. max-messages is the max number of messages
   to dequeue at once. This is potentially a blocking call if there are no messages to be dequeued."
  ([q-url max-messages wait-secs]
   (sqs/receive-message :queue-url q-url :delete false :wait-time-seconds wait-secs :max-number-of-messages max-messages))
  ([q-url max-messages wait-secs profile]
   (sqs/receive-message profile :queue-url q-url :delete false :wait-time-seconds wait-secs :max-number-of-messages max-messages)))


(defn delete-message
  ([q-url msg]
   (sqs/delete-message (assoc msg :queue-url q-url)))
  ([q-url msg profile]
   (sqs/delete-message profile (assoc msg :queue-url q-url))))

(s/defn return-to-queue
  "Returns a message to the queue."
  [q-url :- s/Str msg profile :- s/Str]
  (sqs/change-message-visibility profile (merge msg {:queue-url q-url :visibility-timeout 0})))

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

(s/defn subscribe-to-topics :- s/Str
  "Subscribes sqs queue q-url to topics. Answers with the q-url for q-name."
  [q-url :- s/Str topic-names :- [s/Str] profile :- s/Str]
  (let [q-arn (url->arn q-url profile)
        topic-arns (map sns/make-topic topic-names)
        q-policy (subscribe-policy q-arn topic-arns)]
    (sqs/set-queue-attributes profile :queue-url q-url :attributes {"Policy" q-policy})
    (run! #(sns/subscribe-queue-to-topic q-arn %1 profile) topic-arns)
    q-url))


(defn assign-dead-letter-queue
  [q-url dlq-url max-receive-count profile]
  (let [dlq-arn (url->arn dlq-url)
        policy (json/generate-string {:maxReceiveCount max-receive-count
                                      :deadLetterTargetArn dlq-arn})]
    (sqs/set-queue-attributes profile q-url {"RedrivePolicy" policy})))

(defn mk-queue-with-redrive-policy
  "Makes a queue with a redrive policy attached. Returns the q-url.
  Creates a dead letter queue with name dlq-name if necessary and
  sets the dlq-name as the dead letter queue for q-name."
  [q-name dlq-name profile]
  (let [q-url (mk q-name profile)
        dlq-url (mk dlq-name profile)]
    (assign-dead-letter-queue q-url dlq-url 10 profile)
    q-url))
