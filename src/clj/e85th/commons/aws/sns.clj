(ns e85th.commons.aws.sns
  (:require [amazonica.aws.sns :as sns]
            [e85th.commons.aws.models :as m]
            [schema.core :as s]
            [cheshire.core :as json]))

(s/defn mk-topic
  "Create a topic name"
  ([topic-name :- s/Str]
   ;; nb create-topic creates or returns the existing topic
   (mk-topic m/default-profile topic-name))
  ([profile :- m/Profile topic-name :- s/Str]
   ;; nb create-topic creates or returns the existing topic
   (:topic-arn (sns/create-topic profile :name topic-name))))

(s/defn subscribe-queue-to-topic
  "Subscribes the queue to the topic with raw message delivery."
  ([q-arn :- s/Str t-arn :- s/Str]
   (subscribe-queue-to-topic m/default-profile q-arn t-arn))
  ([profile :- m/Profile q-arn :- s/Str t-arn :- s/Str]
   (let [{:keys [subscription-arn]} (sns/subscribe profile :protocol :sqs :endpoint q-arn :topic-arn t-arn)]
     ;; raw message delivery avoids SNS meta data envelope
     (sns/set-subscription-attributes profile
                                      :subscription-arn subscription-arn
                                      :attribute-name :RawMessageDelivery
                                      :attribute-value true))))
