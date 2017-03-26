(ns e85th.commons.xml-test
  "Tests the xml namespace."
  (:require [e85th.commons.xml :as xml]
            [taoensso.timbre :as log]
            [clojure.test :refer :all]))


(deftest parse-states-test
(let [source "test/data/xml/states.xml"
      path->listeners {[:states :state] {:start (fn [ctx {:keys [attrs]}]
                                                  (assoc ctx :current attrs))}
                       [:states :state :capital] {:end (fn [ctx {:keys [value]}]
                                                         (assoc-in ctx [:current :capital] value))}
                       [:states :state :cities :city] {:start (fn [ctx {:keys [attrs]}]
                                                                (assoc-in ctx [:current :city] (:name attrs)))
                                                       :end (fn [ctx _]
                                                              (assoc-in ctx [:current :city] nil))}
                       [:states :state :cities :city :attractions :attraction] {:end (fn [ctx {:keys [attrs]}]
                                                                                       (let [cur (:current ctx)
                                                                                             cur (assoc cur :attraction (:name attrs))]
                                                                                         (update-in ctx [:results] (fnil conj []) cur)))}}
      res (:results (xml/parse path->listeners source {}))]
  (is (= res
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
         :attraction "Brooklyn Bridge"}]))))
