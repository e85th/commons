(ns e85th.commons.net.url-test
  (:require [e85th.commons.net.url :as url]
            [clojure.string :as string]
            [expectations :refer :all]
            [expectations.clojure.test :refer [defexpect]]))



(defexpect map->query-test
  ;; simple
  (expect "a=1" (url/map->query {:a 1}))
  (expect "a=1&b=2" (url/map->query {:a 1 :b 2}))

  ;; array
  (expect "a%5B0%5D=1&a%5B1%5D=2" (url/map->query {:a [1 2]}))

  ;; map
  (expect "user%5Bid%5D=1&user%5Bname%5D=mary" (url/map->query {:user {:id 1
                                                                       :name "mary"}})))
