(defproject me.joshkingsley/pedestal.lambda "0.2.0-SNAPSHOT"
  :description "Chain provider for Pedestal services running on AWS Lambda."
  :url "https://github.com/jkxyz/pedestal-lambda"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.9.0"]
                 [org.clojure/data.json "0.2.6"]
                 [io.pedestal/pedestal.service "0.5.4"]
                 [io.pedestal/pedestal.jetty "0.5.4"]
                 [ring/ring-core "1.7.0-RC1"]
                 [com.amazonaws/aws-lambda-java-core "1.2.0"]]
  :deploy-repositories {"releases" {:url "https://repo.clojars.org" :creds :gpg}})
