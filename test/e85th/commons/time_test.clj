(ns e85th.commons.time-test
  (:require [e85th.commons.time :as time]
            [clj-time.core :as t]
            [clojure.test :refer :all]))

(deftest interval->duration-components-test
  ;; returned seq is (days hrs mins secs millis)
  (is (= [0 0 0 0 0] (time/interval->duration-components (t/interval (t/date-time 2016) (t/date-time 2016)))))
  (is (= [0 2 3 4 5] (time/interval->duration-components (t/interval (t/date-time 2016 1 1) (t/date-time 2016 1 1 2 3 4 5)))))
  (is (= [0 0 2 0 0] (time/interval->duration-components (t/interval (t/date-time 2015 12 31 23 59) (t/date-time 2016 1 1 0 1))))))

(deftest interval->humanized-string-test
  (is (= "0 ms" (time/interval->humanized-string (t/interval (t/date-time 2016) (t/date-time 2016)))))
  (is (= "2 hrs, 3 min, 4 s, 5 ms" (time/interval->humanized-string (t/interval (t/date-time 2016 1 1) (t/date-time 2016 1 1 2 3 4 5)))))
  (is (= "2 min, 0 s, 0 ms" (time/interval->humanized-string (t/interval (t/date-time 2015 12 31 23 59) (t/date-time 2016 1 1 0 1))))))


(deftest deconstruct-teste
  (is (= [2012 12 20 7 12 30 777] (time/deconstruct (t/date-time 2012 12 20 7 12 30 777)))))
