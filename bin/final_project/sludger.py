#!/usr/local/bin/python3.5

# from ctypes import CDLL
# lib = cdll.LoadLibrary('./sludger.so')
import scrypt 
import Queue
import sys
import os
import socket
import errno
import traceback
from threading import Thread
from subprocess import PIPE, Popen
from struct import *
from multiprocessing import Process

class Header:
    def __init__(self):
        (self.type, self.size, self.custom) = unpack("!HHL", '\x00\x02\x00\x10\x00\x00\x00\x00')

    def serialize(self):
        return pack("!HHL", self.type, self.size, self.custom)

def encrypt(inq, outq):
    while( True ):
        data = inq.get()
        # Thank you Primm
        outq.put(scrypt.hash(str(data), 'I Hate Liam Echlin', N=2048, r=4, p=4))
       # print("hashed hazmat")
        inq.task_done()
        
   
def sender(queue):
    # TODO set time limit for checking
    header = Header()
    header.size = 1416
    while(True):
        #set up connection
        payload = bytearray()
        payload.extend(header.serialize())
        count = 0
        while(count < 22):
            #print("have %d sludge " % (count))
            hash = queue.get()
            payload.extend(hash)
            count += 1
        try:
            print("sending sludge downstream")
            attempts = 0
            while ( attempts < 10 ):
                try:
                    sludge_outgoing = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
                    sludge_outgoing.connect(("downstream", 4444))
                    sludge_outgoing.send(payload)
                    sludge_outgoing.close()
                    break;
                except:
                    traceback.print_exc()
                    if ( attemps == 9 ):
                        raise Exception
                attempts +=1
        except:
            # TODO make error log 
            f = open('/home/sbartholomew/sludgeOut', 'wb')
            f.write(payload)
            f.close()
#             while (count > 0):
#               #  print("hazmatin sending")
#                 sludge_outgoing.send(
#                     bytes(hazmat[count], 'utf-8'))
#                 count -= 1
#             
            
            
            

def main():

    request_queue = Queue.Queue(maxsize=0)
    output_queue = Queue.Queue(maxsize=0)

    BUFFER_SIZE = 4
    
    for i in range(4):
        worker = Thread(target=encrypt, args=(request_queue, output_queue))
        worker.setDaemon(True)
        worker.start()
    dispatcher = Thread(target=sender, args=(output_queue,))
    dispatcher.setDaemon(True)
    dispatcher.start()
    
    print("Sludger v 1.01 is now listening")
    #io = os.open('/home/sbartholomew/sludgePipe', os.O_RDONLY | os.O_NONBLOCK)
    with open('/home/sbartholomew/sludgePipe') as fifo:
        while(True):
            try:
                buffer = fifo.read(BUFFER_SIZE)
            except OSError as err:
                if err.errno == errno.EAGAIN or err.errno == errno.EWOULDBLOCK:
                    buffer = None
                else:
                     raise # something else has happened
                 
            if buffer is None:
                continue
                #noting was recieved
            else:
            #    print(unpack('!I', buffer))
                request_queue.put( unpack('!I', buffer) ) 
        
if __name__ == "__main__":
    main();