/*
 *   Copyright 2015, Frankfurt University of Applied Sciences
 *
 *   This software is released under the terms of the Eclipse Public License 
 *   (EPL) 1.0. You can find a copy of the EPL at: 
 *   http://opensource.org/licenses/eclipse-1.0.php
 *
 */

package data_producer.generator;

import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class DataGenerator {

    final private DataForwarder dataForwarder;
    final private ByteBuffer data;
    private Thread thread;
    private boolean running = false;
    
    private final int statsOutputInterval;
    private long invocationCount = 0;
    private long oldInvocationCount = 0;
    private long oldTimestamp = 0;
    private ScheduledExecutorService statsOutputExecutor = null;

    public DataGenerator(int dataInstanceSize, DataForwarder forwarder) {
    	this(dataInstanceSize, forwarder, 0);
    }
    
    public DataGenerator(int dataInstanceSize, DataForwarder forwarder, int statsOutputInterval) {
    	if (dataInstanceSize <= 0) {
    		throw new RuntimeException("Data instance has to be bigger than 0.");
    	}
    	if (forwarder == null) {
    		throw new RuntimeException("Forwarder must not be null.");
    	}
    	
        this.dataForwarder = forwarder;

        this.statsOutputInterval = statsOutputInterval;

        data = ByteBuffer.allocate(dataInstanceSize);
        byte[] dataTmp = new byte[dataInstanceSize];
        Arrays.fill(dataTmp, (byte) 'a');
        data.put(dataTmp);
    }

    public void generateBlocking(int dataInstanceCount) {
    	startStatsOutput();
        for (int i = 0; i < dataInstanceCount; i++) {
            data.flip();
            dataForwarder.forwardData(data);
            invocationCount++;
        }
    }

    public void generateNonBlocking() {
        running = true;
        thread = new Thread(new Runnable() {
            @Override
            public void run() {
            	startStatsOutput();
                while (running) {
                    data.flip();
                    dataForwarder.forwardData(data);
                    invocationCount++;
                }
            }
        });
        thread.start();
    }

    public void stop() {
        running = false;
        if (statsOutputExecutor != null) {
        	statsOutputExecutor.shutdownNow();
        }
    }
    
    private void startStatsOutput() {
    	if (statsOutputInterval > 0) {
    		oldTimestamp = System.nanoTime();
    		invocationCount = 0;
    		
    		statsOutputExecutor = new ScheduledThreadPoolExecutor(1);
    		statsOutputExecutor.scheduleAtFixedRate(new Runnable() {
				
				@Override
				public void run() {
					long currentInvocationCount = invocationCount;
					long invocationCountDelta = currentInvocationCount - oldInvocationCount;
					oldInvocationCount = currentInvocationCount;
					
					long currentTimestamp = System.nanoTime();
					long timestampDelta = currentTimestamp - oldTimestamp;
					oldTimestamp = currentTimestamp;
					
					double dataInstancesPerSecond = invocationCountDelta / (timestampDelta / Math.pow(10,9));
					System.out.printf("dips: %f\n", dataInstancesPerSecond);
				}
			}, statsOutputInterval, statsOutputInterval, TimeUnit.MILLISECONDS);
    	}
    }

}
