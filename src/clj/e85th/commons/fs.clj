(ns e85th.commons.fs
  (:require [clojure.spec.alpha :as s]
            [e85th.commons.ext :as ext]
            [clojure.java.io :as io]
            [clojure.string :as str])
  (:import [java.io File RandomAccessFile]
           [java.util.zip ZipFile GZIPInputStream GZIPOutputStream]
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

(extend-protocol io/Coercions
  Path
  (as-file [this]
    (.toFile this))
  (as-url [this]
    (-> this .toUri .toURL)))


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


(defn temp-name
  "Create a temporary file name like what is created for [[temp-file]]
   and [[temp-dir]]."
  ([prefix] (temp-name prefix ""))
  ([prefix suffix]
   (format "%s%s-%s%s" prefix (System/currentTimeMillis)
           (long (rand 0x100000000)) suffix)))

(def input-stream? (partial instance? java.io.InputStream))
(def output-stream? (partial instance? java.io.OutputStream))

(defn input-stream
  "open-opts are keywords corresponding to `OpenOption`"
  [file-path & open-opts]
  (Files/newInputStream (path file-path)
                        (into-array OpenOption (normalize-open-options open-opts))))


(defn output-stream
  "open-opts are `OpenOption`"
  [file-path & open-opts]
  (Files/newOutputStream (path file-path)
                         (into-array OpenOption (normalize-open-options open-opts))))


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


(defn file-name
  [file]
  (some-> file path .getFileName))

(defn file-extension
  "Returns the last token separated by a `.` if any.
   `foo` => `foo`, `foo.bar` => `bar`, `foo.bar.txt` => `txt`.
  Input `file` is a path, or path like."
  [file]
  (some-> file file-name str (str/split #"\.") last))

(defn strip-file-extension
  "Returns a Path with the file extension removed."
  [file]
  (let [file (path file)
        parent-path (.getParent file)
        f-name (some-> file file-name str)]
    (if-let [idx (str/last-index-of f-name ".")]
      (resolve-path parent-path (subs f-name 0 idx))
      file)))


(defn parent
  [file]
  (-> file io/file .getParentFile))

(defn unzip
  "Takes the path to a zipfile `source` and unzips it to target-dir."
  ([source]
   (unzip source (name source)))
  ([source target-dir]
   (with-open [zip (ZipFile. (io/file source))]
     (let [entries (enumeration-seq (.entries zip))
           target-file #(io/file target-dir (str %))]
       (doseq [entry entries :when (not (.isDirectory ^java.util.zip.ZipEntry entry))
               :let [f (target-file entry)]]
         (create-dirs (parent f))
         (io/copy (.getInputStream zip entry) f))))
   target-dir))

(defn gunzip
  "Takes a path to a gzip file `source` and unzips it."
  ([source] (gunzip source (name source)))
  ([source target]
   (io/copy (-> source (input-stream :read) GZIPInputStream.)
            (output-stream target :create :write))))


(def ext->uncompresser
  {"gz" gunzip
   "zip" unzip})

(defn uncompress
  "uncompress file if required and return the uncompressed file path.
   If file is not compressed return input file."
  ([src]
   (uncompress src (strip-file-extension src)))
  ([src dest]
   (if-let [uncompresser (ext->uncompresser (file-extension src))]
     (let [dest (strip-file-extension src)]
       (uncompresser (str src) (str dest))
       dest)
     src)))


(defn gzip-file
  ([src]
   (gzip-file src (str src ".gz")))
  ([src dest]
   (with-open [src-stream (input-stream src :read)
               dest-stream (GZIPOutputStream. (output-stream dest :create :write))]
     (io/copy src-stream dest-stream)
     dest)))
