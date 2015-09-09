#!/bin/bash

PREFIX=$1
DATA_INSTANCE_SIZE=$2
BULK_SIZE=$3
DURATION=$4
OUTPUT_PREFIX="${PREFIX}_jni-full-copy_s_${DATA_INSTANCE_SIZE}_b_${BULK_SIZE}"

echo "Running prefix: ${OUTPUT_PREFIX}"

vmstat -n 1 &> ${OUTPUT_PREFIX}_vmstat.raw &
java -jar data_consumer-full-copy.jar -d ${DURATION} -m jni -b ${BULK_SIZE} -s ${DATA_INSTANCE_SIZE} &> ${OUTPUT_PREFIX}_data-consumer.raw &

echo "Sleeping..."
sleep 2s

echo "Sleeping while running benchmark..."
sleep ${DURATION}

killall vmstat

