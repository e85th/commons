(ns e85th.commons.cv
  "Computer Vision"
  (:require [schema.core :as s]
            [com.stuartsierra.component :as component]))


(defprotocol IOcr
  (image-url->text
    [this img-url]
    [this img-url max-results]
    "Takes an image url and answers with the text read from the image."))

(defrecord NilOcr []
  component/Lifecycle
  (start [this] this)
  (stop [this] this)

  IOcr
  (image-url->text [this img-url])
  (image-url->text [this img-url max-results]))

(defn new-nil-ocr
  "Constructs an IOcr instance that does nothing."
  []
  (map->NilOcr {}))
