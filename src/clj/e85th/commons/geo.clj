(ns e85th.commons.geo
  (:require [schema.core :as s]
            [e85th.commons.net.rpc :as rpc]
            [com.stuartsierra.component :as component]
            [clojure.string :as string]))

(s/defschema Geocode
  {:lat s/Num
   :lng s/Num
   :place-id s/Str})

(s/defschema PlaceSearch
  {:lat s/Num
   :lng s/Num
   :name s/Str
   ;; radius in meters
   (s/optional-key :radius) s/Num})

(defprotocol IGeocoder
  (geocode [this address])
  (place-search [this search-params]))

(def goog-geocode-url "https://maps.googleapis.com/maps/api/geocode/json")
(def goog-places-url "https://maps.googleapis.com/maps/api/place/nearbysearch/json")

(s/defn ^:private parse-goog-geocode :- Geocode
  [{:keys [status error_message] :as geocode-result}]
  (if (= "OK" status)
    (-> geocode-result
        (get-in [:results 0 :geometry :location])
        (assoc :place-id (get-in geocode-result [:results 0 :place_id])))
    (throw (ex-info "Geocoding failed" geocode-result))))

(s/defn ^:private goog-address->geocode :- Geocode
  [api-key :- s/Str address :- s/Str]
  (->> {:address address :region "us" :key api-key}
      (rpc/alt-sync-api-call! :get goog-geocode-url)
      parse-goog-geocode))

(s/defn ^:private goog-place-search
  "Google place search with radius defaulted to 500 meters"
  [api-key :- s/Str {:keys [lat lng] :as args} :- PlaceSearch]
  (let [params (-> (merge {:radius 500} args)
                   (select-keys [:radius :name])
                   (assoc :location (format "%s,%s" lat lng) :key api-key))]
    (->> params
         (rpc/alt-sync-api-call! :get goog-places-url)
         parse-goog-geocode)))

(defrecord GoogleGeocoder [api-key]
  component/Lifecycle
  (start [this] this)
  (stop [this] this)

  IGeocoder
  (geocode [this address]
    (goog-address->geocode api-key address))

  (place-search [this search-params]
    (goog-place-search api-key search-params)))

(s/defn new-google-geocoder
  [api-key :- s/Str]
  (map->GoogleGeocoder {:api-key api-key}))


(s/defn compose-address
  "Takes components of an address and filters out non-blanks and constructs
   a ', ' separated string."
  [street city state zip]
  (->> [street city state zip]
       (filter (complement string/blank?))
       (interpose ", " )
       (apply str)))
