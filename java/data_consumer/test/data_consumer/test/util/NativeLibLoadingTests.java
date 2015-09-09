/*
 *   Copyright 2015, Frankfurt University of Applied Sciences
 *
 *   This software is released under the terms of the Eclipse Public License 
 *   (EPL) 1.0. You can find a copy of the EPL at: 
 *   http://opensource.org/licenses/eclipse-1.0.php
 *
 */

package data_consumer.test.util;

import org.junit.Test;

import data_consumer.config.Constants;
import data_consumer.util.NativeLibHelper;

public class NativeLibLoadingTests {

    @Test
    public void loadDataGeneratorLibFromCwdTest() {
        System.load(Constants.DATA_GENERATOR_LIB_CWD_PATH);
    }
    
    @Test
    public void loadDataGeneratorJniLibFromCwdTest() {
        System.load(Constants.DATA_GENERATOR_JNI_LIB_CWD_PATH);
    }
    
    @Test
    public void simpleNativeLibHelperLoadFromCwdTest() {
        NativeLibHelper.loadLibsFromCwd();
    }
}
