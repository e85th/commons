(ns e85th.commons.mq
  "Message queuing/publishing. Messages sent and received are tuples [:module/event data].
   The first element is the message type and anything else is up to the publisher/consumer.
   This tuple based message structure is used in sente and is known as a variant."
  (:require [schema.core :as s]))

(s/defschema Message
  [(s/one s/Keyword "msg-type") (s/one s/Any "data")])

(defprotocol IMessagePublisher
  (publish [this msg]))
