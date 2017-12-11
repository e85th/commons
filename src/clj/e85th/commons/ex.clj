(ns e85th.commons.ex
  (:require [e85th.commons.util :as u]
            [clojure.string :as str])
  (:import [e85th.commons.exceptions ValidationExceptionInfo AuthExceptionInfo ForbiddenExceptionInfo NotFoundExceptionInfo]))

(def ex-type ::ex-type)
(def ex-msg ::ex-msg)


;; kind - keyword, msg str
(defn generic
  "Creates and returns a new generic exception."
  ([kind]
   (generic kind (str kind)))
  ([kind msg]
   (generic kind msg {}))
  ([kind msg data-map]
   (ex-info msg (merge data-map {ex-type kind ex-msg msg}))))

(defn validation
  "kind becomes the error code in keyword form."
  ([kind]
   (validation kind (str kind)))
  ([kind msg]
   (validation kind msg {}))
  ([kind msg data-map]
   (validation kind msg data-map nil))
  ([kind msg data-map cause]
   (ValidationExceptionInfo. msg (merge data-map {ex-type kind ex-msg msg}) cause)))

(defn validation?
  [x]
  (instance? ValidationExceptionInfo x))

(defn auth
  "No single arity which just takes msg-or-msgs because there should be specificity
   which can be used for UIs to display more user friendly message potentially. kind becomes the
   error code in keyword form."
  ([kind]
   (auth kind (str kind)))
  ([kind msg]
   (auth kind msg {}))
  ([kind msg data-map]
   (auth kind msg data-map nil))
  ([kind msg data-map cause]
   (AuthExceptionInfo. msg (merge data-map {ex-type kind ex-msg msg}) cause)))

(defn auth?
  [x]
  (instance? AuthExceptionInfo x))

(defn forbidden
  "No single arity which just takes msg-or-msgs because there should be specificity
   which can be used for UIs to display more user friendly message potentially. kind becomes the
   error code in keyword form."
  ([kind]
   (forbidden kind (str kind)))
  ([kind msg]
   (forbidden kind msg {}))
  ([kind msg data-map]
   (forbidden kind msg data-map nil))
  ([kind msg data-map cause]
   (ForbiddenExceptionInfo. msg (merge data-map {ex-type kind ex-msg msg}) cause)))

(defn forbidden?
  [x]
  (instance? ForbiddenExceptionInfo x))

(defn not-found
  ([]
   (not-found "Resource not found."))
  ([msg]
   (not-found :error/not-found msg {}))
  ([kind msg data-map]
   (NotFoundExceptionInfo. msg (merge data-map {ex-type kind ex-msg msg}))))

(defn not-found?
  [x]
  (instance? NotFoundExceptionInfo x))

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
