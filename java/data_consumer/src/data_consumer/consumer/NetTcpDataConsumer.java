/*
 *   Copyright 2015, Frankfurt University of Applied Sciences
 *
 *   This software is released under the terms of the Eclipse Public License 
 *   (EPL) 1.0. You can find a copy of the EPL at: 
 *   http://opensource.org/licenses/eclipse-1.0.php
 *
 */

package data_consumer.consumer;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;

public class NetTcpDataConsumer extends DataConsumer {

    private ServerSocketChannel serverChannel;
    private SocketChannel channel;
    
    public NetTcpDataConsumer(int tcpPort, int bufferSize) throws IOException {
        super(bufferSize);
        serverChannel = ServerSocketChannel.open();
        serverChannel.bind(new InetSocketAddress(tcpPort));
    }

    @Override
    protected void consumeDataHook() {
        try {
            channel.read(consumeBuffer);
        } catch (IOException e) {
            if (isRunning()) {
                e.printStackTrace();
            }
        }
        if (consumeBuffer.remaining() == 0) {
            notifyConsumeHandler();
        }
    }
    
    @Override
    protected void initHook() {
        try {
            channel = serverChannel.accept();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    @Override
    protected void shutdownHook() {
        try {
            channel.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        try {
            serverChannel.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        try {
            serverChannel.socket().close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
