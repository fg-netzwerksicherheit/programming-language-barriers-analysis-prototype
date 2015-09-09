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

public class BulkForwarder implements DataForwarder {

	private byte[] bufferArray;
	private int offset;
	private int sizeFieldSize;

	private ByteBuffer bulkBuffer;
	private DataForwarder forwarder;

	public BulkForwarder(DataForwarder forwarder, int dataInstanceSize,
			int bulkSize) {
		if (dataInstanceSize <= 255) {
			sizeFieldSize = 1;
		} else if (dataInstanceSize <= 65535) {
			sizeFieldSize = 2;
		} else {
			sizeFieldSize = 4;
		}

		bulkBuffer = ByteBuffer
				.allocateDirect((dataInstanceSize + sizeFieldSize) * bulkSize);
		bufferArray = new byte[(dataInstanceSize + sizeFieldSize) * bulkSize];
		this.forwarder = forwarder;
	}

	@Override
	public void forwardData(ByteBuffer data) {
		byte[] dataArray = data.array();

//		for (int i = 0; i < sizeFieldSize; i++) {
//			bufferArray[offset + ((sizeFieldSize - 1) - i)] = (byte) ((dataArray.length >> (i * 8)) & 0xFF);
//		}
		if (sizeFieldSize == 1) {
			bufferArray[offset] = (byte) ((dataArray.length) & 0xFF);
		} else if (sizeFieldSize == 2) {
			bufferArray[offset + 1] = (byte) ((dataArray.length) & 0xFF);
			bufferArray[offset] = (byte) ((dataArray.length >> 8) & 0xFF);
		} else {
			bufferArray[offset + 3] = (byte) ((dataArray.length) & 0xFF);
			bufferArray[offset + 2] = (byte) ((dataArray.length >> 8) & 0xFF);
			bufferArray[offset + 1] = (byte) ((dataArray.length >> 16) & 0xFF);
			bufferArray[offset] = (byte) ((dataArray.length >> 24) & 0xFF);
		}
		offset += sizeFieldSize;

		System.arraycopy(dataArray, 0, bufferArray, offset, dataArray.length);
		offset += dataArray.length;
		if (offset == bufferArray.length) {
			bulkBuffer.clear();
			bulkBuffer.put(bufferArray);
			bulkBuffer.flip();
			forwarder.forwardData(bulkBuffer);
			offset = 0;
		}

		// bulkBuffer.put((byte) data.remaining());
		// bulkBuffer.put(data);
		//
		// if (bulkBuffer.remaining() == 0) {
		// bulkBuffer.flip();
		// forwarder.forwardData(bulkBuffer);
		// bulkBuffer.clear();
		// }
	}
}
