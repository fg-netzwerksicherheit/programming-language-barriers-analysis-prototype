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
from threading import Thread
from ctypes import *



parser = argparse.ArgumentParser(description='Python-based data consumer to read from files.')
parser.add_argument('-s', '--data_instance_size', type=int, default=100,
                   help='Size of a single data instance in bytes.')
parser.add_argument('-b', '--bulk_size', type=int, default=10,
                   help='Bulk size in bytes.')
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
runDuration = args.run_duration

print("Using buffer Size: " + str(bufferSize))
print("Shuting down after " + str(runDuration) + " seconds.")



running = True
invocationCount = 0


dgLibPath = "../../c/data_generator/data_generator_lib.so"
cdll.LoadLibrary(dgLibPath)
dgLib = CDLL(dgLibPath)

readBuffer = bytearray(bufferSize)

CB_FUNC = CFUNCTYPE(None, POINTER(c_char), c_int, c_void_p)
def pyCbFunc(data, size, userData):
    global invocationCount
    global readBuffer
    readBuffer = bytearray(data[:size])
    invocationCount += 1
cbFunc = CB_FUNC(pyCbFunc)


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
            print("rdips: " + str((invocationCountDelta * bulkSize) / timeDelta))

StatsThread().start()

if bulkSize == 1 :
    dgLib.generate_data(dataInstanceSize, 0, runDuration, 1000, cbFunc, None)
else:
    dgLib.generate_data_bulk_method_double_buffer(dataInstanceSize, 0, runDuration, 1000, bulkSize, cbFunc, None)



print("Shutting down...")
running = False


