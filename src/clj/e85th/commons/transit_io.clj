(ns e85th.commons.transit-io
  "Tagged literal extensions for various data types."
  (:require [cognitect.transit :as transit]
            [clj-time.coerce :as t-coerce])
  (:import [org.joda.time DateTime ReadableInstant]))

(def joda-time-writer
  (transit/write-handler
   (constantly "m")
   (fn [v] (-> ^ReadableInstant v .getMillis))
   (fn [v] (-> ^ReadableInstant v .getMillis .toString))))


(def writers
  {ReadableInstant joda-time-writer})

(def readers
  {"m" t-coerce/from-long})
