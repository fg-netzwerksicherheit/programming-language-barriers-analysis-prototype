/*
 *   Copyright 2015, Frankfurt University of Applied Sciences
 *
 *   This software is released under the terms of the Eclipse Public License 
 *   (EPL) 1.0. You can find a copy of the EPL at: 
 *   http://opensource.org/licenses/eclipse-1.0.php
 *
 */

package data_consumer.test.io;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.FileSystems;
import java.nio.file.StandardOpenOption;

import org.junit.Test;

import data_consumer.config.Constants;
import data_consumer.util.CommandExecutionHelper;

public class FifoFileInputTests {

    @Test
    public void testMkFifo() {
        String fifoName = "test.fifo";

        CommandExecutionHelper.rm("test.fifo");
        File f = new File(fifoName);
        assertFalse(f.exists());

        CommandExecutionHelper.forceCreateNewFifo(fifoName);

        File f2 = new File(fifoName);
        assertTrue(f2.exists());
    }

    @Test
    public void simpleSingleInstanceReceiveTest() throws IOException, InterruptedException {
        String fifoName = Constants.CWD + "/test.fifo";
        int dataInstanceSize = 100;
        int dataInstanceCount = 1;
        ByteBuffer buffer = ByteBuffer.allocate(dataInstanceSize * dataInstanceCount);

        CommandExecutionHelper.forceCreateNewFifo(fifoName);

        FileChannel fc = FileChannel.open(FileSystems.getDefault().getPath(fifoName), StandardOpenOption.READ);
        /*
         * CommandExecutionHelper injects dummy data into the pipe to break the
         * block of above open call. However, we have to consume this data first
         * via below call before going on.
         */
        fc.read(ByteBuffer.allocate(2));

        CommandExecutionHelper.delayedDataGeneratorExec("-Cfile_out_cb -F" + fifoName + " -s " + dataInstanceSize + " -c "
                + dataInstanceCount, 0);
        Thread.sleep(100);

        int readCount = fc.read(buffer);

        fc.close();

        assertEquals(dataInstanceSize * dataInstanceCount, readCount);
    }
    
    @Test
    public void simpleMultipleInstanceReceiveTest() throws IOException, InterruptedException {
        String fifoName = Constants.CWD + "/test.fifo";
        int dataInstanceSize = 100;
        int dataInstanceCount = 100000;
        ByteBuffer buffer = ByteBuffer.allocate(dataInstanceSize * dataInstanceCount);

        CommandExecutionHelper.forceCreateNewFifo(fifoName);

        FileChannel fc = FileChannel.open(FileSystems.getDefault().getPath(fifoName), StandardOpenOption.READ);
        /*
         * CommandExecutionHelper injects dummy data into the pipe to break the
         * block of above open call. However, we have to consume this data first
         * via below call before going on.
         */
        fc.read(ByteBuffer.allocate(2));

        CommandExecutionHelper.delayedDataGeneratorExec("-Cfile_out_cb -F" + fifoName + " -s " + dataInstanceSize + " -c "
                + dataInstanceCount, 0);
        Thread.sleep(100);
        
        int readCount = 0;
        int currentReadCount = 0;
        do {
            currentReadCount = fc.read(buffer);
            readCount += currentReadCount;
        } while (currentReadCount > 0);   
        
        fc.close();
        
        assertEquals(dataInstanceSize * dataInstanceCount, readCount);
    }
}
