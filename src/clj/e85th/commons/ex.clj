(ns e85th.commons.ex
  (:require [schema.core :as s]
            [e85th.commons.util :as u]
            [clojure.string :as string]))

(def ex-type ::ex-type)
(def ex-errors ::ex-errors)
(def validation ::validation)
(def not-found ::not-found)


(s/defn new-generic-exception
  "Creates and returns a new validation exception."
  [kind :- s/Keyword msg-or-msgs :- (s/conditional string? s/Str :else [s/Str])]
  (let [msgs (u/as-vector msg-or-msgs)
        msg-str (string/join "; " msgs)]
    (ex-info msg-str {ex-type kind ex-errors msgs})))

(def new-validation-exception (partial new-generic-exception validation))

(defn new-not-found-exception
  ([]
   (new-not-found-exception "Resource not found."))
  ([msg]
   (new-not-found-exception not-found msg)))


(defn wrap-not-found
  "Returns a function that throws NotFoundException if f
   return nil"
  [f]
  (fn throw-if-not-found
    [& args]
    (if-let [found (apply f args)]
      found
      (throw (new-not-found-exception)))))
