Pedestal for AWS Lambda
=======================

This library provides a [Pedestal](http://pedestal.io) chain provider
implementation for use with services which run on AWS Lambda, using
Amazon API Gateway.

Usage
-----

See the `example/` directory for a project running a simple HTTP handler.

``` clojure
(require '[io.pedestal.http :as http])
(require '[me.joshkingsley.pedestal.lambda :refer [api-gateway-provider handle]])

;; The service map can be created with the `:io.pedestal.http/chain-provider`
;; key to initialize the service with the API Gateway provider.
(def service {::http/routes #{,,,}
              ::http/chain-provider api-gateway-provider})

;; Adds the default interceptors and Lambda handler fn to the service map.
(def service-with-handler (http/create-provider service))

;; Executes the interceptor chain and streams the response back to API Gateway.
(handle service-with-handler input output context)
```

Development
-----------

### Local integration testing

Install the following tools:

* Docker
* [AWS SAM CLI](https://docs.aws.amazon.com/lambda/latest/dg/sam-cli-requirements.html)

Build the test handler and run the local API Gateway:

```
$ lein with-profile local-dev uberjar
$ sam local start-api -t dev-sam-template.yml
```

To Do
-----

- [ ] Error handling
- [ ] Improve API
- [ ] Tests
