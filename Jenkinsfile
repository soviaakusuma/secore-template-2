node {
  try {
    stage('Prepare') {
        //notifyBuild('STARTED')
        checkout scm
        versionNumber = VersionNumber versionNumberString: '${BUILD_DATE_FORMATTED, "yy.MM"}.${BUILDS_THIS_MONTH}', versionPrefix: '', buildsAllTime: '12'
        echo "VersionNumber: ${versionNumber}"
        timestamp = VersionNumber versionNumberString: '${BUILD_DATE_FORMATTED, "yyyy-MM-dd HH:mm:ss Z"}'
        echo "timestamp: ${timestamp}"
        gitBranch = sh(script: "git name-rev --name-only HEAD", returnStdout: true).trim()
        gitCommit = sh(script: "git rev-parse HEAD", returnStdout: true).trim()
    }
    stage('Build') {
        sh "echo ${versionNumber} > build.version"
        sh "/usr/lib/gradle/4.8.1/bin/gradle clean build"
    }
//    stage('Test') {
//        try {
//            sh "./test.sh"
//        } finally {
//            sh "docker-compose logs --no-color --timestamps > docker-compose.log 2>&1"
//            archive 'docker-compose.log'
//            sh "docker-compose run --rm -T testsql pg_dump -Fc stampede-schema > test.pgc"
//            archive 'test.pgc'
//            sh "docker-compose --no-ansi down --volumes --remove-orphans"
//        }
//    }
    stage('Tag') {
        // add tag
        sh "git tag -a \"${versionNumber}\" -m \"Jenkins build from ${gitBranch} commit ${gitCommit} on ${timestamp}\""

        // push tag
        sh "git push origin \"${versionNumber}\""
    }
    stage('Push Docker image') {
        sh "./push"
    }
//    stage('Publish to Maven') {
//        sh "gradle -P ssh.user=cruisecontrol upload"
//    }
    stage('Results') {
        currentBuild.displayName = versionNumber
        archive 'build/reports'
//        archive 'target/*.jar'
//        archive 'build/libs/*.jar'
//        archive '*.tar.gz'
    }
  } catch (e) {
    // If there was an exception thrown, the build failed
    currentBuild.result = "FAILED"
    throw e
  } finally {
    // Success or failure, always send notifications
    notifyBuild(currentBuild.result)
    // cleanup workspace
    step([$class: 'WsCleanup'])
    // FIXME: cleanup images
  }
}

def notifyBuild(String buildStatus = 'STARTED') {
  // build status of null means successful
  buildStatus =  buildStatus ?: 'SUCCESSFUL'
 
  // Override default values based on build status
  if (buildStatus == 'STARTED') {
    color = 'YELLOW'
    colorCode = '#FFFF00'
  } else if (buildStatus == 'SUCCESSFUL') {
    color = 'GREEN'
    colorCode = '#00FF00'
  } else {
    color = 'RED'
    colorCode = '#FF0000'
  }

  // send notification if this build was not successful or if the previous build wasn't successful
  if ( (currentBuild.previousBuild != null && currentBuild.previousBuild.result != 'SUCCESS') || buildStatus != 'SUCCESSFUL') {
    slackSend (color: colorCode, message: "${buildStatus}: ${env.JOB_NAME} [<${env.BUILD_URL}|${env.BUILD_NUMBER}>]")
  
    emailext (
      subject: '$DEFAULT_SUBJECT',
      body: '$DEFAULT_CONTENT',
      recipientProviders: [
          [$class: 'CulpritsRecipientProvider'],
          [$class: 'DevelopersRecipientProvider'],
          [$class: 'RequesterRecipientProvider']
      ], 
      replyTo: '$DEFAULT_REPLYTO',
      to: '$DEFAULT_RECIPIENTS, cc:builds@inomial.com'
    )
  }
}
