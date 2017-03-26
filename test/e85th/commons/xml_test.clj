(ns e85th.commons.xml-test
  "Tests the xml namespace."
  (:require [e85th.commons.xml :as xml :refer [defparser]]
            [taoensso.timbre :as log]
            [clojure.test :refer :all]))

(def states-xml-file "test/data/xml/states.xml")

(defn on-state
  [ctx {:keys [attrs]}]
  (assoc ctx :current attrs))

(defn on-capital
  [ctx {:keys [value]}]
  (assoc-in ctx [:current :capital] value))

(defn on-city-start
  [ctx {:keys [attrs]}]
  (assoc-in ctx [:current :city] (:name attrs)))

(defn on-city-end
  [ctx el]
  (assoc-in ctx [:current :city] nil))

(defn on-attraction
  [ctx {:keys [attrs]}]
  (let [cur (:current ctx)
        cur (assoc cur :attraction (:name attrs))]
    (update-in ctx [:results] (fnil conj []) cur)))


(defparser state-parser
  {[:states] [xml/ignore (fn [c e] (:results c))]
   [:states :state] [on-state xml/ignore]
   [:states :state :capital] [xml/ignore on-capital]
   [:states :state :cities :city] [on-city-start on-city-end]
   [:states :state :cities :city :attractions :attraction] [xml/ignore on-attraction]})

(deftest parse-states-test
  (is (= (state-parser states-xml-file  {})
         [{:name "California",
           :code "CA",
           :capital "Sacremento",
           :city "Los Angeles",
           :attraction "LAX"}
          {:name "California",
           :code "CA",
           :capital "Sacremento",
           :city "Los Angeles",
           :attraction "Rodeo Drive"}
          {:name "California",
           :code "CA",
           :capital "Sacremento",
           :city "Los Angeles",
           :attraction "Miracle Mile"}
          {:name "California",
           :code "CA",
           :capital "Sacremento",
           :city "San Francisco",
           :attraction "Alcatraz"}
          {:name "California",
           :code "CA",
           :capital "Sacremento",
           :city "San Francisco",
           :attraction "Golden Gate Bridge"}
          {:name "California",
           :code "CA",
           :capital "Sacremento",
           :city "San Francisco",
           :attraction "Fishermen's Wharf"}
          {:name "New York",
           :code "NY",
           :capital "Albany",
           :city "New York",
           :attraction "Empire State Building"}
          {:name "New York",
           :code "NY",
           :capital "Albany",
           :city "New York",
           :attraction "Statue of Liberty"}
          {:name "New York",
           :code "NY",
           :capital "Albany",
           :city "New York",
           :attraction "Brooklyn Bridge"}])))

(deftest root-test
  (is (= :states (xml/root states-xml-file))))
