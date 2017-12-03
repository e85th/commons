(ns e85th.commons.components
  (:require [com.stuartsierra.component :as component]))

;; Used for dependencies of all other components
(defrecord App []
  component/Lifecycle
  (start [this] this)
  (stop [this] this))

(defmethod clojure.core/print-method App
  [system ^java.io.Writer writer]
  (.write writer "#<App>"))

(defn new-app
  "Creates an App defrecord which can be used to assoc all dependencies on to.
   deps is what is passed to component/using. deps is either a map
   of keyword -> keyword or a seq of keywords"
  ([]
   (map->App {}))
  ([deps]
   (component/using (map->App {}) deps)))

(defn component-keys
  "Given [:a 1 :b 2] returns [:a :b]"
  [args]
  (mapv first (partition 2 args)))
