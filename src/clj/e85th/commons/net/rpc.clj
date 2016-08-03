(ns e85th.commons.net.rpc
  (:refer-clojure :exclude [await])
  (:require ;[org.httpkit.client :as http]
            [ajax.core :as http]
            [clj-http.client :as httpc]
            [clojure.core.match :refer [match]]
            [schema.core :as s]))

(def kw->method
  {:delete http/DELETE
   :get http/GET
   :post http/POST
   :put http/PUT})

(def alt-kw->method
  {:delete httpc/delete
   :get httpc/get
   :post httpc/post
   :put httpc/put})

(defn await
  "Derefs a promise created by api-call!.
   The promise should have [boolean request-details-map response]. This
   function will block until the promise is delivered."
  ([p]
   (await p true))
  ([p throw?]
   (let [[success? request response] @p]
     (match [throw? success?]
       [true true] response
       [true false] (throw (ex-info "API call failed." {:request request :response response}))
       [false true] [success? response]
       [false false] [success? (:response response)]))))

(s/defn api-call!
  "Calls the api endpoint indicated. Returns a promise.
  (rpc/api-call! :get endpoint)"
  ([method-kw :- s/Keyword url :- s/Str]
   (api-call! method-kw url nil))
  ([method-kw :- s/Keyword
    url :- s/Str
    params :- (s/maybe {s/Any s/Any})]
   (let [f (kw->method method-kw)
         request {:method method-kw :url url :params params}
         p (promise)]
     (f url (cond-> {:handler #(deliver p [true request %])
                     :error-handler #(deliver p [false request %])
                     :format :json
                     :response-format :json
                     :keywords? true}
              params (assoc :params params)))
     p)))

(s/defn sync-api-call!
  "Synchronus (blocking) api call."
  ([method-kw url]
   (sync-api-call! method-kw url nil))
  ([method-kw url params]
   (-> (api-call! method-kw url params)
       (await true))))

(s/defn alt-sync-api-call!
  ([method-kw url]
   (alt-sync-api-call! method-kw url nil))
  ([method-kw url params]
   (alt-sync-api-call! method-kw url params nil))
  ([method-kw url params opts]
   (let [f (alt-kw->method method-kw)
         get? (= :get method-kw)
         args (cond-> {:as :json}
                (and get? (seq params)) (assoc :query-params params)
                (and (not get?) (seq params)) (assoc :body params :content-type :json :accept :json))]
     (:body (f url args opts)))))
