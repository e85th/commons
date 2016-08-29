(ns e85th.commons.net.rpc
  (:refer-clojure :exclude [await get])
  (:require [org.httpkit.client :as http]
            [cheshire.core :as json]
            [clojure.core.match :refer [match]]
            [schema.core :as s]))

(defn check-successful-response
  "Checks that the input response map has a successful status code (200 series) and returns the
   input response.  If unsuccessful (non 200 series response code), invokes non-success-fn with the
   response map.  The 1 arity function throws an exception."
  ([resp]
   (check-successful-response resp #(throw (ex-info "Unsuccesful response." %))))
  ([{:keys [status body] :as resp} non-success-fn]
   (if (#{200 201 202 203 204} status)
     resp
     (non-success-fn resp))))

(defn parse-response-body
  [{:keys [body]}]
  (json/parse-string body true))

(def ^{:doc "Checks for a successful response and parses the response body to a clojure data structure"}
  parse-successful-response (comp parse-response-body check-successful-response))

(s/defn call!
  "Returns a promise. Derefing the promise will yield the http response. http-opts
   is a map of options used by http-kit."
  [method :- s/Keyword url :- s/Str http-opts]
  (let [http-fn (method {:get http/get :post http/post :put http/put :delete http/delete})]
    (assert http-fn (format "Unknown method: %s" method))
    (http-fn url http-opts)))

(s/defn sync-call!
  "Makes a blocking http call.  Returns the parsed response body as a clojure
   data structure. http-opts is a map of options used by http-kit."
  [method :- s/Keyword url :- s/Str http-opts]
  (-> @(call! method url http-opts)
      parse-successful-response))
