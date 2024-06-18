pipeline {
  agent any
  stages {
    stage('Maven Build') {
      steps {
        sh 'docker run --rm -v "$(pwd)":/usr/src/mymaven -v maven-repo:/root/.m2 -w /usr/src/mymaven maven:3.8.6-openjdk-11 sh -c "mvn clean install && chown -R `id -u jenkins`:`id -g jenkins` target"'
      }
    }

    stage('Deploy') {
      environment {
        TTS_UTILS_DEPLOY_SERVER = credentials('TTS_UTILS_DEPLOY_SERVER')
        TTS_UTILS_DEPLOY_DIR = credentials('TTS_UTILS_DEPLOY_DIR')
      }
      steps {
        sh 'scp target/TTS_UTILS-1.0.jar $TTS_UTILS_DEPLOY_SERVER:$TTS_UTILS_DEPLOY_DIR'
      }
    }

  }
}