(ns e85th.commons.net.rpc
  (:refer-clojure :exclude [await])
  (:require [ajax.core :as http]
            [clojure.core.match :refer [match]]
            [schema.core :as s]))

(def kw->method
  {:delete http/DELETE
   :get http/GET
   :post http/POST
   :put http/PUT})

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
         request ({:method method-kw :url url :params params})
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
   (api-call! method-kw url nil))
  ([method-kw url params]
   (-> (api-call! method-kw url params)
       (await true))))
