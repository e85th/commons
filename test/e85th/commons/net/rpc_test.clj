(ns e85th.commons.net.rpc-test
  (:require [e85th.commons.net.rpc :as rpc]
            [clojure.string :as string]
            [expectations :refer :all]
            [expectations.clojure.test :refer [defexpect]]))

(defexpect html-get-test
  (let [body (rpc/sync-call! :get "http://google.com" {})]
    (expect string? body)
    (expect (string/starts-with? body "<!doctype html>"))))

(defexpect json-get-test
  (let [data (rpc/sync-call! :get "http://jsonplaceholder.typicode.com/posts/1" {})]
    (expect [:userId :id :title :body] (keys data))))

(defexpect json-post-test
  (let [data (rpc/sync-call! :post "http://jsonplaceholder.typicode.com/posts" {})]
    (expect {:id 101} data)))

(defexpect json-put-test
  (let [data (rpc/sync-call! :put "http://jsonplaceholder.typicode.com/posts/1" {})]
    (expect {:id 1} data)))

(defexpect json-delete-test
  (let [data (rpc/sync-call! :delete "http://jsonplaceholder.typicode.com/posts/1" {})]
    (expect {} data)))
