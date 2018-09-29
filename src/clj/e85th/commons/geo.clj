(ns e85th.commons.geo
  (:require [clojure.spec.alpha :as s]
            [com.stuartsierra.component :as component]
            [clojure.string :as str]))

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

(defprotocol IGeocoder
  "Geocoding"
  (geocode [this address] "Geocode an address otherwise throws Exception.")
  (place-search [this search-params] "Geocode an address otherwise throws Exception."))

(defrecord NilGeocoder [geocode place]
  component/Lifecycle
  (start [this] this)
  (stop [this] this)

  IGeocoder
  (geocode [this address]
    geocode)

  (place-search [this search-params]
    place))

(defn new-nil-geocoder
  "Creates a new geocoder that returns the same values. Does not make network calls."
  ([]
   (let [geocode {:lat 40.7128 :lng 74.0059 :place-id "12345" :location-type "Approximate"}
         place (select-keys geocode [:lat :lng :place-id])]
     (new-nil-geocoder geocode place)))
  ([geocode place]
   (map->NilGeocoder {:geocode geocode :place place})))

(defn compose-address
  "Takes components of an address and filters out non-blanks and constructs
   a ', ' separated string."
  [street city state zip]
  (->> [street city state zip]
       (remove str/blank?)
       (interpose ", " )
       (apply str)))

(defn new-lat-lng
  "Creates a new LatLng"
  [lat lng]
  (map->LatLng {:lat lat :lng lng}))

(def earth-radius-miles 3959)
(def earth-radius-km 6372.8)

(defn haversine
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
