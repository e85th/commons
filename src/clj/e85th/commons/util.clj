(ns e85th.commons.util
  (:require [clojure.java.io :as io]
            [clj-time.coerce :as time-coerce]
            [taoensso.timbre :as log]
            [taoensso.timbre.appenders.core :as appenders]
            [taoensso.timbre.appenders.3rd-party.rotor :as rotor]
            [superstring.core :as str]
            [e85th.commons.ext :as ext]
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


(defn production?
  [env-name]
  (-> env-name str/lower-case keyword (= :production)))

(def development? (complement production?))

(defn normalize-env
  "env is a string and returns a nilable keyword."
  [env]
  (reduce (fn [_ [known-env env-aliases]]
            (when (env-aliases env)
              (reduced known-env)))
          nil
          environment-info))

(defn known-env?
  "env is a nilable keyword"
  [env]
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
  (some-> (format "META-INF/maven/%s/%s/pom.properties" group-id artifact-id)
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
  "Answers with the current version from pom.properties or nil"
  ([group-id artifact-id]
   (some-> (build-properties group-id artifact-id)
           (str/split #"\n")
           (nth 2)
           (str/split #"=")
           second))
  ([group-id artifact-id not-found]
   (or (build-version group-id artifact-id) not-found)))

(defn log-file-with-suffix
  "log-file is a string that ends in .log.  Adds the suffix before the
   .log if there is a suffix."
  [log-file suffix]
  (cond-> log-file
    (seq suffix) (str/replace #".log$" (str "-" suffix ".log"))))

(defn add-shutdown-hook
  "Adds a shutdown hook. f is a no arg function."
  [^Runnable f]
  (-> (Runtime/getRuntime) (.addShutdownHook (Thread. f))))

(defn set-utc-tz
  "Run the app in UTC timezone. Do this early in app startup
   to properly deal with timezones and jdbc."
  []
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


(defn bytes->base64-str
  "Takes an array of bytes and returns the base64 encoded string"
  [bb]
  (String. (Base64/encodeBase64 bb)))
