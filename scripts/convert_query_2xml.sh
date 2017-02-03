#/bin/bash
export MAVEN_OPTS="-Xms8192m -server"
bash_cmd="mvn compile exec:java -Dexec.mainClass=apps.QueryReaderFactory -Dexec.args='$@' "
bash -c "$bash_cmd"
if [ "$?" != "0" ] ; then
  exit 1
fi


