(ns e85th.commons.util
  (:require [schema.core :as s]
            [schema.coerce :as schema-coerce]
            [clojure.java.io :as io]
            [clj-time.coerce :as time-coerce]
            [taoensso.timbre :as log]
            [taoensso.timbre.appenders.core :as appenders]
            [taoensso.timbre.appenders.3rd-party.rotor :as rotor]
            [clojure.string :as string])
  (:import [java.sql SQLException]
           [org.joda.time DateTimeZone DateTime]
           [java.util UUID]
           [java.util TimeZone]))

(defn log-throwable
  ([^Throwable ex]
   (log-throwable ex ""))
  ([^Throwable ex ^String uuid]
   (doseq [t (take-while identity (iterate (fn [^Throwable t]
                                             (if (instance? SQLException t)
                                               (.getNextException ^SQLException (cast SQLException t))
                                               (.getCause t)))
                                           ex))]
     (log/error t uuid))))


(s/defn production?
  [env-name]
  (-> env-name string/lower-case (= "production")))

(def development? (complement production?))

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
       (string/join \newline)))

(s/defn log-file-with-suffix
  "log-file is a string that ends in .log.  Adds the suffix before the
   .log if there is a suffix."
  [log-file :- s/Str suffix :- (s/maybe s/Str)]
  (cond-> log-file
    (seq suffix) (string/replace #".log$" (str "-" suffix ".log"))))

(defn parse-int
  [s]
  (Integer/parseInt (string/trim s)))

(defn parse-long
  [s]
  (Long/parseLong (string/trim s)))

(defn parse-float
  [s]
  (Float/parseFloat (string/trim s)))

(defn parse-double
  [s]
  (Double/parseDouble (string/trim s)))

(defn coerce-int
  [s]
  (try
    (parse-int s)
    (catch NumberFormatException ex
      (int (parse-double s)))))


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

(def schema-string-coercions
  (merge schema-coerce/+string-coercions+ {DateTime time-coerce/from-string s/Keyword keyword}))

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
   f is a no arg function. Returns the thread that is started."
  [thread-name period f]
  (start-user-thread thread-name (fn []
                                   (while true
                                     (f)
                                     (Thread/sleep period)))))

(defn uuid
  "Generates a new uuid."
  []
  (str (UUID/randomUUID)))



(defn hostname
  "Answes with the host name for the current machine."
  []
  (-> (java.net.InetAddress/getLocalHost) .getHostName))
