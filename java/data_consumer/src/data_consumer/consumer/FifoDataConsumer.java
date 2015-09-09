/*
 *   Copyright 2015, Frankfurt University of Applied Sciences
 *
 *   This software is released under the terms of the Eclipse Public License 
 *   (EPL) 1.0. You can find a copy of the EPL at: 
 *   http://opensource.org/licenses/eclipse-1.0.php
 *
 */

package data_consumer.consumer;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.FileSystems;
import java.nio.file.StandardOpenOption;

import data_consumer.util.CommandExecutionHelper;

public class FifoDataConsumer extends DataConsumer {
    
    private FileChannel fc;
    private String fifoName;

    public FifoDataConsumer(String fifoName, int bufferSize) throws IOException {
        super(bufferSize);
        
        this.fifoName = fifoName;
        CommandExecutionHelper.forceCreateNewFifo(fifoName);
        fc = FileChannel.open(FileSystems.getDefault().getPath(fifoName), StandardOpenOption.READ);
        /*
         * CommandExecutionHelper injects dummy data into the pipe to break the
         * block of above open call. However, we have to consume this data first
         * via below call before going on.
         */
        fc.read(ByteBuffer.allocate(2));
    }

    @Override
    protected void consumeDataHook() {
        try {
            fc.read(consumeBuffer);
        } catch (IOException e) {
            if (isRunning()) {
                e.printStackTrace();
            }
        }
        
        if (consumeBuffer.remaining() == 0) {
            notifyConsumeHandler();
        }
    }
    
    @Override
    protected void shutdownHook() {
        try {
            fc.close();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        CommandExecutionHelper.rm(fifoName);
    }

}
