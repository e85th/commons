(ns e85th.commons.data.csv-test
  (:require [e85th.commons.data.csv :as csv]
            [clojure.test :refer :all]))

(deftest row->csv-test
  (is (= (str "a,b,1,2" \newline) (csv/row->csv ["a" "b" "1" "2"])))
  (is (= (str "a,b,1,2," \newline) (csv/row->csv ["a" "b" "1" "2" ""])))
  (is (= (str \newline) (csv/row->csv []))))
