/*
 *   Copyright 2015, Frankfurt University of Applied Sciences
 *
 *   This software is released under the terms of the Eclipse Public License 
 *   (EPL) 1.0. You can find a copy of the EPL at: 
 *   http://opensource.org/licenses/eclipse-1.0.php
 *
 */

package data_consumer.consumer;

import java.nio.ByteBuffer;

import data_consumer.jni.CallbackHandler;
import data_consumer.jni.DataGenerator;

public class JniDataConsumer extends DataConsumer {

    private int dataInstanceSize;
    private int dataInstanceCount;
    private int bulkSize;
    private int dataGenerationDuration;
    private DataGenerator dataGenerator;

    public JniDataConsumer(int dataInstanceSize, int dataInstanceCount, int bulkSize, int dataGenerationDuration) {
        super((bulkSize == 1) ? dataInstanceSize : (dataInstanceSize + ((dataInstanceSize <= 255) ? 1 : ((dataInstanceSize <= 65535) ? 2 : 4))) * bulkSize);

        this.dataInstanceSize = dataInstanceSize;
        this.dataInstanceCount = dataInstanceCount;
        this.bulkSize = bulkSize;
        this.dataGenerationDuration = dataGenerationDuration;
        dataGenerator = new DataGenerator();
    }

    @Override
    protected void initHook() {
        CallbackHandler cb = new CallbackHandler() {
            @Override
            public void processData(ByteBuffer data, int size, Object userData) {
                notifyConsumeHandler(data);

/*
                // Alternatively, we can do a deep copy.
                consumeBuffer.put(data);
                if (consumeBuffer.remaining() == 0) {
                    notifyConsumeHandler();
                }
*/
            }
        };
        dataGenerator.generateData(dataInstanceSize, dataInstanceCount, dataGenerationDuration, 0, bulkSize, cb,
                null);
    }

    @Override
    protected void consumeDataHook() {
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            if (isRunning()) {
                e.printStackTrace();
            }
        }
    }
}
