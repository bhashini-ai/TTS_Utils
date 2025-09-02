pipeline {
  agent any
  stages {
    stage('Maven Build') {
      steps {
        script {
          def workDir = pwd()
          def jenkinsUid = sh(returnStdout: true, script: 'id -u').trim()
          def jenkinsGid = sh(returnStdout: true, script: 'id -g').trim()
          // Run Maven inside container as Jenkins user
          docker.image('maven:3.9.9-ibm-semeru-21-jammy').inside(
            "-u ${jenkinsUid}:${jenkinsGid} " + // run as jenkins user
            "-e HOME=/home/jenkins " + // ensure HOME points to user home
            "-v ${workDir}:/usr/src/mymaven " + // workspace mount
            "-v /home/jenkins/.m2:/home/jenkins/.m2 " + // persistent Maven repo
            "-w /usr/src/mymaven" // working directory
          ) {
            sh 'mvn clean install'
          }
        }
      }
    }

    stage('Deploy') {
      environment {
        TTS_UTILS_DEPLOY_SERVER = credentials('TTS_UTILS_DEPLOY_SERVER')
        TTS_UTILS_DEPLOY_DIR = credentials('TTS_UTILS_DEPLOY_DIR')
      }
      steps {
        sh 'scp target/TTS_Utils-1.0.jar $TTS_UTILS_DEPLOY_SERVER:$TTS_UTILS_DEPLOY_DIR'
      }
    }

  }
}