(ns e85th.commons.time
  (:require [clj-time.core :as t]
            [clj-time.periodic :as p]
            [clj-time.format :as fmt]
            [schema.core :as s]
            [clojure.string :as string])
  (:import [org.joda.time DateTime DateTimeZone]
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


(defn ^String date->str
  "DateTime formatted to year-month-day ie 2016-08-03"
  [dt]
  (fmt/unparse (fmt/formatter :year-month-day) dt))

(defn ^String ts->str
  [dt]
  (fmt/unparse (fmt/formatter :date-time) dt))

(defn millis->duration-components
  [millis]
  (let [divs (reverse (reductions * 1000 [60 60 24]))
        ;; re
        reducer (fn [ans n]
                  (let [[remainder comps] ((juxt first rest) ans)]
                    (when (zero? remainder)
                      (reduced ans))
                    ;; add to comps quotient and the new remainder
                    (conj comps (int (/ remainder n)) (rem remainder n))))]

    (reverse (reduce reducer (list millis) divs))))

(defn interval->duration-components
  "Given an interval returns a 5 tuple of integers representing
  [days hours minutes secs millis]"
  ([t1 t2]
   (interval->duration-components (t/interval t1 t2)))
  ([i]
   (millis->duration-components (t/in-millis i))))

(defn interval->humanized-string
  "Given an interval returns a 5 tuple of integers representing
  [days hours minutes secs millis]"
  ([t1 t2]
   (interval->humanized-string (t/interval t1 t2)))
  ([i]
   (let [units ["days" "hrs" "min" "s" "ms"]
         t-comps (drop-while zero? (interval->duration-components i))
         units (drop (- (count units) (count t-comps)) units)
         reducer (fn [ans [qty unit]]
                   (conj ans (str qty " " unit)))]
     (if (seq t-comps)
       (string/join ", " (reduce reducer [] (map vector t-comps units)))
       "0 ms"))))

(s/defn with-zone-retain-fields
  "Gets the equivalent UTC DateTime if the timezone were changed to timezone.
   Use to turn 2017-12-11T18:00:00Z and you want to
   UTC equivalent "
  [date :- DateTime timezone :- DateTimeZone]
  (.withZoneRetainFields date timezone))

(def ^{:doc "Answers with a seq of the constitutents of the date time. [year month day  hour minute second ms]"}
  deconstruct (juxt t/year t/month t/day t/hour t/minute t/second t/milli))
