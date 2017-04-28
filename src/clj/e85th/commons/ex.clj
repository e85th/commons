(ns e85th.commons.ex
  (:require [schema.core :as s]
            [e85th.commons.util :as u]
            [clojure.string :as str])
  (:import [e85th.commons.exceptions ValidationExceptionInfo AuthExceptionInfo]))

(def ex-type ::ex-type)
(def ex-msg ::ex-msg)
(def not-found ::not-found)


(s/defn generic
  "Creates and returns a new generic exception."
  ([kind :- s/Keyword]
   (generic kind (str kind)))
  ([kind :- s/Keyword msg :- s/Str]
   (generic kind msg {}))
  ([kind :- s/Keyword msg :- (s/maybe s/Str) data-map]
   (ex-info msg (merge data-map {ex-type kind ex-msg msg}))))

(s/defn validation
  "kind becomes the error code in keyword form."
  ([kind]
   (validation kind (str kind)))
  ([kind msg]
   (validation kind msg {}))
  ([kind msg data-map]
   (validation kind msg data-map nil))
  ([kind :- s/Keyword msg :- s/Str data-map cause]
   (ValidationExceptionInfo. msg (merge data-map {ex-type kind ex-msg msg}) cause)))

(defn validation?
  [x]
  (instance? ValidationExceptionInfo x))

(s/defn auth
  "No single arity which just takes msg-or-msgs because there should be specificity
   which can be used for UIs to display more user friendly message potentially. kind becomes the
   error code in keyword form."
  ([kind]
   (auth kind (str kind)))
  ([kind msg]
   (auth kind msg {}))
  ([kind msg data-map]
   (auth kind msg data-map nil))
  ([kind :- s/Keyword msg :- s/Str data-map cause]
   (AuthExceptionInfo. msg (merge data-map {ex-type kind ex-msg msg}) cause)))

(defn auth?
  [x]
  (instance? AuthExceptionInfo x))

(defn not-found
  ([]
   (not-found "Resource not found."))
  ([msg]
   (generic not-found msg)))


(defn wrap-not-found
  "Returns a function that throws NotFoundException if f
   return nil"
  [f]
  (fn throw-if-not-found
    [& args]
    (if-let [found (apply f args)]
      found
      (throw (not-found)))))

(defn type+msg
  "Answers with a tuple of [keyword string map]"
  [ex]
  (let [data (ex-data ex)
        kind (ex-type data)]
    [kind (ex-msg data)]))

(defn error-tuple
  "Returns a triple of [keyword string map] or constructs an error-tuple."
  ([ex]
   (let [data (ex-data ex)]
     [(ex-type data) (ex-msg data) (dissoc data ex-type ex-msg)]))
  ([kind msg data]
   [kind (or msg "") (or data {})]))
