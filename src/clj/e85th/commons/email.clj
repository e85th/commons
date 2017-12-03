(ns e85th.commons.email
  (:refer-clojure :exclude [send])
  (:require [com.stuartsierra.component :as component]
            [taoensso.timbre :as log]
            [e85th.commons.util :as u]
            [clojure.spec.alpha :as s]
            [postal.core :as postal]
            [clojure.string :as str]))


(def html-content-type "text/html")

(def ^{:doc "Default content type text/html"}
  default-content-type html-content-type)

(s/def ::host string?)
(s/def ::port nat-int?)
(s/def ::user string?)
(s/def ::pass string?)
(s/def ::tls boolean?)

(s/def ::smtp-config (s/keys :req-un [::host]
                             :opt-un [::port ::user ::pass ::tls]))


(s/def ::from string?)
(s/def ::to (s/coll-of string?))
(s/def ::subject string?)
(s/def ::body string?)
(s/def ::cc (s/coll-of string?))
(s/def ::bcc (s/coll-of string?))
(s/def ::content-type string?)

(s/def ::message (s/keys :req-un [::from ::to ::subject ::body]
                         :opt-un [::cc ::bcc ::content-type]))


(defn make-subject-prefixer
  "Creates a function which applies the prefix to the subject if prefix is a non-empty string.
   Otherwise returns the identity function. This is useful for prefixing for eg [TEST] in
   dev/staging emails."
  [prefix]
  (if (seq prefix)
    (partial str prefix)
    identity))

(defn env->subject-prefixer
  "Generates a prefixer that prefixes [TEST] if non prod env."
  [env-name]
  (let [prefix (when-not (u/production? env-name)
                 "[TEST] ")]
    (make-subject-prefixer prefix)))

(defn- send-message
  "Sends the message."
  [smtp-config
   subject-modifier-fn
   {:keys [subject body content-type] :or {content-type default-content-type} :as msg}]
  (s/assert ::message msg)
  (let [msg (merge msg {:subject (subject-modifier-fn subject)
                        :body [{:type content-type :content-type content-type :content body}]})
        {:keys [code] :as response} (postal/send-message smtp-config msg)]
    (assert (zero? code) (format "Error sending email. %s" response))))

(defprotocol IEmailSender
  (send [this msg]))

(defrecord SmtpEmailSender [smtp-config subject-modifier-fn]
  component/Lifecycle
  (start [this]
    (log/infof "SmtpEmailSender host: %s" (:host smtp-config))
    this)

  (stop [this] this)

  IEmailSender
  (send [this msg]
    (send-message smtp-config subject-modifier-fn msg)))

;;----------------------------------------------------------------------
(s/fdef new-smtp-email-sender
        :args (s/cat :smtp-config ::smtp-config :subject-modifier-fn fn?))

(defn new-smtp-email-sender
  "Creates a new smtp email sender."
  ([smtp-config]
   (new-smtp-email-sender smtp-config identity))
  ([smtp-config subject-modifier-fn]
   (map->SmtpEmailSender {:smtp-config smtp-config
                          :subject-modifier-fn subject-modifier-fn})))

(defrecord NilEmailSender []
  component/Lifecycle
  (start [this] this)
  (stop [this] this)

  IEmailSender
  (send [this msg]))

(defn new-nil-email-sender
  []
  (map->NilEmailSender {}))



(defn valid?
  "Valid email address?. The regex was copied from regular-expressions.info"
  [address]
  (some? (re-seq #"^[a-z0-9._%+-]+@[a-z0-9.-]+\.[a-z]{2,}$" (str/lower-case address))))

(def invalid? (complement valid?))

(defn domain
  "Parse the domain from a valid email address ie foo@example.com returns example.com
   An invalid email address will throw an AssertionError."
  [s]
  (let [parts (str/split s #"@")]
    (assert (= 2 (count parts)))
    (second parts)))

(defn username
  "Parse the username from a valid email address ie foo@example.com returns foo.
   An invalid email address will throw an AssertionError."
  [s]
  (let [parts (str/split s #"@")]
    (assert (= 2 (count parts)))
    (first parts)))


;;----------------------------------------------------------------------
(s/fdef normalize
        :args (s/cat :s string?)
        :ret string?)

(defn normalize
  "Does not lower-case because the local part can be case sensitive."
  [s]
  (str/trim s))
