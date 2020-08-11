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

def isPullRequest() {
  return BRANCH_NAME =~ /PR-.*/
}

void runFunctionalTests(String baseDir) {
  try {
    retry(2) {
      sh "mvn -B -nsu -f ${baseDir}/pom.xml verify"
    }
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
  triggers {
    upstream(
      threshold: hudson.model.Result.SUCCESS,
      upstreamProjects: '/nuxeo/nuxeo/master',
    )
  }
  environment {
    MAVEN_ARGS = '-B -nsu -Dnuxeo.skip.enforcer=true'
    MAVEN_OPTS = "$MAVEN_OPTS -Xms512m -Xmx3072m"
    CONNECT_PREPROD_URL = 'https://nos-preprod-connect.nuxeocloud.com/nuxeo'
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

    stage('Build') {
      steps {
        setGitHubBuildStatus('maven/build', 'Build', 'PENDING')
        container('maven') {
          echo """
            ----------------------------------------
            Compile
            ----------------------------------------
          """
          echo "MAVEN_OPTS=$MAVEN_OPTS"
          sh "mvn ${MAVEN_ARGS} -V -N install"
          sh "mvn ${MAVEN_ARGS} -T4C -DskipTests -f code/pom.xml install"
        }
      }
      post {
        success {
          setGitHubBuildStatus('maven/build', 'Build', 'SUCCESS')
        }
        unsuccessful {
          setGitHubBuildStatus('maven/build', 'Build', 'FAILURE')
        }
      }
    }

    stage('Run "dev" unit tests') {
      steps {
        setGitHubBuildStatus('utests/dev', 'Unit tests - dev environment', 'PENDING')
        container('maven') {
          echo """
            ----------------------------------------
            Run "dev" unit tests
            ----------------------------------------
          """
          sh "mvn ${MAVEN_ARGS} -f code/pom.xml test"
        }
      }
      post {
        always {
          junit testResults: '**/target/surefire-reports/*.xml'
        }
        success {
          setGitHubBuildStatus('utests/dev', 'Unit tests - dev environment', 'SUCCESS')
        }
        unsuccessful {
          setGitHubBuildStatus('utests/dev', 'Unit tests - dev environment', 'FAILURE')
        }
      }
    }

    stage('Build Nuxeo Packages') {
      steps {
        setGitHubBuildStatus('packages/build', 'Build Nuxeo packages', 'PENDING')
        container('maven') {
          echo """
            ----------------------------------------
            Package
            ----------------------------------------
          """
          sh 'mvn ${MAVEN_ARGS} -DskipTests -f packages/pom.xml install'
        }
      }
      post {
        success {
          setGitHubBuildStatus('packages/build', 'Build Nuxeo packages', 'SUCCESS')
        }
        unsuccessful {
          setGitHubBuildStatus('packages/build', 'Build Nuxeo packages', 'FAILURE')
        }
      }
    }

    stage('Run "dev" functional tests') {
      steps {
        setGitHubBuildStatus('ftests/dev', 'Functional tests - dev environment', 'PENDING')
        container('maven') {
          echo """
            ----------------------------------------
            Run "dev" functional tests
            ----------------------------------------
          """
          runFunctionalTests('ftests')
        }
        findText regexp: ".*ERROR.*", fileSet: "ftests/nuxeo-diff-jsf-ui-ftests/**/log/server.log"
        findText regexp: ".*ERROR.*", fileSet: "ftests/nuxeo-jsf-ui-hotreload-tests/**/log/server.log"
        findText regexp: ".*ERROR.*", fileSet: "ftests/nuxeo-jsf-ui-webdriver-tests/**/log/server.log"
        findText regexp: ".*ERROR.*", fileSet: "ftests/nuxeo-lang-ext-incomplete-jsf-ui-ftests/**/log/server.log"
        findText regexp: ".*ERROR.*", fileSet: "ftests/nuxeo-platform-forms-layout-demo-tests/**/log/server.log"
        findText regexp: ".*ERROR.*", fileSet: "ftests/nuxeo-signature-jsf-ui-ftests/**/log/server.log"
        findText regexp: ".*ERROR.*", fileSet: "ftests/nuxeo-smart-search-jsf-ui-ftests/**/log/server.log"
        findText regexp: ".*ERROR.*", fileSet: "ftests/nuxeo-virtual-navigation-jsf-ui-ftests/**/log/server.log"
      }
      post {
        always {
          junit testResults: '**/target/failsafe-reports/*.xml'
        }
        success {
          setGitHubBuildStatus('ftests/dev', 'Functional tests - dev environment', 'SUCCESS')
        }
        unsuccessful {
          setGitHubBuildStatus('ftests/dev', 'Functional tests - dev environment', 'FAILURE')
        }
      }
    }
  }

  post {
    always {
      script {
        if (!isPullRequest()) {
          // update JIRA issue
          step([$class: 'JiraIssueUpdater', issueSelector: [$class: 'DefaultIssueSelector'], scm: scm])
        }
      }
    }
  }
}
