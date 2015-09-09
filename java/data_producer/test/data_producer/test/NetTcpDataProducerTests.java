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
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.util.Arrays;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.junit.Before;
import org.junit.Test;

import data_consumer.config.Constants;
import data_consumer.consumer.DataConsumeHandler;
import data_consumer.consumer.DataConsumer;
import data_consumer.consumer.NetTcpDataConsumer;
import data_producer.generator.BulkForwarder;
import data_producer.generator.DataGenerator;
import data_producer.generator.SingleInstanceNetTcpForwarder;

public class NetTcpDataProducerTests {

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
    public void simpleNetTcpDataOutputTest() throws InterruptedException, IOException {
        assertFalse(wasRun);
        assertEquals(0, receivedRemaining);

        final CountDownLatch latch = new CountDownLatch(1);
        int dataInstanceSize = 100;

        byte[] dataTmp = new byte[dataInstanceSize];
        Arrays.fill(dataTmp, (byte) 'a');
        ByteBuffer data = ByteBuffer.allocate(dataInstanceSize);
        data.put(dataTmp);
        data.flip();

        DataConsumer consumer = new NetTcpDataConsumer(Constants.TCP_PORT, dataInstanceSize);
        consumer.setDataConsumeHandler(new DataConsumeHandler() {
            @Override
            public void consumeData(ByteBuffer data) {
                wasRun = true;
                receivedRemaining = data.remaining();
                latch.countDown();
            }
        });
        consumer.start();

        SocketChannel channel = SocketChannel.open();
        channel.connect(new InetSocketAddress(InetAddress.getByName("127.0.0.1"), Constants.TCP_PORT));
        channel.write(data);

        latch.await(500, TimeUnit.MILLISECONDS);

        consumer.stop();
        channel.close();

        assertTrue(wasRun);
        assertEquals(dataInstanceSize, receivedRemaining);
    }

    @Test
    public void singleInstanceNetTcpDataForwarderTest() throws InterruptedException, IOException {
        assertFalse(wasRun);
        assertEquals(0, receivedRemaining);
        assertEquals(0, receivedCount);

        final CountDownLatch latch = new CountDownLatch(1);
        int dataInstanceSize = 100;
        int dataInstanceCount = 1;
        String ipAddress = "127.0.0.1";

        DataConsumer consumer = new NetTcpDataConsumer(Constants.TCP_PORT, dataInstanceSize);
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

        SingleInstanceNetTcpForwarder forwarder = new SingleInstanceNetTcpForwarder(ipAddress, Constants.TCP_PORT);

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
    public void bulkMethodNetTcpDataForwarderTest() throws IOException, InterruptedException {
        assertFalse(wasRun);
        assertEquals(0, receivedRemaining);
        assertEquals(0, receivedCount);

        int dataInstanceSize = 100;
        int dataInstanceCount = 1;
        int bulkSize = 10;
        String ipAddress = "127.0.0.1";
        final CountDownLatch latch = new CountDownLatch(1);

        DataConsumer consumer = new NetTcpDataConsumer(Constants.TCP_PORT, (dataInstanceSize + 1) * bulkSize);
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

        SingleInstanceNetTcpForwarder singleInstanceForwarder = new SingleInstanceNetTcpForwarder(ipAddress, Constants.TCP_PORT);
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
