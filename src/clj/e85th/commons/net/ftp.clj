(ns e85th.commons.net.ftp
  "usage:
  (def client (connect \"ftp.sec.gov\" \"anonymous\" \"anonymous\"))
  (get client \"edgar/full-index/company.gz\" \"company.gz\" binary)
  (disconnect client) "
  (:refer-clojure :exclude [get])
  (:require [clojure.java.io :as io])
  (:import [org.apache.commons.net.ftp FTP FTPClient]))


(def ascii ::ascii)
(def binary ::binary)

(defn connect
  "Connect to the host with user/pass. Returns FTPClient instance."
  [^String host ^String user ^String pass]
  (let [client (FTPClient.)]
    (doto client
      (.setControlEncoding "UTF-8")
      (.connect host)
      (.login user pass)
      ;; send NOOP every 1 minute to keep the control channel alive (for routers etc)
      (.setControlKeepAliveTimeout 60))
    client))

(defn disconnect
  "disconnects the client"
  [^FTPClient client]
  (.logout client))

(defn ls
  "Lists files in the given directory."
  [^FTPClient client ^String dir]
  (.listNames client dir))

(defn get
  "remote-file string path, local-file, string path, transfer-mode :ascii :binary"
  [^FTPClient client remote-name local-name transfer-mode]
  (.setFileType client (if (= binary transfer-mode) FTP/BINARY_FILE_TYPE FTP/ASCII_FILE_TYPE))
  (with-open [outstream (java.io.FileOutputStream. (io/as-file local-name))]
    (.retrieveFile client remote-name outstream)))

(defn cd
  "Change directory."
  [^FTPClient client path]
  (.changeWorkingDirectory client path))
