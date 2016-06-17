(ns e85th.commons.aws.sqs
  (:require [amazonica.aws.sqs :as sqs]
            [clojure.spec :as s]
            [clojure.spec.gen :as gen]
            [clojure.spec.test :as stest]))

(s/fdef full-name
  :args (s/cat :first-name string? :last-name string?)
  :ret string?
  :fn (fn [{:keys [args ret]}]
        (= 42 (str (:first-name args) " " (:last-name args)))))


(defn full-name
  [first-name last-name]
  (str first-name " " last-name))

;(s/instrument #'full-name)

(full-name "dj" "jd")


(stest/check-var #'full-name)
