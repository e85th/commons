(ns e85th.commons.aws.s3
  (:require [schema.core :as s]
            [amazonica.aws.s3 :as s3]
            [taoensso.timbre :as log]
            [me.raynes.fs :as fs]
            [clojure.string :as str]
            [clojure.string :as string])
  (:import [com.amazonaws.services.s3.model DeleteObjectsRequest$KeyVersion]))


(s/defn new-delete-request-key
  "Generates a new delete objects request key version."
  ([k :- s/Str]
   (DeleteObjectsRequest$KeyVersion. k))
  ([k :- s/Str v :- s/Str]
   (DeleteObjectsRequest$KeyVersion. k v)))

(s/defn ensure-no-leading-slash
  [path]
  (if (str/starts-with? path "/") (str/replace-first path #"/" "") path))

(s/defn rm-dir
  "There really are no directories. Deleting all items with a .../key/..
  removes the directory."
  [bucket :- s/Str path :- s/Str]
  (let [path (ensure-no-leading-slash path)
        {:keys [object-summaries]} (s3/list-objects bucket path)
        xform (comp (map :key)
                 (map new-delete-request-key))]
    (when (seq object-summaries)
      (s3/delete-objects
       {:bucket-name bucket
        ;; keys needs to be not lazy
        :keys (into [] xform object-summaries)}))))


(s/defn exists?
  "Note this won't find directories that you see in the s3 console.  There are no directories
   it is just a ui thing.  There are only objects."
  ([bucket :- s/Str path :- s/Str]
   (let [path (ensure-no-leading-slash path)]
     (s3/does-object-exist bucket path)))
  ([bucket :- s/Str path :- s/Str target :- s/Str]
   (let [path-to-target (if (str/ends-with? path "/")
                          (str path target)
                          (str path "/" target))]
     (exists? bucket path-to-target))))
