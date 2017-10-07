(ns e85th.commons.geo-test
  (:require [e85th.commons.geo :as geo]
            [expectations :refer :all]
            [expectations.clojure.test :refer [defexpect]]))

(defexpect compose-address-test
  (expect "180 East 85th Street, New York, NY, 10028"
          (geo/compose-address "180 East 85th Street" "New York" "NY" "10028"))
  (expect "New York, NY, 10028" (geo/compose-address " " "New York" "NY" "10028"))
  (expect "New York, NY" (geo/compose-address " " "New York" "NY" ""))
  (expect "" (geo/compose-address "" "" "" "")))
