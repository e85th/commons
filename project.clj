(defproject e85th/commons "0.1.0-SNAPSHOT"
  :description "Various infrastructure and utilities to bootstrap an application/server."
  :url "http://github.com/e85th/commons"
  :license {:name "Apache License 2.0"
            :url "http://www.apache.org/licenses/LICENSE-2.0"}
  :dependencies [[org.clojure/clojure "1.8.0"]

                 [org.clojure/java.jdbc "0.5.8"]
                 [hikari-cp "1.7.1"] ; db connection pool

                 [com.stuartsierra/component "0.3.1"]

                 ;; -- Logging
                 [com.taoensso/timbre "4.4.0"]

                 ;; -- contracts
                 [prismatic/schema "1.1.2"]

                 ;; -- async http requests
                 [cljs-ajax "0.5.8"]

                 ;; -- pattern matching
                 [org.clojure/core.match "0.3.0-alpha4"]

                 ;; -- JSON
                 [cheshire "5.6.1"]
                 ;; -- DateTime
                 [clj-time "0.12.0"]

                 ;; -- AWS
                 [amazonica "0.3.59" :exclusions [com.amazonaws/aws-java-sdk]]
                 [com.amazonaws/aws-java-sdk-core "1.11.8"]
                 [com.amazonaws/aws-java-sdk-s3 "1.11.8"]
                 [com.amazonaws/aws-java-sdk-sqs "1.11.8"]
                 [com.amazonaws/aws-java-sdk-sns "1.11.8"]
                 [com.amazonaws/aws-java-sdk-ses "1.11.8"]

                 ;; -- FTP
                 [commons-net "3.5"]]

  :source-paths ["src/clj"]
  :java-source-paths ["src/java"]

  :deploy-repositories [["releases" :clojars]
                        ["snapshots" :clojars]])
