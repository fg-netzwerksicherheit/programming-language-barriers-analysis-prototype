/*
 *   Copyright 2015, Frankfurt University of Applied Sciences
 *
 *   This software is released under the terms of the Eclipse Public License 
 *   (EPL) 1.0. You can find a copy of the EPL at: 
 *   http://opensource.org/licenses/eclipse-1.0.php
 *
 */

package data_consumer.test.consumer;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.junit.Before;
import org.junit.Test;

import data_consumer.consumer.DataConsumeHandler;
import data_consumer.consumer.DataConsumer;
import data_consumer.consumer.JniDataConsumer;

public class JniConsumerTests {
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
    public void simpleJniDataConsumerTest() throws InterruptedException, IOException {
        assertFalse(wasRun);

        int dataInstanceSize = 100;
        int dataInstanceCount = 1;
        int bulkSize = 1;
        int dataGenerationDuration = 0;

        final CountDownLatch latch = new CountDownLatch(1);

        DataConsumer consumer = new JniDataConsumer(dataInstanceSize, dataInstanceCount, bulkSize, dataGenerationDuration);
        consumer.setDataConsumeHandler(new DataConsumeHandler() {
            @Override
            public void consumeData(ByteBuffer data) {
                wasRun = true;
                latch.countDown();
            }
        });
        consumer.start();
        
        latch.await(500, TimeUnit.MILLISECONDS);
        
        consumer.stop();

        assertTrue(wasRun);
    }
}
