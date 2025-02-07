# Apache Pulsar User-Defined Sink in Numaflow with Springboot for local development

This project is a Numaflow pipeline with 3 vertices where the 3rd vertex is an Apache Pulsar User-Defined Sink.
To implement the image for the User Defined Sink, Spring Boot is used 
BIn this project, we are implementing a custom UDsink to support Apache Pulsar since this does not currently exist. This is meant to be the playground before this project is implemented in https://github.com/numaproj-contrib/apache-pulsar-java which will allow for Numaflow to have built in support for Apache Pulsar.

##### What is Numaflow
An open-source tool specifically designed to handle and process large streams of data with low latency efficiently. Numaflow provides a framework that allows developers to create data stream processing pipelines, which filter, transform, and aggregate data as it flows from sources to sinks. The primary goal of Numaflow is to enable the development of scalable and fault-tolerant data processing applications that can handle large volumes of data with minimal delay.

#####  What is Apache Pulsar
[Apache Pulsar](https://pulsar.apache.org/) is a distributed, open source pub-sub messaging and streaming platform for real-time workloads. Some notable features include multiple subscription types, guaranteed message delivery, native support for multiple clusters in a Pulsar instance with geo-replication of messages across clusters.

Use cases: https://pulsar.apache.org/case-studies/

## Numaflow Pipeline

This project uses the [simple-sink pipeline](https://github.com/numaproj/numaflow/blob/main/examples/1-simple-pipeline.yaml) which has 3 vertices.

**Vertex 1:** [Generator Source](https://numaflow.numaproj.io/user-guide/sources/generator/) - in the Pipeline.yaml file users choose the rate, and length of message, and the generator source will just keep "logging" messages based on that.

**Vertex 2:** [Cat](https://numaflow.numaproj.io/user-guide/user-defined-functions/map/builtin-functions/cat/) - A built-in UDF which does nothing but return the same messages it receives.

**Vertex 3:** UD Sink - Takes its received messages and publishes them to a topic in Apache Pulsar.
- **Sink:** The endpoint for processed data that has been outputted from the platform, which is then sent to an external system or application. The purpose of the Sink is to deliver the processed data to its ultimate destination
- **User-defined sink:** A custom Sink that a user can write using Numaflow SDK when the user needs to output the processed data to a system or using a certain transformation that is not supported by the platform's built-in sinks

## How this project works


#### 1) Docker Compose

When docker-compose up is ran, it starts up Pulsar and Pulsar Manager inside a Docker container.
The Docker Compose file creates two services: pulsar and pulsar-manager, each based on  Docker images.
Docker checks whether these images are already present locally on your computer. If they are not found locally, Docker automatically downloads them from the Docker Hub registry or the specified registry before creating containers based on these images.
It exposes ports 6650 (for clients to connect to the Pulsar service) and 8080 (for the HTTP server) on the host machine. 

The pulsar-manager service uses the Pulsar Manager image for managing and monitoring the Pulsar instance. 
It maps ports 9527 and 7750 from the container to the host for accessing its web interface.

Locally we can use: http://localhost:9527/#/login?redirect=%2F to see the Pulsar Manager UI. 
 - Create credentials with curl command
 - Create new environement. 
   - Need to use http://pulsar:8080 for both the service and bookies URL
     - Why? IDK. Do this based on: https://pulsar.apache.org/docs/4.0.x/getting-started-helm/#step-4-use-pulsar-manager-to-manage-the-cluster

Both pulsar manager and pulsar are mapped from its port in the container to a port on the local machine.

#### 2) mvn clean install
Once we have Pulsar up, we can start our Spring Boot application by running mvn clean install. 
`mvn clean install` cleans the project directory by remving all files generated in previous builds, and then proceeds to default lifecycle
which includes compiling,running tests, packaging, and installing 
Maven plugins will also run.
The jib-maven-plugin builds the Docker container image and is configured to do so during the package phase.
**Image Building Configuration**
* From Image: amazoncorretto:17
  * This is the base image used to create your Docker image.
* Main Class: com.jacque1ine.SpringPulsarApplication
  * This specifies the main class to use when running the container.
* To Image: my-sink:v0.0.3
  * This is the target image and tag that will be created

#### 3) Apply pipeline 
Now that the image is built, we can use this image for our UDSink.
The sink named out is configured to use a user-defined sink (udsink) based on a container image.
The specified image for the sink container is my-sink:v0.0.3 (which is what we named the image built from the plugin).
This image should be built and present locally on all Kubernetes nodes where the pipeline might execute, 
since the imagePullPolicy is set to Never. 
If the image is not locally available, the container will fail to start.


## Commands

Pulsar:
- ` docker-compose up`

Numaflow:
*     mvn clean install
*     kubectl apply -f pipeline.yaml
*     kubectl -n numaflow-system port-forward deployment/numaflow-server 8443:8443

Pulsar Manager set up account:
````
  CSRF_TOKEN=$(curl http://localhost:7750/pulsar-manager/csrf-token)
curl \
-H "X-XSRF-TOKEN: $CSRF_TOKEN" \
-H "Cookie: XSRF-TOKEN=$CSRF_TOKEN;" \
-H "Content-Type: application/json" \
-X PUT http://localhost:7750/pulsar-manager/users/superuser \
-d '{"name": "admin", "password": "apachepulsar", "description": "test", "email": "username@test.org"}'
````

## Why use Spring Boot? 
**Dependency Injection (`@Autowire`d):**
Spring Boot's dependency injection mechanism enables clean and straightforward configuration of project components. 
Ex: `EventPublisher` is injected into `NumaflowSink`, and `PulsarTemplate` is injected into `EventPublisher`, reducing component coupling and simplifying system management and scalability.

**Component-based Architecture (`@Component`):**
Using` @Component` annotations, Spring Boot automatically detects and registers beans, easing the setup and configuration of `NumaflowSink` and `EventPublisher`. 
These components are integrated into the Spring context, benefiting from the full support of the Spring framework, including lifecycle management and post-processing.

**Configuration Management (`@Value`):**
Spring Boot supports externalizing configurations and accessing them via the `@Value `annotation. 
For example, the topic name (topicName1) in the EventPublisher class is externalized, enhancing the flexibility and environment-independence of the application.
Also, keeps the codebase simpler and cleaner since the business logic is not muddled with configuration details

**Lifecycle Management (`@PostConstruct`):**
The @PostConstruct annotation defines startup behavior in a thread-safe manner. 
For example, in `NumaflowSink` to start the server and in `EventPublisher` to initialize and validate the state of `PulsarTemplate`, ensuring that components are ready for use upon initialization.

While using Spring Boot for this project provided lots of benefits there were also tradeoffs compared to if we just used native Java. 
These include larger memory consumption, learning curve of spring boot, and challenges with
debugging issues because of the layers of abstraction and the magic of auto-configuration, making it sometimes hard to trace and fix bug.

Nonetheless, the benefits Spring Boot provided outweighed its cons, and was used for the project. 

## Challenges / Errors 


## "this.publisher" is null
`java.lang.NullPointerException: Cannot invoke "com.jacque1ine.producer.EventPublisher.publishPlainMessage(String)" because "this.publisher" is null
`

**DIDNT WORK -**
**Manually instantiated instance (`new NumaflowSink()`):**

Was previously creating a new server like this: `server = new Server(new NumaflowSink());`
The server class needs to be passed a Sinker to be created.

This instance is manually created and not managed by Spring.
Since it is not managed by Spring, it won't have any of its dependencies (like EventPublisher) injected. 
So, the publisher field remains null, leading to the `NullPointerException`.

**WORKED - Spring-managed instance (this)** 

In this approach, this refers to the current instance of NumaflowSink that has been created and managed by Spring. Thus, all dependencies marked with @Autowired inside this instance have already been injected by Spring. Therefore, when this instance (this) is passed to the Server constructor, the publisher field is not null, and it works as expected.The @PostConstruct annotated startServer method will be invoked after the Spring context is fully initialized, ensuring all dependencies are injected.
`server = new Server(this);` passes the fully-initialized Spring-managed NumaflowSink instance into the Server constructor.

In general:

**Spring-managed Beans**
* When you let Spring manage a bean, it takes care of creating an instance of the bean, initializing it, and injecting any dependencies that it requires. 
* Allows Spring to properly inject any other Spring-managed components into that bean, including fields annotated with @Autowired.


**Manually Instantiated Beans**

When you manually instantiate a bean using new, you bypass the entire Spring lifecycle. This means that Spring doesn’t get a chance to inject its dependencies into that instance.

---

## Extra_hosts configuration: host.docker.internal 
**Client**: requests resources or services from a server. 

**Server**: provides resources or services to users

In this project, the Numaflow source is the server, while Pulsar is the client. 
Each Numaflow vertex is running as a seperate pod in Kubernetes. 
The source is generating messages at a constant rating and sending them to the Sink. 
The sink then needs to take these messages, and get them into Pulsar so that Pulsar can publish these messages.
The problem is, Pulsar is running in a container locally. 
All the pods can communicate with each other since they are in the same cluster and can use Pod IP addresses but Pulsar is not running in a pod in a k8s cluster. 

So, was originally getting the following error:
```
o.a.pulsar.client.impl.ConnectionPool    : 
Failed to open connection to localhost/<unresolved>:6650 : 
org.apache.pulsar.shade.io.netty.channel.AbstractChannel$AnnotatedConnectException:
 finishConnect(..) failed: Connection refused: localhost/127.0.0.1:6650

````

### How I fixed: 
To get a Kubernetes pod to access a container running locally on a machine: 

**Docker Containers and Host Network:**

Docker facilitates communications between the containers and the host machine using special DNS entries like host.docker.internal.

Within the context of Docker and local development environments, "host-gateway" often specifically refers to the entry point through which Docker containers connect to the host machine's network and further to external networks.
I added `extra_hosts: - "host.docker.internal:host-gateway"` to the Docker Compose file, which explicitly declared for the Docker container running Pulsar to recognize and resolve host.docker.internal to the host machine's gateway.
This allows processes within the container to interact with services or processes running on the host machine itself, essentially bridging the gap between the isolated container environment and the host environment.

**Configuring the Pulsar Client in Kubernetes:**

In the service configuration for the Pulsar client in the Spring application running in a Kubernetes pod, the service URL was changed to pulsar://host.docker.internal:6650. This action directs the client to seek the local Pulsar service running in Docker at that address.

**Kubernetes Networking Configuration:**

For a Kubernetes pod to access host.docker.internal, host.docker.internal should route to the host machine on which the Kubernetes clusters (like Rancher, which often run on the same host as Docker in development environments) are running. 
Rancher has network configurations that internally resolve host.docker.internal to reach the host machine's network.

**How It Ties Together:**

When the Pulsar client inside the Kubernetes pod attempts to connect to pulsar://host.docker.internal:6650, it successfully resolves host.docker.internal to the host machine (the local computer), where Docker routes the traffic to the port 6650 on which the Pulsar service is listening. The configurations in the Docker Compose for port mapping and routing of host.docker.internal facilitate this process.
