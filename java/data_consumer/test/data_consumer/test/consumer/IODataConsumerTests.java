/*
 *   Copyright 2015, Frankfurt University of Applied Sciences
 *
 *   This software is released under the terms of the Eclipse Public License 
 *   (EPL) 1.0. You can find a copy of the EPL at: 
 *   http://opensource.org/licenses/eclipse-1.0.php
 *
 */

package data_consumer.test.consumer;

import static org.junit.Assert.*;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.junit.Before;
import org.junit.Test;

import data_consumer.config.Constants;
import data_consumer.consumer.DataConsumeHandler;
import data_consumer.consumer.DataConsumer;
import data_consumer.consumer.FifoDataConsumer;
import data_consumer.consumer.NetTcpDataConsumer;
import data_consumer.util.CommandExecutionHelper;

public class IODataConsumerTests {

    public boolean wasRun;
    public int receivedRemaining;
    public int invocationCount;

    @Before
    public void setup() {
        wasRun = false;
        receivedRemaining = 0;
        invocationCount = 0;
    }

    @Test
    public void simpleNetTcpIODataConsumerTest() throws InterruptedException, IOException {
        assertFalse(wasRun);

        final CountDownLatch latch = new CountDownLatch(1);

        DataConsumer consumer = new NetTcpDataConsumer(Constants.TCP_PORT, 100);
        consumer.setDataConsumeHandler(new DataConsumeHandler() {
            @Override
            public void consumeData(ByteBuffer data) {
                wasRun = true;
                latch.countDown();
            }
        });
        consumer.start();

        CommandExecutionHelper.delayedDataGeneratorExec("-Cnet_tcp_out_cb -H127.0.0.1 -s 100 -c 1", 250);

        latch.await(500, TimeUnit.MILLISECONDS);

        consumer.stop();

        assertTrue(wasRun);
    }

    @Test
    public void simpleFifoIODataConsumerTest() throws InterruptedException, IOException {
        assertFalse(wasRun);

        final CountDownLatch latch = new CountDownLatch(1);
        String fifoName = "foo_bar.fifo";

        DataConsumer consumer = new FifoDataConsumer(fifoName, 100);
        consumer.setDataConsumeHandler(new DataConsumeHandler() {
            @Override
            public void consumeData(ByteBuffer data) {
                wasRun = true;
                latch.countDown();
            }
        });
        consumer.start();

        CommandExecutionHelper.delayedDataGeneratorExec("-Cfile_out_cb -F" + fifoName + " -s 100 -c 1", 250);

        latch.await(500, TimeUnit.MILLISECONDS);

        consumer.stop();

        assertTrue(wasRun);
    }

    @Test
    public void bigBufferNetTcpIODataConsumerTest() throws InterruptedException, IOException {
        assertFalse(wasRun);
        assertEquals(0, receivedRemaining);

        final CountDownLatch latch = new CountDownLatch(1);
        int dataInstanceSize = 100;
        int dataInstanceCount = 10000;

        DataConsumer consumer = new NetTcpDataConsumer(Constants.TCP_PORT, dataInstanceCount * dataInstanceSize);
        consumer.setDataConsumeHandler(new DataConsumeHandler() {
            @Override
            public void consumeData(ByteBuffer data) {
                wasRun = true;
                receivedRemaining = data.remaining();
                latch.countDown();
            }
        });
        consumer.start();

        CommandExecutionHelper.delayedDataGeneratorExec("-Cnet_tcp_out_cb -H127.0.0.1 -s " + dataInstanceSize + " -c " + dataInstanceCount,
                250);

        latch.await(500, TimeUnit.MILLISECONDS);

        consumer.stop();

        assertTrue(wasRun);
        assertEquals(dataInstanceCount * dataInstanceSize, receivedRemaining);
    }

    @Test
    public void bigBufferFifoIODataConsumerTest() throws InterruptedException, IOException {
        assertFalse(wasRun);
        assertEquals(0, receivedRemaining);

        final CountDownLatch latch = new CountDownLatch(1);
        String fifoName = "foo_bar.fifo";
        int dataInstanceSize = 100;
        int dataInstanceCount = 10000;

        DataConsumer consumer = new FifoDataConsumer(fifoName, dataInstanceCount * dataInstanceSize);
        consumer.setDataConsumeHandler(new DataConsumeHandler() {
            @Override
            public void consumeData(ByteBuffer data) {
                wasRun = true;
                receivedRemaining = data.remaining();
                latch.countDown();
            }
        });
        consumer.start();

        CommandExecutionHelper.delayedDataGeneratorExec("-Cfile_out_cb -F" + fifoName + " -s " + dataInstanceSize + " -c "
                + dataInstanceCount, 250);

        latch.await(500, TimeUnit.MILLISECONDS);

        consumer.stop();

        assertTrue(wasRun);
        assertEquals(dataInstanceCount * dataInstanceSize, receivedRemaining);
    }
    
    @Test
    public void countNetTcpIODataConsumerTest() throws InterruptedException, IOException {
        assertFalse(wasRun);
        assertEquals(0, invocationCount);

        int dataInstanceSize = 100;
        int dataInstanceCount = 10;
        final CountDownLatch latch = new CountDownLatch(dataInstanceCount);

        DataConsumer consumer = new NetTcpDataConsumer(Constants.TCP_PORT, dataInstanceSize);
        consumer.setDataConsumeHandler(new DataConsumeHandler() {
            @Override
            public void consumeData(ByteBuffer data) {
                wasRun = true;
                invocationCount++;
                latch.countDown();
            }
        });
        consumer.start();

        CommandExecutionHelper.delayedDataGeneratorExec("-Cnet_tcp_out_cb -H127.0.0.1 -s " + dataInstanceSize + " -c " + dataInstanceCount,
                250);

        latch.await(500, TimeUnit.MILLISECONDS);

        consumer.stop();

        assertTrue(wasRun);
        assertEquals(dataInstanceCount, invocationCount);
    }
    
    @Test
    public void countFifoIODataConsumerTest() throws InterruptedException, IOException {
        assertFalse(wasRun);
        assertEquals(0, invocationCount);

        String fifoName = "foo_bar.fifo";
        int dataInstanceSize = 100;
        int dataInstanceCount = 10;
        final CountDownLatch latch = new CountDownLatch(dataInstanceCount);

        DataConsumer consumer = new FifoDataConsumer(fifoName, dataInstanceSize);
        consumer.setDataConsumeHandler(new DataConsumeHandler() {
            @Override
            public void consumeData(ByteBuffer data) {
                wasRun = true;
                invocationCount++;
                latch.countDown();
            }
        });
        consumer.start();

        CommandExecutionHelper.delayedDataGeneratorExec("-Cfile_out_cb -F" + fifoName + " -s " + dataInstanceSize + " -c "
                + dataInstanceCount, 250);

        latch.await(500, TimeUnit.MILLISECONDS);

        consumer.stop();

        assertTrue(wasRun);
        assertEquals(dataInstanceCount, invocationCount);
    }
}
