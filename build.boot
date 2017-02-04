(set-env!
 :source-paths #{"src/clj" "src/java"}
 :resource-paths #{"src/clj" "test"}
 :dependencies '[[org.clojure/clojure "1.8.0"]
                 [org.clojure/java.jdbc "0.5.8"]
                 [hikari-cp "1.7.2"] ; db connection pool
                 [com.stuartsierra/component "0.3.1"]
                 ;; -- file system utils
                 [me.raynes/fs "1.4.6"]
                 ;; -- Logging
                 [com.taoensso/timbre "4.7.4"]
                 ;; -- contracts
                 [prismatic/schema "1.1.2"]
                 ;; -- async http requests
                 [http-kit "2.2.0"]
                 ;; -- pattern matching
                 [org.clojure/core.match "0.3.0-alpha4"]
                 ;; -- JSON
                 [cheshire "5.6.3"]
                 ;; -- DateTime
                 [clj-time "0.12.0"]
                 [org.clojure/data.csv "0.1.3"]
                 [com.googlecode.libphonenumber/libphonenumber "7.4.5"]
                 ;; -- Email
                 [com.draines/postal "2.0.0"]
                 ;; -- encryption/tokens
                 [buddy "1.0.0"]
                 [slingshot "0.12.2"]
                 ;; -- AWS
                 [amazonica "0.3.66" :exclusions [com.amazonaws/aws-java-sdk]]
                 [com.amazonaws/aws-java-sdk-core "1.11.18"]
                 [com.amazonaws/aws-java-sdk-s3 "1.11.18"]
                 [com.amazonaws/aws-java-sdk-sqs "1.11.18"]
                 [com.amazonaws/aws-java-sdk-sns "1.11.18"]
                 [com.amazonaws/aws-java-sdk-ses "1.11.18"]
                 ;; -- FTP
                 [commons-net "3.5"]
                 ;; -- Test
                 [adzerk/boot-test "1.1.2" :scope "test"]
                 [metosin/boot-alt-test "0.2.1" :scope "test"]])

(set-env! :source-paths #{"test"})
(require '[adzerk.boot-test :refer :all])
(require '[metosin.boot-alt-test :refer [alt-test]])

(deftask testing
  "Profile setup for running tests."
  []
  (set-env! :source-paths #(conj % "test"))
  (javac)
  identity)

(deftask unit-test
  "Runs the unit-test task"
  []

  (javac))

(deftask dev
  "Starts the dev task."
  []
  (comp
   (repl)
   (watch)

   ))

(deftask build
  "Builds a jar for deployment."
  []
  (comp
   (pom)
   (jar)
   (target)))

(task-options!
 pom {:project 'e85th/commons
      :version "0.1.4"
      :description "Various infrastructure and utilities to bootstrap an application/server."
      :url "http://github.com/e85th/commons"
      :scm {:url "http://github.com/e85th/commons"}
      :license {"Apache License 2.0" "http://www.apache.org/licenses/LICENSE-2.0"}})
