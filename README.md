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

- copy `secore-template` from github 
- also copy `secore-template/.gitignore` from github
- rename the source directory (ie, from `src/main/java/com/inomial/template`)
- change package name in `Main.java`
- change `settings.gradle` to set the project name
- change `mainClassName` in `build.gradle`
- replace `$SERVICE` definition in `mkdocker`
- replace `$SERVICE` definition in `Dockerfile`
- replace `secore-template` in `docker-compose.yml`
- if needed, [reserve TCP ports on the local Mac OS X host interface](https://wiki.inomial.net/home/devstack_host_ports)
  for the HTTP and JVM debug listening ports (8080 and 9009 respectively). This will allow you to connect your IDE's
  native debugger to debug the microservice, and use dcurl to invoke the microservice's HTTP REST API.
- if mapping port 8080 to the host interface, you may also want to update the `DEVSTACK_REDIRS` list in the
  `bin/dcurl` script in the `inomial/tools` repository, so that `dcurl` will recognise your microservice.
- update `Jenkinsfile` to change the project name
- run "./gradlew clean" to get rid of any secore artefacts (also check in docker/)
- build and run the microservice: `./run`
- To use GROW, uncomment the lines in Dockerfile, entrypoint.sh and build.gradle.
- update `README.md` to remove these instructions :)

There is plenty of room to improve the template (and this documentation);
please take a moment to make things better than you found them.

### Grow support

If your microservice needs to use Grow to manage its DB schema and objects,
then there are commented-out sections inside the `settings.gradle`,
`build.gradle` and `docker/entrypoint.sh` scripts that need to be uncommented
so you can integrate Grow into your application.

### Adding functionality

Functionality is added to the template by updating the Main method. You can
add servlets, message consumers and more. See the
[cheat sheet](https://github.com/inomial/secore) for more information.

### Packaging and Namespaces

Microservices developed for use by all customers should be packaged into the
`com.inomial` package. For example, the rate card engine is in `com.inomial.rating.jackpot`.

Microservices developed specifically for customers should be packaged into
the `net.inomial` package, using the same name used for their web service.
For example, `net.inomial.example`. In general, you shouldn't shorten
the customer name.

### Repository naming

Be consistent! Microservices developed for customers should be put into a
repository using the package name, e.g. `inomial/net.inomial.example`, and
all other names for customer-specific packages should follow this convention (eg
docker container names, etc)

Internal application repositories can be named consistent with the service name.

### Starting applications

Use the `./run` script as a one-stop-shop command to compile your project,
build a docker image and container, and run the microservice.

The microservice will run in the foreground of the terminal it was launched from.
  * Press Ctrl-C to terminate the microservice and return to the shell prompt.
  * Press Ctrl-P,Ctrl-Q to detach the microservice from foreground and leave running.
  * Press Ctrl-\ to obtain an instant thread/heap report from the microservice JVM.

The microservice's docker container by default will take on a name of the following format:

  ```
  ${USER}_${SERVICE}_1
  ```

where:
  * `${USER}` is your UNIX username (this is to distinguish your built container
    from the `devstack_xxxx_1` containers).
  * `${SERVICE}` is the name of the project.

The `./run` script also supports some command-line arguments that can perform additional
tasks or set JVM system properties or performance settings when launching your microservice,
in order to spare you from having to edit build script files for these common tasks.

Type `./run --help` to see a list of supported command-line arguments.

Type `./run clean` to force Gradle to execute the `clean` task before building.

Type `./run debug` to make the microservice JVM pause on startup until a remote
debugger connects to the port `9009/tcp` inside the docker container (you will
need to make a port mapping in `docker-compose.yml` to expose this port on the
host machine if you want to connect an IDE debugger such as Eclipse or
IntelliJ). This can be useful if you need to set a breakpoint inside the
`main()` method.

Type `./run -Dname.of.system.property=value ...` to set JVM system properties
upon startup for this invocation of the microservice *only*. This can be useful
if you want to enable verbose or diganostic logging on a library your
microservice uses.

#### Launching the project from out-of-directory

The `run` script will happily work from any current directory; it'll simply figure out
the relative paths to its requisite inputs and launch the commands with the correct
paths. For instance, you could launch a new microservice that you haven't yet checked
out from GitHub with just two shell commands:

  ```sh
  git clone https://github.com/inomial/secore-template.git /path/to/working/copy/secore-template
  /path/to/working/copy/secore-template/run
  ```
