/*
 *   Copyright 2015, Frankfurt University of Applied Sciences
 *
 *   This software is released under the terms of the Eclipse Public License 
 *   (EPL) 1.0. You can find a copy of the EPL at: 
 *   http://opensource.org/licenses/eclipse-1.0.php
 *
 */

package data_producer.generator;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;

public class SingleInstanceNetTcpForwarder implements DataForwarder, Stoppable {

    protected SocketChannel channel;
    
    public SingleInstanceNetTcpForwarder(String ipAddress, int tcpPort) throws IOException {
        channel = SocketChannel.open();
        channel.connect(new InetSocketAddress(InetAddress.getByName(ipAddress), tcpPort));
    }

    @Override
    public void forwardData(ByteBuffer data) {
        try {
            channel.write(data);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void stop() {
        try {
            channel.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
