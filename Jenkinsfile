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
        upstream(upstreamProjects: "DockerBuild_secore-base/master", threshold: hudson.model.Result.SUCCESS)
    }

    parameters {
        booleanParam(name: 'SKIP_TAG_AND_PUBLISH', defaultValue: false, description: 'Skips tagging and publishing steps for when you merge changes but dont want to bump version and publish a new build')
        booleanParam(name: 'TEST_UPGRADE', defaultValue: true, description: 'Disable to skip Grow schema upgrade test')
        booleanParam(name: 'IGNORE_VULNERABILITIES', defaultValue: false, description: 'Enable to allow the scan stage to pass even if critical vulnerabilities are detected')
    }

    stages {
        stage('Prepare') {
            steps {
                script {
                    if (env.BRANCH_NAME == 'master') {
                        versionNumber = VersionNumber versionNumberString: '${BUILD_DATE_FORMATTED, "yy.MM"}.${BUILDS_THIS_MONTH}', versionPrefix: '', worstResultForIncrement: 'SUCCESS'
                        versionNumberInt = VersionNumber versionNumberString: '${BUILD_DATE_FORMATTED, "yyMM"}${BUILDS_THIS_MONTH, XXX}', versionPrefix: '', worstResultForIncrement: 'SUCCESS'
                        currentBuild.displayName = versionNumber
                    } else {
                        versionNumber = env.BUILD_NUMBER
                        versionNumberInt = 'null'
                    }
                    timestamp = VersionNumber versionNumberString: '${BUILD_DATE_FORMATTED, "yyyy-MM-dd HH:mm:ss Z"}'
                    gitCommit = sh(script: "git rev-parse HEAD", returnStdout: true).trim()
                    gradleVersion = sh(script: "awk -F= '\$1==\"distributionUrl\"{print\$2}' gradle/wrapper/gradle-wrapper.properties|sed -n 's/.*\\/gradle-\\(.*\\)-bin\\.zip\$/\\1/p'", returnStdout: true).trim()
                    gradle = "/usr/lib/gradle/${gradleVersion}/bin/gradle"

                    lastStableBuildVer = sh(script: "git tag -n | grep -E '^[2-9][0-9]\\.(0[1-9]|1[0-2])\\.[1-9]([0-9]+)?\\s+Jenkins build from master commit ' | sort -V | tail -n1 | awk '{print\$1}'", returnStdout: true).trim()
                    if (lastStableBuildVer.isEmpty() && env.BRANCH_NAME == 'master') {
                        timeout(time: 600, unit: 'SECONDS') {
                            input message:"Unable to determine version of last stable build, if this is not a new project then something is broken.", ok: "Continue anyway (Grow upgrade test will be skipped)."
                        }
                    }
                }
                echo "Building from commit ${gitCommit} in ${BRANCH_NAME} branch at ${timestamp}"
                echo "Last stable master build: ${lastStableBuildVer}"
                echo "VersionNumber: ${versionNumber} / ${versionNumberInt}"
                echo "GradleVersion: ${gradleVersion}"
                sh "if [ ! -x \"$gradle\" ]; then echo \"Gradle version not available, try: \$(ls -1 /usr/lib/gradle/|tr '\n' ' ')\"; exit 1; fi"
                sh 'java -version'
            }
        }
        stage('Build') {
            steps {
                sh "if [ '${versionNumberInt}' != null ]; then find . -path '*/src/main/grow' -type d -print0 | xargs -0 -P4 -I{} sh -c \"echo 'Setup {}/package.version' && echo ${versionNumberInt} > '{}/package.version'\"; fi"
                sh "find . -type f -name mkdocker -print0 | xargs -0 -P4 -I{} sh -ec 'vfile=\$(dirname \"{}\")/build.version; if [ -f \"\$vfile\" ]; then echo \"Setup \$vfile\"; echo ${versionNumber} > \"\$vfile\"; fi'"
                sh "${gradle} clean build"
            }
        }
        stage('Scan') {
            agent {
                docker {
                    reuseNode true
                    image 'aquasec/trivy'
                    args '-u root -v "$HOME/trivy/:/root/.cache/trivy" -v /var/run/docker.sock:/var/run/docker.sock --entrypoint='
                }
            }
            environment {
                IMAGE = """${ sh(script: '. build/version.properties && echo inomial.io/$project', returnStdout: true).trim() }"""
                TRIVY_NO_PROGRESS = "true"
                TRIVY_IGNORE_UNFIXED = "true"
                TRIVY_EXIT_CODE = "1"
            }
            steps {
                sh "trivy image --clear-cache"
                script {
                    // non-critical vulnerabilities will mark the stage as unstable
                    catchError(buildResult: 'SUCCESS', stageResult: 'UNSTABLE') {
                        try {
                            sh 'trivy image --severity UNKNOWN,LOW,MEDIUM,HIGH "$IMAGE"'
                        } catch (Throwable e) {
                            if (env.CHANGE_ID) {
                                pullRequest.comment('Non-critical vulnerabilities found')
                            }
                            error "Caught ${e.toString()}"
                        }
                    }

                    // critical vulnerabilities will fail the job, unless the IGNORE_VULNERABILITIES build parameter is enabled
                    catchError(buildResult: params.IGNORE_VULNERABILITIES ? 'SUCCESS' : 'UNSTABLE', stageResult: 'FAILURE') {
                        try {
                            sh 'trivy image --severity CRITICAL "$IMAGE"'
                        } catch (Throwable e) {
                            if (env.CHANGE_ID) {
                                pullRequest.comment((params.IGNORE_VULNERABILITIES ? '[IGNORED]' : '[FATAL]') + ' Critical vulnerabilities found')
                            }
                            error "Caught ${e.toString()}"
                        }
                    }
                }
            }
        }
        stage('Test') {
            steps {
                sh "./test.sh"
            }
            post {
                always {
                    archiveArtifacts artifacts: '**/test-results/**', excludes: '**/test-results/**/binary/**'
//                    junit '**/build/test-results/**/*.xml'
                }
            }
        }
        stage('Test upgrade') {
            when {
                expression {
                    lastStableBuildVer.isEmpty() == false && params.TEST_UPGRADE
                }
            }
            steps {
                sh "INITVERSION=${lastStableBuildVer} ./test.sh"
            }
            post {
                always {
                    archiveArtifacts artifacts: '**/test-results/**', excludes: '**/test-results/**/binary/**'
//                    junit '**/build/test-results/**/*.xml'
                }
            }
        }
        stage('Tag') {
            when {
                branch 'master'
                expression {
                    return ! params.SKIP_TAG_AND_PUBLISH
                }
            }
            environment {
                GIT_AUTH = credentials('github_inomial-ci')
            }
            steps {
                // add tag
                sh "git tag -a \"${versionNumber}\" -m \"Jenkins build from ${BRANCH_NAME} commit ${gitCommit} on ${timestamp}\""

                // push tag
                sh 'git config --local credential.helper "!f() { echo username=\\$GIT_AUTH_USR; echo password=\\$GIT_AUTH_PSW; }; f"'
                sh "git push origin \"${versionNumber}\""
            }
        }
        stage('Release') {
            when {
                branch 'master'
                expression {
                    return ! params.SKIP_TAG_AND_PUBLISH
                }
            }
            parallel {
//                stage('Publish to Maven') {
//                    environment {
//                        MAVEN_REPO = 'sftp://maven.inomial.com:22/maven'
//                        MAVEN_AUTH = credentials('ipa_cruisecontrol')
//                    }
//                    steps {
//                        sh "${gradle} publish"
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
                if (env.BRANCH_NAME == 'master') {
                    notifyBuild()
                }
            }

//            archiveArtifacts artifacts: '**/build/libs/**/*.jar', fingerprint: true
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

def notifyBuild() {
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
            message: "${buildStatus}: ${env.JOB_NAME} [<${env.RUN_DISPLAY_URL}|${env.BUILD_DISPLAY_NAME}>]"
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
