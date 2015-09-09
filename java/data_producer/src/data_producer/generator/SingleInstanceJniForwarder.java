/*
 *   Copyright 2015, Frankfurt University of Applied Sciences
 *
 *   This software is released under the terms of the Eclipse Public License 
 *   (EPL) 1.0. You can find a copy of the EPL at: 
 *   http://opensource.org/licenses/eclipse-1.0.php
 *
 */

package data_producer.generator;

import java.nio.ByteBuffer;

public class SingleInstanceJniForwarder implements DataForwarder {
	
	public SingleInstanceJniForwarder(int bulkSize, int statsOutputInterval) {
		System.load(System.getProperty("user.dir") + "/SingleInstanceJniForwarder.so");
		init(bulkSize, statsOutputInterval);
	}

	native public void init(int bulkSize, int statsOutputInterval);
	
	@Override
	native public void forwardData(ByteBuffer data);

}
