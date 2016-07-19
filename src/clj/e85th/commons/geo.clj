(ns e85th.commons.geo
  (:require [schema.core :as s]
            [e85th.commons.net.rpc :as rpc]
            [com.stuartsierra.component :as component]
            [clojure.string :as string]))

(s/defschema Geocode
  {:lat s/Num
   :lng s/Num})

(s/defschema PlaceSearch
  {:lat s/Num
   :lng s/Num
   :radius s/Num ;; in meters
   :name s/Str})

(s/defschema PlaceSearchResult
  {:geocode Geocode
   :place-id s/Str})


(defprotocol IGeocoder
  (geocode [this address])
  (place-search [this search-params]))

(def goog-geocode-url "https://maps.googleapis.com/maps/api/geocode/json")
(def goog-places-url "https://maps.googleapis.com/maps/api/place/nearbysearch/json")

(s/defn ^:private parse-goog-geocode :- Geocode
  [geocode-result]
  (-> geocode-result :results first :geometry :location))

(s/defn ^:private goog-address->geocode :- Geocode
  [api-key :- s/Str address :- s/Str]
  (->> {:address address :region "us" :key api-key}
      (rpc/sync-api-call! :get goog-geocode-url)
      parse-goog-geocode))

(s/defn ^:private parse-place-search
  "Returns the first place that is in the result."
  [place-results]
  {:location (get-in place-results [:results 0 :geometry :location])
   :place-id (get-in place-results [:results 0 :place_id])})

(s/defn ^:private goog-place-search
  [api-key :- s/Str {:keys [lat lng] :as args} :- PlaceSearch]
  (let [params (-> args
                   (select-keys [:radius :name])
                   (assoc :location (format "%s,%s" lat lng) :key api-key))]
    (->> params
         (rpc/sync-api-call! :get goog-places-url)
         parse-place-search)))

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
