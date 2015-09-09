CC = gcc
CFLAGS = -fPIC -pthread -O3

all: ctypes_data_receiver_test

ctypes_data_receiver_test:
	$(CC) $(CFLAGS) -lrt -o ctypes_data_receiver_test.so ctypes_data_receiver_test_c-code.c -shared

clean:
	-rm -f ctypes_data_receiver_test.so

