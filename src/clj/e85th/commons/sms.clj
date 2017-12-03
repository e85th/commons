(ns e85th.commons.sms
  (:refer-clojure :exclude [send])
  (:require [clojure.spec.alpha :as s]
            [com.stuartsierra.component :as component]))


(s/def ::to-nbr   string?)
(s/def ::from-nbr string?)
(s/def ::body     string?)

(s/def ::message (s/keys :req-un [::to-nbr ::body]
                         :opt-un [::from-nbr]))

(defprotocol ISmsSender
  (send [this msg]))

(defrecord NilSmsSender []
  component/Lifecycle
  (start [this] this)
  (stop [this] this)

  ISmsSender
  (send [this msg]))

(defn new-nil-sms-sender
  "Constructs an ISmsSender that does nothing."
  []
  (map->NilSmsSender {}))
