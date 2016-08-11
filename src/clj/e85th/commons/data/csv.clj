(ns e85th.commons.data.csv
  (:require [clojure.data.csv :as csv]
            [clojure.string :as string])
  (:import [java.io StringWriter]))


(defn row->csv
  "Takes a row ie a seq of values and returns a csv string"
  ([row]
   (row->csv {} row))

  ([opts row]
   ;; Closing a string writer has no effect according to the javadoc
   (row->csv (StringWriter.) opts row))

  ([^StringWriter writer opts row]
   (apply csv/write-csv (reduce into [writer [row]] opts))
   (let [s (str writer)]
     (-> writer .getBuffer (.setLength 0))
     s)))

(comment
  (def sw (StringWriter.))
  (map (comp string/trim (partial row->csv sw {})) [ ["a" "b" "1" "2"] ["omg" "how" "are"]] )
  )
