#!/bin/bash

PREFIX="test-c-to-java"
DURATION=60
SINGLE_RUN_CMD="./single_jni-direct_benchmark.sh"

#for j in 10 50 100 250 500 1000 10000
for j in 4 64 1000
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

    for i in $(seq 400 200 800)
    do
        $SINGLE_RUN_CMD ${PREFIX} $j $i $DURATION
    done

    for i in $(seq 1000 1000 2000)
    do
        $SINGLE_RUN_CMD ${PREFIX} $j $i $DURATION
    done

    for i in $(seq 4000 2000 10000)
    do
        $SINGLE_RUN_CMD ${PREFIX} $j $i $DURATION
    done
done

