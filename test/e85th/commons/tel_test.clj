(ns e85th.commons.tel-test
  "Tests the tel namespace."
  (:require [e85th.commons.tel :as tel])
  (:import [e85th.commons.exceptions PhoneNumberException])
  (:use clojure.test))

(def valid-number-1 "212-212-2121")
(def valid-number-2 "917-917-9179")

(deftest phone-number-instance?
  (is (false? (tel/phone-number-instance? nil)))
  (is (false? (tel/phone-number-instance? "")))
  (is (false? (tel/phone-number-instance? 1)))
  (is (true? (tel/phone-number-instance? (tel/parse valid-number-1)))))

(deftest parse-numbers
  (testing "nil"
    (is (thrown? PhoneNumberException (tel/parse nil))))

  (testing "strings"
    (is (nil? (tel/parse "")))
    (is (nil? (tel/parse " ")))
    (is (nil? (tel/parse "1")))
    (is (tel/phone-number-instance? (tel/parse valid-number-1)))))

(deftest valid-numbers
  (testing "nil"
    (is (false? (tel/valid? nil))))

  (testing "strings"
    (is (false? (tel/valid? "")))
    (is (false? (tel/valid? " ")))
    (is (false? (tel/valid? "1")))
    (is (true? (tel/valid? "212-212-2121")))))

(deftest normalize-numbers
  (testing "nil"
    (is (thrown? PhoneNumberException (tel/normalize nil))))

  (testing "strings"
    (is (thrown? PhoneNumberException (tel/normalize "")))
    (is (thrown? PhoneNumberException (tel/normalize " ")))
    (is (thrown? PhoneNumberException (tel/normalize "1")))
    (is (= "+12122122121" (tel/normalize "212-212-2121")))))

(deftest format-numbers
  (testing "nil"
    (is (thrown? PhoneNumberException (tel/format nil))))

  (testing "strings"
    (is (thrown? PhoneNumberException (tel/format "")))
    (is (thrown? PhoneNumberException (tel/format " ")))
    (is (thrown? PhoneNumberException (tel/format "1")))
    (is (= "(212) 212-2121" (tel/format "212-212-2121")))))

(deftest match-numbers
  (testing "nil"
    (is (false? (tel/match? nil nil))))

  (testing "strings"
    (is (false? (tel/match? "" "")))
    (is (false? (tel/match? "1" "1")))
    (is (false? (tel/match? valid-number-1 valid-number-2)))
    (is (true? (tel/match? valid-number-1 valid-number-1)))))
