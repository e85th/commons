(ns e85th.commons.ex
  (:require [schema.core :as s]
            [e85th.commons.util :as u]
            [clojure.string :as string])
  (:import [e85th.commons.exceptions ValidationExceptionInfo]))

(def ex-type ::ex-type)
(def ex-msgs ::ex-msgs)
(def validation ::validation)
(def not-found ::not-found)


(s/defn new-generic-exception
  "Creates and returns a new validation exception."
  ([kind :- s/Keyword msg-or-msgs :- (s/conditional string? s/Str :else [s/Str])]
   (new-generic-exception kind msg-or-msgs {}))
  ([kind :- s/Keyword msg-or-msgs :- (s/conditional string? s/Str :else [s/Str]) data-map]
   (let [msgs (u/as-vector msg-or-msgs)
         msg-str (string/join "; " msgs)]
     (ex-info msg-str (merge data-map {ex-type kind ex-msgs msgs})))))

(s/defn new-validation-exception
  ([kind msg-or-msgs]
   (new-validation-exception kind msg-or-msgs {}))
  ([kind msg-or-msgs data-map]
   (new-validation-exception kind msg-or-msgs data-map nil))
  ([kind :- s/Keyword
    msg-or-msgs :- (s/conditional string? s/Str :else [s/Str])
    data-map
    cause]
   (let [msgs (u/as-vector msg-or-msgs)
         msg-str (string/join "; " msgs)]
     (ValidationExceptionInfo. msg-str (merge data-map {ex-type kind ex-msgs msgs}) cause))))

(defn validation-exception?
  [x]
  (instance? ValidationExceptionInfo x))

(defn new-not-found-exception
  ([]
   (new-not-found-exception "Resource not found."))
  ([msg]
   (new-generic-exception not-found msg)))


(defn wrap-not-found
  "Returns a function that throws NotFoundException if f
   return nil"
  [f]
  (fn throw-if-not-found
    [& args]
    (if-let [found (apply f args)]
      found
      (throw (new-not-found-exception)))))

(defn type+msgs
  "Answers with a tuple of [keyword string-error-msgs]"
  [ex]
  (let [data (ex-data ex)
        kind (ex-type data)
        error-msgs (or (ex-msgs data) [])]
    [kind error-msgs]))
