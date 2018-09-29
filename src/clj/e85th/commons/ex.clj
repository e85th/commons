(ns e85th.commons.ex
  (:require [e85th.commons.util :as u]
            [clojure.pprint]
            [clojure.string :as str])
  (:import [clojure.lang ExceptionInfo]))


(def ex-type :exception/type)
(def ex-msg :exception/message)

(def exception-category :exception/category)

(def auth-exception :exception.category/auth)
(def forbidden-exception :exception.category/forbidden)
(def generic-exception :exception.category/generic)
(def not-found-exception :exception.category/not-found)
(def validation-exception :exception.category/validation)

(defn exception
  [{:keys [category type msg data cause]}]
  (let [msg (or msg (str type))
        data (or data {})
        ex-map (cond-> {ex-type type ex-msg msg}
                 category (assoc exception-category category))]
       (ex-info msg (merge ex-map data) cause)))

(defn- exception*
  ([category type]
   (exception {:category category :type type}))
  ([category type msg]
   (exception {:category category :type type :msg msg}))
  ([category type msg data]
   (exception {:category category :type type :msg msg :data data}))
  ([category type msg data cause]
   (exception {:category category :type type :msg msg :data data :cause cause})))

(def generic (partial exception* generic-exception))

(def validation (partial exception* validation-exception))

(def auth (partial exception* auth-exception))

(def forbidden (partial exception* forbidden-exception))

(defn not-found
  ([]
   (not-found "Resource not found."))
  ([msg]
   (not-found :error/not-found msg {}))
  ([type msg data]
   (exception {:category not-found-exception :type type :msg msg :data data})))

(defn of-category?
  [category ex]
  (-> ex ex-data exception-category (= category)))

(def validation? (partial of-category? validation-exception))

(def auth? (partial of-category? auth-exception))

(def forbidden? (partial of-category? forbidden-exception))

(def not-found? (partial of-category? not-found-exception))

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
        type (ex-type data)]
    [type (ex-msg data)]))

(defn error-tuple
  "Returns a triple of [keyword string map] or constructs an error-tuple."
  ([ex]
   (let [data (ex-data ex)]
     [(ex-type data) (ex-msg data) (dissoc data ex-type ex-msg)]))
  ([type msg data]
   [type (or msg "") (or data {})]))
