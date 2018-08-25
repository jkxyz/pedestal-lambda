Overview
--------

This example shows usage of the API Gateway chain provider with the
`RequestStreamHandler` interface to provide a HTTP service which can
be run on AWS Lambda.

The `template.yml` file is a CloudFormation template which can be used
for local testing using the AWS SAM CLI.

Running the example
-------------------

Running the example requires the [AWS SAM CLI](https://docs.aws.amazon.com/lambda/latest/dg/sam-cli-requirements.html),
Docker, and Leiningen.

```
$ lein uberjar
$ sam local start-api
$ curl localhost:3000
# => Hello World!
```
