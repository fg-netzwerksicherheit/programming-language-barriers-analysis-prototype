/*
 *   Copyright 2015, Frankfurt University of Applied Sciences
 *
 *   This software is released under the terms of the Eclipse Public License 
 *   (EPL) 1.0. You can find a copy of the EPL at: 
 *   http://opensource.org/licenses/eclipse-1.0.php
 *
 */

package data_consumer.stats;

import java.nio.ByteBuffer;

import data_consumer.consumer.DataConsumeHandler;
import data_consumer.consumer.DataConsumer;

public class StatsCollector {

    private DataConsumer dataConsumer;
    private long invocationCount = 0;
    private long invocationCountOld = 0;
    private long nanoTimeOld = 0;

    public StatsCollector(DataConsumer dataConsumer) {
        dataConsumer.setDataConsumeHandler(new DataConsumeHandler() {
            @Override
            public void consumeData(ByteBuffer data) {
                invocationCount++;
            }
        });
        this.dataConsumer = dataConsumer;
    }

    public void start() {
        nanoTimeOld = System.nanoTime();
        dataConsumer.start();
    }

    public void stop() {
        dataConsumer.stop();
    }

    public long getInvocationCount() {
        return invocationCount;
    }

    public double getInvocationsPerSecond() {
        long currentTime = System.nanoTime();
        long timeDelta = currentTime - nanoTimeOld;
        nanoTimeOld = currentTime;

        long invocationCountTmp = invocationCount;
        long invocationCountDelta = invocationCountTmp - invocationCountOld;
        invocationCountOld = invocationCountTmp;

        return (((double) invocationCountDelta) / (((double) timeDelta) / Math.pow(10, 9)));
    }

}
