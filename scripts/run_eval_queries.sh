#/bin/bash
export MAVEN_OPTS="-Xms8192m -server"
input=$1
if [ "$input" = "" ] ; then
  echo "Specify the input file as the (1st argument)"
  exit 1
fi

if [ ! -f "$input" ] ; then
  echo "The specified input file '$input' cannot be found!"
  exit 1
fi

output=$2
if [ "$output" = "" ] ; then
  echo "Specify the top-level directory for indices of two types (2d argument)"
  exit 1
fi

max_query_qty=$3
if [ "$max_query_qty" = "" ] ; then
  echo "Specify the maximum number of queries (3d argument)"
  exit 1
fi

REP_QTY=$4
if [ "$REP_QTY" = "" ] ; then
  echo "Specify the number of times to run the Lucene pipeline (4th argument)"
  exit 1
fi

DO_EVAL=$5
if [ "$DO_EVAL" = "" ] ; then
  echo "Specify the flag that switches on/off evaluation (5th argument)"
  exit 1
fi

# Retrieve 100 entries
N=100

TREC_EVAL_VER="9.0.4"

TREC_EVAL_DIR="trec_eval-${TREC_EVAL_VER}"
if [ ! -d $TREC_EVAL_DIR -o ! -f "$TREC_EVAL_DIR/trec_eval" ] ; then
  rm -rf "$TREC_EVAL_DIR"
  echo "Downloading and building missing trec_eval" 
  wget https://github.com/usnistgov/trec_eval/archive/v${TREC_EVAL_VER}.tar.gz
  if [ "$?" != "0" ] ; then
    echo "Error downloading trec_eval"
    exit 1
  fi
  tar -zxvf v${TREC_EVAL_VER}.tar.gz
  if [ "$?" != "0" ] ; then
    echo "Error unpacking the trec_eval archive!"
    exit 1
  fi
  cd $TREC_EVAL_DIR
  if [ "$?" != "0" ] ; then
    echo "Cannot changed dir to $TREC_EVAL_VER"
    exit 1
  fi
  make
  if [ "$?" != "0" ] ; then
    echo "Error building trec_eval"
    exit 1
  fi
  cd -
  if [ "$?" != "0" ] ; then
    echo "Cannot change dir back to the starting dir"
    exit 1
  fi
  rm v${TREC_EVAL_VER}.tar.gz
fi

for type in standard fixed ; do
  INDEX_DIR="$output/$type/index"
  if [ ! -d "$INDEX_DIR" ] ; then
    echo "There is no directory $INDEX_DIR"
    exit 1
  fi

  RUN_DIR=

  QREL_FILE="$output/$type/runs/qrels.txt"
  QREL_FILE_SHORT="$output/$type/runs/qrels_short.txt"

  if [ ! -f "$QREL_FILE" ] ; then
    echo "There is no qrels.txt file in the directory $INDEX_DIR did the indexing procedure finish properly?"
    exit 1
  fi

  flag=""
  if [ "$type" = "standard" ] ; then
    echo "Querying the index using the standard Lucene similarity"
  else
    echo "Querying the index using the fixed Lucene similarity"
    flag=" -bm25fixed "
  fi

  OUT_FILE="$output/$type/runs/trec_run"
  LOG_FILE="$output/$type/query.log"
  echo > $LOG_FILE
  if [ "$?" != "0" ] ; then
    echo "Error writing to $LOG_FILE"
    exit 1
  fi
  for ((i=0;i<$REP_QTY;i++)) ; do
    echo "Query iteration $(($i+1))"
    scripts/lucene_query.sh -s data/stopwords.txt -i "$input" -d "$INDEX_DIR" -prob 1.0 -n $N -max_query_qty "$max_query_qty" -o "$OUT_FILE" $flag 2>&1 >> ${LOG_FILE}

    if [ "$?" != "0" ] ; then
      echo "lucene_query.sh failed!"
      exit 1
    fi
  done
  if [ "$DO_EVAL" = "1" ] ; then
    echo "Let's evaluate output quality"
    head -$max_query_qty $QREL_FILE > $QREL_FILE_SHORT
    if [ "$?" != "0" ] ; then
      echo "Failed to create $QREL_FILE_SHORT"
      exit 1
    fi
    EVAL_REPORT_PREFIX="$output/$type/runs/eval"
    scripts/eval_output.py "$TREC_EVAL_DIR/trec_eval" "$QREL_FILE_SHORT" "$OUT_FILE" "$EVAL_REPORT_PREFIX"
    if [ "$?" != "0" ] ; then
      echo "eval_output failed!"
      exit 1
    fi
  fi
  
done
if [ "$DO_EVAL" = "1" ] ; then
  echo "Let's now compute p-values and ratios"
  for metr in recall "recall@10" "P@1" map ; do
    echo "============================================="
    echo " Evaluation metric: $metr "
    echo "============================================="
    EVAL_REPORT_STANDARD="$output/standard/runs/eval.$metr"
    EVAL_REPORT_FIXED="$output/fixed/runs/eval.$metr"
    scripts/p-val.R "$EVAL_REPORT_FIXED" "$EVAL_REPORT_STANDARD"
  done
fi


