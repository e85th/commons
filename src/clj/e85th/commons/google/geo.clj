(ns e85th.commons.google.geo
  (:require [clj-http.client :as http]
            [e85th.commons.geo :as geo]
            [e85th.commons.ex :as ex]
            [com.stuartsierra.component :as component]
            [cheshire.core :as json]
            [clojure.string :as str]))


(def geocode-url "https://maps.googleapis.com/maps/api/geocode/json")
(def places-url "https://maps.googleapis.com/maps/api/place/nearbysearch/json")

(defn- call
  [req-params]
  (let [{:keys [status body request] :as resp} (http/request req-params)]
    (when-not (= 200 status)
      (throw (ex-info "geocoding failed" resp)))
    body))

(defn- parse-geocode
  [{:keys [status] :as geocode-result}]
  (if (= "OK" status)
    (-> geocode-result
        (get-in [:results 0 :geometry :location])
        (assoc :location-type (get-in geocode-result [:results 0 :geometry :location_type]))
        (assoc :place-id (get-in geocode-result [:results 0 :place_id])))
    (throw (ex-info "geocoding failed" {:status status}))))

(defn- address->geocode
  "api-key and address are strings. Returns a map with keys :lat :lng :place-id and :location-type"
  [api-key address]
  (let [{:keys [status body] :as resp} (->> {:method :get
                                             :url geocode-url
                                             :query-params {:address address :region "us" :key api-key}
                                             :as :json
                                             :save-request? true}
                                            call)]
    (parse-geocode (json/parse-string body true))))

(defn- parse-place
  "Returns a map of :lat :lng and a :place-id as a String."
  [{:keys [status] :as geocode-result}]
  (if (= "OK" status)
    (-> geocode-result
        (get-in [:results 0 :geometry :location])
        (assoc :place-id (get-in geocode-result [:results 0 :place_id])))
    (throw (ex-info "geocoding failed" {:status status}))))

(defn- search-for-place
  "Google place search with radius defaulted to 500 meters. api-key is a string,
   and the args is a map of :lat :lng, :name and optionally :radius in meters"
  [api-key {:keys [lat lng] :as args}]
  (let [params (-> (merge {:radius 500} args)
                   (select-keys [:radius :name])
                   (assoc :location (format "%s,%s" lat lng) :key api-key))
        {:keys [status body] :as resp} (->> {:method :get
                                             :url places-url
                                             :query-params params
                                             :as :json
                                             :save-request? true}
                                            call)]
    (parse-place (json/parse-string body true))))

(defrecord GoogleGeocoder [api-key]
  component/Lifecycle
  (start [this] this)
  (stop [this] this)

  geo/IGeocoder
  (geocode [this address]
    (address->geocode api-key address))

  (place-search [this search-params]
    (search-for-place api-key search-params)))

(defn new-geocoder
  [api-key]
  (map->GoogleGeocoder {:api-key api-key}))
