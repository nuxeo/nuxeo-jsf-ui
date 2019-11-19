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
])

JSF_SHA = null

void setGitHubBuildStatus(String context, String message, String state) {
  // set status on nuxeo/nuxeo
  step([
    $class: 'GitHubCommitStatusSetter',
    reposSource: [$class: 'ManuallyEnteredRepositorySource', url: 'https://github.com/nuxeo/nuxeo/'],
    contextSource: [$class: 'ManuallyEnteredCommitContextSource', context: context],
    statusResultSource: [$class: 'ConditionalStatusResultSource', results: [[$class: 'AnyBuildResult', message: message, state: state]]],
    commitShaSource    : [$class: "ManuallyEnteredShaSource", sha: params.NUXEO_COMMIT_SHA],
  ])

  if (JSF_SHA != null) {
    // set status on nuxeo/nuxeo-jsf-ui if needed
    step([
      $class: 'GitHubCommitStatusSetter',
      reposSource: [$class: 'ManuallyEnteredRepositorySource', url: 'https://github.com/nuxeo/nuxeo-jsf-ui/'],
      contextSource: [$class: 'ManuallyEnteredCommitContextSource', context: context],
      statusResultSource: [$class: 'ConditionalStatusResultSource', results: [[$class: 'AnyBuildResult', message: message, state: state]]],
      commitShaSource    : [$class: "ManuallyEnteredShaSource", sha: JSF_SHA],
    ])
  }
}

String getParentVersion() {
  return readMavenPom().getParent().getVersion()
}

String getCurrentVersion() {
  return readMavenPom().getVersion()
}

String getNewVersion() {
  return params.NUXEO_VERSION;
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

void updateParentVersion() {
  // update the parent version
  echo """
    ----------------------------------------
    Update parent version
    ----------------------------------------
    Current parent version: ${PARENT_VERSION}
    New parent version: ${NEW_PARENT_VERSION}
  """
  // update only the first version
  sh """
    perl -i -pe 'BEGIN { \$/ = undef; } s|<version>${PARENT_VERSION}</version>|<version>${NEW_PARENT_VERSION}</version>|' pom.xml
  """
}

void updateVersion() {
  echo """
    ----------------------------------------
    Update version
    ----------------------------------------
    Current version: ${CURRENT_VERSION}
    New version: ${NEW_VERSION}
  """
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
    CURRENT_VERSION = getCurrentVersion()
    MAVEN_OPTS = "$MAVEN_OPTS -Xms512m -Xmx3072m"
    NEW_PARENT_VERSION = getNewVersion()
    NEW_VERSION = getNewVersion()
    PARENT_VERSION = getParentVersion()
  }

  parameters {
    string(name: 'NUXEO_BRANCH', defaultValue: '', description: '')
    string(name: 'NUXEO_COMMIT_SHA', defaultValue: '', description: '')
    string(name: 'NUXEO_VERSION', defaultValue: '', description: '')
  }

  stages {
    stage('Check Parameters') {
      steps {
        container('maven') {
          script {
            if (params.NUXEO_BRANCH == '' || params.NUXEO_COMMIT_SHA == '' || params.NUXEO_VERSION == '') {
              currentBuild.result = 'ABORTED';
              currentBuild.description = "Missing parameters, not triggered from nuxeo pipeline, aborting build."
              error(currentBuild.description)
            }
          }
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
            CURRENT_VERSION = ${CURRENT_VERSION}
            MAVEN_OPTS = ${MAVEN_OPTS}
            NEW_PARENT_VERSION = ${NEW_PARENT_VERSION}
            NEW_VERSION = ${NEW_VERSION}
            NUXEO_BRANCH = ${params.NUXEO_BRANCH}
            NUXEO_COMMIT_SHA = ${params.NUXEO_COMMIT_SHA}
            NUXEO_VERSION = ${params.NUXEO_VERSION}
            PARENT_VERSION = ${PARENT_VERSION}
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

    stage('Checkout') {
      steps {
        container('maven') {
          echo """
            ----------------------------------------
            Checkout
            ----------------------------------------
          """
          script {
            try {
              sh "git fetch origin ${params.NUXEO_BRANCH}:${params.NUXEO_BRANCH}"
              sh "git checkout ${params.NUXEO_BRANCH}"
              JSF_SHA = sh(script: 'git rev-parse HEAD', returnStdout: true).trim()
            } catch (err) {
              // branch does not exist on nuxeo-jsf-ui repository, keep using initial checkout branch
            }
          }
        }
      }
    }

    stage('Update version') {
      steps {
        container('maven') {
          updateParentVersion()
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

    stage('Deploy Maven artifacts') {
      steps {
        setGitHubBuildStatus('jsfui/deploy', 'Deploy Maven artifacts', 'PENDING')
        container('maven') {
          echo """
            ----------------------------------------
            Deploy Maven artifacts
            ----------------------------------------
          """
          sh 'mvn -B -nsu -T0.8C -DskipTests deploy'
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
