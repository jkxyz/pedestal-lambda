(ns me.joshkingsley.pedestal.lambda
  (:require [clojure.data.json :as json]
            [clojure.string :as string]
            [ring.util.request]
            [ring.util.response]
            [io.pedestal.interceptor.chain :as interceptor.chain]
            [io.pedestal.interceptor :as interceptor]
            [io.pedestal.http.impl.servlet-interceptor])
  (:import (java.io InputStream
                    OutputStream
                    ByteArrayInputStream
                    ByteArrayOutputStream
                    InputStreamReader
                    BufferedReader
                    OutputStreamWriter)
           com.amazonaws.services.lambda.runtime.Context))

(defn- event-body-stream [event]
  (ByteArrayInputStream. (.getBytes ^String (or (get event "body") "") "UTF-8")))

(defn- event-headers [event]
  (persistent!
   (reduce
    (fn [hs [k v]] (assoc! hs (string/lower-case k) v))
    (transient {})
    (get event "headers"))))

(defn- event-query-string [event]
  (let [params (or (get event "queryStringParameters") {})]
    (string/join "&" (reduce (fn [ps [k v]] (conj ps (str k "=" v))) [] params))))

(defn- event-remote-addr [event]
  (get-in event ["requestContext" "identity" "sourceIp"]))

(defn- event-request-method [event]
  (keyword (string/lower-case (get event "httpMethod"))))

(defn- event-uri [event]
  (get-in event ["requestContext" "path"]))

(defn- headers-content-type [headers]
  (ring.util.request/content-type {:headers headers}))

(defn- headers-content-length [headers]
  (ring.util.request/content-length {:headers headers}))

(defn- headers-character-encoding [headers]
  (ring.util.request/character-encoding {:headers headers}))

(defn- headers-protocol [headers]
  (str "HTTP/" (first (string/split (get headers "via" "1.1") #" " 2))))

(defn- apigw-event->request
  "Creates a Ring request map from an API Gateway event object."
  [event]
  (let [headers (event-headers event)
        content-type (headers-content-type headers)
        content-length (headers-content-length headers)
        character-encoding (headers-character-encoding headers)
        request {:async-supported? false
                 :body (event-body-stream event)
                 :headers headers
                 :path-info (get event "path")
                 :protocol (headers-protocol headers)
                 :query-string (event-query-string event)
                 :remote-addr (event-remote-addr event)
                 :request-method (event-request-method event)
                 :server-name (get headers "host")
                 :server-port (get headers "x-forwarded-port")
                 :scheme (get headers "x-forwarded-proto")
                 :uri (event-uri event)}]
    (cond-> request
      content-type (assoc :content-type content-type)
      content-length (assoc :content-length content-length)
      character-encoding (assoc :character-encoding character-encoding))))

(defn- write-output [^OutputStream output apigw-response]
  (with-open [output-writer (OutputStreamWriter. output "UTF-8")]
    (json/write apigw-response output-writer)))

(defn- response-body-as-string [body]
  (let [body-stream (ByteArrayOutputStream.)]
    (io.pedestal.http.impl.servlet-interceptor/write-body-to-stream body body-stream)
    (.toString body-stream "UTF-8")))

(defn- ring-response->apigw-response [{:keys [status headers body]}]
  {:statusCode status
   :headers headers
   :body (response-body-as-string body)})

(defn- default-content-type [body]
  (io.pedestal.http.impl.servlet-interceptor/default-content-type body))

(defn- set-default-content-type [response]
  (if (ring.util.response/get-header response "content-type")
    response
    (ring.util.response/content-type response
                                     (default-content-type (:body response)))))

(defn- leave-apigw-response [{response :response :as context}]
  (write-output (::output-stream context)
                (ring-response->apigw-response
                 (set-default-content-type response))))

(def ^:private apigw-response
  (interceptor/interceptor {:name ::apigw-response
                            :leave leave-apigw-response}))

(defn- enter-ring-request [{event :apigw-event :as context}]
  (assoc context :request (apigw-event->request event)))

(def ^:private ring-request
  (interceptor/interceptor {:name ::ring-request
                            :enter enter-ring-request}))

(defn- terminator-inject [context]
  (interceptor.chain/terminate-when context
                                    #(ring.util.response/response? (:response %))))

(def ^:private terminator-injector
  (interceptor/interceptor {:name ::terminator-injector
                            :enter terminator-inject}))

(defn- read-input [^InputStream input]
  (with-open [reader (BufferedReader. (InputStreamReader. input))]
    (json/read reader)))

(defn- handler-fn [interceptors default-context]
  (fn [^InputStream input ^OutputStream output ^Context context]
    (let [event (read-input input)
          context (merge default-context
                         {:apigw-event event
                          :apigw-context context
                          ::output-stream output})]
      (interceptor.chain/execute context interceptors))))

(defn- apigw-handler-fn
  ([interceptors] (apigw-handler-fn interceptors {}))
  ([interceptors default-context]
   (handler-fn (concat [ring-request
                        terminator-injector
                        apigw-response]
                       interceptors)
               default-context)))

(defn api-gateway-provider [{interceptors :io.pedestal.http/interceptors
                             :as service-map}]
  (assoc service-map ::handler-fn (apigw-handler-fn interceptors)))

(defn handle [service-map input output context]
  ((::handler-fn service-map) input output context))
