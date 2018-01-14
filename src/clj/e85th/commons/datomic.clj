(ns e85th.commons.datomic
  (:require [com.stuartsierra.component :as component]
            [datomic.api :as d]
            [e85th.commons.ex :as ex]
            [e85th.commons.ext :as ext]))

(defrecord Datomic [uri cn]
  component/Lifecycle

  (start [this]
    (if cn
      this
      (assoc this :cn (do
                        (d/create-database uri)
                        (d/connect uri)))))

  (stop [this]
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


(defn filter-kv
  "Get entities that satisfy filter conditions
   specified in m. m is a map of keyword -> values
   and become conditions in the `:where` clause.
   Answers with a seq of datomic entities that match.
   If a value is nil then adds that as a `missing?` call."
  [db m]
  (let [f (fn [[k v]]
            (if (some? v)
              ['?e k v]
              [(list 'missing? '$ '?e k)]))
        qm {:query {:find ['?e]
                    :in ['$]
                    :where (mapv f m)}
            :args [db]}]
    (->> (d/query qm)
         (map d/entity))))

(defn get-by-composite-key
  "Returns the only entity that matches filter conditions in `m`.
   Throws when more than one match is found."
  [db m]
  (let [ans (filter-kv db m)
        n (count ans)]
    (when (> n 1)
      (throw (ex/generic :error/composite-key
                         "Expected only 1 item for composite-key" {:m m
                                                                   :count n})))
    (first ans)))


(defn prepare-update
  "m is a map of updated values, if m has nil values lookup
  the current value and generate a retraction."
  ([entity m]
   (let [fact-gen (fn [ans [k v]]
                    (cond-> ans
                      (nil? v) (-> (update :rms conj [:db/retract (:db/id entity) k (k entity)])
                                   (update :x dissoc k))))
         {:keys [x rms]} (reduce fact-gen {:x m :rms []} m)]
     (cond-> rms
       (seq x) (conj x))))
  ([db eid-or-lookup m]
   (prepare-update (d/entity db eid-or-lookup) m)))


(defn prepare-create
  "Removes all keys where there is an associated nil value."
  [m]
  (ext/filter-vals some? m))
