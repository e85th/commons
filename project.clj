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

                 [com.googlecode.libphonenumber/libphonenumber "7.4.0"]

                 ;; -- Email
                 [com.draines/postal "2.0.0"]

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

  :plugins [[com.jakemccrary/lein-test-refresh "0.10.0"]
            [codox "0.8.13"]
            [lein-exec "0.3.6"]
            [lein-version-script "0.1.0"]
            [test2junit "1.1.2"]
            [lein-kibit "0.1.2"] ; static code analyzer for clojure
            [lein-ancient "0.6.7" :exclusions [org.clojure/clojure]]]

  :profiles {:dev  [:project/dev  :profiles/dev]
             :test [:project/test :profiles/test]
             :uberjar {:aot :all}
             :profiles/dev  {}
             :profiles/test {}
             :project/dev   {:dependencies [[reloaded.repl "0.2.2"]
                                            [org.clojure/tools.namespace "0.2.11"]
                                            [org.clojure/tools.nrepl "0.2.12"]
                                            [eftest "0.1.1"]]
                             :source-paths   ["dev/src"]
                             :resource-paths ["dev/resources"]
                             :repl-options {:init-ns user}
                             :env {:port "7000"}}
             :project/test  {}}

  :deploy-repositories [["releases" :clojars]
                        ["snapshots" :clojars]])
