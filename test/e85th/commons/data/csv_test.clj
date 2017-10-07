(ns e85th.commons.data.csv-test
  (:require [e85th.commons.data.csv :as csv]
            [expectations.clojure.test :refer [defexpect]]
            [expectations :refer :all]))

(defexpect row->csv-test
  (expect (str "a,b,1,2" \newline) (csv/row->csv ["a" "b" "1" "2"]))
  (expect (str "a,b,1,2," \newline) (csv/row->csv ["a" "b" "1" "2" ""]))
  (expect (str \newline) (csv/row->csv [])))
