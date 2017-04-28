(ns e85th.commons.net.ftp
  "usage:
  (def client (connect \"ftp.sec.gov\" \"anonymous\" \"anonymous\"))
  (get client \"edgar/full-index/company.gz\" \"company.gz\" binary)
  (disconnect client) "
  (:refer-clojure :exclude [get])
  (:require [clojure.java.io :as io]
            [me.raynes.fs :as fs]
            [e85th.commons.ex :as ex])
  (:import [org.apache.commons.net.ftp FTP FTPClient]))


(def ascii ::ascii)
(def binary ::binary)

(defn connect
  "Connect to the host with user/pass. Returns FTPClient instance.
  The client is set to local passive mode which allows for using the client
  from behind a firewall.  The remove verification enabled is disabled
  otherwise getting a file fails."
  [^String host ^String user ^String pass]
  (doto (FTPClient.)
    (.setControlEncoding "UTF-8")
    (.connect host)
    (.login user pass)
    ;; send NOOP every 1 minute to keep the control channel alive (for routers etc)
    (.setControlKeepAliveTimeout 60)
    (.enterLocalPassiveMode)
    (.setRemoteVerificationEnabled false)))

(defn disconnect
  "disconnects the client"
  [^FTPClient client]
  (.logout client))

(defn ls
  "Lists files in the given directory."
  [^FTPClient client ^String dir]
  (.listNames client dir))

(defn get
  "remote-file string path, local-file, string path, transfer-mode :ascii :binary.
  If no transfer mode is specified uses binary. Throws an exception if file is not retrieved successfully.."
  ([client remote-name local-name]
   (get client remote-name local-name binary))
  ([^FTPClient client remote-name local-name transfer-mode]
   (.setFileType client (if (= binary transfer-mode) FTP/BINARY_FILE_TYPE FTP/ASCII_FILE_TYPE))
   (fs/mkdirs (fs/parent local-name))
   (with-open [outstream (java.io.FileOutputStream. (io/as-file local-name))]
     (let [success? (.retrieveFile client remote-name outstream)]
       (when-not success?
         (throw (ex/generic ::ftp-get-failed "FTP get failed." {:remote-name remote-name :local-name local-name})))))))

(defn cd
  "Change directory."
  [^FTPClient client path]
  (.changeWorkingDirectory client path))


(defn local-passive-mode
  "Puts the client into local passive mode."
  [^FTPClient client]
  (.enterLocalPassiveMode client))

(defn use-epsv-with-ipv4
  [^FTPClient client ^Boolean enable?]
  (.setUseEPSVwithIPv4 client enable?))

(defn enable-remote-verification
  [^FTPClient client ^Boolean enable?]
  (.setRemoteVerificationEnabled client enable?))
