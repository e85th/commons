(defproject e85th/commons "0.1.17"
  :description "Various infrastructure and utilities to bootstrap an application/server."
  :url "http://github.com/e85th/commons"
  :license {:name "Apache License 2.0"
            :url "http://www.apache.org/licenses/LICENSE-2.0"}
  :dependencies [[org.clojure/clojure "1.9.0-alpha15" :scope "provided"]

                 [org.clojure/java.jdbc "0.5.8"]
                 [hikari-cp "1.7.2"] ; db connection pool

                 [com.stuartsierra/component "0.3.1"]
                 [com.datomic/datomic-free "0.9.5554" :scope "provided"]

                 ;; -- file system utils
                 [me.raynes/fs "1.4.6"]

                 ;; -- Logging
                 [com.taoensso/timbre "4.7.4"]

                 ;; -- contracts
                 [prismatic/schema "1.1.4"]

                 ;; -- async http requests
                 [http-kit "2.2.0"]

                 ;; -- pattern matching
                 [org.clojure/core.match "0.3.0-alpha4"]

                 ;; -- JSON
                 [cheshire "5.6.3"]
                 [com.cognitect/transit-clj "0.8.297"]
                 ;; -- DateTime
                 [clj-time "0.12.0"]
                 [org.clojure/data.csv "0.1.3"]

                 [com.googlecode.libphonenumber/libphonenumber "7.4.5"]

                 ;; -- Email
                 [com.draines/postal "2.0.0"]

                 ;; -- encryption/tokens
                 [buddy "1.0.0"]

                 [slingshot "0.12.2"]


                 ;; -- FTP
                 [commons-net "3.5"]]

  :source-paths ["src/clj"]
  :java-source-paths ["src/java"]

  ;; only to quell lein-cljsbuild when using checkouts
  :cljsbuild {:builds []}

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
