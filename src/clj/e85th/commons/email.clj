(ns e85th.commons.email
  (:refer-clojure :exclude [send])
  (:require [schema.core :as s]
            [com.stuartsierra.component :as component]
            [taoensso.timbre :as log]
            [e85th.commons.util :as u]
            [postal.core :as postal])
  (:import [clojure.lang IFn]))

(def html-content-type "text/html")

(def ^{:doc "Default content type text/html"}
  default-content-type html-content-type)

(s/defschema SmtpConfig
  {:host s/Str
   (s/optional-key :port) s/Int
   (s/optional-key :user) s/Str
   (s/optional-key :pass) s/Str
   (s/optional-key :tls) s/Bool})

(s/defschema Message
  {:from s/Str
   :to [s/Str]
   :subject s/Str
   :body s/Str
   (s/optional-key :cc) [s/Str]
   (s/optional-key :bcc) [s/Str]
   (s/optional-key :content-type) s/Str
   s/Keyword s/Any})

(s/defn make-subject-prefixer :- IFn
  "Creates a function which applies the prefix to the subject if prefix is a non-empty string.
   Otherwise returns the identity function. This is useful for prefixing for eg [TEST] in
   dev/staging emails."
  [prefix :- (s/maybe s/Str)]
  (if (seq prefix)
    (partial str prefix)
    identity))

(s/defn env->subject-prefixer :- IFn
  "Generates a prefixer that prefixes [TEST] if non prod env."
  [env-name]
  (let [prefix (if-not (u/production? env-name) "[TEST] ")]
    (make-subject-prefixer prefix)))

(defn- send-message
  "Sends the message."
  [smtp-config
   subject-modifier-fn
   {:keys [subject body content-type] :or {content-type default-content-type} :as msg}]
  (s/validate Message msg)
  (let [msg (merge {:subject (subject-modifier-fn subject)
                    :body [{:type content-type :content body}]} msg)
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

(s/defn new-smtp-email-sender
  "Creates a new smtp email sender."
  ([smtp-config :- SmtpConfig]
   (new-smtp-email-sender smtp-config identity))
  ([smtp-config :- SmtpConfig subject-modifier-fn :- IFn]
   (map->SmtpEmailSender (merge {:subject-modifier-fn identity} smtp-config))))

(defrecord NilEmailSender []
  component/Lifecycle
  (start [this] this)
  (stop [this] this)

  IEmailSender
  (send [this msg]))

(defn new-nil-email-sender
  []
  (map->NilEmailSender {}))



(s/defn valid?
  "Valid email address?. The regex was copied from the bouncer library."
  [address :- s/Str]
  (some? (re-seq #"^[^@]+@[^@\\.]+[\\.].+" address)))
