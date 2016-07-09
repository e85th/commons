(ns e85th.commons.data.fixed-width
  "Fixed width file parser.
   Code adapted from http://www.lexicallyscoped.com/2015/01/05/parsing-flat-files-in-clojure.html.

(def parse-rules
  {:cols [{:name :company
           :slice [0 62]}
          {:name :form
           :slice [62 74]}
          {:name :cik
           :slice [74 86]
           :type :long}
          {:name :date
           :slice [86 98]
           :type :date}
          {:name :file
           :slice [98]}]
   :skip-lines 10
   :skip-line? (some-fn string/blank? (partial re-seq #\"^-+$\"))})

  "
  (:require [clojure.string :as string]
            [clj-time.coerce :as t-coerce]
            [taoensso.timbre :as log])
  (:import [java.io BufferedReader]))

(defmulti parse-data first)

(defmethod parse-data :date
  [[_ s]]
  (t-coerce/from-string s))

(defmethod parse-data :long
  [[_ s]]
  (Long/parseLong s))

(defmethod parse-data :int
  [[_ s]]
  (Integer/parseInt s))

(defmethod parse-data :float
  [[_ s]]
  (Float/parseFloat s))

(defmethod parse-data :double
  [[_ s]]
  (Double/parseDouble s))

(defn lazy-file-lines
  "file is a string
  http://stackoverflow.com/questions/4118123/read-a-very-large-text-file-into-a-list-in-clojure/13312151#13312151"
  [file]
  (letfn [(helper [^BufferedReader rdr]
            (lazy-seq
             (if-let [line (.readLine rdr)]
               (cons line (helper rdr))
               (do (.close rdr) nil))))]
    (helper (clojure.java.io/reader file))))

(defn skip-lines
  [n fseq]
  (cond->> fseq
    (and n (pos? n)) (drop n)))

(defn slice-line
  "line is a string, start and end are ints"
  [line [start end]]
  (let [end (or end (count line))]
    (assert (< start end) "invalid col slice start must be less than end")
    (subs line start end)))

(defn line->column-val
  "Takes a line of text and applies transforms and produces a value."
  [line {:keys [name slice parse type]}]
  (assert (not (and parse type)) (format "Can't specify both type and parse for col spec %s" name))
  (let [s (string/trim (slice-line line slice))]
    (cond
      parse (parse s)
      type (parse-data [type s])
      :else s)))

(defn line->map
  "Convert a line to a map"
  [{:keys [cols] :as rules} line]
  (let [r (fn [ans {:keys [name] :as col-spec}]
            (assoc ans name (line->column-val line col-spec)))]
    (reduce r {} cols)))

(defn parse-file
  "file can be a string (path to file), url, or uri."
  [file {:keys [skip-line?] :or {skip-line? (constantly false)} :as rules}]
  (let [f-seq (->> (lazy-file-lines file)
                   (skip-lines (:skip-lines rules)))
        xf (comp (remove skip-line?)
              (map (partial line->map rules)))]
    (sequence xf f-seq)))
