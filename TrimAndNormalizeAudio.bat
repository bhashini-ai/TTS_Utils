set JAVA_HOME=C:\Program Files\Java\jdk-11.0.8
set PATH=%JAVA_HOME%\bin;%PATH%
cd C:\TTS_Utils
set CLASSPATH=TTS_Utils-1.0.jar;.\lib\commons-cli-1.5.0.jar;%CLASSPATH%
java -Dfile.encoding=UTF-8 ai.bhashini.tts.utils.TrimAndNormalizeAudio %*
