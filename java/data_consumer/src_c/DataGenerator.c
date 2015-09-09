/*
 *   Copyright 2015, Frankfurt University of Applied Sciences
 *
 *   This software is released under the terms of the Eclipse Public License 
 *   (EPL) 1.0. You can find a copy of the EPL at: 
 *   http://opensource.org/licenses/eclipse-1.0.php
 *
 */

#include "DataGenerator.h"
#include <data_generator_lib.h>
#include <stdlib.h>
#include <string.h>

typedef struct cb_user_data_type {
	JNIEnv *env;
	jmethodID java_callback_method_id;
	jobject java_callback_handler_object;
	jobject java_user_data;
} cb_user_data_type;



void jni_cb(const void *data, int size, void *user_data);

jclass get_class_ref(JNIEnv *env, const char *class_name);
jmethodID get_method_id_ref(JNIEnv *env, const char *class_name, const char *method_name, const char *method_signature);



JNIEXPORT jint JNICALL Java_data_1consumer_jni_DataGenerator_generateData
  (JNIEnv *env, jobject obj, jint data_instance_size, jint data_instance_count, jint data_generation_duration, jint stats_output_interval, jint bulk_size, jobject java_callback_handler, jobject user_data)
{
    jmethodID cb_method = get_method_id_ref(env, "data_consumer/jni/CallbackHandler",
                                            "processData", "(Ljava/nio/ByteBuffer;ILjava/lang/Object;)V");
    if (cb_method == NULL || bulk_size < 1) {
        return -1;
    }

    cb_user_data_type cb_user_data;
    cb_user_data.env = env;
    cb_user_data.java_callback_method_id = cb_method;
    cb_user_data.java_callback_handler_object = java_callback_handler;
    cb_user_data.java_user_data = user_data;

    if (bulk_size == 1) {
        generate_data(data_instance_size, data_instance_count, data_generation_duration, stats_output_interval, jni_cb, (void *) &cb_user_data);
    } else {
        generate_data_bulk_method_double_buffer(data_instance_size, data_instance_count, data_generation_duration, stats_output_interval, bulk_size, jni_cb, (void *) &cb_user_data);
    }

    return 0;
}

void jni_cb(const void *data, int size, void *user_data)
{
    cb_user_data_type *cb_user_data = (cb_user_data_type*) user_data;

    JNIEnv *env = cb_user_data->env;

    jobject buffer = (*env)->NewDirectByteBuffer(env, (void *) data, size);
	if (buffer == NULL) {
		return;
	}

	(*env)->CallVoidMethod(env,
                           cb_user_data->java_callback_handler_object,
                           cb_user_data->java_callback_method_id,
                           buffer,
                           size,
                           cb_user_data->java_user_data);

	(*env)->DeleteLocalRef(env, buffer);
}

jclass get_class_ref(JNIEnv *env, const char *class_name)
{
    jclass tmp_class_ref = (*env)->FindClass(env, class_name);
    if (tmp_class_ref == NULL) {
        return NULL;
    }

    jclass class_ref = (*env)->NewGlobalRef(env, tmp_class_ref);
    (*env)->DeleteLocalRef(env, tmp_class_ref);

    return class_ref;
}

jmethodID get_method_id_ref(JNIEnv *env, const char *class_name, const char *method_name, const char *method_signature)
{
    jclass tmp_class = get_class_ref(env, class_name);
    if (tmp_class == NULL) {
        return NULL;
    }

    jmethodID method_id = (*env)->GetMethodID(env, tmp_class, method_name, method_signature);

    return method_id;
} 

