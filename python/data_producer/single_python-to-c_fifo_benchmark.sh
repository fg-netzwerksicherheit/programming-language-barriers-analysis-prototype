#!/bin/bash

PREFIX=$1
DATA_INSTANCE_SIZE=$2
BULK_SIZE=$3
DURATION=$4
OUTPUT_PREFIX="${PREFIX}_fifo_s_${DATA_INSTANCE_SIZE}_b_${BULK_SIZE}"

echo "Running prefix: ${OUTPUT_PREFIX}"

mkfifo default.fifo
vmstat -n 1 &> ${OUTPUT_PREFIX}_vmstat.raw &

../../c/data_consumer/data_consumer_from_file -Fdefault.fifo -s ${DATA_INSTANCE_SIZE} -d $(($DURATION + 1000)) -S 1000 -b ${BULK_SIZE} &> ${OUTPUT_PREFIX}_data-consumer.raw &

echo "Sleeping..."
sleep 2s

./data_producer_to_file.py -d ${DURATION} -b ${BULK_SIZE} -s ${DATA_INSTANCE_SIZE} &> ${OUTPUT_PREFIX}_data-producer.raw

echo "Sleeping after benchmark..."
sleep 4s

killall vmstat
rm -f default.fifo

