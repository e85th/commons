(defproject commons "0.1.0-SNAPSHOT"
  :description "Various infrastructure and utilities to bootstrap an application/server."
  :url "http://github.com/e85th/commons"
  :license {:name "Apache License 2.0"
            :url "http://www.apache.org/licenses/LICENSE-2.0"}
  :dependencies [[org.clojure/clojure "1.9.0-alpha7"]

                 ;; -- Logging
                 [com.taoensso/timbre "4.4.0"]

                 ;; -- AWS
                 [amazonica "0.3.59" :exclusions [com.amazonaws/aws-java-sdk]]
                 [com.amazonaws/aws-java-sdk-core "1.11.8"]
                 [com.amazonaws/aws-java-sdk-s3 "1.11.8"]
                 [com.amazonaws/aws-java-sdk-sqs "1.11.8"]
                 [com.amazonaws/aws-java-sdk-sns "1.11.8"]
                 [com.amazonaws/aws-java-sdk-ses "1.11.8"]
                 [org.clojure/test.check "0.9.0"]
                 ]

  :source-paths ["src/clj"]
  :java-source-paths ["src/java"])
