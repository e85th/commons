(ns e85th.commons.util
  (:require [schema.core :as s]
            [clojure.java.io :as io]
            [taoensso.timbre :as log]
            [clojure.string :as string])
  (:import [java.sql SQLException]))

(defn log-throwable [^Throwable ex]
  (doseq [t (take-while identity (iterate (fn [^Throwable t]
                                            (if (instance? SQLException t)
                                              (.getNextException ^SQLException (cast SQLException t))
                                              (.getCause t)))
                                          ex))]
    (log/error t)))

(s/defn production?
  [env-name]
  (-> env-name string/lower-case (= "production")))

(def development? (complement production?))

(defn build-properties
  "Returns the build properties created by lein."
  [group-id artifact-id]
  (slurp (io/resource (format "META-INF/maven/%s/%s/pom.properties" group-id artifact-id))))

(defn parse-int
  [s]
  (Integer/parseInt (string/trim s)))

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
