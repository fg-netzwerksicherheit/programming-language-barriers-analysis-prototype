/*
 *   Copyright 2015, Frankfurt University of Applied Sciences
 *
 *   This software is released under the terms of the Eclipse Public License 
 *   (EPL) 1.0. You can find a copy of the EPL at: 
 *   http://opensource.org/licenses/eclipse-1.0.php
 *
 */

package data_producer.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.FileSystems;
import java.nio.file.StandardOpenOption;
import java.util.Arrays;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.junit.Before;
import org.junit.Test;

import data_consumer.consumer.DataConsumeHandler;
import data_consumer.consumer.DataConsumer;
import data_consumer.consumer.FifoDataConsumer;
import data_producer.generator.BulkForwarder;
import data_producer.generator.DataGenerator;
import data_producer.generator.SingleInstanceFifoForwarder;

public class FifoDataProducerTests {

    public boolean wasRun;
    public int receivedRemaining;
    public int receivedCount;
    
    @Before
    public void setup() {
        wasRun = false;
        receivedRemaining = 0;
        receivedCount = 0;
    }
    
    @Test
    public void simpleFifoDataOutputTest() throws IOException, InterruptedException {
        assertFalse(wasRun);
        assertEquals(0, receivedRemaining);

        final CountDownLatch latch = new CountDownLatch(1);
        String fifoName = "foo_bar.fifo";
        int dataInstanceSize = 100;
        
        byte[] dataTmp = new byte [dataInstanceSize];
        Arrays.fill(dataTmp, (byte) 'a');
        ByteBuffer data = ByteBuffer.allocate(dataInstanceSize);
        data.put(dataTmp);
        data.flip();
        
        DataConsumer consumer = new FifoDataConsumer(fifoName, dataInstanceSize);
        consumer.setDataConsumeHandler(new DataConsumeHandler() {
            @Override
            public void consumeData(ByteBuffer data) {
                wasRun = true;
                receivedRemaining = data.remaining();
                latch.countDown();
            }
        });
        consumer.start();
       
        FileChannel fc = FileChannel.open(FileSystems.getDefault().getPath(fifoName), StandardOpenOption.WRITE);
        fc.write(data);

        latch.await(500, TimeUnit.MILLISECONDS);

        consumer.stop();

        assertTrue(wasRun);
        assertEquals(dataInstanceSize, receivedRemaining);
    }
    
    @Test
    public void singleInstanceFifoDataForwarderTest() throws IOException, InterruptedException {
        assertFalse(wasRun);
        assertEquals(0, receivedRemaining);
        assertEquals(0, receivedCount);

        String fifoName = "foo_bar.fifo";
        int dataInstanceSize = 100;
        int dataInstanceCount = 1;
        final CountDownLatch latch = new CountDownLatch(1);
        
        DataConsumer consumer = new FifoDataConsumer(fifoName, dataInstanceSize);
        consumer.setDataConsumeHandler(new DataConsumeHandler() {
            @Override
            public void consumeData(ByteBuffer data) {
                wasRun = true;
                receivedRemaining = data.remaining();
                receivedCount++;
                latch.countDown();
            }
        });
        consumer.start();
       
        SingleInstanceFifoForwarder forwarder = new SingleInstanceFifoForwarder(fifoName);
        
        DataGenerator dataGenerator = new DataGenerator(dataInstanceSize, forwarder);
        dataGenerator.generateBlocking(dataInstanceCount);

        latch.await(500, TimeUnit.MILLISECONDS);
        
        forwarder.stop();
        consumer.stop();
        
        assertTrue(wasRun);
        assertEquals(dataInstanceSize, receivedRemaining);
        assertEquals(dataInstanceCount, receivedCount);
    }
    
    @Test
    public void bulkMethodFifoDataForwarderTest() throws IOException, InterruptedException {
        assertFalse(wasRun);
        assertEquals(0, receivedRemaining);
        assertEquals(0, receivedCount);

        String fifoName = "foo_bar.fifo";
        int dataInstanceSize = 100;
        int dataInstanceCount = 1;
        int bulkSize = 10;
        final CountDownLatch latch = new CountDownLatch(1);
        
        DataConsumer consumer = new FifoDataConsumer(fifoName, (dataInstanceSize + 1) * bulkSize);
        consumer.setDataConsumeHandler(new DataConsumeHandler() {
            @Override
            public void consumeData(ByteBuffer data) {
                wasRun = true;
                receivedRemaining = data.remaining();
                receivedCount++;
                latch.countDown();
            }
        });
        consumer.start();
       
        SingleInstanceFifoForwarder singleInstanceForwarder = new SingleInstanceFifoForwarder(fifoName);
        BulkForwarder forwarder = new BulkForwarder(singleInstanceForwarder, dataInstanceSize, bulkSize);
        
        DataGenerator dataGenerator = new DataGenerator(dataInstanceSize, forwarder);
        dataGenerator.generateBlocking(bulkSize);

        latch.await(500, TimeUnit.MILLISECONDS);
        
        singleInstanceForwarder.stop();
        consumer.stop();
        
        assertTrue(wasRun);
        assertEquals((dataInstanceSize + 1) * bulkSize, receivedRemaining);
        assertEquals(dataInstanceCount, receivedCount);
    }
}
