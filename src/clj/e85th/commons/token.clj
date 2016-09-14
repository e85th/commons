(ns e85th.commons.token
  "Various tokens and token factories."
  (:require [com.stuartsierra.component :as component]
            [clj-time.core :as t]
            [schema.core :as s]
            [buddy.sign.jwt :as jwt]
            [buddy.core.hash :as hash]
            [buddy.auth.backends.token :as token-backend]
            [slingshot.slingshot :as ss]
            [taoensso.timbre :as log]
            [e85th.commons.ex :as ex])
  (:import [clojure.lang IFn]))

(def token-decrypt-failed-ex ::token-decrypt-failed-ex)
(def data-key ::data)

(s/defn rand-token :- s/Str
  "Generates a random token of size n (default 5)."
  ([]
   (rand-token 5))
  ([n]
   (assert (pos? n))
   (apply str (repeatedly n #(rand-nth "ABCDEFGHIJKLMNOPQRSTUVWXYZ1234567890")))))

(s/defn log-auth-error
  [request ex]
  (log/infof "auth error ex: %s" ex))

(defprotocol ITokenFactory
  (data->token [this data])
  (token->data [this token])
  (token->data! [this token])
  (backend [this]))

(defrecord Sha256TokenFactory [secret token-ttl-minutes on-error-fn]
  component/Lifecycle
  (start [this]
    (let [hashed-secret (hash/sha256 secret)
          opts {:alg :dir :enc :a128cbc-hs256}
          backend-opts {:secret hashed-secret
                        :options opts
                        :on-error on-error-fn}]
      (assoc this
             :secret hashed-secret
             :opts opts
             :backend (token-backend/jwe-backend backend-opts))))

  (stop [this] this)

  ITokenFactory
  (data->token [this data]
    (jwt/encrypt {data-key data
                  :exp (t/plus (t/now) (t/minutes token-ttl-minutes))}
                 secret
                 (:opts this)))

  (token->data [this token]
    (ss/try+
     (let [token-data (jwt/decrypt token secret)]
       (or (data-key token-data) token-data))
     (catch [:type :validation] ex
       (log/infof "Token decrypt failed: %s" ex))
     (catch Exception ex
       (log/warnf ex))))

  (token->data! [this token]
    (or (token->data this token)
        (throw (ex/new-auth-exception token-decrypt-failed-ex "Token decrypt failed."))))

  (backend [this]
    (:backend this)))

(s/defn new-sha256-token-factory
  "Create a new token factory with SHA256 implementation."
  ([secret :- s/Str token-ttl-minutes :- s/Int]
   (new-sha256-token-factory secret token-ttl-minutes log-auth-error))
  ([secret :- s/Str token-ttl-minutes :- s/Int on-error-fn :- IFn]
   (map->Sha256TokenFactory {:secret secret :token-ttl-minutes token-ttl-minutes
                             :on-error-fn on-error-fn})))
