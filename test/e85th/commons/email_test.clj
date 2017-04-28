(ns e85th.commons.email-test
  "Tests the email namespace."
  (:require [e85th.commons.email :as email]
            [clojure.test :refer :all]))

(deftest domain
  (is (= "example.com" (email/domain "foo@example.com")))
  (is (thrown? AssertionError (email/domain "")))
  (is (thrown? AssertionError (email/domain "foo@example.com@bx.com"))))

(deftest username
  (is (= "foo" (email/username "foo@example.com")))
  (is (thrown? AssertionError (email/username "")))
  (is (thrown? AssertionError (email/username "foo@example.com@bx.com"))))
