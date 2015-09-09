#!/bin/bash

PREFIX=$1
DATA_INSTANCE_SIZE=$2
BULK_SIZE=$3
DURATION=$4
OUTPUT_PREFIX="${PREFIX}_jni_s_${DATA_INSTANCE_SIZE}_b_${BULK_SIZE}"

echo "Running prefix: ${OUTPUT_PREFIX}"

vmstat -n 1 &> ${OUTPUT_PREFIX}_vmstat.raw &

echo "Sleeping..."
sleep 2s

java -jar data_producer.jar -d ${DURATION} -m jni -b ${BULK_SIZE} -s ${DATA_INSTANCE_SIZE} &> ${OUTPUT_PREFIX}_data-producer.raw

echo "Sleeping after benchmark..."
sleep 2s

killall vmstat

