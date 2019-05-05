(ns e85th.commons.ext
  "Fns applicable to both jvm and js"
  (:refer-clojure :exclude [random-uuid uuid])
  (:require [superstring.core :as str]
            [clojure.walk :as walk]
            [clojure.set :as set])
  #?(:clj (:import [java.util UUID])))


;;----------------------------------------------------------------------
;; Parse data types
;;----------------------------------------------------------------------
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

(defn parse-uuid
  ([s]
   #?(:clj (UUID/fromString s)
      :cljs (cljs.core/uuid s)))
  ([s default]
   (try-parse parse-uuid s default)))

;;----------------------------------------------------------------------
;; Parse data types
;;----------------------------------------------------------------------
(def not-blank? "Opposite of str/blank" (complement str/blank?))

(defn unqualify
  "Unqualifies a keyword or symbol. Given :hello/world => :world.
   Given 'hello/world returns 'world."
  [x]
  (cond
    (keyword? x) (keyword (name x))
    (symbol?  x) (symbol  (name x))
    :else (throw (ex-info (str "Not a symbol or keyword: " x) {:x x :type (type x)}))))

(defn symbol->keyword
  [sym]
  (keyword (namespace sym) (name sym)))

(defn keyword->symbol
  [k]
  (.-sym k))

(defn lisp-case-keyword
  "Turns :helloHow to :hello-how
  Namespace is omitted in returned value."
  [x]
  (-> x name str/lisp-case keyword))

(defn camel-case-keyword
  "Namespace is omitted in returned value."
  [x]
  (-> x name str/camel-case keyword))

(defn walk
  "Adapted from clojure.walk/keywordize-keys. Uses `walk/postwalk`
   and calls `key-fn` on each key and `val-fn` on each value
   for each map entry.  The 2 arity version invokes `kv-fn`
   for each map entry. Recursively changes all keys and values
   as specified by `kv-fn` or `key-fn` and `val-fn`."
  ([kv-fn m]
   (walk/postwalk (fn [x]
                    (if (map? x)
                      (into (with-meta {} (meta x)) (map kv-fn x))
                      x))
                  m))
  ([key-fn val-fn m]
   (let [f (fn [[k v]]
             [(key-fn k) (val-fn v)])]
     (walk f m))))

(def ^:const elided "~elided~")

(defn elide-vals
  "Walks the map eliding the values whose key appears in key-set."
  ([key-set m]
   (elide-vals elided key-set m))
  ([elision-value key-set m]
   (walk (fn [[k v]]
           (if (contains? key-set k)
             [k elision-value]
             [k v]))
         m)))

(defn elide-paths
  [elision-value coll & paths]
  (reduce (fn [ans path]
            (cond-> ans
              (and (seq path)
                   (some? (get-in ans path))) (assoc-in path elision-value)))
          coll
          paths))

(defn elide-paths*
  "paths is a collection of vectors that can be used to
   navigate a collection. Each path must be a non empty
   vector otherwise that path is skipped."
  ([coll & paths]
   (apply elide-paths elided coll paths)))

(defn- key-xformer
  "Returns a function which takes in some key `k`
   and applies `f` to `k` if `k` satisfies `string?`
   or `keyword?`"
  [f]
  (fn [k]
    (cond-> k
      (or (string? k) (keyword? k)) f)))

(defn camel-case-keys
  "Camel case all keys in map m."
  [m]
  (walk (key-xformer camel-case-keyword) identity m))

(defn lisp-case-keys
  "Lisp case all keys in map m."
  [m]
  (walk (key-xformer lisp-case-keyword) identity m))


(defn random-uuid
  "Generates a new uuid."
  []
  #?(:clj (UUID/randomUUID)
     :cljs (cljs.core/random-uuid)))

;;----------------------------------------------------------------------
;; Collections
;;----------------------------------------------------------------------
(defn as-vector
  [x]
  (if (vector? x) x [x]))

(defn as-coll
  [x]
  (if (coll? x) x [x]))


(defn map-kv
  "Apply `f` a two arity function to each
   map entry in `m`."
  [f m]
  (reduce-kv (fn [m k v]
               (let [[k v] (f k v)]
                 (assoc m k v)))
             {}
             m))


(defn map-keys
  "Applies a function `f` to each key in map `m`.
   This is shallow, it does not walk `m`."
  [f m]
  (reduce-kv (fn [m k v]
               (assoc m (f k) v))
             {}
             m))

(defn map-vals
  "Applies a function `f` to each value in map `m`.
   This is shallow, it does not walk `m`."
  [f m]
  (reduce-kv (fn [m k v]
               (assoc m k (f v)))
             {}
             m))

(defn filter-kv
  "Apply `pred` a two arity function to each map entry
   and include the map entry if `pred` returns truthy."
  [pred m]
  (reduce-kv (fn [m k v]
               (if (pred k v)
                 (assoc m k v)
                 m))
             {}
             m))

(defn filter-keys
  "Apply `pred` a one arity function to each map key and
   include the map entry if `pred` returns truthy."
  [pred m]
  (reduce-kv (fn [m k v]
               (if (pred k)
                 (assoc m k v)
                 m))
             {}
             m))

(defn filter-vals
  "Apply `pred` a one arity function to each map val and
   include the map entry if `pred` returns truthy."
  [pred m]
  (reduce-kv (fn [m k v]
               (if (pred v)
                 (assoc m k v)
                 m))
             {}
             m))

(defn remove-kv
  [pred m]
  (filter-kv (complement pred) m))

(defn remove-keys
  [pred m]
  (filter-keys (complement pred) m))

(defn remove-vals
  [pred m]
  (filter-vals (complement pred) m))

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
  "Prunes the map according to `pred`. By default removes values which
   are `nil`, empty string or empty collection."
  ([m]
   (prune-map m (fn [[k v]]
                  (or (nil? v)
                      (and (string? v)
                           (str/blank? v))
                      (and (coll? v)
                           (empty? v))))))
  ([m pred]
   (into {} (remove pred m))))



(defn- leaf-paths*
  [m ctx]
  (let [rfn (fn [ans k]
              (let [ans (update ans :path conj k)
                    v (k m)
                    ans (if (map? v)
                          (leaf-paths* v ans)
                          (update ans :all conj (:path ans)))]
                (update ans :path pop)))]
    (reduce rfn ctx (keys m))))

(defn leaf-paths
  "Returns a seq of all unique paths leading to a leaf in the map (tree)."
  [m]
  (:all (leaf-paths* m {:path [] :all []})))
