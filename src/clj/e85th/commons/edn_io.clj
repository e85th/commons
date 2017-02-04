(ns e85th.commons.edn-io
  "Tagged literal extensions for various data types."
  (:require [e85th.commons.time :as time]
            [clj-time.format :as fmt])
  (:import [org.joda.time DateTime]
           [java.io Writer]))

(defmethod print-method DateTime [this ^Writer w]
  (.write w "#datetime \"")
  (.write w (time/ts->str this))
  (.write w "\""))

(def tag-readers
  {'datetime fmt/parse})
