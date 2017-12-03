(ns e85th.commons.token
  "Various tokens and token factories."
  (:require [com.stuartsierra.component :as component]
            [clj-time.core :as t]
            [clojure.spec.alpha :as s]
            [buddy.sign.jwt :as jwt]
            [buddy.core.hash :as hash]
            [buddy.auth.backends.token :as token-backend]
            [taoensso.timbre :as log]
            [e85th.commons.ex :as ex])
  (:import [clojure.lang IFn]))

(def token-decrypt-failed-ex ::token-decrypt-failed-ex)

(defn rand-token
  "Generates a random token of size n (default 5)."
  ([]
   (rand-token 5))
  ([n]
   (rand-token n "0123456789"))
  ([n src]
   (assert (pos? n))
   (apply str (repeatedly n #(rand-nth src)))))

(defn log-auth-error
  [request ex]
  (log/infof "auth error ex: %s" ex))

(defprotocol ITokenFactory
  (data->token [this data])
  (token->data [this token])
  (token->data! [this token])
  (backend [this]))

(defrecord Sha256TokenFactory [secret token-ttl-minutes on-error-fn token-name]
  component/Lifecycle
  (start [this]
    (let [hashed-secret (hash/sha256 secret)
          opts {:alg :dir :enc :a128cbc-hs256}
          backend-opts {:secret hashed-secret
                        :options opts
                        :token-name token-name
                        :on-error on-error-fn}]
      (assoc this
             :secret hashed-secret
             :opts opts
             :backend (token-backend/jwe-backend backend-opts))))

  (stop [this] this)

  ITokenFactory
  (data->token [this data]
    (jwt/encrypt (assoc data :exp (t/plus (t/now) (t/minutes token-ttl-minutes)))
                 secret
                 (:opts this)))
  (token->data [this token]
    (try
     (dissoc (jwt/decrypt token secret) :exp)
     (catch Exception ex
       (if (some-> ex ex-data :type (= :validation))
         (log/infof "Token decrypt failed: %s" ex)
         (log/warnf ex)))))

  (token->data! [this token]
    (or (token->data this token)
        (throw (ex/auth token-decrypt-failed-ex "Token decrypt failed."))))

  (backend [this]
    (:backend this)))


(s/fdef new-sha256-token-factory
        :args (s/cat :secret string? :token-ttl-minutes nat-int? :token-name (s/? string?) :on-error-fn (s/? fn?)))

(defn new-sha256-token-factory
  "Create a new token factory with SHA256 implementation."
  ([secret token-ttl-minutes]
   (new-sha256-token-factory secret token-ttl-minutes "Bearer"))
  ([secret token-ttl-minutes token-name]
   (new-sha256-token-factory secret token-ttl-minutes token-name log-auth-error))
  ([secret token-ttl-minutes token-name on-error-fn]
   (map->Sha256TokenFactory {:secret secret :token-ttl-minutes token-ttl-minutes
                             :token-name token-name :on-error-fn on-error-fn})))
