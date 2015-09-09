/*
 *   Copyright 2015, Frankfurt University of Applied Sciences
 *
 *   This software is released under the terms of the Eclipse Public License 
 *   (EPL) 1.0. You can find a copy of the EPL at: 
 *   http://opensource.org/licenses/eclipse-1.0.php
 *
 */

package data_consumer.config;

public class Constants {

    public static final String CWD = System.getProperty("user.dir");
    public static final String DATA_GENERATOR_LIB_NAME = "data_generator_lib.so";
    public static final String DATA_GENERATOR_EXEC_NAME = "data_generator";
    public static final String DATA_GENERATOR_CWD_PATH = CWD + "/../../c/data_generator";
    public static final String DATA_GENERATOR_LIB_CWD_PATH = DATA_GENERATOR_CWD_PATH + "/" + DATA_GENERATOR_LIB_NAME;
    public static final String DATA_GENERATOR_EXEC_CWD_PATH = DATA_GENERATOR_CWD_PATH + "/" + DATA_GENERATOR_EXEC_NAME;
    public static final String DATA_GENERATOR_JNI_LIB_NAME = "DataGenerator.so";
    public static final String DATA_GENERATOR_JNI_LIB_CWD_PATH = CWD + "/" + DATA_GENERATOR_JNI_LIB_NAME;
    
    public static final int RETURN_CODE_SUCCESS = 0;
    
    public static final int TCP_PORT = 38907;
    
    public static final String CONNECTION_METHOD_FIFO = "fifo";
    public static final String CONNECTION_METHOD_NET_TCP = "net_tcp";
    public static final String CONNECTION_METHOD_JNI = "jni";
    
}
