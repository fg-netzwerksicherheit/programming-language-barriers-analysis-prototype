/*
 *   Copyright 2015, Frankfurt University of Applied Sciences
 *
 *   This software is released under the terms of the Eclipse Public License 
 *   (EPL) 1.0. You can find a copy of the EPL at: 
 *   http://opensource.org/licenses/eclipse-1.0.php
 *
 */

package data_consumer.cli;

import uk.co.flamingpenguin.jewel.cli.Option;

public interface CommandLineArgs {

    @Option(defaultValue = "1", shortName = "b", description = "Consume data in bulks of the given size.")
    int getBulkSize();
    
    @Option(defaultValue = "5", shortName = "d", description = "Run for the specified time in seconds and shut down when the time has elapsed.")
    int getRunDuration();
    
    @Option(defaultValue = "1000", shortName = "S", description = "Output stats every n milliseconds.")
    int getStatsOutInterval();
    
    @Option(defaultValue = "100", shortName = "s", description = "Data instance size.")
    int getDataInstanceSize();
    
    @Option(description = "Method used connecting the data generator and the consumer."
            + "Available values are: fifo, net_tcp, jni", shortName = "m", defaultValue = "fifo")
    String getConnectionMethod();
    
    @Option(description = "Name of the fifo from where the data is read.", shortName = "F", defaultValue = "default.fifo")
    String getFifoName();
    
    @Option(defaultValue = "38907", shortName = "P", description = "Port used for network based interconnection.")
    int getPortNumber();
    
    @Option(description = "Display help.", shortName = "h", helpRequest = true)
    boolean getHelp();
    
}
