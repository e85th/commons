(ns e85th.commons.geo-test
  (:require [e85th.commons.geo :as geo]
            [clojure.test :refer :all]))

(deftest compose-address-test
  (is (= "180 East 85th Street, New York, NY, 10028"
         (geo/compose-address "180 East 85th Street" "New York" "NY" "10028")))
  (is (= "New York, NY, 10028"
         (geo/compose-address " " "New York" "NY" "10028")))
  (is (= "New York, NY"
         (geo/compose-address " " "New York" "NY" "")))
  (is (= ""
         (geo/compose-address "" "" "" ""))))
