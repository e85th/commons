(ns e85th.commons.net.url
  (:require [clojure.string :as str])
  (:import [java.net URLEncoder]))

(defn url-encode
  [string]
  (some-> string str (URLEncoder/encode "UTF-8") (.replace "+" "%20")))

(defn- query-params-kv
  [k v]
  (str (url-encode (name k))
       "="
       (url-encode (or v ""))))

(defn- wrap-brackets
  [x]
  (str "[" (cond-> x
             (keyword? x) name)
       "]"))

(defn- build-params*
  [prefix obj]
  (let [prefix (name prefix)]
    (cond
      (map? obj) (for [[k v] (seq obj)
                       :let [new-prefix (str prefix (wrap-brackets k))]]
                   (build-params* new-prefix v))
      (sequential? obj) (map-indexed (fn [i v]
                                       (build-params* (str prefix (wrap-brackets i)) v))
                                     obj)
      :else (query-params-kv prefix obj))))

(defn map->query
  [m]
  (assert (map? m))
  ;; sort makes testing easier
  (->> (for [[prefix v] (sort (seq m))]
         (build-params* prefix v))
       flatten
       (str/join "&")))
