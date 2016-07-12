(ns e85th.commons.mq
  "Message queuing/publishing. Messages sent and received are tuples [:module/event data].
   The first element is the message type and anything else is up to the publisher/consumer.
   This tuple based message structure is used in sente and is known as a variant.")

(defprotocol IMessagePublilsher
  (enqueue [this msg])
  (publish [this msg]))
