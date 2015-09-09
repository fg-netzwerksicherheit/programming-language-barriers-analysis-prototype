/*
 *   Copyright 2015, Frankfurt University of Applied Sciences
 *
 *   This software is released under the terms of the Eclipse Public License 
 *   (EPL) 1.0. You can find a copy of the EPL at: 
 *   http://opensource.org/licenses/eclipse-1.0.php
 *
 */

package data_consumer.test.io;

import static org.junit.Assert.*;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;

import org.junit.Test;

import data_consumer.config.Constants;
import data_consumer.util.CommandExecutionHelper;

public class SocketChannelReceiveTests {

    @Test
    public void simpleSingleInstanceReceiveTest() throws IOException {
        int dataInstanceSize = 100;
        int dataInstanceCount = 1;
        ByteBuffer buffer = ByteBuffer.allocate(dataInstanceSize * dataInstanceCount);

        ServerSocketChannel serverChannel = ServerSocketChannel.open();
        serverChannel.bind(new InetSocketAddress(Constants.TCP_PORT));

        CommandExecutionHelper.delayedDataGeneratorExec("-Cnet_tcp_out_cb -H127.0.0.1 -s " + dataInstanceSize + 
                " -c " + dataInstanceCount, 250);
        SocketChannel channel = serverChannel.accept();
        int readCount = channel.read(buffer);
        
        channel.close();
        serverChannel.close();
        
        assertEquals(dataInstanceSize * dataInstanceCount, readCount);
    }
    
    @Test
    public void simpleMultipleInstanceReceiveTest() throws IOException {
        int dataInstanceSize = 100;
        int dataInstanceCount = 100000;
        ByteBuffer buffer = ByteBuffer.allocate(dataInstanceSize * dataInstanceCount);

        ServerSocketChannel serverChannel = ServerSocketChannel.open();
        serverChannel.bind(new InetSocketAddress(Constants.TCP_PORT));

        CommandExecutionHelper.delayedDataGeneratorExec("-Cnet_tcp_out_cb -H127.0.0.1 -s " + dataInstanceSize + 
                " -c " + dataInstanceCount, 250);
        SocketChannel channel = serverChannel.accept();
        
        int readCount = 0;
        int currentReadCount = 0;
        do {
            currentReadCount = channel.read(buffer);
            readCount += currentReadCount;
        } while (currentReadCount > 0);   
        
        channel.close();
        serverChannel.close();
        
        assertEquals(dataInstanceSize * dataInstanceCount, readCount);
    }
}
