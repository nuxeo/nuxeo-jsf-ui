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

pipeline {
  agent {
    label 'jenkins-nuxeo-jsf-11'
  }
  triggers {
    upstream(
      threshold: hudson.model.Result.SUCCESS,
      upstreamProjects: '/nuxeo/release-nuxeo',
    )
  }
  environment {
    MAVEN_ARGS = '-B -nsu -Dnuxeo.skip.enforcer=true -Prelease'
    MAVEN_OPTS = "$MAVEN_OPTS -Xms512m -Xmx3072m"
    CONNECT_PROD_URL = 'https://connect.nuxeo.com/nuxeo'
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
            sh "mvn ${MAVEN_ARGS} -V versions:update-parent -DgenerateBackupPoms=false"

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
          echo """
          ----------------------------------------
          Upload Nuxeo Packages to ${CONNECT_PROD_URL}
          ----------------------------------------
          """
          withCredentials([usernameColonPassword(credentialsId: 'connect-prod', variable: 'CONNECT_PASS')]) {
            sh """
              PACKAGES_TO_UPLOAD="packages/nuxeo-marketplace-jsf-ui/target/nuxeo-marketplace-jsf-ui-*.zip packages/nuxeo-*-package/target/nuxeo-*-package-*.zip"
              for file in \$PACKAGES_TO_UPLOAD ; do
                curl --fail -i -u "$CONNECT_PASS" -F package=@\$(ls \$file) "$CONNECT_PROD_URL"/site/marketplace/upload?batch=true ;
              done
            """
          }
        }
      }
    }

    stage('Bump master branch') {
      steps {
        container('maven') {
          script {
            // fetch version released
            sh "git checkout release"
            def releaseVersion = readMavenPom().getVersion()
            // fetch current master SNAPSHOT version
            sh "git checkout master"
            def currentVersion = readMavenPom().getVersion()

            echo """
            ----------------------------------------
            Update master parent version to latest SNAPSHOT
            ----------------------------------------
            """
            sh "mvn ${MAVEN_ARGS} -V versions:update-parent -DgenerateBackupPoms=false -DallowSnapshots=true"
            // fetch new SNAPSHOT version, same as the new parent version
            def nextVersion = readMavenPom().getParent().getVersion()

            echo """
            ----------------------------------------
            Update master version from ${currentVersion} to ${nextVersion}
            ----------------------------------------
            """
            sh """
              mvn ${MAVEN_ARGS} versions:set -DnewVersion=${nextVersion} -DgenerateBackupPoms=false
              perl -i -pe 's|<nuxeo.jsf.version>.*?</nuxeo.jsf.version>|<nuxeo.jsf.version>${nextVersion}</nuxeo.jsf.version>|' pom.xml

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
}
