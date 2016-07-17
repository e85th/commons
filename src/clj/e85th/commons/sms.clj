(ns e85th.commons.sms
  (:refer-clojure :exclude [send])
  (:require [schema.core :as s]))

(s/defschema Message
  {:from-nbr s/Str
   :to-nbr s/Str
   :body s/Str})

(defprotocol ISmsSender
  (send [this msg]))
