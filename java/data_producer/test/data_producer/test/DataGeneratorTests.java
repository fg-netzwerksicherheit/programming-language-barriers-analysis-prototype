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

import java.nio.ByteBuffer;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.junit.Before;
import org.junit.Test;

import data_producer.generator.DataForwarder;
import data_producer.generator.DataGenerator;

public class DataGeneratorTests {

    public boolean wasRun;
    public int receivedRemaining;
    
    @Before
    public void setup() {
        wasRun = false;
        receivedRemaining = 0;
    }
    
    @Test
    public void simpleDataGeneratorTest() throws InterruptedException {
        assertFalse(wasRun);
        assertEquals(0, receivedRemaining);
        
        int dataInstanceSize = 100;
        int dataInstanceCount = 1;
        
        DataForwarder forwarder = new DataForwarder() {
            @Override
            public void forwardData(ByteBuffer data) {
                wasRun = true;
                receivedRemaining = data.remaining();
            }
        };
        
        DataGenerator dataGenerator = new DataGenerator(dataInstanceSize, forwarder);
        dataGenerator.generateBlocking(dataInstanceCount);
        
        assertTrue(wasRun);
        assertEquals(dataInstanceSize, receivedRemaining);
    }
    
    @Test
    public void simpleNonBlockingDataGeneratorTest() throws InterruptedException {
        assertFalse(wasRun);
        
        final CountDownLatch latch = new CountDownLatch(1);
        
        int dataInstanceSize = 100;
        
        DataForwarder forwarder = new DataForwarder() {
            @Override
            public void forwardData(ByteBuffer data) {
                wasRun = true;
                latch.countDown();
            }
        };
        
        DataGenerator dataGenerator = new DataGenerator(dataInstanceSize, forwarder);
        dataGenerator.generateNonBlocking();
        latch.await(100, TimeUnit.MILLISECONDS);
        dataGenerator.stop();
        
        assertTrue(wasRun);
    }
}
