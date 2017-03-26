(ns e85th.commons.xml
  "XML parsing done via a listener based approach.  Internally, a streaming xml reader is used
   to allow processing of larger documents.  Takes a more xpath like approach.  Register listeners
   for each path of interest.  This approach helps flatten XML docs during processing and
   can facilitate persisting data elsewhere as traversal happens."
  (:require [clojure.java.io :as io]
            [clojure.string :as str])
  (:import [java.io StringReader]
           [java.net URLEncoder]
           [javax.xml.stream XMLInputFactory XMLStreamConstants XMLStreamReader]))

(defn- encode-uri
  "URLEncode the input uri."
  [uri]
  (URLEncoder/encode uri "UTF-8"))

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

(defn- make-input-factory
  "Props is a map of keys as specified in input-factory-props."
  [props]
  (let [fac (XMLInputFactory/newInstance)]
    (doseq [[k v] props
            :when (contains? input-factory-props k)]
      (.setProperty fac (input-factory-props k) v))
    fac))

(defn- make-stream-reader
  ([source]
   (make-stream-reader {} source))
  ([props source]
   (.createXMLStreamReader (make-input-factory props) source)))

(defn- qname
  "Taken from clojure.data.xml. Returns the name as a ns qualified keyword if ns present."
  ([local] (qname "" local))
  ([uri local] (keyword (when-not (str/blank? uri)
                          (encode-uri (str "xmlns." uri)))
                        local))
  ([uri local prefix] (qname uri local)))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Attributes
(defn- attr-name
  "Reads the attribute at index idx and returns it as a qname."
  [^XMLStreamReader rdr idx]
  (qname (.getAttributeNamespace rdr idx)
         (.getAttributeLocalName rdr idx)
         (.getAttributePrefix rdr idx)))

(defn- attr-value
  "Answers with the attribute value at idx."
  [^XMLStreamReader rdr idx]
  (.getAttributeValue rdr idx))

(defn- attr-count
  "Returns the attribute count."
  [^XMLStreamReader rdr]
  (.getAttributeCount rdr))

(defn- attrs
  "Returns all attrs as a map with keyword keys.
   This is more or less from clojure.data.xml."
  [^XMLStreamReader rdr]
  (persistent!
   (reduce (fn [tr i]
             (assoc! tr (attr-name rdr i) (attr-value rdr i)))
           (transient {})
           (range (attr-count rdr)))))

(defn- tag
  "Reads and returns the tag at the current rdr location."
  [^XMLStreamReader rdr]
  (qname (.getNamespaceURI rdr)
         (.getLocalName rdr)
         (.getPrefix rdr)))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; XML Stream and Path Listeners
(defn ignore
  "Returns the ctx unmodified."
  [ctx el]
  ctx)

(defn- listener*
  "kind is either :start or :end. Answers with the registered listener
  or a nil listener which just returns the ctx unmodified."
  [kind path->listeners path]
  (or (-> path path->listeners kind) ignore))

(def start-listener (partial listener* first))
(def end-listener (partial listener* second))

(defn- on-characters
  "Characters event handler."
  [^XMLStreamReader rdr {:keys [path path->data opts] :as state}]
  (if-let [data (path->data path)]
    (let [text (.getText rdr)
          data' (assoc data :value text)]
      (assoc state :path->data (assoc path->data path data')))
    state))

(defn- on-start-element
  "Reads the current tag that the reader is on. Adds the tag to the path.
   Finds any start element listener for the path and calls it."
  [^XMLStreamReader rdr {:keys [ctx path path->data path->listeners] :as state}]
  (let [path (conj path (tag rdr))
        listener (start-listener path->listeners path)
        element {:attrs (attrs rdr)}]
    (assoc state :ctx (listener ctx element) :path path :path->data (assoc path->data path element))))

(defn- on-end-element
  "Reads the current end tag that the reader is on. Finds the end element listner
   if any and calls it. Pops the current tag from the path. Sets path->data to nil after."
  [^XMLStreamReader rdr {:keys [ctx path path->data path->listeners] :as state}]
  (let [element (path->data path) ; current value for element including attrs
        listener (end-listener path->listeners path)]
    (assoc state :ctx (listener ctx element) :path (pop path) :path->data (dissoc path->data path))))

(def start-element? (partial = XMLStreamConstants/START_ELEMENT))

(def ^{:private true} stream-event->handler
  {XMLStreamConstants/CHARACTERS on-characters
   XMLStreamConstants/END_ELEMENT on-end-element
   XMLStreamConstants/START_ELEMENT on-start-element})

(defn- pull
  "path->listeners is a map of vectors with keywords designating a path to
  a map of {:start (fn[ctx element]...) :end (fn[ctx element]...)} listeners."
  [^XMLStreamReader rdr path->listeners opts ctx]
  (loop [next? (.hasNext rdr)
         state {:ctx ctx
                :path []
                :path->data {}
                :path->listeners path->listeners
                :opts opts}]
    (if next?
      (let [event-code (.next rdr)
            event-handler (get stream-event->handler event-code (fn [rdr state] state))
            new-state (event-handler rdr state)]
        (recur (.hasNext rdr) new-state))
      (:ctx state))))

(defn parse
  "Parses an xml document designated by source. ctx is whatever you'd like
   to have the listener function called with, quite often it will just be a map.
   path->listeners is a map of vectors to tuple. vector elements must be keywords
   designating an xpath like path. The value is a 2 elements tuple for the start and end
   element functions called during parsing. The function takes two args (fn [ctx el] ... ctx).
   NB. the new context must be returned. el is a map with keys attrs and value which
   represent an element's attributes and value. attrs are available at both start and
   end element, but value is only available on the end element listener."
  ([path->listeners source]
   (parse path->listeners source {}))
  ([path->listeners source ctx]
   (parse {} path->listeners source ctx))
  ([xml-opts path->listeners source ctx]
   (let [xml-opts* (merge {:coalescing true
                           :supporting-external-entities false}
                          xml-opts)]
     (with-open [src (io/reader source)]
       (pull (make-stream-reader xml-opts* src)
             path->listeners xml-opts ctx)))))


(defmacro defparser
  "Defines a parser that can be called. (defparser foo {path [on-start on-end]})
   Use: (foo source {})."
  ([parser-name path->listeners]
   `(defparser ~parser-name {} ~path->listeners))
  ([parser-name xml-opts path->listeners]
   `(def ~parser-name (partial parse ~xml-opts ~path->listeners))))


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Utils
(defn string-source
  "Creates a source from an xml string suitable to pass to parse."
  [s]
  (StringReader. s))

(defn root
  "Gets the root tag as a keyword."
  [source]
  (with-open [src (io/reader source)]
    (loop [rdr (make-stream-reader src)]
      (if (and (.hasNext rdr)
               (= XMLStreamConstants/START_ELEMENT (.next rdr)))
        (tag rdr)
        (recur rdr)))))
