CC = gcc
CFLAGS = -fPIC -O3

JAVAH = javah
CLASSPATH = bin
JNI_INCLUDES = -I/usr/lib/jvm/java-1.7.0/include -I/usr/lib/jvm/java-1.7.0/include/linux
DATA_GENERATOR_LIB_INCLUDE = -I../../c/data_generator

all: DataGenerator

DataGenerator: javah
	$(CC) $(CFLAGS) $(JNI_INCLUDES) $(DATA_GENERATOR_LIB_INCLUDE) -l:../../c/data_generator/data_generator_lib.so -Wl,-rpath=../../c/data_generator -o DataGenerator.so src_c/DataGenerator.c -shared

javah:
	$(JAVAH) -cp $(CLASSPATH) -o src_c/DataGenerator.h data_consumer.jni.DataGenerator

clean:
	-rm -f src_c/DataGenerator.h
	-rm -f DataGenerator.so

