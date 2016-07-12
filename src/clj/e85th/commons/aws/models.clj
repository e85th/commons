(ns e85th.commons.aws.models
  (:require [schema.core :as s])
  (:import [clojure.lang IFn]))

(def default-profile
  {:profile "default"})

(s/defschema Profile
  {:profile s/Str})

(s/defschema MessageProcessorParams
  {:q-name s/Str
   :on-message-fn IFn
   (s/optional-key :profile) Profile
   (s/optional-key :topic-names) [s/Str]
   (s/optional-key :thread-name) s/Str
   (s/optional-key :dynamic?) s/Bool
   (s/optional-key :resources) s/Any})

(s/defschema MessagePublisherParams
  {:topic-name s/Str
   (s/optional-key :profile) Profile})
