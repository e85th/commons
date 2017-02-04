(ns e85th.commons.net.url-test
  (:require [e85th.commons.net.url :as url]
            [clojure.string :as string]
            [clojure.test :refer :all]))


(deftest map->query-test
  (testing "simple"
    (is (= "a=1" (url/map->query {:a 1})))
    (is (= "a=1&b=2" (url/map->query {:a 1 :b 2}))))

  (testing "array"
    (is (= "a%5B0%5D=1&a%5B1%5D=2" (url/map->query {:a [1 2]}))))

  (testing "map"
    (is (= "user%5Bid%5D=1&user%5Bname%5D=mary" (url/map->query {:user {:id 1
                                                                        :name "mary"}})))))
