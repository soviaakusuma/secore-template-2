node {
    stage('Preparation') {
        checkout scm
        versionNumber = VersionNumber versionNumberString: '${BUILD_DATE_FORMATTED, "yy.MM"}.${BUILDS_THIS_MONTH}', versionPrefix: '', buildsAllTime: '12'
        echo "VersionNumber: ${versionNumber}"
    }
    stage('Build Gradle project') {
        sh "gradle clean build"
    }
    stage('Build Docker image') {
        sh "./mkdocker"
    }
    stage('Push Docker image') {
        sh "docker tag inomial.io/secore-template inomial.io/secore-template:${versionNumber}"

        // archive image
        sh "docker save inomial.io/secore-template:${versionNumber} | gzip > secore-template-${versionNumber}.tar.gz"

        // tag and push if tests pass (as $revision and as latest)
        sh "docker push inomial.io/secore-template:${versionNumber}"
        sh "docker push inomial.io/secore-template:latest"

        // send email/slack
        // cleanup images
    }
    stage('Results') {
        currentBuild.displayName = versionNumber
        archive '*.tar.gz'
        archive '*/build/libs/*.jar'
        // cleanup workspace
        step([$class: 'WsCleanup'])
    }
}
