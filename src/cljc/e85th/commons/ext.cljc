(ns e85th.commons.ext
  "Fns applicable to both jvm and js"
  (:refer-clojure :exclude [random-uuid])
  (:require [superstring.core :as str]
            [clojure.walk :as walk]
            [clojure.set :as set])
  #?(:clj (:import [java.util UUID])))


(defn try-parse
  [f s default]
  (try
    (f s)
    (catch #?(:clj Exception :cljs js/Error) ex
        default)))

(defn parse-bool
  ([x]
   (let [x (if (string? x)
             (str/trim (str/lower-case x))
             x)]
     (parse-bool x #{"true" "yes" "on" "1" 1 true :true :yes :on :1})))
  ([x true-set]
   (some? (true-set x))))

(defn parse-int
  ([s]
   #?(:clj (Integer/parseInt (str/trim s))
      :cljs (js/parseInt s)))
  ([s default]
   (try-parse parse-int s default)))

(defn parse-long
  ([s]
   #?(:clj (Long/parseLong (str/trim s))
      :cljs (parse-int s)))
  ([s default]
   (try-parse parse-long s default)))

(defn parse-float
  ([s]
   #?(:clj (Float/parseFloat (str/trim s))
      :cljs (js/parseFloat s)))
  ([s default]
   (try-parse parse-float s default)))

(defn parse-double
  ([s]
   #?(:clj (Double/parseDouble (str/trim s))
      :cljs (parse-float s)))
  ([s default]
   (try-parse parse-double s default)))

(def not-blank? "Opposite of str/blank" (complement str/blank?))

(def elided "~elided~")

(defn elide-values
  "Walks the map eliding the values whose key appears in key-set."
  [key-set m]
  (walk/postwalk
   (fn [x]
     (let [[k v] (if (vector? x) x [])]
       (if (and k (contains? key-set k))
         [k elided]
         x)))
   m))

(defn elide-paths
  "paths is a collection of vectors that can be used to
   navigate a collection. Each path must be a non empty
   vector otherwise that path is skipped."
  [coll & paths]
  (reduce (fn [ans path]
            (cond-> ans
              (and (seq path)
                   (some? (get-in ans path))) (assoc-in path elided)))
          coll
          paths))


(defn- tr-keys
  "Adapted from clojure.walk/keywordize-keys.
   Calls tr-key-fn for each map key encountered.
   Recursively changes all keys to the transformation
   provided by tr-key-fn."
  [tr-key-fn m]
  (let [f (fn [[k v]]
            (if (or (string? k) (keyword? k))
              [(tr-key-fn k) v]
              [k v]))]
    ;; only apply to maps
    (walk/postwalk (fn [x]
                     (if (map? x)
                       (into {} (map f x)) x))
                   m)))

(defn lisp-case-keyword
  "Turns :helloHow to :hello-how
  Namespace is omitted in returned value."
  [x]
  (-> x name str/lisp-case keyword))

(defn camel-case-keyword
  "Namespace is omitted in returned value."
  [x]
  (-> x name str/camel-case keyword))

(defn camel-case-keys
  "Camel case all keys in map m."
  [m]
  (tr-keys camel-case-keyword m))

(defn lisp-case-keys
  "Lisp case all keys in map m."
  [m]
  (tr-keys lisp-case-keyword m))


(defn random-uuid
  "Generates a new uuid."
  []
  #?(:clj (UUID/randomUUID)
     :cljs (cljs.core/random-uuid)))

(defn as-vector
  [x]
  (if (vector? x) x [x]))

(defn as-coll
  [x]
  (if (coll? x) x [x]))

(defn dissoc-in
  "Dissociates an entry from a nested associative structure returning a new
  nested structure. keys is a sequence of keys. Any empty maps that result
  will not be present in the new structure."
  [m [k & ks :as keys]]
  (if ks
    (if-let [nextmap (get m k)]
      (let [newmap (dissoc-in nextmap ks)]
        (if (seq newmap)
          (assoc m k newmap)
          (dissoc m k)))
      m)
    (dissoc m k)))

(defn assoc-some
  "Associate a key `k` to map `m` if `v` is not nil."
  ([m k v]
   (if (nil? v)
     m
     (assoc m k v)))
  ([m k v & kvs]
   (assert (even? (count kvs)) "Expected even number key value pairs.")
   (reduce (fn [m [k v]]
             (assoc-some m k v))
           (assoc-some m k v)
           (partition 2 kvs))))

(defn assoc-in+
  "Similar to assoc-in except can specify multiple kv pairs"
  [m & path-vals]
  (assert (even? (count path-vals)) "Expected even number of paths and values")
  (reduce (fn [m [path v]]
            (assoc-in m path v))
          m
          (partition 2 path-vals)))

(defn group-by+
  "Similar to group by, but allows applying val-fn to each item in the grouped by list of each key.
   Can also apply val-agg-fn to the result of mapping val-fn. All input fns are 1 arity.
   If val-fn and val-agg-fn were the identity fn then this behaves the same as group-by."
  ([key-fn val-fn xs]
   (group-by+ key-fn val-fn identity xs))
  ([key-fn val-fn val-agg-fn xs]
   (reduce (fn [m [k v]]
             (assoc m k (val-agg-fn (map val-fn v))))
           {}
           (group-by key-fn xs))))

(defn intersect-with
  "Returns a map whose keys exist in boty map-1 and map-2. f is a 2 arity function
   that is invoked wht the value from map-1 and map-2 respectively for each matching key."
  [f map-1 map-2]
  (reduce (fn [m [k v]]
            (cond-> m
              (contains? map-2 k) (assoc k (f v (map-2 k)))))
          {}
          map-1))

(defn conform-map
  "select-keys on map with keys from kmap and renamed keys to be from kmap."
  [map kmap]
  (-> (select-keys map (keys kmap))
      (set/rename-keys kmap)))

(defn key=
  "Returns a predicate that takes a map as an argument.
   The predicate applies k to the map and does an equality check against v."
  [k v]
  (comp (partial = v) k))


(defn deep-merge
  "Deep merge a data structure. Taken from http://stackoverflow.com/questions/17327733/merge-two-complex-data-structures"
  [a b]
  (merge-with (fn [x y]
                (cond (map? y) (deep-merge x y)
                      (vector? y) (concat x y)
                      :else y))
              a b))

(defn prune-map
  "Prunes the map according to `pred?`"
  ([m]
   (prune-map m (fn [[k v]]
                  (or (nil? v)
                      (and (string? v)
                           (str/blank? v))))))
  ([m pred?]
   (into {} (remove pred? m))))
