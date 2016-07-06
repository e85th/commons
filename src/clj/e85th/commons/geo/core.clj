(ns e85th.commons.geo.core
  (:require [schema.core :as s]
            [e85th.commons.net.rpc :as rpc]
            [com.stuartsierra.component :as component]))

(s/defschema Geocode
  {:lat s/Num
   :lng s/Num})

(defprotocol IGeocoder
  (geocode [this address]))

(def goog-geocode-url "https://maps.googleapis.com/maps/api/geocode/json")

(s/defn parse-goog-geocode :- Geocode
  [geocode-result]
  (-> geocode-result :results first :geometry :location))

(s/defn goog-address->geocode :- Geocode
  [api-key :- s/Str address :- s/Str]
  (-> {:address address :region "us" :key api-key}
      (rpc/sync-api-call! :get goog-geocode-url)
      parse-goog-geocode))

(s/defrecord GoogleGeocoder [api-key]
  component/Lifecycle
  (start [this] this)
  (stop [this] this)

  IGeocoder
  (geocode [this address]
    (goog-address->geocode api-key address)))
