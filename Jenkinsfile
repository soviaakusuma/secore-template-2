pipeline {
    agent any

    environment {
        // request specific Java version
        JAVA_HOME = "${tool 'jdk11'}"
        PATH      = "${env.JAVA_HOME}/bin:${env.PATH}"
    }

    triggers {
//        pollSCM('H/10 * * * *')
//        cron(BRANCH_NAME == "master" ? "H 14 * * 2" : "")
        upstream(upstreamProjects: "secore PUBLISH/master", threshold: hudson.model.Result.SUCCESS)
    }

    stages {
        stage('Prepare') {
            steps {
                script {
                    if (env.BRANCH_NAME == 'master') {
                        versionNumber = VersionNumber versionNumberString: '${BUILD_DATE_FORMATTED, "yy.MM"}.${BUILDS_THIS_MONTH}', versionPrefix: ''
                        versionNumberInt = VersionNumber versionNumberString: '${BUILD_DATE_FORMATTED, "yyMM"}${BUILDS_THIS_MONTH, XXX}', versionPrefix: ''
                        currentBuild.displayName = versionNumber
                    } else {
                        versionNumber = env.BUILD_NUMBER
                        versionNumberInt = 'null'
                    }
                    timestamp = VersionNumber versionNumberString: '${BUILD_DATE_FORMATTED, "yyyy-MM-dd HH:mm:ss Z"}'
                    gitCommit = sh(script: "git rev-parse HEAD", returnStdout: true).trim()
                    gradleVersion = sh(script: "awk -F= '\$1==\"distributionUrl\"{print\$2}' gradle/wrapper/gradle-wrapper.properties|sed -n 's/.*\\/gradle-\\(.*\\)-bin\\.zip\$/\\1/p'", returnStdout: true).trim()
                    gradle = "/usr/lib/gradle/${gradleVersion}/bin/gradle"
                }
                echo "Building from commit ${gitCommit} in ${BRANCH_NAME} branch at ${timestamp}"
                echo "VersionNumber: ${versionNumber} / ${versionNumberInt}"
                echo "GradleVersion: ${gradleVersion}"
                sh "if [ ! -x \"$gradle\" ]; then echo \"Gradle version not available, try: \$(ls -1 /usr/lib/gradle/|tr '\n' ' ')\"; exit 1; fi"
                sh 'java -version'
            }
        }
        stage('Build') {
            steps {
                sh "if [ -d src/main/grow ] && [ '${versionNumberInt}' != null ]; then echo ${versionNumberInt} > src/main/grow/package.version; fi"
                sh "echo ${versionNumber} > build.version"
                sh "${gradle} clean build"
            }
        }
        stage('Test') {
            steps {
                sh "./test.sh"
            }
            post {
                always {
                    archiveArtifacts artifacts: '**/test-results/**'
//                    junit '**/build/test-results/**/*.xml'
                }
            }
        }
        stage('Tag') {
            when {
                branch 'master'
            }
            steps {
                // add tag
                sh "git tag -a \"${versionNumber}\" -m \"Jenkins build from ${BRANCH_NAME} commit ${gitCommit} on ${timestamp}\""

                // push tag
                sh "git push origin \"${versionNumber}\""
            }
        }
        stage('Release') {
            when {
                branch 'master'
            }
            parallel {
//                stage('Publish to Maven') {
//                    steps {
//                        sh "${gradle} -P ssh.user=cruisecontrol upload"
//                    }
//                }
//                stage('Archive Docker image') {
//                    steps {
//                        sh "./push archive"
//                        archiveArtifacts artifacts: '*.tar.gz', fingerprint: true
//                    }
//                }
                stage('Push Docker image') {
                    steps {
                        sh "./push"
                    }
                }
            }
        }
    }

    post {
        always {
            script {
                // build status of null means successful
                buildStatus = currentBuild.result ?: 'SUCCESS'

                // Override default values based on build status
                if (buildStatus == 'STARTED' || buildStatus == 'UNSTABLE') {
                    colorCode = '#d69d46'
                } else if (buildStatus == 'SUCCESS') {
                    colorCode = '#43b688'
                } else {
                    colorCode = '#9e040e'
                }

                // send notification if this build was not successful or if the previous build wasn't successful
                if ( (currentBuild.previousBuild != null && currentBuild.previousBuild.result != 'SUCCESS') || buildStatus != 'SUCCESS') {
                    slackSend (
                        color: colorCode,
                        message: "${buildStatus}: ${env.JOB_NAME} [<${env.BUILD_URL}|${env.BUILD_NUMBER}>]"
                    )

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

            archiveArtifacts artifacts: '**/build/libs/**/*.jar', fingerprint: true
//            archiveArtifacts artifacts: '**/target/*.jar', fingerprint: true, allowEmptyArchive: true
//            archiveArtifacts artifacts: '**/build/reports'
        }

        cleanup {
            // cleanup workspace
            cleanWs disableDeferredWipeout: true
        }
    }

    options {
//        disableConcurrentBuilds()
        buildDiscarder(logRotator(numToKeepStr:'20'))
        timeout(time: 60, unit: 'MINUTES')
        skipStagesAfterUnstable()
        timestamps()
        ansiColor('xterm')
    }
}
