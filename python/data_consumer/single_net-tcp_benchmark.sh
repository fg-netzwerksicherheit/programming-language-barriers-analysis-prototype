#!/bin/bash

PREFIX=$1
DATA_INSTANCE_SIZE=$2
BULK_SIZE=$3
DURATION=$4
OUTPUT_PREFIX="${PREFIX}_net-tcp_s_${DATA_INSTANCE_SIZE}_b_${BULK_SIZE}"

echo "Running prefix: ${OUTPUT_PREFIX}"

vmstat -n 1 &> ${OUTPUT_PREFIX}_vmstat.raw &
./data_consumer_from_net_tcp.py -d ${DURATION} -b ${BULK_SIZE} -s ${DATA_INSTANCE_SIZE} &> ${OUTPUT_PREFIX}_data-consumer.raw &

echo "Sleeping..."
sleep 2s

LD_LIBRARY_PATH=../../c/data_generator ../../c/data_generator/data_generator -Cnet_tcp_out_cb -H127.0.0.1 -s ${DATA_INSTANCE_SIZE} -d $(($DURATION - 4)) -S 1000 -b ${BULK_SIZE} &> ${OUTPUT_PREFIX}_data-generator.raw &

echo "Sleeping while running benchmark..."
sleep ${DURATION}

killall vmstat

