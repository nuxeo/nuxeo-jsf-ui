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

String getCurrentVersion() {
  return readMavenPom().getVersion()
}

String getNewVersion() {
  String currentVersion = getCurrentVersion()
  return BRANCH_NAME == 'master' ? currentVersion : "${BRANCH_NAME}-" + currentVersion
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

void updateVersion() {
  echo """
    ----------------------------------------
    Update version
    ----------------------------------------
    Current version: ${CURRENT_VERSION}
    New version: ${NEW_VERSION}
  """
  echo "MAVEN_OPTS=$MAVEN_OPTS"
  sh """
    mvn -nsu versions:set -DnewVersion=${NEW_VERSION} -DgenerateBackupPoms=false
    perl -i -pe 's|<nuxeo.jsf.version>${CURRENT_VERSION}</nuxeo.jsf.version>|<nuxeo.jsf.version>${NEW_VERSION}</nuxeo.jsf.version>|' pom.xml
  """
}

pipeline {
  agent {
    label 'jenkins-nuxeo-jsf-11'
  }
  environment {
    CHANGE_BRANCH = "$CHANGE_BRANCH"
    CURRENT_VERSION = getCurrentVersion()
    MAVEN_OPTS = "$MAVEN_OPTS -Xms512m -Xmx3072m"
    NEW_VERSION = getNewVersion()
  }

  stages {
    stage('Set labels') {
      steps {
        container('maven') {
          echo """
          ----------------------------------------
          Set Kubernetes resource labels
          ----------------------------------------
          """
          echo "Set label 'branch: ${BRANCH_NAME}' on pod ${NODE_NAME}"
          sh """
            kubectl label pods ${NODE_NAME} branch=${BRANCH_NAME}
          """
        }
      }
    }

    stage('Print Variables') {
      steps {
        container('maven') {
          echo """
            ----------------------------------------
            Print Variables
            ----------------------------------------
            BRANCH_NAME = ${BRANCH_NAME}
            CHANGE_BRANCH = ${CHANGE_BRANCH}
            CURRENT_VERSION = ${CURRENT_VERSION}
            MAVEN_OPTS = ${MAVEN_OPTS}
            NEW_VERSION = ${NEW_VERSION}
          """
          sh 'mvn -v'
        }
      }
    }

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

    stage('Check Nuxeo branch') {
      when {
        branch 'PR-*'
      }
      steps {
        container('maven') {
          withCredentials([usernameColonPassword(credentialsId: 'jx-pipeline-git-github-git', variable: 'GITHUB_USERNAME_PASSWORD')]) {
            script {
              def branchRef = sh(script: "curl -u '${GITHUB_USERNAME_PASSWORD}' https://api.github.com/repos/nuxeo/nuxeo/git/ref/heads/${CHANGE_BRANCH} | jq -r '.ref'", returnStdout: true).trim();
              if (branchRef != 'null') {
                currentBuild.result = 'ABORTED';
                currentBuild.description = "Branch: ${CHANGE_BRANCH} exists on nuxeo/nuxeo repository, aborting build."
                error(currentBuild.description)
              }
            }
          }
        }
      }
    }

    stage('Update version') {
      steps {
        container('maven') {
          updateVersion()
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
            ----------------------------------------
          """
          sh 'mvn -B -nsu -N install'
          sh 'mvn -B -nsu -T0.8C -DskipTests -f code/pom.xml install'
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
            ----------------------------------------
          """
          sh "mvn -B -nsu -f code/pom.xml test"
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
            ----------------------------------------
          """
          sh 'mvn -B -nsu -DskipTests -f packages/pom.xml install'
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
            ----------------------------------------
          """
          script {
            try {
              runFunctionalTests('ftests')
              setGitHubBuildStatus('jsfui/ftests/dev', 'Functional tests - dev environment', 'SUCCESS')
            } catch (err) {
              setGitHubBuildStatus('jsfui/ftests/dev', 'Functional tests - dev environment', 'FAILURE')
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
