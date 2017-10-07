(ns e85th.commons.util-test
  (:require [expectations.clojure.test :refer [defexpect]]
            [expectations :refer :all]
            [schema.core :as s]
            [e85th.commons.util :as u]))

(defexpect normalize-env-test
  (expect :production (u/normalize-env "prd"))
  (expect :production (u/normalize-env "prod"))
  (expect :production (u/normalize-env "production"))

  (expect :staging (u/normalize-env "stg"))
  (expect :staging (u/normalize-env "stage"))
  (expect :staging (u/normalize-env "staging"))

  (expect :test (u/normalize-env "tst"))
  (expect :test (u/normalize-env "test"))
  (expect :test (u/normalize-env "testing"))

  (expect :development (u/normalize-env "dev"))
  (expect :development (u/normalize-env "development")))

(defexpect known-env-test
  (expect false (u/known-env? nil))
  (expect false (u/known-env? :fake-env-name))
  (expect true (u/known-env? :test))
  (expect true (u/known-env? :development))
  (expect true (u/known-env? :production))
  (expect true (u/known-env? :staging)))

(defexpect schema-keys-test
  (expect [:a :b :c] (u/schema-keys {:a s/Str :b s/Str :c s/Str}))
  (expect [:a :b :c] (u/schema-keys {(s/optional-key :a) s/Str :b s/Str :c s/Str})))
