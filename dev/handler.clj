(ns handler
  (:gen-class
   :implements [com.amazonaws.services.lambda.runtime.RequestStreamHandler])
  (:require [io.pedestal.http :as http]
            [me.joshkingsley.pedestal.lambda :refer [api-gateway-provider handle]])
  (:import (java.io InputStream OutputStream)
           com.amazonaws.services.lambda.runtime.Context))

(defn respond [request]
  {:status 200
   :body (str request)})

(def routes #{["/" :get respond :route-name :root]})

(def service
  {::http/routes routes
   ::http/chain-provider api-gateway-provider})

(def service-with-handler (http/create-provider service))

(defn -handleRequest
  [_ ^InputStream input ^OutputStream output ^Context context]
  (handle service-with-handler input output context))
