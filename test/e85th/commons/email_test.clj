(ns e85th.commons.email-test
  "Tests the email namespace."
  (:require [e85th.commons.email :as email]
            [expectations.clojure.test :refer [defexpect expect]]))

(defexpect domain
  (expect "example.com" (email/domain "foo@example.com"))
  (expect AssertionError (email/domain ""))
  (expect AssertionError (email/domain "foo@example.com@bx.com")))

(defexpect username
  (expect "foo" (email/username "foo@example.com"))
  (expect AssertionError (email/username ""))
  (expect AssertionError (email/username "foo@example.com@bx.com")))
