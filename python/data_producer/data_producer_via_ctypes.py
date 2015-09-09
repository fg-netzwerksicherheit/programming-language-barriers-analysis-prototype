#!/usr/bin/python

#
#  Copyright 2015, Frankfurt University of Applied Sciences
#
#  This software is released under the terms of the Eclipse Public License 
#  (EPL) 1.0. You can find a copy of the EPL at: 
#  http://opensource.org/licenses/eclipse-1.0.php
#

import argparse
import io
import time
from ctypes import *
from threading import Thread



parser = argparse.ArgumentParser(description='Python-based data producer to write to files.')
parser.add_argument('-s', '--data_instance_size', type=int, default=64,
                   help='Size of a single data instance in bytes.')
parser.add_argument('-b', '--bulk_size', type=int, default=100,
                   help='Bulk size in bytes.')
parser.add_argument('-d', '--run_duration', type=int, default=10,
                   help='Bulk size in bytes.')
args = parser.parse_args()

dataInstanceSize = args.data_instance_size
bulkSize = args.bulk_size
bulkOverhead = 0
if bulkSize > 1 :
    if dataInstanceSize <= 255 :
        bulkOverhead += 1
    elif dataInstanceSize <= 65535 :
        bulkOverhead += 2
    else :
        bulkOverhead += 4
bufferSize = (dataInstanceSize + bulkOverhead) * bulkSize
runDuration = args.run_duration

print("Using buffer Size: " + str(bufferSize))
print("Shuting down after " + str(runDuration) + " seconds.")


nativeBufferType = c_char * bufferSize

libPath = "./ctypes_data_receiver_test.so"
cdll.LoadLibrary(libPath)
myLib = CDLL(libPath)
myLib.init(bulkSize, 1000)


running = True
invocationCount = 0

class StatsThread(Thread):
    def __init__(self):
        Thread.__init__(self)
        self.daemon = True

    def run(self):
        global invocationCount
        global running
        oldInvocationCount = 0
        oldTime = time.time()
        while running:
            time.sleep(1)
            currentInvocationCount = invocationCount
            currentTime = time.time()

            timeDelta = currentTime - oldTime
            oldTime = currentTime

            invocationCountDelta = currentInvocationCount - oldInvocationCount
            oldInvocationCount = currentInvocationCount

            print("timeDelta: " + str(timeDelta) + " ; invocationCountDelta: " + str(invocationCountDelta))
            print("dips: " + str((invocationCountDelta * bulkSize) / timeDelta))

data = bytearray()
for i in range(dataInstanceSize):
    data.append('a')

class SingleInstanceWriteThread(Thread):
    def __init__(self):
        global bufferSize
        Thread.__init__(self)
        self.daemon = True

    def run(self):
        global dataInstanceSize
        global invocationCount
        global running
        while running:
            bufferInstance = nativeBufferType.from_buffer(data)
            myLib.forward_data(bufferInstance, bufferSize)
            invocationCount += 1

class BulkMethodWriteThread(Thread):
    def __init__(self):
        global bufferSize
        Thread.__init__(self)
        self.daemon = True
        print('Using buffer size: ' + str(bufferSize))
        self.bulkBuffer = bytearray(bufferSize)
        self.offset = 0
        self.outFile = None

    def run(self):
        if bulkOverhead == 1:
            self.writeWithByte()
        elif bulkOverhead == 2:
            self.writeWithShort()
        else:
            self.writeWithInt()

    def writeWithByte(self):
        global running
        global dataInstanceSize
        while running:
            self.bulkBuffer[self.offset] = dataInstanceSize
            self.offset += 1
            self.bulkBuffer[self.offset:(self.offset + dataInstanceSize)] = data
            self.offset += dataInstanceSize
            if self.offset == bufferSize :
                self.writeOutBuffer()

    def writeWithShort(self):
        global running
        global dataInstanceSize
        while running:
            self.bulkBuffer[self.offset] = (dataInstanceSize >> 8) & 0xff
            self.offset += 1
            self.bulkBuffer[self.offset] = (dataInstanceSize) & 0xff
            self.offset += 1
            self.bulkBuffer[self.offset:(self.offset + dataInstanceSize)] = data
            self.offset += dataInstanceSize
            if self.offset == bufferSize :
                self.writeOutBuffer()

    def writeWithInt(self):
        global running
        global dataInstanceSize
        while running:
            self.bulkBuffer[self.offset] = (dataInstanceSize >> 24) & 0xff
            self.offset += 1
            self.bulkBuffer[self.offset] = (dataInstanceSize >> 16) & 0xff
            self.offset += 1
            self.bulkBuffer[self.offset] = (dataInstanceSize >> 8) & 0xff
            self.offset += 1
            self.bulkBuffer[self.offset] = (dataInstanceSize) & 0xff
            self.offset += 1
            self.bulkBuffer[self.offset:(self.offset + dataInstanceSize)] = data
            self.offset += dataInstanceSize
            if self.offset == bufferSize :
                self.writeOutBuffer()

    def writeOutBuffer(self):
        global invocationCount
        bufferInstance = nativeBufferType.from_buffer(self.bulkBuffer)
        myLib.forward_data(bufferInstance, bufferSize)
        invocationCount += 1
        self.offset = 0

StatsThread().start()

t = None
if bulkSize == 1 :
    t = SingleInstanceWriteThread()
else :
    t = BulkMethodWriteThread()
t.start()



time.sleep(runDuration)
print("Shutting down...")
running = False


