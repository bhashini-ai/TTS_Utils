export CLASSPATH=./target/TTS_Utils-1.0.jar:./target/lib/commons-cli-1.5.0.jar
java -Dfile.encoding=UTF-8 ai.bhashini.tts.utils.TrimAndNormalizeAudio $@
