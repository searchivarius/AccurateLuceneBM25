#!/usr/bin/env python
import argparse
import sys
import math
import numpy as np
import subprocess as sp

def Usage(err):
  if not err is None:
    print err
  print "Usage: <path to gdeval script> <qrel file> <trec-format output file> <prefix of report files> "
  sys.exit(1)

def saveTsv(fname, arr):
  f=open(fname,'w')
  f.write("\t".join(str(s) for s in arr) + '\n')
  f.close()

ERR20  = 'err@20'
NDCG20 = 'ndcg@20'

def readResults(lines):
  prevId=''
  res=dict()
  f=True
  for s in lines:
    if s == '': continue
    if f:
      f=False
      continue
    arr=s.split(',')
    if (len(arr) != 4):
      raise Exception("wrong-format line: '%s'" % s)
    (runid, qid, ndcg, err) = arr
    if not qid in res: res[qid]=dict()
    entry=res[qid]
    entry[NDCG20]=float(ndcg)
    entry[ERR20] =float(err)
  return res
  

if len(sys.argv) != 5:
  Usage(None)

gdevalScript=sys.argv[1]
qrelFile=sys.argv[2]
trecOut=sys.argv[3]
outPrefix = sys.argv[4]
output=sp.check_output([gdevalScript, qrelFile, trecOut]).replace('\t', ' ').split('\n')
res=readResults(output)

err20_overall=res['amean'][ERR20]
ndcg20_overall=res['amean'][NDCG20]
queryQty=0

val_ndcg20   = []
val_err20    = []

for qid, entry in res.iteritems():
  if qid == 'amean': continue
  queryQty+=1
  val_ndcg20.append(entry[NDCG20])
  val_err20.append(entry[ERR20])

reportText=""
reportText += "ndcg@20:          %f" % ndcg20_overall
reportText += "\n"
reportText += "err@20:           %f" % err20_overall
reportText += "\n"
reportText += "# of queries:     %d" % queryQty
reportText += "\n"

sys.stdout.write(reportText)

fRep=open(outPrefix +'.rep','w')
fRep.write(reportText)
fRep.close()

fGdeval=open(outPrefix +'.gdeval','w')
for line in output:
  fGdeval.write(line.rstrip() + '\n')
fGdeval.close()
saveTsv(outPrefix + '.ndcg@20',       val_ndcg20)
saveTsv(outPrefix + '.err@20',        val_err20)

