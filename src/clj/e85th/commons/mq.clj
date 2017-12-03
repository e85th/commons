(ns e85th.commons.mq
  "Message queuing/publishing. Messages sent and received are tuples [:module/event data].
   The first element is the message type and anything else is up to the publisher/consumer.
   This tuple based message structure is used in sente and is known as a variant."
  (:require [clojure.spec.alpha :as s]
            [com.stuartsierra.component :as component]))

(s/def ::message (s/tuple keyword? any?))

(defprotocol IMessagePublisher
  (publish [this msg]))


(defrecord NilMessagePublisher []
  component/Lifecycle
  (start [this] this)
  (stop [this] this)

  IMessagePublisher
  (publish [this msg]))

(defn new-nil-message-publisher
  "Constructs an IMessagePublisher that does nothing."
  []
  (map->NilMessagePublisher {}))
