(ns e85th.commons.token-test
  "Tests the token namespace."
  (:require [e85th.commons.token :as token]
            [expectations.clojure.test :refer [defexpect expect]]
            [com.stuartsierra.component :as component])
  (:import [clojure.lang ExceptionInfo]))

(defexpect rand-token-test
  (expect 5 (count (token/rand-token)))
  (expect 3 (count (token/rand-token 3)))
  (expect 1 (count (token/rand-token 1)))
  (expect AssertionError (token/rand-token 0)))

(defexpect sha256-token-factory-test
  (let [token-factory (component/start (token/new-sha256-token-factory "secret" 10))
        data {:user-id 42 :first-name "John" :last-name "Smith"}
        roundtrip-fn (comp (partial token/token->data token-factory)
                        (partial token/data->token token-factory))]
    (expect data (roundtrip-fn data))))

(defexpect sha256-token-factory-test-fail
  (let [token-factory-1 (component/start (token/new-sha256-token-factory "foo" 10))
        token-factory-2 (component/start (token/new-sha256-token-factory "bar" 10))
        data {:user-id 42 :first-name "John" :last-name "Smith"}
        ;; this one should return nil
        roundtrip-fn (comp (partial token/token->data token-factory-2)
                        (partial token/data->token token-factory-1))

        ;; this one should throw an exception
        roundtrip-ex-fn (comp (partial token/token->data! token-factory-2)
                           (partial token/data->token token-factory-1))]
    (expect nil? (roundtrip-fn data))
    (expect ExceptionInfo (roundtrip-ex-fn data))))
