(ns e85th.commons.tel-test
  "Tests the tel namespace."
  (:require [e85th.commons.tel :as tel]
            [expectations.clojure.test :refer [defexpect expect]])
  (:import [e85th.commons.exceptions PhoneNumberException]))

(def valid-number-1 "212-212-2121")
(def valid-number-2 "917-917-9179")

(defexpect phone-number-instance?
  (expect false (tel/phone-number-instance? nil))
  (expect false (tel/phone-number-instance? ""))
  (expect false (tel/phone-number-instance? 1))
  (expect true (tel/phone-number-instance? (tel/parse valid-number-1))))

(defexpect parse-numbers
  (expect PhoneNumberException (tel/parse nil))

  ;; strings
  (expect nil? (tel/parse ""))
  (expect nil? (tel/parse " "))
  (expect nil? (tel/parse "1"))
  (expect tel/phone-number-instance? (tel/parse valid-number-1)))

(defexpect valid-numbers
  (expect false (tel/valid? nil))

  ;; strings
  (expect false (tel/valid? ""))
  (expect false (tel/valid? " "))
  (expect false (tel/valid? "1"))
  (expect true (tel/valid? "212-212-2121")))

(defexpect normalize-numbers
  (expect PhoneNumberException (tel/normalize nil))

  (expect PhoneNumberException (tel/normalize ""))
  (expect PhoneNumberException (tel/normalize " "))
  (expect PhoneNumberException (tel/normalize "1"))
  (expect "+12122122121" (tel/normalize "212-212-2121")))

(defexpect format-numbers
  (expect PhoneNumberException (tel/format nil))

  (expect PhoneNumberException (tel/format ""))
  (expect PhoneNumberException (tel/format " "))
  (expect PhoneNumberException (tel/format "1"))
  (expect "(212) 212-2121" (tel/format "212-212-2121")))

(defexpect match-numbers
  (expect false (tel/match? nil nil))

  (expect false (tel/match? "" ""))
  (expect false (tel/match? "1" "1"))
  (expect false (tel/match? valid-number-1 valid-number-2))
  (expect true (tel/match? valid-number-1 valid-number-1)))
