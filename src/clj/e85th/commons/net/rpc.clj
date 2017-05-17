(ns e85th.commons.net.rpc
  (:refer-clojure :exclude [await get])
  (:require [org.httpkit.client :as http]
            [cheshire.core :as json]
            [clojure.core.match :refer [match]]
            [schema.core :as s]
            [taoensso.timbre :as log]
            [clojure.string :as string]
            [clojure.edn :as edn]))

(def http-success-status-codes #{200 201 202 203 204})

(defn success?
  "Answers if the input status-code is a http success code."
  [status-code]
  (http-success-status-codes status-code))

(s/defn content-type->name :- s/Keyword
  [s :- (s/maybe s/Str)]
  (let [s (string/lower-case (or s ""))]
    (cond
      (string/includes? s "application/json") :json
      (string/includes? s "application/edn")  :edn
      :else :other)))

(defn json?
  "Answers true if the content-type is application/json. Ignores case, handles nil."
  [content-type]
  (= :json (content-type->name content-type)))

(defn edn?
  "Answers true if the content-type is application/json. Ignores case, handles nil."
  [content-type]
  (= :edn (content-type->name content-type)))


(defn check-successful-response
  "Checks that the input response map has a successful status code (200 series) and returns the
   input response.  If unsuccessful (non 200 series response code), invokes non-success-fn with the
   response map.  The 1 arity function throws an exception."
  ([resp]
   (check-successful-response resp #(throw (ex-info "Unsuccesful response." %))))
  ([{:keys [status body] :as resp} non-success-fn]
   (if (success? status)
     resp
     (non-success-fn resp))))

(defn parse-response-body
  ([resp]
   (parse-response-body {} resp))
  ([edn-read-opts {:keys [body headers] :as resp}]
   (let [{:keys [content-type]} headers
         ct-name (content-type->name content-type)]
     (case ct-name
       :json (json/parse-string body true)
       :edn (let [edn-str (cond-> body
                            (instance? java.io.InputStream body) slurp)]
              (edn/read-string edn-read-opts edn-str))
       :other body
       body))))

(def ^{:doc "Checks for a successful response and parses the response body to a clojure data structure"}
  parse-successful-response (comp parse-response-body check-successful-response))

(s/defn call!
  "Returns a promise if no callback is passed in.
   Derefing the promise will yield the http response.
   http-opts is a map of options used by http-kit."
  ([method :- s/Keyword url :- s/Str http-opts]
   (call! (assoc http-opts :method method :url url)))
  ([req]
   (http/request req))
  ([method :- s/Keyword url :- s/Str http-opts cb]
   (call! (assoc http-opts :method method :url url) cb))
  ([req cb]
   (http/request req cb)))

(s/defn sync-call!
  "Makes a blocking http call.  Returns the parsed response body as a clojure
   data structure. http-opts is a map of options used by http-kit."
  ([method :- s/Keyword url :- s/Str http-opts]
   (-> @(call! method url http-opts)
       parse-successful-response))
  ([req]
   (-> req call! deref parse-successful-response)))


(s/defn new-request
  [method url]
  {:method method
   :url url})

(s/defn json-content
  "Adds in content-type and json encodes the body."
  ([req]
   (json-content req nil))
  ([req body]
   (cond-> req
     true (update-in [:headers] merge {"Content-Type" "application/json" "Accept" "application/json"})
     body (assoc-in [:body] (json/generate-string body)))))

(s/defn bearer-auth
  "Add a bearer authorization in to the request headers"
  [req token]
  (assoc-in req [:headers "Authorization"] (str "Bearer " token)))

(s/defn query-params
  [req params-map]
  (update-in req [:query-params] merge params-map))
