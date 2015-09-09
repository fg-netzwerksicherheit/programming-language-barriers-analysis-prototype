CC = gcc
CFLAGS = -fPIC -pthread -O3

JAVAH = javah
CLASSPATH = bin
JNI_INCLUDES = -I/usr/lib/jvm/java-1.7.0/include -I/usr/lib/jvm/java-1.7.0/include/linux

all: SingleInstanceJniForwarder

SingleInstanceJniForwarder: javah
	$(CC) $(CFLAGS) $(JNI_INCLUDES) -lrt -o SingleInstanceJniForwarder.so src_c/SingleInstanceJniForwarder.c -shared

javah:
	$(JAVAH) -cp $(CLASSPATH) -o src_c/SingleInstanceJniForwarder.h data_producer.generator.SingleInstanceJniForwarder

clean:
	-rm -f src_c/SingleInstanceJniForwarder.h
	-rm -f SingleInstanceJniForwarder.so

