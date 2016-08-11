(ns e85th.commons.util-test
  (:require [clojure.test :refer :all]
            [e85th.commons.util :as u]))

(deftest normalize-env-test
  (is (= :production) (u/normalize-env "prd"))
  (is (= :production) (u/normalize-env "prod"))
  (is (= :production) (u/normalize-env "production"))

  (is (= :staging) (u/normalize-env "stg"))
  (is (= :staging) (u/normalize-env "stage"))
  (is (= :staging) (u/normalize-env "staging"))

  (is (= :test) (u/normalize-env "tst"))
  (is (= :test) (u/normalize-env "test"))
  (is (= :test) (u/normalize-env "testing"))

  (is (= :development) (u/normalize-env "dev"))
  (is (= :development) (u/normalize-env "development")))

(deftest known-env-test
  (is (false? (u/known-env? nil)))
  (is (false? (u/known-env? :fake-env-name)))
  (is (true? (u/known-env? :test)))
  (is (true? (u/known-env? :development)))
  (is (true? (u/known-env? :production)))
  (is (true? (u/known-env? :staging))))
