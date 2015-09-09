/*
 *   Copyright 2015, Frankfurt University of Applied Sciences
 *
 *   This software is released under the terms of the Eclipse Public License 
 *   (EPL) 1.0. You can find a copy of the EPL at: 
 *   http://opensource.org/licenses/eclipse-1.0.php
 *
 */

package data_consumer;

import java.io.IOException;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import uk.co.flamingpenguin.jewel.cli.ArgumentValidationException;
import uk.co.flamingpenguin.jewel.cli.Cli;
import uk.co.flamingpenguin.jewel.cli.CliFactory;
import data_consumer.cli.CommandLineArgs;
import data_consumer.config.Constants;
import data_consumer.consumer.DataConsumer;
import data_consumer.consumer.FifoDataConsumer;
import data_consumer.consumer.JniDataConsumer;
import data_consumer.consumer.NetTcpDataConsumer;
import data_consumer.stats.StatsCollector;

public class DataConsumerMainClass {
    public static final String VERSION = "java_data_consumer-0.1";

    public static void main(String args[]) {
        System.out.println(VERSION);

        /*
         * Parse command line arguments. Print help if necessary.
         */
        final CommandLineArgs arguments;
        final Cli<CommandLineArgs> cli = CliFactory.createCli(CommandLineArgs.class);
        try {
            arguments = cli.parseArguments(args);
        } catch (ArgumentValidationException e) {
            System.out.println("Could not parse the given arguments.\nValid arguments are:\n");
            System.out.println(cli.getHelpMessage());
            return;
        }

        if (arguments.getHelp()) {
            System.out.println(cli.getHelpMessage());
            return;
        }

        System.out.println(arguments.toString());
        /*
         * Done with pre-processing command line arguments.
         */

        /*
         * Prepare DataConsumer.
         */
        final String connectionMethod = arguments.getConnectionMethod();
        final DataConsumer consumer;
        final int bulkSize = arguments.getBulkSize();
        int dataInstanceSize = arguments.getDataInstanceSize();
        if (bulkSize > 1) {
        	if (dataInstanceSize <= 255) {
        		dataInstanceSize += 1;
        	} else if (dataInstanceSize <= 65535) {
        		dataInstanceSize += 2;
        	} else {
        		dataInstanceSize += 4;
        	}
        }
        final int bufferSize = bulkSize * dataInstanceSize;

        try {
            if (connectionMethod.compareTo(Constants.CONNECTION_METHOD_FIFO) == 0) {
                System.out.println("Using fifo connection method.");
                consumer = new FifoDataConsumer(arguments.getFifoName(), bufferSize);
            } else if (connectionMethod.compareTo(Constants.CONNECTION_METHOD_NET_TCP) == 0) {
                System.out.println("Using net_tcp connection method.");
                consumer = new NetTcpDataConsumer(arguments.getPortNumber(), bufferSize);
            } else if (connectionMethod.compareTo(Constants.CONNECTION_METHOD_JNI) == 0) {
                System.out.println("Using jni connection method.");
                consumer = new JniDataConsumer(arguments.getDataInstanceSize(), 0, arguments.getBulkSize(), arguments.getRunDuration());
            } else {
                System.out.println("Defaulting to fifo connection method.");
                consumer = new FifoDataConsumer(arguments.getFifoName(), bufferSize);
            }
        } catch (IOException e) {
            e.printStackTrace();
            return;
        }

        /*
         * Set up stats output.
         */
        final StatsCollector statsCollector = new StatsCollector(consumer);

        ScheduledExecutorService executor = new ScheduledThreadPoolExecutor(1);
        executor.scheduleAtFixedRate(new Runnable() {
            @Override
            public void run() {
                System.out.printf("rdips: %f\n", (statsCollector.getInvocationsPerSecond() * arguments.getBulkSize()));
            }
        }, arguments.getStatsOutInterval(), arguments.getStatsOutInterval(), TimeUnit.MILLISECONDS);

        statsCollector.start();

        /*
         * Run for the specified time.
         */
        int runDuration = arguments.getRunDuration();
        if (runDuration > 0) {
            try {
                System.out.println("Shutting down in " + runDuration + " seconds");
                Thread.sleep(1000 * runDuration);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        } else {
            System.out.println("Error: run durations smaller zero are not supported.");
        }

        /*
         * Clean up and exit.
         */
        System.out.println("Cleaning up and leaving...");
        statsCollector.stop();
        executor.shutdownNow();

        return;
    }
}
