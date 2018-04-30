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
- rename the source directory (ie, from `src/main/java/com/inomial/template`)
- change package name in `Main.java`
- change `settings.gradle` to set the project name
- change `mainClassName` in `build.gradle`
- replace `$SERVICE` in `mkdocker`
- replace `$SERVICE` in `Dockerfile`
- replace `secore-template` in `docker-compose.yml`
- update `Jenkinsfile` to change the project name
- run "gradle clean" to get rid of any secore artefacts (also check in docker/)
- build and run the microservice: `gradle up`
- update `README.md` to remove these instructions :)

There is plenty of room to improve the template (and this documentation);
please take a moment to make things better than you found them.

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

The gradle build file contains the task "up" which will compile the code,
build the docker image, and run it with docker-compose. So you can build and
run the whole project with a single command:

    gradle up
