/*
 *   Copyright 2015, Frankfurt University of Applied Sciences
 *
 *   This software is released under the terms of the Eclipse Public License 
 *   (EPL) 1.0. You can find a copy of the EPL at: 
 *   http://opensource.org/licenses/eclipse-1.0.php
 *
 */

#include <argtable2.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <pthread.h>
#include <signal.h>
#include <time.h>
#include <unistd.h>

typedef int bool;
#define true 1
#define false 0

void *shutdown_handler(void *sec);
void sig_int_handler(int signal_number);
void *stats_handler(void *msec);

void stats_output(struct timespec *time_1, struct timespec *time_2, int count);
double timespec_delta(struct timespec start, struct timespec end);

static unsigned long invocation_count;
static bool running;
static struct timespec tmp_time_1, tmp_time_2;

typedef struct stats_out_args {
    int stats_output_interval;
    int bulk_size;
} stats_out_args;

int main (int argc, char **argv)
{
    int i, buffer_size, bulk_size, data_instance_size, nerrors, read_size;
    FILE *fp = NULL;
    pthread_t shutdown_thread, stats_out_thread;
    void *data;

    running = true;
    invocation_count = 0;

    /*
     * Define command line args.
     */
    struct arg_int *data_instance_size_arg = arg_int0("s", "data_instance_size", "<bytes>", "Size of a single data instance in bytes.");
    struct arg_int *bulk_size_arg = arg_int0("b", "bulk_size", "<int>", "Number of data instances to be transfered in a bulk. Used with -Cfile_out_cb and -Cnet_tcp_out_cb.");
    struct arg_int *run_duration = arg_int0("d", "run_duration", "<sec>", "Duration for how long the tool is run.");
    struct arg_int *stats_interval = arg_int0("S", "stats_interval", "<msec>", "Interval in which stats are emitted in msec.");
    struct arg_str *file_name = arg_str0("F", "file_name", "<string>", "Name of the file to which the data will be written.");
    struct arg_lit *help = arg_lit0("h", "help", "Print help.");
    struct arg_end *end = arg_end(20);

    void* argtable[] = {data_instance_size_arg, bulk_size_arg, run_duration, stats_interval, file_name, help, end};

    /*
     * Set command line args default values.
     */
    for (i=0; i < data_instance_size_arg->hdr.maxcount; i++) {
        data_instance_size_arg->ival[i]=100;
    }
    for (i=0; i < run_duration->hdr.maxcount; i++) {
        run_duration->ival[i]=0;
    }
    for (i=0; i < bulk_size_arg->hdr.maxcount; i++) {
        bulk_size_arg->ival[i]=1;
    }
    for (i=0; i < stats_interval->hdr.maxcount; i++) {
        stats_interval->ival[i]=1000;
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
    /*
     * Finished handling command line args stuff.
     */

    data_instance_size = data_instance_size_arg->ival[0];
    bulk_size = bulk_size_arg->ival[0];
    if (bulk_size > 1) {
        if (data_instance_size <= 255) {
            data_instance_size += sizeof(char);
        } else if (data_instance_size <= 65535) {
            data_instance_size += sizeof(short);
        } else {
            data_instance_size += sizeof(int);
        }
    }
    buffer_size = data_instance_size * bulk_size;

    data = malloc(buffer_size);
    if (data == NULL) {
        fprintf(stderr, "Error allocating memory.");
        arg_freetable(argtable, sizeof(argtable)/sizeof(argtable[0]));
        return -1;
    }

    if (signal(SIGINT, sig_int_handler) == SIG_ERR) {
        fprintf(stderr, "Note: We won't be able to catch SIGINT.\n");
    }

    struct stats_out_args stats_args;
    stats_args.stats_output_interval = stats_interval->ival[0];
    stats_args.bulk_size = bulk_size;
    if (stats_args.stats_output_interval > 0) {
        pthread_create(&stats_out_thread, NULL, stats_handler, &stats_args);
        /* Give stats_out_thread a head start such that if stats_output_interval is 1s and
         * data_generation_duration is 5s, stats output is emitted 5 times.
         */
        usleep(1000);
    }

    const char *file_name_str = file_name->sval[0];
    fp = fopen(file_name_str, "r");
    if (fp == NULL) {
        fprintf(stderr, "Error opening file for reading: %s", file_name_str);
        arg_freetable(argtable, sizeof(argtable)/sizeof(argtable[0]));
        return -1;
    }

    pthread_create(&shutdown_thread, NULL, shutdown_handler, &(run_duration->ival[0]));

    while (running) {
        read_size = fread(data, 1, buffer_size, fp);
        if (read_size != buffer_size) {
            fprintf(stderr, "read_size (%i) does not match buffer_size (%i).", read_size, buffer_size);
            break;
        }
        invocation_count++;
    }

    if (fp != NULL) {
        fclose(fp);
    }
    
    arg_freetable(argtable, sizeof(argtable)/sizeof(argtable[0]));
    return 0;
}

void *shutdown_handler(void *sec)
{
    sleep(*(unsigned int *)sec);
    fprintf(stderr, "Terminating data generation via shutdown_handler.\n");
    running = false;
}

void sig_int_handler(int signal_number)
{
    if (signal_number == SIGINT) {
        fprintf(stderr, "Terminating data generation due to signal.\n");
        running = false;
    }
}

void *stats_handler(void *arguments)
{
    struct stats_out_args *args = (stats_out_args *) arguments;
    unsigned long delta;
    unsigned long invocation_count_old = 0;
    useconds_t interval = (args->stats_output_interval) * 1000;

    for (;;) {
        usleep(interval);
        delta = (invocation_count - invocation_count_old) * args->bulk_size;
        stats_output(&tmp_time_1, &tmp_time_2, delta);
        invocation_count_old = invocation_count;
    }
}

void stats_output(struct timespec *time_1, struct timespec *time_2, int count)
{
    double elapsed_time;

    clock_gettime(CLOCK_REALTIME, time_2);

    elapsed_time = timespec_delta(*time_1, *time_2);

    *time_1 = *time_2;

    fprintf(stderr, "rdips: %f\n", (((double) count) / elapsed_time));
}

// Based on: http://www.guyrutenberg.com/2007/09/22/profiling-code-using-clock_gettime/
double timespec_delta(struct timespec start, struct timespec end)
{
    struct timespec temp;
    if ((end.tv_nsec - start.tv_nsec) < 0) {
        temp.tv_sec = end.tv_sec - start.tv_sec - 1;
        temp.tv_nsec = 1000000000 + end.tv_nsec - start.tv_nsec;
    } else {
        temp.tv_sec = end.tv_sec - start.tv_sec;
        temp.tv_nsec = end.tv_nsec - start.tv_nsec;
    }
    return ((temp.tv_sec * 1000000000.0) + temp.tv_nsec) / 1000000000.0;
}

