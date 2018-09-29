(defproject e85th/commons "0.1.29"
  :description "Various infrastructure and utilities to bootstrap an application/server."
  :url "http://github.com/e85th/commons"
  :license {:name "Apache License 2.0"
            :url "http://www.apache.org/licenses/LICENSE-2.0"}
  :dependencies [[org.clojure/clojure "1.9.0" :scope "provided"]
                 [superstring "2.1.0"]
                 [org.clojure/java.jdbc "0.7.3"]
                 [hikari-cp "2.2.0"] ; db connection pool

                 [com.stuartsierra/component "0.3.2"]
                 [com.datomic/datomic-free "0.9.5561.62" :scope "provided"]

                 ;; -- file system utils
                 [me.raynes/fs "1.4.6"]

                 ;; -- Logging
                 [com.taoensso/timbre "4.10.0"]

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

                 [com.googlecode.libphonenumber/libphonenumber "8.8.4"]

                 ;; -- Email
                 [com.draines/postal "2.0.2"]

                 ;; -- encryption/tokens
                 [buddy "2.0.0"]

                 ;; -- FTP
                 [commons-net "3.6"]]

  :source-paths ["src/clj" "src/cljc"]
  ;; :java-source-paths ["src/java"]

  ;; only to quell lein-cljsbuild when using checkouts
  :cljsbuild {:builds []}

  :plugins [[codox "0.8.13"]
            [lein-expectations "0.0.8"]
            [lein-kibit "0.1.2"]] ; static code analyzer for clojure

  :profiles {:dev  [:project/dev  :profiles/dev]
             :test [:project/test :profiles/test]
             :uberjar {:aot :all}
             :profiles/dev  {}
             :profiles/test {}
             :project/dev   {:dependencies [[reloaded.repl "0.2.3"]
                                            [orchestra "2017.08.13"]
                                            [expectations "2.2.0-rc3"]
                                            [org.clojure/tools.namespace "0.2.11"]
                                            [org.clojure/tools.nrepl "0.2.13"]]
                             :source-paths   ["dev/src"]
                             :resource-paths ["dev/resources"]
                             :repl-options {:init-ns user}
                             :env {:port "7000"}}
             :project/test  {}}


  :deploy-repositories [["releases"  {:sign-releases false :url "https://clojars.org/repo"}]
                        ["snapshots" {:sign-releases false :url "https://clojars.org/repo"}]])
