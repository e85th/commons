(ns e85th.commons.sql
  (:require [taoensso.timbre :as log]
            [clojure.string :as str]
            [clojure.java.jdbc :as jdbc]
            [clj-time.core :as t]
            [clj-time.coerce :as coerce]
            [hikari-cp.core :as hikari]
            [com.stuartsierra.component :as component]
            [clojure.spec.alpha :as s]
            [e85th.commons.ext :as ext])
  (:import [org.joda.time DateTime]
           [java.sql PreparedStatement SQLException]
           [e85th.commons.exceptions NoRowsUpdatedException]))

(defrecord HikariCP [ds-opts]
  component/Lifecycle

  (start [this]
    (if (:datasource this)
      this
      (assoc this :datasource (hikari/make-datasource ds-opts))))

  (stop [this]
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



(def idealize-row
  "Removes all entries from input map where the value is nil."
  (partial ext/remove-vals nil?))

(def idealize-rs
  "Returns a result set standardized by idealize-row"
  (partial map idealize-row))

(defn query
  "Returns a seq of maps representing the result set.
  (sql/query* (:db system) [\"select * from schema.table where id = ?\" 12])"
  [db sql-and-params]
  (jdbc/query db sql-and-params {:identifiers as-clj-identifier}))

(defn query1
  "Same as `query`, but removes keys with nil value. Similar to Datomic,
   helps with spec, so you don't have (optional) keys where the values are nilable."
  [db sql-and-params]
  (->> (query db sql-and-params)
       idealize-rs))


(defn clojurize-returned-row
  [data]
  (when-not (map? data)
    (throw (ex-info "Expected a map" {:data data
                                      :type-of-data (type data)})))
  (reduce-kv (fn [ans k v]
               (assoc ans (-> k name as-clj-identifier keyword) v))
             (empty data)
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

;;----------------------------------------------------------------------
(s/fdef insert!
        :args (s/cat :db any? :table keyword :row map? :user-id (s/? some?)))

(defn insert!
  "Returns the inserted row as a map. When user-id is specified,
   created-by and updated-by fields will be assocd into the row."
  ([db table row]
   (let [data (jdbc/insert! db table row {:entities as-sql-identifier})
         data (if (map? data) data (first data))]
     (clojurize-returned-row data)))
  ([db table row user-id]
   (insert! db table (assoc-audits user-id row))))

;;----------------------------------------------------------------------
(s/fdef insert-with-create-audits!
        :args (s/cat :db any? :table keyword :row map? :user-id some?))

(defn insert-with-create-audits!
  "Adds in keys :created-by to the row and calls insert!"
  [db table row user-id]
  (insert! db table (assoc-insert-audits user-id row)))


;;----------------------------------------------------------------------
(s/fdef insert-multi!
        :args (s/cat :db any? :table keyword :rows (s/coll-of map?) :batch-size (s/? nat-int?)))

(defn insert-multi!
  "Batch inserts"
  ([db table rows]
   (insert-multi! db table rows default-batch-size))

  ([db table rows batch-size]
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

;;----------------------------------------------------------------------
(s/fdef insert-multi-with-audits!
        :args (s/cat :db any? :table keyword :rows (s/coll-of map?) :user-id some? :batch-size (s/? nat-int?)))

(defn insert-multi-with-audits!
  "Batch insert with audits."
  ([db table rows user-id]
   (insert-multi-with-audits! db table rows user-id default-batch-size))
  ([db table rows user-id batch-size]
   (insert-multi-with-audits* db table rows (partial assoc-audits user-id) default-batch-size)))

;;----------------------------------------------------------------------
(s/fdef insert-multi-with-create-audits!
        :args (s/cat :db any? :table keyword :rows (s/coll-of map?) :user-id some? :batch-size (s/? nat-int?)))

(defn insert-multi-with-create-audits!
  "Batch insert with create audits."
  ([db table rows user-id]
   (insert-multi-with-create-audits! db table rows user-id default-batch-size))
  ([db table rows user-id batch-size]
   (insert-multi-with-audits* db table rows (partial assoc-insert-audits user-id) default-batch-size)))

;;----------------------------------------------------------------------
(s/fdef delete!
        :args (s/cat :db any? :table keyword :where-clause vector?))

(defn delete!
  "Delete. (sql/delete! (:db system) :schema.table-name [\"id = ?\" 42])"
  [db table where-clause]
  (jdbc/delete! db table where-clause {:entities as-sql-identifier}))

;;----------------------------------------------------------------------
(s/fdef update*
        :args (s/cat :db any? :table keyword :row map? :where-clause vector? :optimistic? (s/? boolean?))
        :ret int?)

(defn- update*
  ([db table row where-clause]
   (update* db table row where-clause true))
  ([db table row where-clause optimistic?]
   (let [n (first (jdbc/update! db table row where-clause {:entities as-sql-identifier}))]
     (when (and optimistic? (zero? n))
       (throw (NoRowsUpdatedException.)))
     n)))

;;----------------------------------------------------------------------
(s/fdef update
        :args (s/cat :db any? :table keyword :row map? :where-clause vector? :user-id (s/? some?) :optimistic? (s/? boolean?))
        :ret int?)

(defn update!
  "Returns count of rows updated.
  (sql/update! (:db system) :schema.table-name {:first-name \"Mary\"} [\"id = ?\" 42])"
  ([db table row where-clause]
   (update* db table row where-clause))
  ([db table row where-clause user-id]
   (update! db table row where-clause user-id true))
  ([db table row where-clause user-id optimistic?]
   (update* db table (assoc-update-audits user-id row) where-clause optimistic?)))

(defn unique-violation?
  "Postgres unique violation for the exception? https://www.postgresql.org/docs/current/static/errcodes-appendix.html"
  [^SQLException ex]
  (= "23505" (.getSQLState ex)))


(defn execute-update
  "Returns number of rows affected"
  [cn sql]
  (with-open [stmt (.createStatement cn)]
    (.executeUpdate stmt sql)))

(defn execute-with-cn
  [cn sql]
  (with-open [stmt (.createStatement cn)]
    (.execute stmt sql)))



(defn execute-wo-txn!
  "Executes statement outside of any implicit transactions.
   Useful in executing vacuum for example."
  [db sql]
  (with-open [cn (jdbc/get-connection db)]
    (execute-with-cn cn sql)))

;;----------------------------------------------------------------------
(s/fdef truncate-table!
        :args (s/cat :db any? :table keyword))

(defn truncate-table!
  [db table]
  (jdbc/execute! db [(str "truncate table " (as-sql-identifier  (name table)))]))
