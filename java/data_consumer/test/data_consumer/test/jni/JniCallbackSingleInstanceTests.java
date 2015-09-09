/*
 *   Copyright 2015, Frankfurt University of Applied Sciences
 *
 *   This software is released under the terms of the Eclipse Public License 
 *   (EPL) 1.0. You can find a copy of the EPL at: 
 *   http://opensource.org/licenses/eclipse-1.0.php
 *
 */

package data_consumer.test.jni;

import static org.junit.Assert.*;

import java.nio.ByteBuffer;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import data_consumer.config.Constants;
import data_consumer.jni.DataGenerator;
import data_consumer.jni.CallbackHandler;
import data_consumer.util.NativeLibHelper;

public class JniCallbackSingleInstanceTests {
    
    public int invocationCount;
    public boolean wasRun;
    public int receivedRemaining;
    public int receivedSize;
    public Object receivedUserData;

    @BeforeClass
    static public void setupClass() {
        NativeLibHelper.loadLibsFromCwd();
    }
    
    @Before
    public void setup() {
        invocationCount = 0;
        wasRun = false;
        receivedRemaining = 0;
        receivedSize = 0;
        receivedUserData = null;
    }
    
    @Test
    public void SimpleDataGeneratorJniInteractionTest() {
        int dataInstanceSize = 100;
        int dataInstanceCount = 1;
        int statsOutputInterval = 0;
        CallbackHandler callback = new CallbackHandler() {
            @Override
            public void processData(ByteBuffer data, int size, Object userData) {

            }
        };
        Object userData = null;

        DataGenerator gen = new DataGenerator();
        int ret = gen.generateData(dataInstanceSize, dataInstanceCount, 0, statsOutputInterval, 1, callback, userData);

        assertEquals(Constants.RETURN_CODE_SUCCESS, ret);
    }
    
    @Test
    public void SimpleDataGeneratorJniSingleInstanceCallbackInteractionTest() throws InterruptedException {
        assertFalse(wasRun);
        assertEquals(0, receivedRemaining);
        assertEquals(0, receivedSize);
        assertNull(receivedUserData);
        
        int dataInstanceSize = 100;
        int dataInstanceCount = 1;
        int statsOutputInterval = 0;
        Object userData = "foo";
        
        final CountDownLatch countDownLatch = new CountDownLatch(1);
        CallbackHandler callback = new CallbackHandler() {
            @Override
            public void processData(ByteBuffer data, int size, Object userData) {
                wasRun = true;
                receivedRemaining = data.remaining();
                receivedSize = size;
                receivedUserData = userData;
                countDownLatch.countDown();
            }
        };

        DataGenerator gen = new DataGenerator();
        int ret = gen.generateData(dataInstanceSize, dataInstanceCount, 0, statsOutputInterval, 1, callback, userData);
        countDownLatch.await(100, TimeUnit.MILLISECONDS);

        assertEquals(Constants.RETURN_CODE_SUCCESS, ret);
        assertTrue(wasRun);
        assertEquals(dataInstanceSize, receivedRemaining);
        assertEquals(dataInstanceSize, receivedSize);
        assertSame(userData, receivedUserData);
    }
    
    @Test
    public void SimpleDataGeneratorJniSingleInstanceCallbackInvocationCountTest() throws InterruptedException {
        assertFalse(wasRun);
        assertEquals(0, invocationCount);
        
        final int expectedInvocationCount = 10;
        
        int dataInstanceSize = 100;
        int dataInstanceCount = expectedInvocationCount;
        int statsOutputInterval = 0;
        Object userData = "foo";
        
        final CountDownLatch countDownLatch = new CountDownLatch(expectedInvocationCount);
        CallbackHandler callback = new CallbackHandler() {
            @Override
            public void processData(ByteBuffer data, int size, Object userData) {
                wasRun = true;
                invocationCount++;
                countDownLatch.countDown();
            }
        };

        DataGenerator gen = new DataGenerator();
        int ret = gen.generateData(dataInstanceSize, dataInstanceCount, 0, statsOutputInterval, 1, callback, userData);
        countDownLatch.await(100, TimeUnit.MILLISECONDS);

        assertEquals(Constants.RETURN_CODE_SUCCESS, ret);
        assertTrue(wasRun);
        assertEquals(expectedInvocationCount, invocationCount);
    }
}
