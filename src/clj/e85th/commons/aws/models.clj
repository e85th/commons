(ns e85th.commons.aws.models
  (:require [schema.core :as s]))

(def default-profile
  {:profile "default"})

(s/defschema Profile
  {:profile s/Str})
