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

public abstract class DataConsumer implements Runnable {

    private Thread thread;
    private boolean running;

    protected ByteBuffer consumeBuffer;
    protected DataConsumeHandler dataConsumeHandler;

    public DataConsumer(int bufferSize) {
        consumeBuffer = ByteBuffer.allocate(bufferSize);
    }

    protected void notifyConsumeHandler() {
        consumeBuffer.flip();
        if (dataConsumeHandler != null) {
            dataConsumeHandler.consumeData(consumeBuffer);
        } else {
            System.err.println("Cannot notify null consume handler.");
        }
        consumeBuffer.clear();
    }

    protected void notifyConsumeHandler(ByteBuffer data) {
        dataConsumeHandler.consumeData(data);
    }

    protected void initHook() {
        // No-op default implementation
    };

    protected abstract void consumeDataHook();

    protected void shutdownHook() {
        // No-op default implementation
    }

    public void setDataConsumeHandler(DataConsumeHandler dataConsumeHandler) {
        this.dataConsumeHandler = dataConsumeHandler;
    }

    public void start() {
        running = true;
        thread = new Thread(this);
        thread.start();
    }

    public void stop() {
        running = false;
        thread.interrupt();
    }

    public void run() {
        initHook();

        while (running) {
            consumeDataHook();
        }

        shutdownHook();
    }

    public boolean isRunning() {
        return running;
    }
}
