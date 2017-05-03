(ns e85th.commons.util-test
  (:require [clojure.test :refer :all]
            [schema.core :as s]
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

(deftest group-by+-test
  (let [input [{:kind "role" :name "r1"} {:kind "role" :name "r2"}
               {:kind "permission" :name "p1"} {:kind "permission" :name "p2"}]]
    (testing "empty input"
      (is (= {} (u/group-by+ :kind :name set []))))

    (testing "degenerate case"
      (is (= (group-by :kind input) (u/group-by+ :kind identity identity input))))

    (testing "non-degenerate case"
      (is (= {"role" #{"r1" "r2"} "permission" #{"p1" "p2"}}
             (u/group-by+ :kind :name set input))))))

(deftest intersect-with-test
  (is (= {} (u/intersect-with + {} {:a 1})))
  (is (= {:a 3} (u/intersect-with + {:a 2} {:a 1})))
  (is (= {:a 3} (u/intersect-with + {:a 2} {:a 1 :b 3})))
  (is (= {:a 3 :b 7} (u/intersect-with + {:a 2 :b 4} {:a 1 :b 3})))
  (is (= {nil 3 :b 7} (u/intersect-with + {nil 2 :b 4} {nil 1 :b 3})))
  (is (= {nil 2 :b 12} (u/intersect-with * {nil 2 :b 4} {nil 1 :b 3})))
  (is (= {nil 2/1 :b 4/3} (u/intersect-with / {nil 2 :b 4} {nil 1 :b 3}))))

(deftest parse-bool-test
  (is (true? (u/parse-bool "true")))
  (is (true? (u/parse-bool "True")))
  (is (true? (u/parse-bool "TrUe")))
  (is (true? (u/parse-bool "yes")))
  (is (true? (u/parse-bool "on")))
  (is (true? (u/parse-bool 1)))
  (is (true? (u/parse-bool "1")))

  (is (false? (u/parse-bool "")))
  (is (false? (u/parse-bool nil)))
  (is (false? (u/parse-bool "false"))))


(deftest schema-keys-test
  (is (= [:a :b :c] (u/schema-keys {:a s/Str :b s/Str :c s/Str})))
  (is (= [:a :b :c] (u/schema-keys {(s/optional-key :a) s/Str :b s/Str :c s/Str})))
  )
