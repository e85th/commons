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
           [java.sql PreparedStatement SQLException]
           [e85th.commons.exceptions NoRowsUpdatedException]))

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

(def default-batch-size 25)

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
  "Returns a seq of maps representing the result set.
  (sql/query! (:db system) [\"select * from typeform.evideen_signup where id = ?\" 12]"
  [db sql-and-params]
  (jdbc/query db sql-and-params {:identifiers as-clj-identifier}))


(defn clojurize-returned-row
  [data]
  (reduce-kv (fn [ans k v]
               (assoc ans (-> k name as-clj-identifier keyword) v))
             {}
             data))

(defn- assoc-insert-audits
  [user-id row]
  (assoc row :created-by user-id))

(defn- assoc-update-audits
  [user-id row]
  (assoc row :updated-by user-id :updated-at (t/now)))

(defn- assoc-audits
  [user-id row]
  (->> row
      (assoc-insert-audits user-id)
      (assoc-update-audits user-id)))

(s/defn insert!
  "Returns the inserted row as a map. When user-id is specified,
   created-by and updated-by fields will be assocd into the row."
  ([db table :- s/Keyword row]
   (let [data (jdbc/insert! db table row {:entities as-sql-identifier})
         data (if (map? data) data (first data))]
     (clojurize-returned-row data)))
  ([db table :- s/Keyword row user-id :- s/Int]
   (insert! db table (assoc-audits user-id row))))

(s/defn insert-with-create-audits!
  "Adds in keys :created-by to the row and calls insert!"
  [db table :- s/Keyword row user-id :- s/Int]
  (insert! db table (assoc-insert-audits user-id row)))

(s/defn insert-multi!
  "Batch inserts"
  ([db table :- s/Keyword rows]
   (insert-multi! db table rows default-batch-size))

  ([db table :- s/Keyword rows batch-size :- s/Int]
   (when (seq rows)
     (let [table (-> table name as-sql-identifier)
           col-names (->> rows first keys (map (comp as-sql-identifier name)))
           col-vals  (map vals rows)]

       (doseq [batches (partition-all batch-size col-vals)]
         (jdbc/insert-multi! db table col-names batches))))))

(defn- insert-multi-with-audits*
  [db table rows assoc-audit-fields-fn batch-size]
  (let [rows (map assoc-audit-fields-fn rows)]
    (insert-multi! db table rows batch-size)))

(s/defn insert-multi-with-audits!
  "Batch insert with audits."
  ([db table :- s/Keyword rows user-id :- s/Int]
   (insert-multi-with-audits! db table rows user-id default-batch-size))
  ([db table :- s/Keyword rows user-id :- s/Int batch-size :- s/Int]
   (insert-multi-with-audits* db table rows (partial assoc-audits user-id) default-batch-size)))

(s/defn insert-multi-with-create-audits!
  "Batch insert with create audits."
  ([db table :- s/Keyword rows user-id :- s/Int]
   (insert-multi-with-create-audits! db table rows user-id default-batch-size))
  ([db table :- s/Keyword rows user-id :- s/Int batch-size :- s/Int]
   (insert-multi-with-audits* db table rows (partial assoc-insert-audits user-id) default-batch-size)))

(s/defn delete!
  "Delete. (sql/delete! (:db system) :schema.table-name [\"id = ?\" 42])"
  [db table :- s/Keyword where-clause]
  (jdbc/delete! db table where-clause {:entities as-sql-identifier}))

(s/defn ^:private update* :- s/Int
  ([db table :- s/Keyword row :- {s/Keyword s/Any} where-clause]
   (update* db table row where-clause false))
  ([db table :- s/Keyword row :- {s/Keyword s/Any} where-clause optimistic? :- s/Bool]
   (let [n (first (jdbc/update! db table row where-clause {:entities as-sql-identifier}))]
     (if (and optimistic? (zero? n))
       (throw (NoRowsUpdatedException.))
       n))))

(s/defn update! :- s/Int
  "Returns count of rows updated.
  (sql/update! (:db system) :schema.table-name {:first-name \"Mary\"} [\"id = ?\" 42])
  "
  ([db table :- s/Keyword row :- {s/Keyword s/Any} where-clause]
   (update* db table row where-clause))
  ([db table :- s/Keyword row where-clause user-id :- s/Int]
   (update* db table (assoc-update-audits user-id row) where-clause)))

(s/defn unique-violation? :- s/Bool
  "Postgres unique violation for the exception? https://www.postgresql.org/docs/current/static/errcodes-appendix.html"
  [ex :- SQLException]
  (= "23505" (.getSQLState ex)))


(s/defn truncate-table!
  [db tbl :- s/Keyword]
  (jdbc/execute! db [(str "truncate table " (as-sql-identifier  (name tbl)))]))


(s/defn execute-wo-txn!
  "Executes statement outside of any implicit transactions.
   Useful in executing vacuum for example."
  [db sql]
  (with-open [cn (jdbc/get-connection db)]
    (with-open [stmt (.createStatement cn)]
      (.execute stmt sql))))
