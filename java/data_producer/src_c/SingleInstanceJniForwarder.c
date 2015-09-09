/*
 *   Copyright 2015, Frankfurt University of Applied Sciences
 *
 *   This software is released under the terms of the Eclipse Public License 
 *   (EPL) 1.0. You can find a copy of the EPL at: 
 *   http://opensource.org/licenses/eclipse-1.0.php
 *
 */

#include "SingleInstanceJniForwarder.h"
#include <pthread.h>
#include <time.h>
#include <unistd.h>

static long bulk_size;
static long invocation_count;
static struct timespec tmp_time_1, tmp_time_2;
static useconds_t stats_output_interval;
static pthread_t stats_out_thread;

void *stats_handler(void *msec);
void stats_output(struct timespec *time_1, struct timespec *time_2, int count);
double timespec_delta(struct timespec start, struct timespec end);

JNIEXPORT void JNICALL Java_data_1producer_generator_SingleInstanceJniForwarder_init
  (JNIEnv *env, jobject obj, jint bulk_s, jint stats_output_int)
{
    bulk_size = bulk_s;
    invocation_count = 0;
    clock_gettime(CLOCK_REALTIME, &tmp_time_1);
    stats_output_interval = stats_output_int * 1000;

    if (stats_output_interval > 0) {
        pthread_create(&stats_out_thread, NULL, stats_handler, NULL);
    }
}

JNIEXPORT void JNICALL Java_data_1producer_generator_SingleInstanceJniForwarder_forwardData
  (JNIEnv *env, jobject obj, jobject data)
{
    invocation_count++;
}

void *stats_handler(void *msec)
{
    unsigned long delta;
    unsigned long invocation_count_old = 0;

    for (;;) {
        usleep(stats_output_interval);
        delta = invocation_count - invocation_count_old;
        stats_output(&tmp_time_1, &tmp_time_2, delta * bulk_size);
        invocation_count_old = invocation_count;
    }
}

void stats_output(struct timespec *time_1, struct timespec *time_2, int count)
{
    double elapsed_time;

    clock_gettime(CLOCK_REALTIME, time_2);

    elapsed_time = timespec_delta(*time_1, *time_2);

    *time_1 = *time_2;

    fprintf(stdout, "rdips: %f\n", (((double) count) / elapsed_time));
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

