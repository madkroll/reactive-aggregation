Aggregation Service Guideline
---

# Description
This API service aggregates data from multiple data providers, merges it into single response
and exposes back to client. 

# Technology stack
- Java 11
- Spring Boot
- Project Reactor
- Lombok
- JUnit 4
- AssertJ
- Mockito
- okhttp3
- Testcontainers

# Comments
## First story
First story seemed very reactive by its nature - so I decided to go for Project Reactor and Spring Boot.
It allows designing flows in a non-blocking, asynchronous manner. As result, in theory,
it helps to utilize CPU most efficiently. Which practically saves costs.
This first solution was pretty straight forward and looked simple.

In general, there are some gaps present - it's possible to make it more robust, improve test coverage, improve performance, etc.
My point was to demonstrate different approaches, skills and concepts.

## Second and third stories
These feature requests introduce queues and buffers. In fact, it discards some advantages of reactive approach.
It introduced some parts of blocking behavior.
In the end, most challenging was to:
- share buffer / batches across multiple requests
- then map results from a common stream of responses back to original requests

My solution is to:
- feed all incoming queries into single shared queue
- connect ConnectableFlux to it, so pipeline is able to pull incoming queries
- setup buffering (batch capacity / time frame)
- request data from backend provider as soon as new batch is available
- subscribe all awaiting requests on this shared stream of responses, so each subscriber
can filter only results matching original queries

# TODO
Things I considered as out-of-scope for this assignments, but usually I would create new stories for further work:
- validate input parameters (using javax.validation). Match country codes with ISO-2
- perform load testing / stress testing
- tune data providers settings (timeouts, batch size, queue size / disposal strategy) to adapt best to real load pattern
- tune retry logic to adjust it according to real backend systems, make sure our service does not overload backend
- check if caching is suitable for this service(considering how often data is updated per service)
- rate limiting should probably work across cluster of running instances, so bucket4j seems as a good option
- add health status endpoint

# Build and unit testing
```shell script
cd $PROJECT_DIR
mvn clean install
```

# Run integration tests
There is only one integration test. I liked this approach with stubs for backend services in containers.
So decided to re-use it inside my integration tests by running under Testcontainers utility.

Important: as result it requires preinstalled docker.
```shell script
cd $PROJECT_DIR 
mvn verify -P integration
```

# Start application
To run application as jar file:
```shell script
cd $PROJECT_DIR
chmod +x fedex-web/target/fedex-web-1.0.0-SNAPSHOT.jar
java -jar fedex-web/target/fedex-web-1.0.0-SNAPSHOT.jar
```

In case if building application from sources is not possible, there is a jar already available under bin folder:
```shell script
cd $PROJECT_DIR
java -jar bin/fedex-web-1.0.0-SNAPSHOT.jar
```

By default API expects backend services to run on localhost:80.
This possible to change by updating settings in application.yml (and rebuilding project) or by passing system properties.

# Execute query
```shell script
curl -v "http://localhost:8080/aggregation?pricing=NL&track=123456789&shipments=987654321"
```

Task correction:
>To solve this, we want the service queues to also be
sent out within 5 seconds of the oldest item being inserted into the queue.

Means if we insert elements every 4 seconds - 5 elements going to be sent after 20 seconds.
Does not seem as intended behavior. Sending all we have every 5 seconds - looks as a more correct approach.