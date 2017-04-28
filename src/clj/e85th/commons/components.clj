(ns e85th.commons.components
  (:require [com.stuartsierra.component :as component]
            [schema.core :as s]))

;; Used for dependencies of all other components
(defrecord App []
  component/Lifecycle
  (start [this] this)
  (stop [this] this))

(s/defn new-app
  "Creates an App defrecord which can be used to assoc all dependencies on to.
   deps is what is passed to component/using"
  ([]
   (map->App {}))
  ([deps :- (s/conditional map? {s/Keyword s/Keyword} :else [s/Keyword])]
   (component/using (map->App {}) deps)))

(defn component-keys
  "Given [:a 1 :b 2] returns [:a :b]"
  [args]
  (mapv first (partition 2 args)))
