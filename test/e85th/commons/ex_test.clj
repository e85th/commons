(ns e85th.commons.ex-test
  "Tests the ex namespace."
  (:require [e85th.commons.tel :as tel]
            [e85th.commons.ex :as ex]
            [expectations :refer :all]
            [expectations.clojure.test :refer [defexpect]]))

(defexpect type+msgs-test
  (expect [:foo/type ":foo/type"]              (ex/type+msg (ex/validation :foo/type)))
  (expect [:foo/type "hello"]                  (ex/type+msg (ex/validation :foo/type "hello")))
  (expect [:foo/type "hello foo"]              (ex/type+msg (ex/validation :foo/type "hello foo")))
  (expect [ex/not-found "Resource not found."] (ex/type+msg (ex/not-found)))
  (expect [ex/not-found "Did not find it."]    (ex/type+msg (ex/not-found "Did not find it."))))
