# Secore-template

This is a repository containing a working Java SE microservice, intended to
be used with secore.

### secore cheat sheet

The [secore cheat sheet](https://github.com/inomial/secore) contains quick
snippets to get you up and running with secore. Additional information about
the various secore modules can be found via the links in the cheat sheet.

### Using the template

This template contains a complete (but useless) secore application.

To use this template for your own project, follow these very simple steps:
- Clone `secore-template` from github (exclude secore-template-test dir).
  You can use `-o template` in the clone command to use `template` as the remote
  name instead of `origin`. This will allow you to keep the template repo as a secondary upstream to pull future updates.
- Make sure you also copy .gitignore and .env from secore-template.
- Delete secore-template-test dir

#### Inomial Specific
- Move main class to package for project `com.inomial.<project>` eg `com.inomial.rating.common` for rating-common.
- Change `settings.gradle` to set the project name.
- Change `build.gradle` to set the project description.
- Replace `secore-template` in `docker-compose.yml`.
- Replace `secore-template` in `docker-compose-test.yml`.
- Run `./gradlew clean` to get rid of any secore artefacts (also check in docker/).
- Delete Telflow files: `rm -r pom.xml docker/Dockerfile.telflow src/main/java/com/inomial/secore/template/ConsulApplication.java src/main/resources`.
- If the service is not using grow (does not have a database) delete the related files: `rm -r test.sh testsql.sh postgres-init.sh docker-compose-test.yml .env src/test/sql/ src/main/grow/`
- Build and run the microservice: `./gradlew build` and then `docker-compose up`.
- Uncomment the lines in `build.gradle` if you want to use grow.
- Uncomment the line in `Dockerfile` if QL needs to access the database of this microservice. 
- Update `README.md` to remove these instructions :)

#### Telflow Specific
- Move main class to package for project `com.telflow.<project>` eg `com.telflow.analytics`  for analytics.
- Update `pom.xml`:
  - Update the parent pom version to the latest tagged version of assembly-parent-container.
  - Set `«define name»` and `«define description»` to the project name & description.
  - Change `component-name` to the component name. This is the name used for the main directory in `/opt/telflow`.
  - Update `mainClass` to the new main class.
- Build with `mvn clean install`.
- Run with `mvn exec:java`.
- Delete Inomial files: `rm -r *gradle* *Jenkins* docker/Dockerfile run push docker-compose-test.yml mkdocker test*.sh postgres-init.sh build.version`.
- Update `README.md` to describe the new application :)

There is plenty of room to improve the template (and this documentation);
please take a moment to make things better than you found them.

### Grow support (Inomial)

Including a grow dependency such as sql-core or wrangler-client will now
work automatically.

### JVM and HTTP Debugging (Inomial)

If you want to allow debugging,
[reserve TCP ports on the local Mac OS X host interface](https://wiki.inomial.net/home/devstack_host_ports)
for the HTTP and JVM debug listening ports (8080 and 9009 respectively). This will allow you to connect your IDE's
native debugger to debug the microservice.

Notes:
- these ports will not be available in production
- HTTP rest interfaces should always be tested via nginx. Curl will not generally
  provide enough context to successfully operate.
- if you really want to port 8080 to the host interface, you may also want to update the `DEVSTACK_REDIRS` list in the
  `bin/dcurl` script in the `inomial/tools` repository, so that `dcurl` will recognise your microservice.

### Adding functionality

Functionality is added to the template by updating the Main method. You can
add servlets, message consumers and more. See the
[cheat sheet](https://github.com/inomial/secore) for more information.

### Packaging and Namespaces (Inomial)

Microservices developed for use by all customers should be packaged into the
`com.inomial` package. For example, the rate card engine is in `com.inomial.rating.jackpot`.

Microservices developed specifically for a customer should be packaged into
the `net.inomial` package, using the same name used for their web service.
For example, `net.inomial.example`. In general, you shouldn't shorten
the customer name.

### Repository naming (Inomial)

Be consistent! Microservices developed for customers should be put into a
repository using the package name, e.g. `inomial/net.inomial.example`, and
all other names for customer-specific packages should follow this convention (eg
docker container names, etc)

Internal application repositories can be named consistent with the service name.

### Starting applications (Inomial)

Run `./gradlew build` to build a docker image and container and compile your project 
and then run `docker-compose up` to run the microservice.

The microservice will run in the foreground of the terminal it was launched from.
  * Press Ctrl-C to terminate the microservice and return to the shell prompt.
  * Press Ctrl-P,Ctrl-Q to detach the microservice from foreground and leave running.
  * Press Ctrl-\ to obtain an instant thread/heap report from the microservice JVM.

### Collecting code coverage metrics (Inomial)

You can also profile your microservice for code coverage using the
[JaCoCo](https://www.jacoco.org/) framework.

Launching your microservice with `docker-compose up` will start it with the JaCoCo JVM
agent enabled. Run your integration test suite against your microservice to collect
code coverage data. When done, shut down your microservice either by pressing Ctrl-C
from the controlling terminal or otherwise stopping the docker container.

The JaCoCo execution data will be saved to `docker/agentlib/jacoco.exec` when the
microservice JVM terminates. You can import the data into [EclEmma](https://www.eclemma.org/)
in Eclipse for visualisation purposes, or use [JaCoCo's reporting tools](https://www.jacoco.org/jacoco/trunk/doc/cli.html).

The JaCoCo agent can also run in TCP server/client mode, this is controlled by the
javaagent parameters in the JAVA_OPTS environment variable passed to the container by
docker-compose.yml.
See the [JaCoCo Java agent docs](https://www.eclemma.org/jacoco/trunk/doc/agent.html) for more options.

**Note:** Prior to importing the `jacoco.exec` file into EclEmma you will need to set
the output folder for the `src/main/java` source folder in your microservice project's
*Build Path* configuration in Eclipse to `build/classes/java/main` (same output
directory that Gradle uses) since EclEmma needs to refer to the **exact** same `.class`
files that were loaded inside the JVM during the coverage profiling run.

If you neglect to do this then the coverage will erroneously show up as 0% (all source
lines highlighted in red).

**Note 2:** It may also help to temporarily turn off automatic background recompilation
in Eclipse (*Project* menu -> uncheck *Build Automatically*) when using EclEmma to
prevent Eclipse from overwriting Gradle's output .class files if you alias both their
output folders to be the same directory.
