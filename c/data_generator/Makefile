CC = gcc
CFLAGS = -fPIC -pthread -O3

all: data_generator

data_generator: data_generator_lib.so
	$(CC) $(CFLAGS) -largtable2 -Wl,-rpath=./ -o data_generator data_generator.c data_generator_lib.so

data_generator_lib.so:
	$(CC) $(CFLAGS) -lrt -o data_generator_lib.so data_generator_lib.c -shared

clean:
	-rm -f *.so
	-rm -f data_generator

