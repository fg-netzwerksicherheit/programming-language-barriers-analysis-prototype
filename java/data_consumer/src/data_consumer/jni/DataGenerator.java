/*
 *   Copyright 2015, Frankfurt University of Applied Sciences
 *
 *   This software is released under the terms of the Eclipse Public License 
 *   (EPL) 1.0. You can find a copy of the EPL at: 
 *   http://opensource.org/licenses/eclipse-1.0.php
 *
 */

package data_consumer.jni;

import data_consumer.util.NativeLibHelper;

public class DataGenerator {
    
    static {
        NativeLibHelper.loadLibsFromCwd();
    }

    native public int generateData(int dataInstanceSize, int dataInstanceCount, int dataGenerationDuration,
                                   int statsOutputInterval, int bulkSize, CallbackHandler callback,
                                   Object userData);

}
