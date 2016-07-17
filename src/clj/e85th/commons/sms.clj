(ns e85th.commons.sms
  (:refer-clojure :exclude [send])
  (:require [schema.core :as s]
            [com.stuartsierra.component :as component]))


(s/defschema Message
  {:from-nbr s/Str
   :to-nbr s/Str
   :body s/Str})

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
