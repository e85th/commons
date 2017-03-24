(ns e85th.commons.datomic
  (:require [com.stuartsierra.component :as component]
            [datomic.api :as d]
            [e85th.commons.ex :as ex]
            [taoensso.timbre :as log]))

(defrecord Datomic [uri cn]
  component/Lifecycle

  (start [this]
    (log/infof "Starting Datomic component.")
    (if cn
      this
      (assoc this :cn (do
                        (d/create-database uri)
                        (d/connect uri)))))

  (stop [this]
    (log/infof "Stopping Datomic component.")
    (some-> cn d/release)
    (assoc this :cn nil)))

(defn new-datomic-db
  "Creates a new datomic db."
  [uri]
  (map->Datomic {:uri uri}))

(defn get-db*
  [db-or-cn]
  (cond
    (map? db-or-cn) (-> db-or-cn :cn d/db)
    (instance? datomic.db.Db db-or-cn) db-or-cn
    :else (d/db db-or-cn)))

(defn get-entity-with-attr
  "Answers with the entity for the id and attr specified.
   Makes sure the attr is on the entity otherwise returns nil.
   db is Datomic record, id is an integer attr is a keyword."
  [db id attr]
  (let [ans (d/pull (get-db* db) '[*] id)]
    (when (ans attr)
      ans)))

(def get-entity-with-attr! (ex/wrap-not-found get-entity-with-attr))

(defn get-all-entities-with-attr
  "Answers with all entities that have the specified attribute "
  [db attr]
  ;; FIXME: propbably a way to not use flatten
  (flatten (d/q '[:find (pull ?eid [*] ...)
                  :in $ ?attr
                  :where [?eid ?attr]]
                (get-db* db)
                attr)))


(defn get-entities-with-attr-by-ids
  "Get all entities by id which also have the specified attribute.
   If the ids is an empty seq then returns an empty seq."
  [db attr ids]
  (if (seq ids)
    (flatten (d/q '[:find (pull ?eid [*] ...)
                    :in $ ?attr [?eid ...]
                    :where [?eid ?attr]]
                  (get-db* db) attr ids))
    []))

(defn get-partitions
  "Enumerates all partitions in the db."
  [db]
  (d/q '[:find ?ident :where [:db.part/db :db.install/partition ?p]
         [?p :db/ident ?ident]]
       (get-db* db)))

(defn get-parent
  "Get's the parent for the entity with eid.  ident is a keyword with reverse attribute
   navigation ie :parent/_attribute (NOT :parent/attribute). :parent/attribute must be of
   type :db.type/ref."
  [db ident eid]
  (let [db (get-db* db)
        parent-id (get-in (d/pull db [ident] eid) [ident :db/id])]
    (d/pull db '[*] parent-id)))
