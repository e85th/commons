(ns e85th.commons.ex-test
  "Tests the ex namespace."
  (:require [e85th.commons.tel :as tel]
            [e85th.commons.ex :as ex]
            [clojure.test :refer :all]))

(deftest type+msgs-test
  (is (= [:foo/type ":foo/type"]
         (ex/type+msg (ex/validation :foo/type))))
  (is (= [:foo/type "hello"]
         (ex/type+msg (ex/validation :foo/type "hello"))))
  (is (= [:foo/type "hello foo"]
         (ex/type+msg (ex/validation :foo/type "hello foo"))))
  (is (= [ex/not-found "Resource not found."]
         (ex/type+msg (ex/not-found))))
  (is (= [ex/not-found "Did not find it."]
         (ex/type+msg (ex/not-found "Did not find it.")))))
