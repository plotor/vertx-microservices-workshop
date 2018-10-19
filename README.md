# Vert.x - From zero to (micro-) hero.

This repository is a lab about vert.x explaining how to build distributed _microservice_ reactive applications using
Vert.x.

Instructions are available on http://escoffier.me/vertx-hol

Complete code is available in the `solution` directory.

The application is composed of a set of microservices:

- __quote-generator__： this is an absolutely unrealistic simulator that generates the quotes for 3 fictional companies MacroHard, Divinator, and Black Coat. The market data is published on the Vert.x event bus.
- __compulsive-traders__: these are a set of components that receives quotes from the quote generator and decides whether or not to buy or sell a particular share. To make this decision, they rely on another component called the portfolio service.
- __portfolio-service__: this service manages the number of shares in our portfolio and their monetary value. It is exposed as a service proxy, i.e. an asynchronous RPC service on top of the Vert.x event bus. For every successful operation, it sends a message on the event bus describing the operation. It uses the quote generator to evaluate the current value of the portfolio.
- __audit-service__: that’s the legal side, you know…​ We need to keep a list of all our operations (yes, that’s the law). The audit component receives operations from the portfolio service via an event bus and address . It then stores theses in a database. It also provides a REST endpoint to retrieve the latest set of operations.
- __trader-dashboard__: some UI to let us know when we become rich.

## Teasing

Vert.x is a toolkit to create reactive distributed applications running on the top of the Java Virtual Machine. Vert.x
exhibits very good performances, and a very simple and small API based on the asynchronous, non-blocking
development model.  With vert.x, you can developed microservices in Java, but also in JavaScript, Groovy, Ruby and
Ceylon. Vert.x also lets you interact with Node.JS, .NET or C applications.  

This lab is an introduction to microservice development using Vert.x. The application is a fake _trading_
application, and maybe you are going to become (virtually) rich! The applications is a federation of interaction microservices packaged as _fat-jar_ and creating a cluster.

## Content

 * Vert.x
 * Microservices
 * Asynchronous non-blocking development model
 * Composition of async operations
 * Distributed event bus
 * Database access
 * Providing and Consuming REST APIs
 * Async RPC on the event bus
 * Microservice discovery

## Want to improve this lab ?

Forks and PRs are definitely welcome !

## Building

To build the code:

```bash
mvn clean install
```

To build the documentation:

```bash
cd docs
docker run -it -v `pwd`:/documents/ asciidoctor/docker-asciidoctor "./build.sh" "html"
# or for fish
docker run -it -v (pwd):/documents/ asciidoctor/docker-asciidoctor "./build.sh" "html"
```