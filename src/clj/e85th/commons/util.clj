(ns e85th.commons.util
  (:require [schema.core :as s]
            [schema.coerce :as schema-coerce]
            [clojure.java.io :as io]
            [clj-time.coerce :as time-coerce]
            [taoensso.timbre :as log]
            [taoensso.timbre.appenders.core :as appenders]
            [taoensso.timbre.appenders.3rd-party.rotor :as rotor]
            [superstring.core :as str]
            [clojure.walk :as walk]
            [clojure.set :as set])
  (:import [java.sql SQLException]
           [org.apache.commons.codec.binary Base64 Hex]
           [org.joda.time DateTimeZone DateTime]
           [java.util UUID TimeZone]))

(def environment-info
  {:production #{"prod" "prd" "production"}
   :staging #{"stage" "stg" "staging"}
   :test #{"test" "tst" "testing"}
   :development #{"dev" "development"}})

(defn log-throwable
  ([^Throwable ex]
   (log-throwable ex ""))
  ([^Throwable ex uuid]
   (doseq [t (take-while identity (iterate (fn [^Throwable t]
                                             (if (instance? SQLException t)
                                               (.getNextException ^SQLException (cast SQLException t))
                                               (.getCause t)))
                                           ex))]
     (log/error t uuid))))


(s/defn production?
  [env-name]
  (-> env-name str/lower-case keyword (= :production)))

(def development? (complement production?))

(s/defn normalize-env :- (s/maybe s/Keyword)
  [env :- s/Str]
  (reduce (fn [_ [known-env env-aliases]]
            (when (env-aliases env)
              (reduced known-env)))
          nil
          environment-info))

(s/defn known-env?
  [env :- (s/maybe s/Keyword)]
  (some? (environment-info env)))

(defn known-envs
  ([]
   (known-envs false))
  ([as-str?]
   (cond->> (keys environment-info)
     as-str? (map name))))


(defn build-properties
  "Returns the build properties created by lein."
  [group-id artifact-id]
  (-> (format "META-INF/maven/%s/%s/pom.properties" group-id artifact-id)
      io/resource
      slurp))

(defn build-properties-with-header
  [group-id artifact-id]
  (->> ["\n"
        "------------------------------------------------------------------------"
        "- Build Info                                                           -"
        "------------------------------------------------------------------------"
        (build-properties group-id artifact-id)]
       (str/join \newline)))

(defn build-version
  "Answers with the current version from pom.properties"
  [group-id artifact-id]
  (let [line (-> (build-properties group-id artifact-id)
                 (str/split #"\n")
                 (nth 2))]
    (assert (str/starts-with? line "version=")
            (format "Expected version line to start with version= but is %s" line))
    (second (str/split line #"="))))

(s/defn log-file-with-suffix
  "log-file is a string that ends in .log.  Adds the suffix before the
   .log if there is a suffix."
  [log-file :- s/Str suffix :- (s/maybe s/Str)]
  (cond-> log-file
    (seq suffix) (str/replace #".log$" (str "-" suffix ".log"))))

(defn parse-bool
  ([x]
   (let [x (if (string? x)
             (str/trim (str/lower-case x))
             x)]
     (parse-bool x #{"true" "yes" "on" "1" 1 true})))
  ([x true-set]
   (some? (true-set x))))

(defn parse-int
  [s]
  (Integer/parseInt (str/trim s)))

(defn parse-long
  [s]
  (Long/parseLong (str/trim s)))

(defn parse-float
  [s]
  (Float/parseFloat (str/trim s)))

(defn parse-double
  [s]
  (Double/parseDouble (str/trim s)))

(defn coerce-int
  ([s]
   (try
     (parse-int s)
     (catch NumberFormatException ex
       (int (parse-double s)))))
  ([default-value s]
   (try
     (coerce-int s)
     (catch NumberFormatException ex
       default-value))))

(defn coerce-long
  [s]
  (try
    (parse-long s)
    (catch NumberFormatException ex
      (long (parse-double s)))))

(defn coerce-double
  [s]
  (if (string? s)
    (parse-double s)
    s))


(defn add-shutdown-hook
  "Adds a shutdown hook. f is a no arg function."
  [^Runnable f]
  (-> (Runtime/getRuntime) (.addShutdownHook (Thread. f))))

(defn set-utc-tz
  "Run the app in UTC timezone. Do this early in app startup
   to properly deal with timezones and jdbc."
  []
  (log/info "Setting UTC as runtime timezone.")
  (TimeZone/setDefault (TimeZone/getTimeZone "UTC"))
  (DateTimeZone/setDefault DateTimeZone/UTC))

(defn exit
  "Prints a message and exits"
  [status msg]
  (println msg)
  (System/exit status))


(defn init-logging
  [log-file]
  (printf "Log file will be at: %s" log-file)
  (log/set-config! {:level :info
                    :min-level :info
                    :output-fn (partial log/default-output-fn {:stacktrace-fonts {}})
                    :timestamp-opts {:pattern "yyyy-MM-dd HH:mm:ss.SSS"}})
  (log/merge-config!
   {:appenders {:rotor (rotor/rotor-appender {:path log-file :max-size (* 1024 1024 250)}) ; log rotate files 250 MB
                :println (appenders/println-appender)}}))


(defn- coerce-to-date-time
  "This function is needed because when using transit or edn, the data is already
   parsed to a DateTime so just return the DateTime."
  [x]
  (let [f (cond
            (instance? DateTime x) identity
            (instance? java.util.Date x) time-coerce/from-date
            (integer? x) time-coerce/from-long
            (string? x) time-coerce/from-string)]
    (when-not f
      (throw (Exception. (str "No suitable DateTime coercion for: " (class x)))))
    (f x)))

(def schema-string-coercions
  (merge schema-coerce/+string-coercions+ {DateTime coerce-to-date-time s/Keyword keyword}))

(defn schema-string-coercion-matcher
  "Pulled from schema.coerce"
  [schema]
  (or (schema-string-coercions schema) (schema-coerce/keyword-enum-matcher schema)))


(defn start-thread [daemon? thread-name f]
  "returns the thread"
  (let [t (Thread. nil f thread-name)]
    (doto t
      (.setDaemon daemon?)
      (.start))
    t))

(def start-daemon-thread (partial start-thread true))
(def start-user-thread (partial start-thread false))

(defn periodically
  "Starts a new user thread and every period ms calls f.
   f is a no arg function. Returns the thread that is started.
   Call Thread/interupt to have the thread terminated when it is
   sleeping. f may wish to check Thread/interrupted and throw
   an InterruptedException when heavy processing is anticipated."
  [thread-name period f]
  (start-user-thread thread-name (fn []
                                   (try
                                     (while true
                                       (f)
                                       (Thread/sleep period))
                                     (catch InterruptedException e
                                       (log/infof "%s thread interrupted." thread-name))))))

(defn uuid
  "Generates a new uuid."
  []
  (UUID/randomUUID))

(defn secure-random-hex
  "generates a secure random hex string of size 2n"
  [n]
  (let [bb (byte-array n)]
    (-> (java.security.SecureRandom.) (.nextBytes bb))
    (-> bb Hex/encodeHex String.)))

(defn hostname
  "Answers with the host name for the current machine."
  []
  (-> (java.net.InetAddress/getLocalHost) .getHostName))

(defn url->host
  "Takes a url string and answers with the host."
  [url]
  (-> url java.net.URL. .getHost))

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

(defn make-all-keys-optional
  "m is a schema map. Makes all keys optional in the schema map."
  [m]
  (reduce (fn [ans [k v]]
            (assoc ans (if (s/optional-key? k) k (s/optional-key k)) v))
          {}
          m))

(defn schema-keys
  [m]
  (map #(if (s/optional-key? %) (:k %) %) (keys m)))

(defn schema->update-schema
  "Takes a map schema and makes all keys optional and dissocs the id."
  [m]
  (-> m (dissoc :id) make-all-keys-optional))

(defn as-vector
  [x]
  (if (vector? x) x [x]))

(defn as-coll
  [x]
  (if (coll? x) x [x]))


(defn sleep
  [ms]
  (Thread/sleep ms))

(defn rand-sleep
  [min-ms additional-max-ms]
  (Thread/sleep (+ min-ms (rand-int additional-max-ms))))


(defn class-exists?
  [class-name]
  (try
    (Class/forName class-name)
    true
    (catch Exception ex
      false)))


(defn current-process-id
  "Returns a string or throws an exception Not safe to use according to javadoc"
  []
  (-> (java.lang.management.ManagementFactory/getRuntimeMXBean) .getName (str/split  #"@") first))

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

(defn install-aviso-schema-prefer-methods!
  "Installs Aviso Exception dispatch prefer-methods. Without this, actual exceptions are lost."
  []
  (doseq [x [clojure.lang.IRecord clojure.lang.IPersistentMap java.util.Map]]
    (prefer-method io.aviso.exception/exception-dispatch schema.core.Schema x))
  (doseq [x [clojure.lang.IRecord clojure.lang.IPersistentMap java.util.Map]]
    (prefer-method io.aviso.exception/exception-dispatch schema.core.AnythingSchema x)))

(defn deep-merge
  "Deep merge a data structure. Taken from http://stackoverflow.com/questions/17327733/merge-two-complex-data-structures"
  [a b]
  (merge-with (fn [x y]
                (cond (map? y) (deep-merge x y)
                      (vector? y) (concat x y)
                      :else y))
              a b))

(defn bytes->base64-str
  "Takes an array of bytes and returns the base64 encoded string"
  [bb]
  (String. (Base64/encodeBase64 bb)))


(def ^{:doc "Opposite of str/blank?"} not-blank? (complement str/blank?))

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
