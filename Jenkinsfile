node {
  try {
    stage('Preparation') {
        //notifyBuild('STARTED')
        checkout scm
        versionNumber = VersionNumber versionNumberString: '${BUILD_DATE_FORMATTED, "yy.MM"}.${BUILDS_THIS_MONTH}', versionPrefix: '', buildsAllTime: '12'
        echo "VersionNumber: ${versionNumber}"
    }
    stage('Build Gradle project') {
        sh "/usr/lib/gradle/4.8.1/bin/gradle clean build"
    }
    stage('Push Docker image') {
        sh "docker tag inomial.io/secore-template inomial.io/secore-template:${versionNumber}"

        // archive image
        sh "docker save inomial.io/secore-template:${versionNumber} | gzip > secore-template-${versionNumber}.tar.gz"

        // tag and push if tests pass (as $revision and as latest)
        sh "docker push inomial.io/secore-template:${versionNumber}"
        sh "docker push inomial.io/secore-template:latest"
    }
    stage('Results') {
        currentBuild.displayName = versionNumber
        archive '*.tar.gz'
        archive '*/build/libs/*.jar'
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
