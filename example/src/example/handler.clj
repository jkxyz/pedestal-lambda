(ns example.handler
  (:gen-class
   :implements [com.amazonaws.services.lambda.runtime.RequestStreamHandler])
  (:require [io.pedestal.http :as http]
            [me.joshkingsley.pedestal.lambda :refer [api-gateway-provider handle]])
  (:import (java.io InputStream OutputStream)
           com.amazonaws.services.lambda.runtime.Context))

(defn home-page [request]
  {:status 200
   :body "Hello World!"})

(def routes
  #{["/" :get home-page :route-name :root]})

;; Specify the API Gateway provider as the chain provider in the service.
(def service
  {::http/routes routes
   ::http/chain-provider api-gateway-provider})

;; Adds default interceptors and adds the handler function to the service.
(def service-with-handler
  (http/create-provider service))

;; Implements the RequestStreamHandler interface - this is called when the
;; Lambda function receives an event.
(defn -handleRequest
  [_ ^InputStream input ^OutputStream output ^Context context]
  (handle service-with-handler input output context))
