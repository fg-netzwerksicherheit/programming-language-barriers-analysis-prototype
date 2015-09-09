/*
 *   Copyright 2015, Frankfurt University of Applied Sciences
 *
 *   This software is released under the terms of the Eclipse Public License 
 *   (EPL) 1.0. You can find a copy of the EPL at: 
 *   http://opensource.org/licenses/eclipse-1.0.php
 *
 */

package data_consumer.test.stats;

import static org.junit.Assert.*;

import java.io.IOException;

import org.junit.Test;

import data_consumer.config.Constants;
import data_consumer.consumer.FifoDataConsumer;
import data_consumer.consumer.JniDataConsumer;
import data_consumer.consumer.NetTcpDataConsumer;
import data_consumer.stats.StatsCollector;
import data_consumer.util.CommandExecutionHelper;

public class StatsCollectorTests {

    @Test
    public void simpleNetTcpStatsCollectorTest() throws InterruptedException, IOException {
        int dataInstanceSize = 100;
        int dataInstanceCount = 10;

        StatsCollector statsCollector = new StatsCollector(new NetTcpDataConsumer(Constants.TCP_PORT, dataInstanceSize));
        statsCollector.start();

        CommandExecutionHelper.delayedDataGeneratorExec("-Cnet_tcp_out_cb -H127.0.0.1 -s " + dataInstanceSize + " -c " + dataInstanceCount,
                250);
        
        Thread.sleep(500);

        statsCollector.stop();

        double invocationsPerSecond = statsCollector.getInvocationsPerSecond();
        assertEquals(dataInstanceCount, statsCollector.getInvocationCount());
        assertNotEquals(0.0, invocationsPerSecond);
        assertTrue(0.0 < invocationsPerSecond);
        assertEquals(0.0, statsCollector.getInvocationsPerSecond(), 0.0);
    }
    
    @Test
    public void simpleFifoStatsCollectorTest() throws InterruptedException, IOException {
        int dataInstanceSize = 100;
        int dataInstanceCount = 10;
        String fifoName = "snafu.fifo";

        StatsCollector statsCollector = new StatsCollector(new FifoDataConsumer(fifoName, dataInstanceSize));
        statsCollector.start();

        CommandExecutionHelper.delayedDataGeneratorExec("-Cfile_out_cb -F" + fifoName + " -s " + dataInstanceSize + " -c " + dataInstanceCount,
                250);
        
        Thread.sleep(500);

        statsCollector.stop();

        double invocationsPerSecond = statsCollector.getInvocationsPerSecond();
        assertEquals(dataInstanceCount, statsCollector.getInvocationCount());
        assertNotEquals(0.0, invocationsPerSecond);
        assertTrue(0.0 < invocationsPerSecond);
        assertEquals(0.0, statsCollector.getInvocationsPerSecond(), 0.0);
    }
    
    @Test
    public void simpleJniStatsCollectorTest() throws InterruptedException, IOException {
        int dataInstanceSize = 100;
        int dataInstanceCount = 10;

        StatsCollector statsCollector = new StatsCollector(new JniDataConsumer(dataInstanceSize, dataInstanceCount, 1, 0));
        statsCollector.start();
        
        Thread.sleep(200);

        statsCollector.stop();

        double invocationsPerSecond = statsCollector.getInvocationsPerSecond();
        assertEquals(dataInstanceCount, statsCollector.getInvocationCount());
        assertNotEquals(0.0, invocationsPerSecond);
        assertTrue(0.0 < invocationsPerSecond);
        assertEquals(0.0, statsCollector.getInvocationsPerSecond(), 0.0);
    }
}
