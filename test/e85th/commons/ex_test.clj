(ns e85th.commons.ex-test
  "Tests the ex namespace."
  (:require [e85th.commons.tel :as tel]
            [e85th.commons.ex :as ex]
            [clojure.test :refer :all]))

(deftest type+msgs-test
  (is (= [:foo/type ["hello"]]
         (ex/type+msgs (ex/new-validation-exception :foo/type "hello"))))
  (is (= [:foo/type ["hello" "foo"]]
         (ex/type+msgs (ex/new-validation-exception :foo/type ["hello" "foo"]))))
  (is (= [ex/not-found ["Resource not found."]]
         (ex/type+msgs (ex/new-not-found-exception))))
  (is (= [ex/not-found ["Did not find it."]]
         (ex/type+msgs (ex/new-not-found-exception "Did not find it.")))))
