package data_consumer.util;

import data_consumer.config.Constants;

public class NativeLibHelper {

    public static void loadLibsFromCwd() {
        System.load(Constants.DATA_GENERATOR_LIB_CWD_PATH);
        System.load(Constants.DATA_GENERATOR_JNI_LIB_CWD_PATH);
    }

}
