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
import socket
import time
from threading import Thread



parser = argparse.ArgumentParser(description='Python-based data consumer to read from TCP sockets.')
parser.add_argument('-s', '--data_instance_size', type=int, default=100,
                   help='Size of a single data instance in bytes.')
parser.add_argument('-b', '--bulk_size', type=int, default=10,
                   help='Bulk size in bytes.')
parser.add_argument('-P', '--port_number', type=int, default=38907,
                   help='Port to wich the client will connect.')
parser.add_argument('-d', '--run_duration', type=int, default=5,
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
portNumber = args.port_number
runDuration = args.run_duration
bindToAnyAddress = ''

print("Using buffer Size: " + str(bufferSize))
print("Shuting down after " + str(runDuration) + " seconds.")



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
            currentTime = time.time()
            currentInvocationCount = invocationCount

            timeDelta = currentTime - oldTime
            oldTime = currentTime

            invocationCountDelta = currentInvocationCount - oldInvocationCount
            oldInvocationCount = currentInvocationCount

            print("timeDelta: " + str(timeDelta) + " ; invocationCountDelta: " + str(invocationCountDelta))
            print("rdips: " + str((invocationCountDelta * bulkSize) / timeDelta))

class ReadThread(Thread):
    def __init__(self):
        global bufferSize
        Thread.__init__(self)
        self.daemon = True
        print('Using buffer size: ' + str(bufferSize))
        self.readBuffer = bytearray(bufferSize)

    def run(self):
        global bufferSize
        global invocationCount
        global running
        serverSocket = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
        serverSocket.bind((bindToAnyAddress, portNumber))
        serverSocket.listen(1)
        conn, addr = serverSocket.accept()
        print('Connection established from: ' + str(addr))
        while running:
            readCount = conn.recv_into(self.readBuffer, bufferSize, socket.MSG_WAITALL)
            if readCount == bufferSize:
                invocationCount += 1
            else:
                print('readCount != bufferSize: ' + str(readCount) + ' != ' + str(bufferSize))
            if readCount == 0:
                print('Got zero read count. Leaving read loop...')
                break

StatsThread().start()
ReadThread().start()



time.sleep(runDuration)
print("Shutting down...")
running = False


