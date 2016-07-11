(ns e85th.commons.sql
  (:require [taoensso.timbre :as log]
            [clojure.string :as string]
            [clojure.java.jdbc :as jdbc]
            [clj-time.core :as t]
            [clj-time.coerce :as coerce]
            [hikari-cp.core :as hikari]
            [com.stuartsierra.component :as component]
            [schema.core :as s])
  (:import [org.joda.time DateTime]
           [java.sql PreparedStatement SQLException]))

(defrecord HikariCP [ds-opts]
  component/Lifecycle

  (start [this]
    (log/infof "Starting Hikari jdbc connection pool component.")
    (if (:datasource this)
      this
      (assoc this :datasource (hikari/make-datasource ds-opts))))

  (stop [this]
    (log/infof "Stopping Hikari jdbc connection pool component.")
    (some-> this :datasource hikari/close-datasource)
    (assoc this :datasource nil)))

(defn new-connection-pool
  "Makes a new connection pool with a :datasource key.
   See hikari for the keys in the ds-opts map."
  [ds-opts]
  (map->HikariCP {:ds-opts ds-opts}))

(def batch-size 25)

(defn as-clj-identifier
  [^String s]
  (.replace s \_ \-))

(defn as-sql-identifier
  [^String s]
  (.replace s \- \_))

;; to avoid manually changing sql types to DateTime
(extend-protocol jdbc/IResultSetReadColumn
  java.sql.Date
  (result-set-read-column [v _ _] (coerce/from-sql-date v))

  java.sql.Timestamp
  (result-set-read-column [v _ _] (coerce/from-sql-time v)))

;; to avoid manually changing from DateTime to timestamp
(extend-type org.joda.time.DateTime
  jdbc/ISQLParameter
  (set-parameter [v ^PreparedStatement stmt idx]
    (.setTimestamp stmt idx (coerce/to-sql-time v))))

(s/defn query
  [db sql-and-params]
  (jdbc/query db sql-and-params {:identifiers as-clj-identifier}))


(defn clojurize-returned-row
  [data]
  (reduce-kv (fn [ans k v]
               (assoc ans (-> k name as-clj-identifier keyword) v))
             {}
             data))

(s/defn insert!
  "Returns the inserted row as a map"
  [db table :- s/Keyword row]
  (let [data (jdbc/insert! db table row {:entities as-sql-identifier})]
    (if (map? data)
      (clojurize-returned-row data)
      (map clojurize-returned-row data))))


(s/defn insert-multi!
  "Batch inserts"
  ([db table :- s/Keyword rows]
   (insert-multi! db table rows batch-size))

  ([db table :- s/Keyword rows batch-size :- s/Int]
   (when (seq rows)
     (let [table (-> table name as-sql-identifier)
           col-names (->> rows first keys (map (comp as-sql-identifier name)))
           col-vals  (map vals rows)]

       (doseq [batches (partition-all batch-size col-vals)]
         (jdbc/insert-multi! db table col-names batches))))))

(s/defn delete!
  [db table :- s/Keyword where-clause]
  (jdbc/delete! db table where-clause {:entities as-sql-identifier}))

(s/defn update!
  "Returns count of rows updated."
  [db table :- s/Keyword row where-clause]
  (when (seq row)
    (jdbc/update! db table row where-clause {:entities as-sql-identifier})))

(s/defn unique-violation?
  "Answers if the sql exception is caused by a unique constraint violation.
   See: https://www.postgresql.org/docs/current/static/errcodes-appendix.html"
  [ex :- SQLException]
  (= "23505" (.getSQLState ex)))
