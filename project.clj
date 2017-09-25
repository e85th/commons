(defproject e85th/commons "0.1.25-alpha1"
  :description "Various infrastructure and utilities to bootstrap an application/server."
  :url "http://github.com/e85th/commons"
  :license {:name "Apache License 2.0"
            :url "http://www.apache.org/licenses/LICENSE-2.0"}
  :dependencies [[org.clojure/clojure "1.9.0-beta1" :scope "provided"]
                 [superstring "2.1.0"]
                 [org.clojure/java.jdbc "0.7.1"]
                 [hikari-cp "1.8.0"] ; db connection pool

                 [com.stuartsierra/component "0.3.2"]
                 [com.datomic/datomic-free "0.9.5561.59" :scope "provided"]

                 ;; -- file system utils
                 [me.raynes/fs "1.4.6"]

                 ;; -- Logging
                 [com.taoensso/timbre "4.10.0"]

                 ;; -- contracts
                 [prismatic/schema "1.1.6"]

                 ;; -- async http requests
                 [http-kit "2.2.0"]

                 ;; -- pattern matching
                 [org.clojure/core.match "0.3.0-alpha4"]

                 ;; -- JSON
                 [cheshire "5.8.0"]
                 [com.cognitect/transit-clj "0.8.300"]
                 ;; -- DateTime
                 [clj-time "0.14.0"]
                 [org.clojure/data.csv "0.1.4"]

                 [com.googlecode.libphonenumber/libphonenumber "8.8.2"]

                 ;; -- Email
                 [com.draines/postal "2.0.2"]

                 ;; -- encryption/tokens
                 [buddy "2.0.0"]

                 ;; -- FTP
                 [commons-net "3.6"]]

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
             :project/dev   {:dependencies [[reloaded.repl "0.2.3"]
                                            [org.clojure/tools.namespace "0.2.11"]
                                            [org.clojure/tools.nrepl "0.2.13"]
                                            [eftest "0.3.1"]]
                             :source-paths   ["dev/src"]
                             :resource-paths ["dev/resources"]
                             :repl-options {:init-ns user}
                             :env {:port "7000"}}
             :project/test  {}}


  :deploy-repositories [["releases"  {:sign-releases false :url "https://clojars.org/repo"}]
                        ["snapshots" {:sign-releases false :url "https://clojars.org/repo"}]])
