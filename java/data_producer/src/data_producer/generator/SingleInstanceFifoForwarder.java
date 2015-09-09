/*
 *   Copyright 2015, Frankfurt University of Applied Sciences
 *
 *   This software is released under the terms of the Eclipse Public License 
 *   (EPL) 1.0. You can find a copy of the EPL at: 
 *   http://opensource.org/licenses/eclipse-1.0.php
 *
 */

package data_producer.generator;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.FileSystems;
import java.nio.file.StandardOpenOption;

public class SingleInstanceFifoForwarder implements DataForwarder, Stoppable {

    protected FileChannel fc;
    
    public SingleInstanceFifoForwarder(String fifoName) throws IOException {
        fc = FileChannel.open(FileSystems.getDefault().getPath(fifoName), StandardOpenOption.WRITE);
    }

    @Override
    public void forwardData(ByteBuffer data) {
        try {
            fc.write(data);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void stop() {
        try {
            fc.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
