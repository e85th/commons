(ns e85th.commons.xml
  (:require [clojure.java.io :as io])
  (:import [java.io StringReader]
           [javax.xml.stream XMLInputFactory XMLStreamConstants XMLStreamReader]))

(def ^{:private true} input-factory-props
  {:allocator XMLInputFactory/ALLOCATOR
   :coalescing XMLInputFactory/IS_COALESCING
   :namespace-aware XMLInputFactory/IS_NAMESPACE_AWARE
   :replacing-entity-references XMLInputFactory/IS_REPLACING_ENTITY_REFERENCES
   :supporting-external-entities XMLInputFactory/IS_SUPPORTING_EXTERNAL_ENTITIES
   :validating XMLInputFactory/IS_VALIDATING
   :reporter XMLInputFactory/REPORTER
   :resolver XMLInputFactory/RESOLVER
   :support-dtd XMLInputFactory/SUPPORT_DTD})

(defn make-input-factory
  "Props is a map of keys as specified in input-factory-props."
  [props]
  (let [fac (XMLInputFactory/newInstance)]
    (doseq [[k v] props
            :when (contains? input-factory-props k)]
      (.setProperty fac (input-factory-props k) v))
    fac))

(defn make-stream-reader
  [props source]
  (.createXMLStreamReader (make-input-factory props) source))

(defn string-source
  [s]
  (StringReader. s))

(defn- all-attrs
  [^XMLStreamReader rdr]
  (persistent!
   (reduce (fn [tr i]
             (assoc! tr
                     (keyword (.getAttributeLocalName rdr i))
                     (.getAttributeValue rdr i)))
           (transient {})
           (range (.getAttributeCount rdr)))))

(defn ignore
  [rdr state]
  state)

(defn nil-path-listener
  [ctx element]
  ctx)

(defn character-event
  [^XMLStreamReader rdr {:keys [path path->data opts] :as state}]
  (if-let [data (path->data path)]
    (let [text (.getText rdr)
          data' (assoc data :value text)]
      (assoc state :path->data (assoc path->data path data')))
    state))

(defn start-element-event
  "Reads the current tag that the reader is on. Adds the tag to the path.
   Finds any start element listener for the path and calls it."
  [^XMLStreamReader rdr {:keys [ctx path path->data path->listeners] :as state}]
  ;; read the current node
  ;; add it to the  path
  ;; find the listener thats at the path and call it
  (let [tag (keyword (.getLocalName rdr))
        path (conj path tag)
        {:keys [start] :or {start nil-path-listener}} (path->listeners path) ; start is the user supplied fn
        element {:attrs (all-attrs rdr)}
        path->data (assoc path->data path element)
        ctx (start ctx element)]
    (assoc state :ctx ctx :path path :path->data path->data)))


(defn end-element-event
  "Reads the current end tag that the reader is on. Finds the end element listner
   if any and calls it. Pops the current tag from the path. Sets path->data to nil after."
  [^XMLStreamReader rdr {:keys [ctx path path->data path->listeners] :as state}]
  ;; read the current node
  ;; find the listener thats at the path and call it
  ;; pop the node from the path
  (let [element (path->data path)
        {:keys [end] :or {end nil-path-listener}} (path->listeners path) ; end is the user supplied fn
        ctx (end ctx element)]
    (assoc state
           :ctx ctx
           :path (pop path)
           :path->data (dissoc path->data path))))

(def stream-event->handler
  {XMLStreamConstants/CHARACTERS character-event
   XMLStreamConstants/END_ELEMENT end-element-event
   XMLStreamConstants/START_ELEMENT start-element-event})

(defn pull
  "path->listeners is a map of [:keyword] -> [start-element-fn end-element-fn]"
  [^XMLStreamReader rdr path->listeners opts ctx]
  (loop [next? (.hasNext rdr)
         state {:ctx ctx
                :path []
                :path->data {}
                :path->listeners path->listeners
                :opts opts}]
    (if next?
      (let [event-code (.next rdr)
            event-handler (get stream-event->handler event-code ignore)
            new-state (event-handler rdr state)]
        (recur (.hasNext rdr) new-state))
      (:ctx state))))


(defn parse
  ([path->listeners source ctx]
   (parse path->listeners source ctx {}))
  ([path->listeners source ctx xml-opts]
   (let [xml-opts* (merge {:coalescing true
                           :supporting-external-entities false}
                          xml-opts)
         rdr (make-stream-reader xml-opts* (io/reader source))]
     (pull rdr path->listeners xml-opts ctx))))
