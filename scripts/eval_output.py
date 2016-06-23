#!/usr/bin/env python
import argparse
import sys
import math
import numpy as np
import subprocess as sp

def Usage(err):
  if not err is None:
    print err
  print "Usage: <trec binary> <qrel file> <trec-format output file> <prefix of report files> "
  sys.exit(1)

RECIP_RANK='recip_rank'
NUM_RET='num_ret'
NUM_REL='num_rel'
NUM_REL_RET='num_rel_ret'
P_10='P_10'
MAP='map'
metrics = set([RECIP_RANK, NUM_RET, NUM_REL, NUM_REL_RET, P_10, MAP])

def saveTsv(fname, arr):
  f=open(fname,'w')
  f.write("\t".join(str(s) for s in arr) + '\n')
  f.close()

def readResults(lines):
  prevId=''
  res=dict()
  for s in lines:
    if s == '': continue
    arr=s.split()
    if (len(arr) != 3):
      raise Exception("wrong-format line: '%s'" % s)
    (metr, qid, val) = arr
    if not qid in res: res[qid]=dict()
    entry=res[qid]
    if metr in metrics:
      entry[metr]=float(val)
  return res
  

if len(sys.argv) != 5:
  Usage(None)

trecEvalBin=sys.argv[1]
qrelFile=sys.argv[2]
trecOut=sys.argv[3]
outPrefix = sys.argv[4]
output=sp.check_output([trecEvalBin, "-q", qrelFile, trecOut]).replace('\t', ' ').split('\n')
res=readResults(output)

numRel=res['all'][NUM_REL]
numRelRet=res['all'][NUM_REL_RET]
mrr_overall=res['all'][RECIP_RANK]
map_overall=res['all'][MAP]
p_10_overall=res['all'][P_10]
gotCorrect=0
queryQty=0

val_p_1   = []
val_r_10  = []
val_map   = []
val_recall= []

for qid, entry in res.iteritems():
  if qid == 'all': continue
  queryQty+=1
  val=0
  if (entry[RECIP_RANK]>=0.999): val=1
  val_p_1.append(val) 
  # Because we have only one relevant entry,
  # recall@10 is equal to precision@10 x 10 
  val_r_10.append(entry[P_10]*10)
  val_recall.append(entry[NUM_REL_RET])
  val_map.append(entry[MAP])

gotRight=np.sum(val_p_1)
precision_at_1_overall=float(gotRight)/queryQty


mrrOverall=res['all'][RECIP_RANK]
# sanity check
if numRelRet != np.sum(val_recall):
  raise Exception("Something is wrong, num. relevant returned reported by trec_eval (%d) isn't equal the sum of query-specific values (%d)" % (numRelRet, np.sum(val_recall)))
recall=numRelRet/numRel
reportText=""
reportText += "recall:          %f" % recall
reportText += "\n"
reportText += "recall@10:       %f" % (np.sum(val_r_10)/queryQty)
reportText += "\n"
reportText += "# of queries:    %d" % queryQty
reportText += "\n"
reportText += "got correct:     %d" % int(gotRight)
reportText += "\n"
reportText += "p@1:             %f" % precision_at_1_overall
reportText += "\n"
reportText += "MRR:             %f" % mrr_overall
reportText += "\n"
reportText += "MAP:             %f" % map_overall
reportText += "\n"
sys.stdout.write(reportText)

fRep=open(outPrefix +'.rep','w')
fRep.write(reportText)
fRep.close()

fTrecEval=open(outPrefix +'.trec_eval','w')
for line in output:
  fTrecEval.write(line.rstrip() + '\n')
fTrecEval.close()
saveTsv(outPrefix + '.P@1',       val_p_1)
saveTsv(outPrefix + '.recall',    val_recall)
saveTsv(outPrefix + '.map',       val_map)
saveTsv(outPrefix + '.recall@10', val_r_10)

