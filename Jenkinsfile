/*
 * (C) Copyright 2019 Nuxeo (http://nuxeo.com/) and others.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Contributors:
 *     Thomas Roger <troger@nuxeo.com>
 */

properties([
  [$class: 'GithubProjectProperty', projectUrlStr: 'https://github.com/nuxeo/nuxeo-jsf-ui/'],
  [$class: 'BuildDiscarderProperty', strategy: [$class: 'LogRotator', daysToKeepStr: '60', numToKeepStr: '60', artifactNumToKeepStr: '5']],
  disableConcurrentBuilds(),
])

void setGitHubBuildStatus(String context, String message, String state) {
  step([
    $class: 'GitHubCommitStatusSetter',
    reposSource: [$class: 'ManuallyEnteredRepositorySource', url: 'https://github.com/nuxeo/nuxeo-jsf-ui'],
    contextSource: [$class: 'ManuallyEnteredCommitContextSource', context: context],
    statusResultSource: [$class: 'ConditionalStatusResultSource', results: [[$class: 'AnyBuildResult', message: message, state: state]]],
  ])
}

void runFunctionalTests(String baseDir) {
  try {
    sh "mvn -B -nsu -f ${baseDir}/pom.xml verify"
  } finally {
    try {
      archiveArtifacts allowEmptyArchive: true, artifacts: "${baseDir}/**/target/failsafe-reports/*, ${baseDir}/**/target/**/*.log, ${baseDir}/**/target/*.png, ${baseDir}/**/target/**/distribution.properties, ${baseDir}/**/target/**/configuration.properties"
    } catch (err) {
      echo hudson.Functions.printThrowable(err)
    }
  }
}

pipeline {
  agent {
    label 'jenkins-nuxeo-jsf-11'
  }

  stages {
    stage('Workaround bower root issue') {
      steps {
        container('maven') {
          sh """
            # to be removed when running with jenkins user
            echo '{ "allow_root": true }' > /home/jenkins/.bowerrc
          """
        }
      }
    }

    stage('Compile') {
      steps {
        setGitHubBuildStatus('jsfui/compile', 'Compile', 'PENDING')
        container('maven') {
          echo """
          ----------------------------------------
          Compile
          ----------------------------------------"""
          withEnv(["MAVEN_OPTS=$MAVEN_OPTS -Xms512m -Xmx3072m"]) {
            echo "MAVEN_OPTS=$MAVEN_OPTS"
            sh 'mvn -V -B -nsu -T0.8C -DskipTests -f code/pom.xml install'
          }
        }
      }

      post {
        success {
          setGitHubBuildStatus('jsfui/compile', 'Compile', 'SUCCESS')
        }
        failure {
          setGitHubBuildStatus('jsfui/compile', 'Compile', 'FAILURE')
        }
      }
    }

    stage('Run "dev" unit tests') {
      steps {
        setGitHubBuildStatus('jsfui/utests/dev', 'Unit tests - dev environment', 'PENDING')
        container('maven') {
          echo """
          ----------------------------------------
          Run "dev" unit tests
          ----------------------------------------"""
          withEnv(["MAVEN_OPTS=$MAVEN_OPTS -Xms512m -Xmx3072m"]) {
            echo "MAVEN_OPTS=$MAVEN_OPTS"
            sh "mvn -B -nsu -f code/pom.xml test"
          }
        }
      }

      post {
        always {
          junit testResults: '**/target/surefire-reports/*.xml'
        }
        success {
          setGitHubBuildStatus('jsfui/utests/dev', 'Unit tests - dev environment', 'SUCCESS')
        }
        failure {
          setGitHubBuildStatus('jsfui/utests/dev', 'Unit tests - dev environment', 'FAILURE')
        }
      }
    }

    stage('Package') {
      steps {
        setGitHubBuildStatus('jsfui/package', 'Package', 'PENDING')
        container('maven') {
          echo """
          ----------------------------------------
          Package
          ----------------------------------------"""
          withEnv(["MAVEN_OPTS=$MAVEN_OPTS -Xms512m -Xmx3072m"]) {
            echo "MAVEN_OPTS=$MAVEN_OPTS"
            sh 'mvn -B -nsu -T0.8C -DskipTests -f packages/pom.xml install'
          }
        }
      }

      post {
        success {
          setGitHubBuildStatus('jsfui/package', 'Package', 'SUCCESS')
        }
        failure {
          setGitHubBuildStatus('jsfui/package', 'Package', 'FAILURE')
        }
      }
    }

    stage('Run "dev" functional tests') {
      steps {
        setGitHubBuildStatus('jsfui/ftests/dev', 'Functional tests - dev environment', 'PENDING')
        container('maven') {
          echo """
          ----------------------------------------
          Run "dev" functional tests
          ----------------------------------------"""
          withEnv(["MAVEN_OPTS=$MAVEN_OPTS -Xms512m -Xmx3072m"]) {
            script {
              try {
                echo "MAVEN_OPTS=$MAVEN_OPTS"
                runFunctionalTests('ftests')
                setGitHubBuildStatus('jsfui/ftests/dev', 'Functional tests - dev environment', 'SUCCESS')
              } catch (err) {
                setGitHubBuildStatus('jsfui/ftests/dev', 'Functional tests - dev environment', 'FAILURE')
              }
            }
          }
        }
      }

      post {
        always {
          junit testResults: '**/target/surefire-reports/*.xml'
        }
      }
    }

    stage('Deploy Maven artifacts') {
      steps {
        setGitHubBuildStatus('jsfui/deploy', 'Deploy Maven artifacts', 'PENDING')
        container('maven') {
          echo """
          ----------------------------------------
          Deploy Maven artifacts
          ----------------------------------------"""
          withEnv(["MAVEN_OPTS=$MAVEN_OPTS -Xms512m -Xmx3072m"]) {
            echo "MAVEN_OPTS=$MAVEN_OPTS"
            sh 'mvn -B -nsu -T0.8C -DskipTests deploy'
          }
        }
      }
      post {
        success {
          setGitHubBuildStatus('jsfui/deploy', 'Deploy Maven artifacts', 'SUCCESS')
        }
        failure {
          setGitHubBuildStatus('jsfui/deploy', 'Deploy Maven artifacts', 'FAILURE')
        }
      }
    }
  }

  post {
    always {
      script {
        if (BRANCH_NAME == 'master') {
          // update JIRA issue
          step([$class: 'JiraIssueUpdater', issueSelector: [$class: 'DefaultIssueSelector'], scm: scm])
        }
      }
    }
  }
}
