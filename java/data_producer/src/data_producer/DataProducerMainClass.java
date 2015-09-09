/*
 *   Copyright 2015, Frankfurt University of Applied Sciences
 *
 *   This software is released under the terms of the Eclipse Public License 
 *   (EPL) 1.0. You can find a copy of the EPL at: 
 *   http://opensource.org/licenses/eclipse-1.0.php
 *
 */

package data_producer;

import java.io.IOException;
import java.nio.ByteBuffer;

import uk.co.flamingpenguin.jewel.cli.ArgumentValidationException;
import uk.co.flamingpenguin.jewel.cli.Cli;
import uk.co.flamingpenguin.jewel.cli.CliFactory;
import data_consumer.cli.CommandLineArgs;
import data_consumer.config.Constants;
import data_producer.generator.BulkForwarder;
import data_producer.generator.DataForwarder;
import data_producer.generator.DataGenerator;
import data_producer.generator.SingleInstanceFifoForwarder;
import data_producer.generator.SingleInstanceJniForwarder;
import data_producer.generator.SingleInstanceNetTcpForwarder;
import data_producer.generator.Stoppable;

public class DataProducerMainClass {
    public static final String VERSION = "java_data_producer-0.1";

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
        final DataForwarder forwarder;

        try {
            if (connectionMethod.compareTo(Constants.CONNECTION_METHOD_FIFO) == 0) {
                System.out.println("Using fifo connection method.");
                forwarder = new SingleInstanceFifoForwarder(arguments.getFifoName());
            } else if (connectionMethod.compareTo(Constants.CONNECTION_METHOD_NET_TCP) == 0) {
                System.out.println("Using net_tcp connection method.");
                forwarder = new SingleInstanceNetTcpForwarder("127.0.0.1", arguments.getPortNumber());
            } else if (connectionMethod.compareTo(Constants.CONNECTION_METHOD_JNI) == 0) {
                System.out.println("Using jni connection method.");
                forwarder = new SingleInstanceJniForwarder(arguments.getBulkSize(), arguments.getStatsOutInterval());
            } else if (connectionMethod.compareTo("no_op") == 0) {
            	System.out.println("Using no-op forwarder.");
            	forwarder = new DataForwarder() {
					@Override
					public void forwardData(ByteBuffer data) {
					}
				};
            } else {
                System.out.println("Defaulting to fifo connection method.");
                forwarder = new SingleInstanceFifoForwarder(arguments.getFifoName());
            }
        } catch (IOException e) {
            e.printStackTrace();
            return;
        }

        /*
         * Set up and start data generator.
         */
        final int bulkSize = arguments.getBulkSize();
        DataGenerator dataGenerator;
        if (bulkSize == 1) {
        	System.out.println("Using single instance forwarder...");
            dataGenerator = new DataGenerator(arguments.getDataInstanceSize(), forwarder, arguments.getStatsOutInterval());
        } else if (bulkSize > 1) {
        	System.out.println("Using bulk forwarder...");
            BulkForwarder bulkForwarder = new BulkForwarder(forwarder, arguments.getDataInstanceSize(), bulkSize);
            dataGenerator = new DataGenerator(arguments.getDataInstanceSize(), bulkForwarder, arguments.getStatsOutInterval());
        } else {
            System.out.println("Negative bulk size is not allowed.");
            return;
        }
        
        dataGenerator.generateNonBlocking();

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
        dataGenerator.stop();
        if (forwarder instanceof Stoppable) {
            ((Stoppable) forwarder).stop();
        }

        return;
    }
}
