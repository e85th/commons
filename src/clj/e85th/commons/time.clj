(ns e85th.commons.time
  (:require [clj-time.core :as t]
            [clj-time.periodic :as p]
            [clj-time.format :as fmt]
            [schema.core :as s]
            [clojure.string :as string])
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


(defn interval->duration-components
  "Given an interval returns a 5 tuple of integers representing
  [days hours minutes secs millis]"
  ([t1 t2]
   (interval->duration-components (t/interval t1 t2)))
  ([i]
   ;; divs is millis, millis in 1 minute, millis in 1 hour, millis in 1 day,
   (let [divs (reverse (reductions * 1000 [60 60 24]))
         ;; re
         reducer (fn [ans n]
                   (let [[remainder comps] ((juxt first rest) ans)]
                     (when (zero? remainder)
                       (reduced ans))
                     ;; add to comps quotient and the new remainder
                     (conj comps (int (/ remainder n)) (rem remainder n))))]

     (reverse (reduce reducer (list (t/in-millis i)) divs)))))

(defn interval->humanized-string
  "Given an interval returns a 5 tuple of integers representing
  [days hours minutes secs millis]"
  ([t1 t2]
   (interval->humanized-string (t/interval t1 t2)))
  ([i]
   (let [units ["day" "hr" "min" "sec" "millis"]
         ;; butlast gets rid of millis
         t-comps (butlast (drop-while zero? (interval->duration-components i)))
         units (butlast (drop (- (count units) (count t-comps)) units))
         f (fn [qty unit]
             (str qty " " unit (if (= 1 qty) "" "s")))
         reducer (fn [ans [qty unit]]
                   (conj ans (f qty unit)))]
     (string/join ", " (reduce reducer [] (map vector t-comps units))))))
