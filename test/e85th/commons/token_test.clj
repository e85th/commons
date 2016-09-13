(ns e85th.commons.token-test
  "Tests the token namespace."
  (:require [e85th.commons.token :as token]
            [clojure.test :refer :all]
            [com.stuartsierra.component :as component])
  (:import [e85th.commons.exceptions AuthExceptionInfo]))

(deftest rand-token-test
  (is (= 5 (count (token/rand-token))))
  (is (= 3 (count (token/rand-token 3))))
  (is (= 1 (count (token/rand-token 1))))
  (is (thrown? AssertionError (token/rand-token 0))))

(deftest sha256-token-factory-test
  (let [token-factory (component/start (token/new-sha256-token-factory "secret" 10))
        data {:user-id 42 :first-name "John" :last-name "Smith"}
        roundtrip-fn (comp (partial token/token->data token-factory)
                        (partial token/data->token token-factory))]
    (is (= data (roundtrip-fn data)))))

(deftest sha256-token-factory-test-fail
  (let [token-factory-1 (component/start (token/new-sha256-token-factory "foo" 10))
        token-factory-2 (component/start (token/new-sha256-token-factory "bar" 10))
        data {:user-id 42 :first-name "John" :last-name "Smith"}
        ;; this one should return nil
        roundtrip-fn (comp (partial token/token->data token-factory-2)
                        (partial token/data->token token-factory-1))

        ;; this one should throw an exception
        roundtrip-ex-fn (comp (partial token/token->data! token-factory-2)
                           (partial token/data->token token-factory-1))]
    (is (nil? (roundtrip-fn data)))
    (is (thrown? AuthExceptionInfo (roundtrip-ex-fn data)))))
