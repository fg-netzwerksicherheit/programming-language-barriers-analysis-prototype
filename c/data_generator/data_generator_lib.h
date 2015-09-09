/*
 *   Copyright 2015, Frankfurt University of Applied Sciences
 *
 *   This software is released under the terms of the Eclipse Public License 
 *   (EPL) 1.0. You can find a copy of the EPL at: 
 *   http://opensource.org/licenses/eclipse-1.0.php
 *
 */

#define DATA_GENERATOR_SUCCESS 0
#define DATA_INSTANCE_MEMORY_ALLOCATION_ERROR -1

typedef struct bulk_buffer {
    void *data;
    long bytes;
    long data_instance_count;
} bulk_buffer;

/*
 * Typedef for the callback that is called by the data generator.
 * The first argument is a pointer to the data instance.
 * The second argument is the length of the data instance in bytes.
 * The third argument is a pointer to optional user data.
 */
typedef void (*data_processing_cb)(const void*, int, void*);

/*
 * Data generation function.
 * The data_instance_size sets the size of an individual data instance in bytes.
 * The data_instance_max_count specifies how many data instances are generated.
 * If data_instance_max_count is <= 0 data instances will be generated infinitely.
 * The stats_output_interval determines how often statistics are emitted.
 * The interval is measured as number of generated data instances;
 * i.e., for 1000 every 1000th data instance stats will be emitted.
 * A value <= 0 disables stats output.
 * The data_processing_cb is a pointer to the function that will be called for each data instance.
 * user_data is a pointer to optional user data that will be passed to the call back.
 */
int generate_data(int data_instance_size, int data_instance_max_count, int data_generation_duration,
                  int stats_output_interval, data_processing_cb cb, void *user_data);

/*
 * This function is similar to the classic data generation function except that
 * it uses a double bufferd bulk buffer approach.
 */
int generate_data_bulk_method_double_buffer
    (int data_instance_size, int data_instance_max_count, int data_generation_duration,
     int stats_output_interval, int bulk_size, data_processing_cb cb, void *user_data);

