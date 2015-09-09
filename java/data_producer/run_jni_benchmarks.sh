#!/bin/bash

PREFIX="test-2-core-i7-java-producer"
DURATION=15
SINGLE_RUN_CMD="./single_jni_benchmark.sh"

mkfifo default.fifo

#for j in 10 50 100 250 500 1000 10000
for j in 4 10 100 1000
do
    echo "Size: $j"

    $SINGLE_RUN_CMD ${PREFIX} $j 1 $DURATION

    for i in $(seq 2 2 6)
    do
        $SINGLE_RUN_CMD ${PREFIX} $j $i $DURATION
    done

    for i in $(seq 10 5 20)
    do
        $SINGLE_RUN_CMD ${PREFIX} $j $i $DURATION
    done

    for i in $(seq 40 20 80)
    do
        $SINGLE_RUN_CMD ${PREFIX} $j $i $DURATION
    done

    for i in $(seq 100 100 200)
    do
        $SINGLE_RUN_CMD ${PREFIX} $j $i $DURATION
    done

    for i in $(seq 400 200 1000)
    do
        $SINGLE_RUN_CMD ${PREFIX} $j $i $DURATION
    done
done

