(ns e85th.commons.google.geo
  (:require [schema.core :as s]
            [e85th.commons.net.rpc :as rpc]
            [e85th.commons.geo :as geo]
            [com.stuartsierra.component :as component]
            [clojure.string :as string])
  (:import [e85th.commons.exceptions GeocodingException]))


(def geocode-url "https://maps.googleapis.com/maps/api/geocode/json")
(def places-url "https://maps.googleapis.com/maps/api/place/nearbysearch/json")

(s/defn ^:private parse-geocode :- geo/Geocode
  [{:keys [status] :as geocode-result}]
  (if (= "OK" status)
    (-> geocode-result
        (get-in [:results 0 :geometry :location])
        (assoc :location-type (get-in geocode-result [:results 0 :geometry :location_type]))
        (assoc :place-id (get-in geocode-result [:results 0 :place_id])))
    (throw (GeocodingException. status))))

(s/defn ^:private address->geocode :- geo/Geocode
  [api-key :- s/Str address :- s/Str]
  (->> {:address address :region "us" :key api-key}
      (rpc/alt-sync-api-call! :get geocode-url)
      parse-geocode))

(s/defn ^:private parse-place :- geo/Place
  [{:keys [status] :as geocode-result}]
  (if (= "OK" status)
    (-> geocode-result
        (get-in [:results 0 :geometry :location])
        (assoc :place-id (get-in geocode-result [:results 0 :place_id])))
    (throw (GeocodingException. status))))

(s/defn ^:private search-for-place :- geo/Place
  "Google place search with radius defaulted to 500 meters"
  [api-key :- s/Str {:keys [lat lng] :as args} :- geo/PlaceSearch]
  (let [params (-> (merge {:radius 500} args)
                   (select-keys [:radius :name])
                   (assoc :location (format "%s,%s" lat lng) :key api-key))]
    (->> params
         (rpc/alt-sync-api-call! :get places-url)
         parse-place)))

(defrecord GoogleGeocoder [api-key]
  component/Lifecycle
  (start [this] this)
  (stop [this] this)

  geo/IGeocoder
  (geocode [this address]
    (address->geocode api-key address))

  (place-search [this search-params]
    (search-for-place api-key search-params)))

(s/defn new-geocoder
  [api-key :- s/Str]
  (map->GoogleGeocoder {:api-key api-key}))
