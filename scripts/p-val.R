#!/usr/bin/env Rscript
args = commandArgs(trailingOnly=TRUE)
if (length(args)!=2) {
  stop("Usage: <the first single-line file> <the second single-line file>\n", call.=FALSE)
}
x<-as.numeric(read.table(args[1]))
print(paste(args[1], " mean: ", mean(x)))
y<-as.numeric(read.table(args[2]))
print(paste(args[2], " mean: ", mean(y)))
print(paste("Mean ratio: ", mean(x)/mean(y)))
t.test(x,y,paired=T)
