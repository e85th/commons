(set-env!
 :source-paths #{"src/java" "test"}
 :resource-paths #{"src/clj"}
 :dependencies '[[org.clojure/clojure "1.9.0-alpha15" :scope "provided"]

                 [org.clojure/java.jdbc "0.5.8"]
                 [hikari-cp "1.7.5"] ; db connection pool

                 [com.stuartsierra/component "0.3.2"]
                 [com.datomic/datomic-free "0.9.5554" :scope "provided"]

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
                 [cheshire "5.7.1"]
                 [com.cognitect/transit-clj "0.8.300"]
                 ;; -- DateTime
                 [clj-time "0.13.0"]
                 [org.clojure/data.csv "0.1.4"]

                 [com.googlecode.libphonenumber/libphonenumber "8.5.2"]

                 ;; -- Email
                 [com.draines/postal "2.0.2"]

                 ;; -- encryption/tokens
                 [buddy "1.3.0"]

                 [slingshot "0.12.2"]


                 ;; -- FTP
                 [commons-net "3.6"]
                 [org.clojure/tools.nrepl "0.2.13"]
                 [cider/cider-nrepl "0.15.0-SNAPSHOT"]
                 [adzerk/boot-test "1.2.0" :scope "test"]
                 [metosin/boot-alt-test "0.2.1" :scope "test"]
                 ]

 :repositories #(conj %
                      ["clojars" {:url "https://clojars.org/repo"
                                  :username (System/getenv "CLOJARS_USER")
                                  :password (System/getenv "CLOJARS_PASS")}]))

(require '[adzerk.boot-test :as boot-test])
(require '[cider.tasks :refer [add-middleware]])

(task-options! add-middleware {:middleware '[cider.nrepl.middleware.apropos/wrap-apropos
                                             cider.nrepl.middleware.classpath/wrap-classpath
                                             cider.nrepl.middleware.complete/wrap-complete
                                             cider.nrepl.middleware.debug/wrap-debug
                                             cider.nrepl.middleware.format/wrap-format
                                             cider.nrepl.middleware.info/wrap-info
                                             cider.nrepl.middleware.inspect/wrap-inspect
                                             cider.nrepl.middleware.macroexpand/wrap-macroexpand
                                             cider.nrepl.middleware.ns/wrap-ns
                                             cider.nrepl.middleware.pprint/wrap-pprint
                                             cider.nrepl.middleware.pprint/wrap-pprint-fn
                                             cider.nrepl.middleware.refresh/wrap-refresh
                                             cider.nrepl.middleware.resource/wrap-resource
                                             cider.nrepl.middleware.stacktrace/wrap-stacktrace
                                             cider.nrepl.middleware.test/wrap-test
                                             cider.nrepl.middleware.trace/wrap-trace
                                             cider.nrepl.middleware.out/wrap-out
                                             cider.nrepl.middleware.undef/wrap-undef
                                             cider.nrepl.middleware.version/wrap-version]})


(deftask test
  "Runs the unit-test task"
  []
  (comp
   (javac)
   (boot-test/test)))



(deftask build
  "Builds a jar for deployment."
  []
  (comp
   (javac)
   (pom)
   (jar)
   (target)))

(deftask dev
  "Starts the dev task."
  []
  (comp
   (repl)
   (watch)))

(deftask deploy
  []
  (comp
   (build)
   (push)))

(task-options!
 pom {:project 'e85th/commons
      :version "0.1.22"
      :description "Various infrastructure and utilities to bootstrap an application/server."
      :url "http://github.com/e85th/commons"
      :scm {:url "http://github.com/e85th/commons"}
      :license {"Apache License 2.0" "http://www.apache.org/licenses/LICENSE-2.0"}}
 push {:repo "clojars"})
