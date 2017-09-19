(defproject cf-async-service-broker "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :min-lein-version "2.0.0"
  :dependencies [[org.clojure/clojure "1.7.0"]
                 [compojure "1.4.0"]
                 [ring "1.5.0"]
                 [ring-json-response "0.2.0"]
                 [ring/ring-jetty-adapter "1.5.0-RC1"]
                 [ring/ring-defaults "0.1.5"]
                 [org.clojure/tools.logging "0.3.1"]
                 ;; [clj-http "3.0.1"]
                 [org.clojure/data.json "0.2.6"]
                 [cf-lib "0.1.0-SNAPSHOT"]
                 [org.clojure/data.codec "0.1.0"]
                 [ring/ring-jetty-adapter "1.5.0-RC1"]
                 [javax.servlet/servlet-api "2.5"]
                 [ring/ring-mock "0.3.0"]
                 ]
  :plugins [[lein-ring "0.9.7"]]
  :ring {:handler cf-async-service-broker.handler/app}
  :profiles
  {:dev {:dependencies [[javax.servlet/servlet-api "2.5"]
                        [ring/ring-mock "0.3.0"]]}})
