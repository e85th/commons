(ns e85th.commons.fs
  (:require [schema.core :as s])
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


(s/defn directory? :- s/Bool
  [path :- Path & link-opts]
  (Files/isDirectory path (into-array LinkOption link-opts)))


(def ^{:doc "Overwrite file option"}
  overwrite ::overwrite)

(def ^{:doc "Atomic Move file option"}
  atomic-move ::atomic-move)

(def ^{:doc "Copy file attrs"}
  copy-attrs ::copy-attrs)

(def ^:private
  opt->copy-option
  {overwrite StandardCopyOption/REPLACE_EXISTING
   atomic-move StandardCopyOption/ATOMIC_MOVE
   copy-attrs StandardCopyOption/COPY_ATTRIBUTES})

(s/defn move
  "copy-opts are fs/overwrite"
  [src dest & copy-opts]
  (Files/move (path src) (path dest) (into-array CopyOption (map opt->copy-option copy-opts))))
