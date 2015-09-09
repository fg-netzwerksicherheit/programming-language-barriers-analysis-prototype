/*
 *   Copyright 2015, Frankfurt University of Applied Sciences
 *
 *   This software is released under the terms of the Eclipse Public License 
 *   (EPL) 1.0. You can find a copy of the EPL at: 
 *   http://opensource.org/licenses/eclipse-1.0.php
 *
 */

package data_consumer.jni;

import java.nio.ByteBuffer;

public interface CallbackHandler {

    void processData(ByteBuffer data, int size, Object userData);

}
