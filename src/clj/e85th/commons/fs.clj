(ns e85th.commons.fs
  (:require [clojure.spec.alpha :as s]
            [me.raynes.fs :as fs]
            [me.raynes.fs.compression :as compression])
  (:import [java.io File]
           [java.nio.file FileSystem FileSystems Files Path CopyOption LinkOption StandardCopyOption]))

(defn ^FileSystem default-fs
  []
  (FileSystems/getDefault))

(defprotocol IPath
  (path [this]))

(extend-protocol IPath
  String
  (path [this]
    (path (-> (default-fs) (.getPath this (make-array String 0)))))

  File
  (path [this]
    (path (.getPath this)))

  Path
  (path [this] this))


(defn directory?
  "path is an instance of java.nio.file.Path"
  [path & link-opts]
  (Files/isDirectory path (into-array LinkOption link-opts)))


(def overwrite   "Overwrite file option"   ::overwrite)
(def atomic-move "Atomic Move file option" ::atomic-move)
(def copy-attrs  "Copy file attrs"         ::copy-attrs)

(def ^:private
  opt->copy-option
  {overwrite StandardCopyOption/REPLACE_EXISTING
   atomic-move StandardCopyOption/ATOMIC_MOVE
   copy-attrs StandardCopyOption/COPY_ATTRIBUTES})

(defn move
  "copy-opts are fs/overwrite"
  [src dest & copy-opts]
  (Files/move (path src) (path dest) (into-array CopyOption (map opt->copy-option copy-opts))))


(def empty-file?
  "One arg takes a file name or File objects."
  (comp zero? fs/size))

(def non-empty-file?
  "complement of empty-file?"
  (complement empty-file?))


(s/fdef bunzip2-and-untar
        :args (s/cat :src string? :dest string? :delete-src? (s/? boolean?)))

(defn bunzip2-and-untar
  ([src dest]
   (bunzip2-and-untar src dest true))
  ([src dest delete-src?]
   (let [tmp-file (fs/temp-name src)]
     (compression/bunzip2 src tmp-file)
     (compression/untar tmp-file dest)
     (fs/delete tmp-file)
     (when delete-src?
       (fs/delete src)))))
