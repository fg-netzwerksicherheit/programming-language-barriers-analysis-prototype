CC = gcc
CFLAGS = -fPIC -pthread -O3

all: data_consumer_from_file data_consumer_from_net_tcp

data_consumer_from_file:
	$(CC) $(CFLAGS) -lrt -largtable2 -o data_consumer_from_file data_consumer_from_file.c

data_consumer_from_net_tcp:
	$(CC) $(CFLAGS) -lrt -largtable2 -o data_consumer_from_net_tcp data_consumer_from_net_tcp.c

clean:
	-rm -f data_consumer_from_file
	-rm -f data_consumer_from_net_tcp

