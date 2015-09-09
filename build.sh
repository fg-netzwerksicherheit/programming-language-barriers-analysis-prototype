#!/bin/bash

echo "Building C-based data generator."
cd c/data_generator
make clean
make

echo -e "\nBuilding C-based data consumer."
cd ../data_consumer
make clean
make

echo -e "\nBuilding Java-based data producer."
cd ../../java/data_producer
ant
make clean
make

echo -e "\nBuilding Java-based data consumer."
cd ../data_consumer
ant
make clean
make

echo -e "\nBuilding Python-based data producer c-based library."
cd ../../python/data_producer
make clean
make

echo "Build finised."
cd ../..

