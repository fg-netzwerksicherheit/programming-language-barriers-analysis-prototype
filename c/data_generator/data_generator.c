/*
 *   Copyright 2015, Frankfurt University of Applied Sciences
 *
 *   This software is released under the terms of the Eclipse Public License 
 *   (EPL) 1.0. You can find a copy of the EPL at: 
 *   http://opensource.org/licenses/eclipse-1.0.php
 *
 */

#include "data_generator_lib.h"
#include <argtable2.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <arpa/inet.h>
#include <sys/socket.h>

#define TCP_PORT 38907

void write_to_file(FILE *fp, const void *data, int size);
void send_via_socket(int socket_descriptor, const void *data, int size);

void file_out_cb(const void *data, int size, void *user_data);
void net_tcp_out_cb(const void *data, int size, void *user_data);
void no_op_cb(const void *data, int size, void *user_data);
void stdout_cb(const void *data, int size, void *user_data);

static int data_instance_size_size;

/*
 * Examples for calling the data generator main function:
 *
 * ./data_generator -Cstdout_cb -s 15 -c 10
 * ./data_generator -Cno_op_cb -s 1000 -c 100000 -i 10000
 * ./data_generator -Cfile_out_cb -Ffoo.out -s 1500 -c 100
 * nc -l 127.0.0.1 38907 & ./data_generator -Cnet_tcp_out_cb -H127.0.0.1 -s 100 -c 10000
 */
int main (int argc, char **argv)
{
    int bulk_size, i, nerrors, socket_descriptor = -1;
    data_processing_cb cb = NULL;
    FILE *fp = NULL;
    void *user_data = NULL;
    struct sockaddr_in server_address;
    unsigned short server_port = TCP_PORT;

    fprintf(stdout, "Dummy data used for benchmarking will be emitted here on stdout\n");
    fprintf(stderr, "Benchmark stats data will be emitted here on stderr.\n\n");

    /*
     * Define command line args.
     */
    struct arg_int *data_instance_size = arg_int0("s", "data_instance_size", "<bytes>", "Size of a single data instance in bytes.");
    struct arg_int *data_instance_count = arg_int0("c", "data_instance_count", "<int>", "Number of data instances to generate. A value smaller equals 0 will trigger the inifinte generation.");
    struct arg_int *data_generation_duration = arg_int0("d", "data_generation_duration", "<sec>", "Generate data for the specified number of seconds. Note: data generation will stop either after the specified number of seconds, or, if data_instance_count is set, after the specified number of data instances was generated, depending on which criterion is reached first.");
    struct arg_int *bulk_size_arg = arg_int0("b", "bulk_size", "<int>", "Number of data instances to be transfered in a bulk. Used with -Cfile_out_cb and -Cnet_tcp_out_cb.");
    struct arg_int *stats_interval = arg_int0("S", "stats_interval", "<msec>", "Interval in which stats are emitted in msec.");
    struct arg_str *cb_name = arg_str0("C", "cb_name", "<string>", "Name of the data processing callback function");
    struct arg_str *file_name = arg_str0("F", "file_name", "<string>", "Name of the file to which the data will be written. Used with -Cfile_out_cb.");
    struct arg_str *host_ip = arg_str0("H", "host_ip", "<string>", "IP of the host to which the data will be sent. Used with -Cnet_tcp_out_cb.");
    struct arg_lit *help = arg_lit0("h", "help", "Print help.");
    struct arg_end *end = arg_end(20);

    void* argtable[] = {data_instance_size, data_instance_count, data_generation_duration, bulk_size_arg, stats_interval, cb_name, file_name, host_ip, help, end};

    if (arg_nullcheck(argtable) != 0) {
        fprintf(stderr, "Error: argtable, insufficient memory\n");
    }

    /*
     * Set command line args default values.
     */
    for (i=0; i < data_instance_size->hdr.maxcount; i++) {
        data_instance_size->ival[i]=100;
    }
    for (i=0; i < data_instance_count->hdr.maxcount; i++) {
        data_instance_count->ival[i]=0;
    }
    for (i=0; i < data_generation_duration->hdr.maxcount; i++) {
        data_generation_duration->ival[i]=0;
    }
    for (i=0; i < bulk_size_arg->hdr.maxcount; i++) {
        bulk_size_arg->ival[i]=1;
    }
    for (i=0; i < stats_interval->hdr.maxcount; i++) {
        stats_interval->ival[i]=0;
    }

    /*
     * Parse args, handle possible errors, and print help if needed.
     */
    nerrors = arg_parse(argc, argv, argtable);

    if (nerrors != 0) {
        fprintf(stderr, "An error occurred while parsing the command line args.\n");
        arg_print_glossary(stderr, argtable, " %-25s %s\n");

        arg_freetable(argtable, sizeof(argtable)/sizeof(argtable[0]));
        return 0;
    }

    if (help->count > 0) {
        fprintf(stderr, "Available options are:\n");
        arg_print_glossary(stderr, argtable, " %-25s %s\n");

        arg_freetable(argtable, sizeof(argtable)/sizeof(argtable[0]));
        return 0;
    }

    if (bulk_size_arg->ival[0] < 1) {
        fprintf(stderr, "Bulk size must be bigger 0.\n");
        arg_freetable(argtable, sizeof(argtable)/sizeof(argtable[0]));
        return -1;
    }
    /*
     * Finished handling command line args stuff.
     */

    /*
     * Determine callback function and if necessary setup things like opening
     * files or network connections.
     */
    const char *cb_name_str = cb_name->sval[0];
    const char *file_name_str = file_name->sval[0];
    const char *host_ip_str = host_ip->sval[0];
    bulk_size = bulk_size_arg->ival[0];

    if (strcmp(cb_name_str, "no_op_cb") == 0) {
        cb = no_op_cb;
    } else if (strcmp(cb_name_str, "file_out_cb") == 0) {
        fp = fopen(file_name_str, "w");

        if (fp == NULL) {
            fprintf(stderr, "Error opening file for writing: %s", file_name_str);
            arg_freetable(argtable, sizeof(argtable)/sizeof(argtable[0]));
            return -1;
        }

        user_data = fp;
        cb = file_out_cb;
    } else if (strcmp(cb_name_str, "net_tcp_out_cb") == 0) {
        socket_descriptor = socket(PF_INET, SOCK_STREAM, IPPROTO_TCP);
        if (socket_descriptor < 0) {
            fprintf(stderr, "Error creating socket.");
            arg_freetable(argtable, sizeof(argtable)/sizeof(argtable[0]));
            return -1;
        }

        memset(&server_address, 0, sizeof(server_address));
        server_address.sin_family = AF_INET;
        server_address.sin_addr.s_addr = inet_addr(host_ip_str);
        server_address.sin_port = htons(server_port);

        if (connect(socket_descriptor, (struct sockaddr *) &server_address, sizeof(server_address)) < 0) {
            fprintf(stderr, "Error creating socket.");
            arg_freetable(argtable, sizeof(argtable)/sizeof(argtable[0]));
            return -1;
        }

        user_data = &socket_descriptor;
        cb = net_tcp_out_cb;
    } else {
        cb = stdout_cb;
    }

    fprintf(stderr, "data_instance_size: %i, data_instance_count: %i, data_generation_duration: %i, bulk_size: %i, stats_interval: %i, cb_name: %s\n\n",
            data_instance_size->ival[0], data_instance_count->ival[0], data_generation_duration->ival[0], bulk_size, stats_interval->ival[0], cb_name_str);

    if (data_instance_size->ival[0] <= 255) {
        data_instance_size_size = sizeof(char);
    } else if (data_instance_size->ival[0] <= 65535) {
        data_instance_size_size = sizeof(short);
    } else {
        data_instance_size_size = sizeof(int);
    }


    if (bulk_size == 1) {
        generate_data(data_instance_size->ival[0], data_instance_count->ival[0],
                      data_generation_duration->ival[0], stats_interval->ival[0], cb, user_data);
    } else if (bulk_size > 1) {
        generate_data_bulk_method_double_buffer(data_instance_size->ival[0], data_instance_count->ival[0],
                                                data_generation_duration->ival[0], stats_interval->ival[0], bulk_size, cb, user_data);
    } else {
        fprintf(stderr, "Error: bulk size smaller zero is not allowed.");
    }

    if (fp != NULL) {
        fclose(fp);
    }
    if (socket_descriptor >= 0) {
        close(socket_descriptor);
    }

    arg_freetable(argtable, sizeof(argtable)/sizeof(argtable[0]));
    return 0;
}

void write_to_file(FILE *fp, const void *data, int size)
{
    int count = fwrite(data, sizeof(char), size, fp);
    if (count != size) {
        fprintf(stderr, "Write count does not match data size. write_count: %i, data_size: %i", count, size);
    }
}

void file_out_cb(const void *data, int size, void *user_data)
{
    write_to_file((FILE *) user_data, data, size);
}

void send_via_socket(int socket_descriptor, const void *data, int size)
{
    int sent_count = send(socket_descriptor, data, size, 0);
    if (sent_count != size) {
        fprintf(stderr, "Send count does not match data size. write_count: %i, data_size: %i", sent_count, size);
    }
}

void net_tcp_out_cb(const void *data, int size, void *user_data)
{
    send_via_socket(*((int *) user_data), data, size);
}

void no_op_cb(const void *data, int size, void *user_data)
{
}

void stdout_cb(const void *data, int size, void *user_data)
{
    fprintf(stdout, "%.*s\n", size, data);
}

