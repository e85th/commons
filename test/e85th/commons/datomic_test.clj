(ns e85th.commons.datomic-test
  (:require [expectations.clojure.test :refer [defexpect expect]]
            [e85th.commons.datomic :as datomic]))

(defexpect prepare-update-test
  (expect [{:a 1 :b 2}] (datomic/prepare-update {} {:a 1 :b 2}))
  (expect #{[:db/retract 42 :b 2] {:a 1}}
          (set (datomic/prepare-update {:db/id 42 :a 1 :b 2} {:a 1 :b nil})))
  (expect #{[:db/retract 42 :a 1] [:db/retract 42 :b 2]}
          (set (datomic/prepare-update {:db/id 42 :a 1 :b 2} {:a nil :b nil}))))

(defexpect prepare-create-test
  (expect {:a 1 :b 2} (datomic/prepare-create {:a 1 :b 2}))
  (expect {:a 1}      (datomic/prepare-create {:a 1 :b nil})))
