(ns e85th.commons.net.rpc-test
  (:require [e85th.commons.net.rpc :as rpc]
            [clojure.string :as string]
            [clojure.test :refer :all]))

(deftest html-get-test
  (let [body (rpc/sync-call! :get "http://google.com" {})]
    (is (string? body))
    (is (string/starts-with? body "<!doctype html>"))))

(deftest json-get-test
  (let [data (rpc/sync-call! :get "http://jsonplaceholder.typicode.com/posts/1" {})]
    (is (= [:userId :id :title :body] (keys data)))))

(deftest json-post-test
  (let [data (rpc/sync-call! :post "http://jsonplaceholder.typicode.com/posts" {})]
    (is (= {:id 101} data))))

(deftest json-put-test
  (let [data (rpc/sync-call! :put "http://jsonplaceholder.typicode.com/posts/1" {})]
    (is (= {:id 1} data))))

(deftest json-delete-test
  (let [data (rpc/sync-call! :delete "http://jsonplaceholder.typicode.com/posts/1" {})]
    (is (= {} data))))
