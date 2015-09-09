/*
 *   Copyright 2015, Frankfurt University of Applied Sciences
 *
 *   This software is released under the terms of the Eclipse Public License 
 *   (EPL) 1.0. You can find a copy of the EPL at: 
 *   http://opensource.org/licenses/eclipse-1.0.php
 *
 */

#include "data_generator_lib.h"
#include <pthread.h>
#include <signal.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <time.h>
#include <unistd.h>

typedef int bool;
#define true 1
#define false 0

typedef struct bulk_method_double_buffer_cb_user_data {
    data_processing_cb cb;
    void *original_user_data;
    int bulk_size;
    bulk_buffer *write_buffer;
    bulk_buffer *read_buffer;
} bulk_method_double_buffer_cb_user_data;

static void *data;
static unsigned long invocation_count;
static bool running;
static struct timespec tmp_time_1, tmp_time_2;
static int data_instance_size_size;

void *shutdown_handler(void *sec);
void sig_int_handler(int signal_number);
void *stats_handler(void *msec);
void stats_output(struct timespec *time_1, struct timespec *time_2, int count);
double timespec_delta(struct timespec start, struct timespec end);

void bulk_method_double_buffer_cb(const void *data, int size, void *user_data);

int generate_data(int data_instance_size, int data_instance_max_count, int data_generation_duration,
                  int stats_output_interval, data_processing_cb cb, void *user_data)
{
    double elapsed_time = 0;
    int i = 0;
    struct timespec start_time, end_time;
    pthread_t shutdown_thread, stats_out_thread;
    running = true;
    invocation_count = 0;

    if (cb == NULL) {
        fprintf(stderr, "Error: cb not set.\n");
        return -1;
    }

    data = malloc(data_instance_size);
    if (data == NULL) {
        return DATA_INSTANCE_MEMORY_ALLOCATION_ERROR;
    }
    memset(data, 'a', data_instance_size);

    if (signal(SIGINT, sig_int_handler) == SIG_ERR) {
        fprintf(stderr, "Note: We won't be able to catch SIGINT.\n");
    }

    if (stats_output_interval > 0) {
        pthread_create(&stats_out_thread, NULL, stats_handler, &stats_output_interval);
        /* Give stats_out_thread a head start such that if stats_output_interval is 1s and
         * data_generation_duration is 5s, stats output is emitted 5 times.
         */
        usleep(1000);
    }

    if (data_generation_duration > 0) {
        pthread_create(&shutdown_thread, NULL, shutdown_handler, &data_generation_duration);
    }

    clock_gettime(CLOCK_REALTIME, &tmp_time_1);
    clock_gettime(CLOCK_REALTIME, &start_time);
    if (data_instance_max_count > 0) {
        fprintf(stderr, "Will generate at most %i data instances.\n", data_instance_max_count);

        for (i = 0; i < data_instance_max_count; i++) {
            cb(data, data_instance_size, user_data);
            invocation_count++;

            if (!running) {
                break;
            }
        }
    } else {
        while (running) {
            cb(data, data_instance_size, user_data);
            invocation_count++;
        }
    }
    clock_gettime(CLOCK_REALTIME, &end_time);
    elapsed_time = timespec_delta(start_time, end_time);

    fprintf(stderr, "Start time: %i.%i\n", start_time.tv_sec, start_time.tv_nsec);
    fprintf(stderr, "End time: %i.%i\n", end_time.tv_sec, end_time.tv_nsec);
    fprintf(stderr, "Elapsed time: %fs\n", elapsed_time);
    fprintf(stderr, "Data instances/second: %f\n", (((double) invocation_count) / elapsed_time));

    free(data);
    return DATA_GENERATOR_SUCCESS;
}

int generate_data_bulk_method_double_buffer
    (int data_instance_size, int data_instance_max_count, int data_generation_duration,
     int stats_output_interval, int bulk_size, data_processing_cb cb, void *user_data)
{
    bulk_method_double_buffer_cb_user_data cb_user_data;

    cb_user_data.cb = cb;
    cb_user_data.original_user_data = user_data;
    cb_user_data.bulk_size = bulk_size;

    int bulk_buffer_entry_size = data_instance_size;

    if (bulk_buffer_entry_size <= 255) {
        data_instance_size_size = sizeof(char);
    } else if (bulk_buffer_entry_size <= 65535) {
        data_instance_size_size = sizeof(short);
    } else {
        data_instance_size_size = sizeof(int);
    }

    fprintf(stderr, "Data instance size: %i\n", data_instance_size);
    fprintf(stderr, "Data instance size size: %i\n", data_instance_size_size);

    bulk_buffer_entry_size += data_instance_size_size;
    
    fprintf(stderr, "Bulk buffer entry size: %i\n", bulk_buffer_entry_size);

    bulk_buffer wb_tmp;
    wb_tmp.data = malloc(bulk_buffer_entry_size * bulk_size);
    wb_tmp.bytes = 0;
    wb_tmp.data_instance_count = 0;
    cb_user_data.write_buffer = &wb_tmp;
    bulk_buffer rb_tmp;
    rb_tmp.data = malloc(bulk_buffer_entry_size * bulk_size);
    rb_tmp.bytes = 0;
    rb_tmp.data_instance_count = 0;
    cb_user_data.read_buffer = &rb_tmp;

    return generate_data(data_instance_size, data_instance_max_count, data_generation_duration,
                         stats_output_interval, bulk_method_double_buffer_cb, &cb_user_data);
}

void bulk_method_double_buffer_cb(const void *data, int size, void *user_data)
{
    bulk_method_double_buffer_cb_user_data *cb_user_data = (bulk_method_double_buffer_cb_user_data*) user_data;

    int offset = 0;
    char *data_ptr = (char *) cb_user_data->write_buffer->data;

    memcpy((void *) (data_ptr + cb_user_data->write_buffer->bytes), &(size), data_instance_size_size);
    offset += data_instance_size_size;

    memcpy((void *) (data_ptr + cb_user_data->write_buffer->bytes + offset), (void *) data, size);
    cb_user_data->write_buffer->bytes += offset + size;
    cb_user_data->write_buffer->data_instance_count++;

    if (cb_user_data->write_buffer->data_instance_count < cb_user_data->bulk_size) {
        return;
    }

    bulk_buffer *tmp_buffer = cb_user_data->read_buffer;
    cb_user_data->read_buffer = cb_user_data->write_buffer;
    cb_user_data->write_buffer = tmp_buffer;
    cb_user_data->write_buffer->bytes = 0;
    cb_user_data->write_buffer->data_instance_count = 0;

    cb_user_data->cb(cb_user_data->read_buffer->data, cb_user_data->read_buffer->bytes, cb_user_data->original_user_data);
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

void *stats_handler(void *msec)
{
    unsigned long delta;
    unsigned long invocation_count_old = 0;
    useconds_t interval = (*(useconds_t *)msec) * 1000;

    for (;;) {
        usleep(interval);
        delta = invocation_count - invocation_count_old;
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

    fprintf(stderr, "dips: %f\n", (((double) count) / elapsed_time));
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

