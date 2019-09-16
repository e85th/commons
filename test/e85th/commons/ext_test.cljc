(ns e85th.commons.ext-test
  (:require [expectations.clojure.test :refer [defexpect expect]]
            [e85th.commons.ext :as ext]))

(defexpect lisp-case-test
  (expect :foo (ext/lisp-case-keyword :foo))
  (expect :foo-bar (ext/lisp-case-keyword :foo-bar))
  (expect :foo-bar (ext/lisp-case-keyword :fooBar)))

(defexpect camel-case-test
  (expect :foo (ext/camel-case-keyword :foo))
  ;; NB. This is lossy.  '-' and '.' yield same camel cased keyword
  (expect :fooBar (ext/camel-case-keyword :foo-bar))
  (expect :fooBar (ext/camel-case-keyword :foo.bar)))

(defexpect lisp-case-keys-test
  (expect {:foo-bar 1 :bar-foo 2} (ext/lisp-case-keys {:foo-bar 1 :bar-foo 2}))
  (expect {:foo-bar 1 :bar-foo 2
           :other-key {:nested-key 3}}
          (ext/lisp-case-keys {:fooBar 1 :barFoo 2
                                :otherKey {:nestedKey 3}})))

(defexpect camel-case-keys-test
  (expect {:fooBar 1 :barFoo 2} (ext/camel-case-keys {:foo-bar 1 :bar-foo 2}))
  (expect {:fooBar 1 :barFoo 2
           :otherKey {:nestedKey 3}}
          (ext/camel-case-keys {:foo-bar 1 :bar-foo 2
                                :other-key {:nested-key 3}})))

(defexpect map-vals-test
  (expect {} (ext/map-vals identity {}))
  (expect {:a 1} (ext/map-vals identity {:a 1}))
  (expect {:a 2} (ext/map-vals inc {:a 1}))
  (expect {:a 2 :b 3} (ext/map-vals inc {:a 1 :b 2})))

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


(defexpect redact-vals-test
  (let [m {:a "foo" :b "bar"}]
    (expect m (ext/redact-vals #{} m)))

  (expect {:a "foo" :b ext/redacted}
          (ext/redact-vals #{:b} {:a "foo" :b "bar"}))

  (expect {:a "foo" :b ext/redacted :c ext/redacted :d 23}
          (ext/redact-vals #{:b :c} {:a "foo" :b "bar" :c "baz" :d 23})))

(defexpect redact-paths-test
  (let [m {:a {:b "foo" :c "bar"}}]
    (expect m (ext/redact-paths ext/redacted m [])))

  (expect {:a {:b ext/redacted :c "bar"}}
          (ext/redact-paths ext/redacted {:a {:b ext/redacted :c "bar"}} [:a :b]))

  (expect {:a {:b ext/redacted :c "bar"}
           :d ext/redacted}
          (ext/redact-paths ext/redacted
                           {:a {:b ext/redacted :c "bar"}
                            :d 23}
                           [:a :b]
                           [:d])))



(defexpect filter-and-remove-test
  (expect {"a" "1" "b" "2"} (ext/map-kv (fn [k v]
                                          [(name k) (str v)])
                                        {:a 1 :b 2}))


  (expect {:a 1 :b 2} (ext/filter-keys keyword? {:a 1 :b 2 "c" 3}))
  (expect {:a :1}     (ext/filter-vals keyword? {:a :1 :b "two" :c [:hello]}))

  (expect {"c" 3} (ext/remove-keys keyword? {:a 1 :b 2 "c" 3}))
  (expect {:b "two" :c [:hello]} (ext/remove-vals keyword? {:a :1 :b "two" :c [:hello]})))



(defexpect leaf-paths-test
  (expect #{[:a :b :c]
            [:a :b :d]
            [:a :b :e]
            [:f :g :h]
            [:f :g :i :j :k]
            [:f :g :i :j :l]}
          (set (ext/leaf-paths
                {:a {:b {:c 1
                         :d 1
                         :e 1}}
                 :f {:g {:h 1
                         :i {:j {:k 1
                                 :l 1}}}}}))))



(defexpect map-invert+test
  (expect {} (ext/map-invert+ {}))

  (expect
   {1 #{:a}
    2 #{:a}
    3 #{:b}
    4 #{:b :c}
    5 #{:c}}
   (ext/map-invert+ {:a [1 2] :b [3 4] :c [4 5]}))


  (expect
   {1 [:a]
    2 [:a]
    3 [:b]
    4 [:b :c]
    5 [:c]}
   (ext/map-invert+ {:a [1 2] :b [3 4] :c [4 5]}
                    {:val-coll []})))
