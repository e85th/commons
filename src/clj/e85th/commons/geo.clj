(ns e85th.commons.geo
  (:require [schema.core :as s]
            [clojure.string :as string])
  (:import [e85th.commons.exceptions GeocodingException]))

(defprotocol IPoint
  (x [this])
  (y [this]))

(defrecord Point [x y]
  IPoint
  (x [this] x)
  (y [this] y))

(defprotocol ILatLng
  (lat [this])
  (lng [this]))

(defrecord LatLng [lat lng]
  ILatLng
  (lat [this] lat)
  (lng [this] lng))

(s/defschema Geocode
  {:lat s/Num
   :lng s/Num
   :place-id s/Str
   :location-type s/Str})

(s/defschema PlaceSearch
  {:lat s/Num
   :lng s/Num
   :name s/Str
   ;; radius in meters
   (s/optional-key :radius) s/Num})

(s/defschema Place
  {:lat s/Num
   :lng s/Num
   :place-id s/Str})

(defprotocol IGeocoder
  "Geocoding"
  (geocode [this address] "Geocode an address otherwise throws GeocodingException.")
  (place-search [this search-params] "Geocode an address otherwise throws GeocodingException."))

(s/defn compose-address
  "Takes components of an address and filters out non-blanks and constructs
   a ', ' separated string."
  [street city state zip]
  (->> [street city state zip]
       (remove string/blank?)
       (interpose ", " )
       (apply str)))

(s/defn new-lat-lng :- LatLng
  "Creates a new LatLng"
  [lat :- s/Num lng :- s/Num]
  (map->LatLng {:lat lat :lng lng}))

(def earth-radius-miles 3959)
(def earth-radius-km 6372.8)

(s/defn haversine :- s/Num
  "Taken from rosettacode.org"
  [earth-radius {lon1 :lng lat1 :lat} {lon2 :lng lat2 :lat}]
  (let [dlat (Math/toRadians (- lat2 lat1))
        dlon (Math/toRadians (- lon2 lon1))
        lat1 (Math/toRadians lat1)
        lat2 (Math/toRadians lat2)
        a (+ (* (Math/sin (/ dlat 2)) (Math/sin (/ dlat 2))) (* (Math/sin (/ dlon 2)) (Math/sin (/ dlon 2)) (Math/cos lat1) (Math/cos lat2)))]
    (* earth-radius 2 (Math/asin (Math/sqrt a)))))

(def haversine-km (partial haversine earth-radius-km))

(def haversine-miles (partial haversine earth-radius-miles))
