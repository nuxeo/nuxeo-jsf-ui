/*
 * (C) Copyright 2020 Nuxeo (http://nuxeo.com/) and others.
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

void uploadPackages(String url, String credentialsId) {
  echo """
  ----------------------------------------
  Upload Nuxeo Packages to ${url}
  ----------------------------------------
  """
  withCredentials([usernameColonPassword(credentialsId: credentialsId, variable: 'auth')]) {
    sh """
      PACKAGES_TO_UPLOAD="packages/nuxeo-marketplace-jsf-ui/target/nuxeo-marketplace-jsf-ui-*.zip packages/nuxeo-*-package/target/nuxeo-*-package-*.zip"
      for file in \$PACKAGES_TO_UPLOAD ; do
        curl --fail -i -u "${auth}" -F package=@\$(ls \$file) "${url}"/site/marketplace/upload?batch=true ;
      done
    """
  }
}

pipeline {
  agent {
    label 'jenkins-nuxeo-jsf-11'
  }

  parameters {
    string(name: 'NUXEO_RELEASE_VERSION', defaultValue: '', description: 'Nuxeo release version.')
  }

  environment {
    MAVEN_ARGS = '-B -nsu -Dnuxeo.skip.enforcer=true -Prelease'
    MAVEN_OPTS = "$MAVEN_OPTS -Xms512m -Xmx3072m"
    CONNECT_PREPROD_URL = 'https://nos-preprod-connect.nuxeocloud.com/nuxeo'
    CONNECT_PROD_URL = 'https://connect.nuxeo.com/nuxeo'
    SLACK_CHANNEL = 'platform-notifs'
  }

  stages {
    stage('Set Kubernetes labels') {
      steps {
        container('maven') {
          echo """
          ----------------------------------------
          Set Kubernetes labels
          ----------------------------------------
          """
          echo "Set label 'branch: master' on pod ${NODE_NAME}"
          sh """
            kubectl label pods ${NODE_NAME} branch=master
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

    stage('Release') {
      steps {
        container('maven') {
          script {
            sh "git checkout -b release"
            echo """
            ----------------------------------------
            Update parent version
            ----------------------------------------
            """
            echo "MAVEN_OPTS=$MAVEN_OPTS"
            sh """
              mvn ${MAVEN_ARGS} -V versions:update-parent "-DparentVersion=[${params.NUXEO_RELEASE_VERSION}]" -DgenerateBackupPoms=false
            """

            def releaseVersion = readMavenPom().getParent().getVersion()
            echo """
            ----------------------------------------
            Update version
            ----------------------------------------
            New version: ${releaseVersion}
            """
            sh """
              mvn ${MAVEN_ARGS} versions:set -DnewVersion=${releaseVersion} -DgenerateBackupPoms=false
              perl -i -pe 's|<nuxeo.jsf.version>.*?</nuxeo.jsf.version>|<nuxeo.jsf.version>${releaseVersion}</nuxeo.jsf.version>|' pom.xml

              git commit -a -m "Release ${releaseVersion}"
              git tag -a v${releaseVersion} -m "Release ${releaseVersion}"
            """

            echo """
            ----------------------------------------
            Build and Test
            ----------------------------------------
            """
            sh "mvn ${MAVEN_ARGS} install"

            if (env.DRY_RUN != "true") {
              sh """
                jx step git credentials
                git config credential.helper store

                git push origin v${releaseVersion}
              """
              currentBuild.description = "Release ${releaseVersion}"
            }
          }
        }
      }
    }

    stage('Deploy Maven artifacts') {
      when {
        not {
          environment name: 'DRY_RUN', value: 'true'
        }
      }
      steps {
        container('maven') {
          echo """
          ----------------------------------------
          Deploy Maven artifacts
          ----------------------------------------
          """
          sh """
            mvn ${MAVEN_ARGS} -DskipTests deploy
          """
        }
      }
    }

    stage('Upload Nuxeo Packages') {
      when {
        not {
          environment name: 'DRY_RUN', value: 'true'
        }
      }
      steps {
        container('maven') {
          uploadPackages("${CONNECT_PROD_URL}", 'connect-prod')
          uploadPackages("${CONNECT_PREPROD_URL}", 'connect-preprod')
        }
      }
    }

    stage('Bump master branch') {
      steps {
        container('maven') {
          script {
            // fetch released version
            sh "git checkout release"
            def releaseVersion = readMavenPom().getVersion()
            // fetch current master SNAPSHOT version
            sh "git checkout master"
            def currentVersion = readMavenPom().getVersion()
            // increment minor version
            def nextVersion = sh(returnStdout: true, script: "perl -pe 's/\\b(\\d+)(?=\\D*\$)/\$1+1/e' <<< ${currentVersion}").trim()

            echo """
            ----------------------------------------
            Update master version from ${currentVersion} to ${nextVersion}
            ----------------------------------------
            """
            sh """
              # first set the new version while the parent pom version is still the old one
              # to avoid maven complaining that the new parent SNAPSHOT is not available
              mvn ${MAVEN_ARGS} versions:set -DnewVersion=${nextVersion} -DgenerateBackupPoms=false
              perl -i -pe 's|<nuxeo.jsf.version>.*?</nuxeo.jsf.version>|<nuxeo.jsf.version>${nextVersion}</nuxeo.jsf.version>|' pom.xml

              # now update the root POM parent version
              perl -i -pe 's|<version>${currentVersion}</version>|<version>${nextVersion}</version>|' pom.xml

              git commit -a -m "Post release ${releaseVersion}, update ${currentVersion} to ${nextVersion}"
            """

            if (env.DRY_RUN != "true") {
              sh """
                jx step git credentials
                git config credential.helper store

                git push origin master
              """
            }
          }
        }
      }
    }

  }
  post {
    success {
      script {
        if (env.DRY_RUN != 'true') {
          sh "git checkout release"
          def releaseVersion = readMavenPom().getVersion()
          currentBuild.description = "Release ${releaseVersion}"
          slackSend(channel: "${SLACK_CHANNEL}", color: 'good', message: "Successfully released nuxeo/nuxeo-jsf-ui ${releaseVersion}: ${BUILD_URL}")
        }
      }
    }
    unsuccessful {
      script {
        if (env.DRY_RUN != 'true') {
          sh "git checkout release"
          def releaseVersion = readMavenPom().getVersion()
          slackSend(channel: "${SLACK_CHANNEL}", color: 'danger', message: "Failed to release nuxeo/nuxeo-jsf-ui ${releaseVersion}: ${BUILD_URL}")
        }
      }
    }
  }
}
