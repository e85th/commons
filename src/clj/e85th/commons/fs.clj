(ns e85th.commons.fs
  (:require [clojure.spec.alpha :as s]
            [e85th.commons.ext :as ext]
            [me.raynes.fs :as fs]
            [me.raynes.fs.compression :as compression])
  (:import [java.io File RandomAccessFile]
           [java.nio.file.attribute FileAttribute]
           [java.nio ByteBuffer]
           [java.nio.charset StandardCharsets]
           [java.nio.channels FileChannel]
           [java.nio.file FileSystem FileSystems FileVisitOption Files Path Paths
            CopyOption LinkOption StandardCopyOption OpenOption StandardOpenOption]))


(def kw->open-option
  {:append StandardOpenOption/APPEND
   :create StandardOpenOption/CREATE
   :create-new StandardOpenOption/CREATE_NEW
   :delete-on-close StandardOpenOption/DELETE_ON_CLOSE
   :dsync StandardOpenOption/DSYNC
   :read StandardOpenOption/READ
   :sparse StandardOpenOption/SPARSE
   :sync StandardOpenOption/SYNC
   :truncate-existing StandardOpenOption/TRUNCATE_EXISTING
   :write StandardOpenOption/WRITE})

(defn normalize-open-options
  "returns a set of StandardOpenOption from an input of
   opts which contains keywords"
  [opts]
  (set (map kw->open-option opts)))

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


(defn size
  [file-path]
  (Files/size (path file-path)))

(def empty-file?
  "One arg takes a file name, Path or File object."
  (comp zero? size))

(def non-empty-file?
  "complement of empty-file?"
  (complement empty-file?))


(defn create-temp-dir
  "Returns a Path object."
  ([]
   (create-temp-dir "" []))
  ([prefix attrs]
   (Files/createTempDirectory prefix (into-array FileAttribute attrs))))

(defn create-temp-file
  "Returns a Path object. file-name can't have a '/' in it"
  ([]
   (create-temp-file (str (ext/random-uuid))))
  ([file-name]
   (create-temp-file nil file-name))
  ([dir file-name]
   (create-temp-file dir file-name []))
  ([dir file-name attrs]
   (if dir
     (Files/createTempFile (path dir) file-name "" (into-array FileAttribute attrs))
     (Files/createTempFile file-name "" (into-array FileAttribute attrs)))))

(defn delete
  "Delete a file or if directory recursively delete the directory."
  [file-path]
  ;; reverse to make sure the directory is last
  (doseq [f (-> (Files/walk (path file-path) (into-array FileVisitOption [FileVisitOption/FOLLOW_LINKS]))
                .sorted
                .iterator
                iterator-seq
                reverse)]
    (Files/deleteIfExists f)))

(s/fdef bunzip2-and-untar
        :args (s/cat :src string? :dest string? :delete-src? (s/? boolean?)))

(defn bunzip2-and-untar
  ([src dest]
   (bunzip2-and-untar src dest true))
  ([src dest delete-src?]
   (let [tmp-file (fs/temp-name src)]
     (compression/bunzip2 src tmp-file)
     (compression/untar tmp-file dest)
     (delete tmp-file)
     (when delete-src?
       (delete src)))))


(def input-stream? (partial instance? java.io.InputStream))
(def output-stream? (partial instance? java.io.OutputStream))

(defn input-stream
  "open-opts are `OpenOption`"
  [file-path & open-opts]
  (Files/newInputStream (path file-path) (into-array OpenOption open-opts)))


(defn output-stream
  "open-opts are `OpenOption`"
  [file-path & open-opts]
  (Files/newOutputStream (path file-path) (into-array OpenOption open-opts)))


(defn create-dirs
  "Create directories including parent dirs. file-attrs are `FileAttribute`"
  [dir-path & file-attrs]
  (Files/createDirectories (path dir-path) (into-array FileAttribute file-attrs)))


(defn resolve-path
  [parent-path file-name]
  (-> parent-path
      path
      (.resolve file-name)))


(defn byte-channel
  ([file-path]
   (byte-channel file-path #{} []))
  ([file-path open-opts file-attrs]
   (Files/newByteChannel (path file-path)
                         open-opts
                         (into-array FileAttribute file-attrs))))

(defn open
  "open a file"
  ([file-path]
   (open file-path #{}))
  ([file-path open-opts]
   (open file-path open-opts []))
  ([file-path open-opts file-attrs]
   (FileChannel/open (path file-path)
                     (normalize-open-options open-opts)
                     (into-array FileAttribute file-attrs))))


(defn write
  "Writes lines to file. NB. this will automatically insert a newline
   after each line."
  ([file-path lines open-opts]
   (write file-path lines StandardCharsets/UTF_8 open-opts))
  ([file-path lines char-set open-opts]
   (Files/write (path file-path) lines char-set
                (into-array (normalize-open-options open-opts)))))


(defn transfer
  "Transfers bytes from src-chan to dest-chan.
   dest-chan should be a WriteableByteChannel."
  ([src-chan dest-chan]
   (transfer src-chan dest-chan 4096))
  ([src-chan dest-chan buffer-size]
   (let [bb (ByteBuffer/allocate buffer-size)]
     (while (not= -1 (.read src-chan bb))
       (.flip bb)
       (.write dest-chan bb)
       (.clear bb)))))

(defn transfer-to
  "Transfer bytes from `src-chan` to `dest-chan`.
   dest-chan is a WriteableByteChannel"
  ([src-chan dest-chan]
   (transfer-to src-chan dest-chan 0))
  ([src-chan dest-chan position]
   (transfer-to src-chan dest-chan position Long/MAX_VALUE))
  ([src-chan dest-chan position max-count]
   (.transferTo src-chan position max-count dest-chan)))

(defn transfer-from
  "Transfer bytes from `src-chan` to `dest-chan`.
   `src-chan is a ReadableByteChannel.`"
  ([src-chan dest-chan]
   (transfer-from src-chan dest-chan 0))
  ([src-chan dest-chan position]
   (transfer-from src-chan dest-chan position Long/MAX_VALUE))
  ([src-chan dest-chan position max-count]
   (.transferFrom dest-chan src-chan position max-count)))
