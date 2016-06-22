#/bin/bash
export MAVEN_OPTS="-Xms8192m -server"
input=$1
if [ "$input" = "" ] ; then
  echo "Specify the input file as the 1st argument (StackOverflow post file)"
  exit 1
fi
output=$2
if [ "$output" = "" ] ; then
  echo "Specify the output file as the 2d argument (this file will be in the YahooAnswers! format"
  exit 1
fi
max_num_rec=$3
ARGS=" -exclude_code -input $input -output $output"
if [ "$max_num_rec" != "" ] ; then
  ARGS="$ARGS -n $max_num_rec"
fi
bash_cmd="mvn compile exec:java -Dexec.mainClass=ConvertStackOverflow -Dexec.args='$ARGS' "
echo $bash_cmd
bash -c "$bash_cmd"
if [ "$?" != "0" ] ; then
  exit 1
fi


