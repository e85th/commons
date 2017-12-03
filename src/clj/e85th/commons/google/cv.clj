(ns e85th.commons.google.cv
  (:require [e85th.commons.cv :as cv]
            [org.httpkit.client :as http]
            [e85th.commons.net.rpc :as rpc]
            [e85th.commons.util :as u]
            [com.stuartsierra.component :as component]))

(def v1-url "https://vision.googleapis.com/v1/images:annotate")

(defn image-url->base64-str
  "Does an http :get on the url which should be a string."
  [image-url]
  (-> (rpc/sync-call! :get image-url {})
      .bytes
      u/bytes->base64-str))

(defn image-url->text*
  "Takes an image-url, fetches the image, base64 encodes it and sends to google for
   OCR purposes.  Returns the text google was able to read."
  [api-key img-url max-results]
  (let [img (image-url->base64-str img-url)
        body {:requests [{:image {:content img}
                          :features [{:type "TEXT_DETECTION" :maxResults max-results}]}]}]
    (-> (rpc/new-request :post v1-url)
        (rpc/json-content body)
        (rpc/query-params {:key api-key})
        rpc/sync-call!
        (get-in [:responses 0 :textAnnotations 0 :description]))))


(defrecord GoogleCloudVisionV1Ocr [api-key]
  component/Lifecycle
  (start [this] this)
  (stop [this] this)

  cv/IOcr
  (image-url->text
    [this img-url]
    (cv/image-url->text this img-url 50))
  (image-url->text
    [this img-url max-results]
    (image-url->text* api-key img-url max-results)))


(defn new-google-v1-ocr
  "Returns a google v1 cloud vision ocr client.  The images must be available via http get when
  calling methods on this implementation."
  [api-key]
  (map->GoogleCloudVisionV1Ocr {:api-key api-key}))
