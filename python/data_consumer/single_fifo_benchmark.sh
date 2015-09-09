#!/bin/bash

PREFIX=$1
DATA_INSTANCE_SIZE=$2
BULK_SIZE=$3
DURATION=$4
OUTPUT_PREFIX="${PREFIX}_fifo_s_${DATA_INSTANCE_SIZE}_b_${BULK_SIZE}"

mkfifo default.fifo

echo "Running prefix: ${OUTPUT_PREFIX}"

vmstat -n 1 &> ${OUTPUT_PREFIX}_vmstat.raw &
./data_consumer_from_file.py -d ${DURATION} -b ${BULK_SIZE} -s ${DATA_INSTANCE_SIZE} -F default.fifo &> ${OUTPUT_PREFIX}_data-consumer.raw &

echo "Sleeping..."
sleep 2s

LD_LIBRARY_PATH=../../c/data_generator ../../c/data_generator/data_generator -Cfile_out_cb -Fdefault.fifo -s ${DATA_INSTANCE_SIZE} -d $(($DURATION - 4)) -S 1000 -b ${BULK_SIZE} &> ${OUTPUT_PREFIX}_data-generator.raw &

echo "Sleeping while running benchmark..."
sleep ${DURATION}

killall vmstat

rm -f default.fifo


