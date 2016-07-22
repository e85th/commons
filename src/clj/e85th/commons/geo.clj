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
       (filter (complement string/blank?))
       (interpose ", " )
       (apply str)))
