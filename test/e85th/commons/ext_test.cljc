(ns e85th.commons.ext-test
  (:require [schema.core :as s]
            [expectations :refer :all]
            [expectations.clojure.test :refer [defexpect]]
            [e85th.commons.ext :as ext]))


(defexpect group-by+-test
  (let [input [{:kind "role" :name "r1"} {:kind "role" :name "r2"}
               {:kind "permission" :name "p1"} {:kind "permission" :name "p2"}]]

    ;; empty
    (expect {} (ext/group-by+ :kind :name set []))

    ;; degenerate case
    (expect (group-by :kind input) (ext/group-by+ :kind identity identity input))

    ;; non degenerate case
    (expect {"role" #{"r1" "r2"} "permission" #{"p1" "p2"}}
            (ext/group-by+ :kind :name set input))))

(defexpect intersect-with-test
  (expect {} (ext/intersect-with + {} {:a 1}))
  (expect {:a 3} (ext/intersect-with + {:a 2} {:a 1}))
  (expect {:a 3} (ext/intersect-with + {:a 2} {:a 1 :b 3}))
  (expect {:a 3 :b 7} (ext/intersect-with + {:a 2 :b 4} {:a 1 :b 3}))
  (expect {nil 3 :b 7} (ext/intersect-with + {nil 2 :b 4} {nil 1 :b 3}))
  (expect {nil 2 :b 12} (ext/intersect-with * {nil 2 :b 4} {nil 1 :b 3}))
  (expect {nil 2/1 :b 4/3} (ext/intersect-with / {nil 2 :b 4} {nil 1 :b 3})))

(defexpect parse-bool-test
  (expect true (ext/parse-bool "true"))
  (expect true (ext/parse-bool "True"))
  (expect true (ext/parse-bool "TrUe"))
  (expect true (ext/parse-bool "yes"))
  (expect true (ext/parse-bool "on"))
  (expect true (ext/parse-bool 1))
  (expect true (ext/parse-bool "1"))

  (expect false (ext/parse-bool ""))
  (expect false (ext/parse-bool nil))
  (expect false (ext/parse-bool "false")))


(defexpect elide-values-test
  (let [m {:a "foo" :b "bar"}]
    (expect m (ext/elide-values #{} m)))

  (expect {:a "foo" :b ext/elided}
          (ext/elide-values #{:b} {:a "foo" :b "bar"}))

  (expect {:a "foo" :b ext/elided :c ext/elided :d 23}
          (ext/elide-values #{:b :c} {:a "foo" :b "bar" :c "baz" :d 23})))

(defexpect elide-paths-test
  (let [m {:a {:b "foo" :c "bar"}}]
    (expect m (ext/elide-paths m [])))

  (expect {:a {:b ext/elided :c "bar"}}
          (ext/elide-paths {:a {:b ext/elided :c "bar"}} [:a :b]))

  (expect {:a {:b ext/elided :c "bar"}
           :d ext/elided}
          (ext/elide-paths {:a {:b ext/elided :c "bar"}
                            :d 23}
                           [:a :b]
                           [:d])))
