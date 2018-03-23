# Secore-template

This is a repository containing a working Java SE microservice, intended to
be used with secore.

This contains a complete but useless application.

To use this template for your own project, follow these very simple steps:

- copy secore-template from github 
- rename the source directory (ie, from src/main/com/inomial/template)
- change settings.gradle to set the project name
- change mainClassName in build.gradle
- replace $SERVICE in mkdocker
- replace $SERVICE Dockerfile
- replace "secore-template" in docker-compose.yml
- update Jenkinsfile to change the project name
- start the microservice: `gradle up`

I'm sure the template can be improved, if you use the tempalte and have time
to progress it, please do. Remember to update the README with the instructions.

If you have questions that you get answered, add the information here for
others to learn from.

# Starting applications

The gradle build file contains the task "up" which will compile the code,
build the docker image, and run it with docker-compose. So you can build and
run the whole project with a single command:

    gradle up
