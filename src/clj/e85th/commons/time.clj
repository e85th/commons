(ns e85th.commons.time
  (:require [clj-time.core :as t]
            [clj-time.periodic :as p]
            [clj-time.format :as fmt]
            [schema.core :as s])
  (:import [org.joda.time DateTime]
           [clojure.lang IFn]))


(s/defn add :- DateTime
  "Adds "
  [unit-fn :- IFn n :- s/Int date :- DateTime]
  (t/plus date (unit-fn n)))

(def add-days (partial add t/days))

(def add-one-day (partial add-days 1))

(defn one-day-ago
  []
  (add-days -1 (t/now)))


(defn date->str
  "DateTime formatted to year-month-day ie 2016-08-03"
  [dt]
  (fmt/unparse (fmt/formatter :year-month-day) dt))


(defn ts->str
  [dt]
  (fmt/unparse (fmt/formatter :date-time) dt))
