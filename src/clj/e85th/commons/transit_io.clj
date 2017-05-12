(ns e85th.commons.transit-io
  "Tagged literal extensions for various data types."
  (:require [cognitect.transit :as transit]
            [clj-time.coerce :as t-coerce])
  (:import [org.joda.time DateTime ReadableInstant]
           [java.io ByteArrayInputStream ByteArrayOutputStream]))

(def joda-time-write-handler
  (transit/write-handler
   (constantly "m")
   (fn [v] (-> ^ReadableInstant v .getMillis))
   (fn [v] (-> ^ReadableInstant v .getMillis str))))


(def writers
  {ReadableInstant joda-time-write-handler})

(def readers
  {"m"  (transit/read-handler
         (fn [s]
           (DateTime. (Long/parseLong s))))})

(defn encode
  "encode a clojure data structure as transit+json string."
  [x]
  (let [out (ByteArrayOutputStream. 4096)
        writer (transit/writer out :json)]
    (transit/write writer x)
    (.toString out)))

(defn decode
  "Decodes a transit+json string as a clojure data structure."
  [^String s]
  (let [in (ByteArrayInputStream. (.getBytes s))
        reader (transit/reader in :json)]
    (transit/read reader)))
