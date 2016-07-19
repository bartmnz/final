#!/usr/local/bin/python3.5

# from ctypes import CDLL
# lib = cdll.LoadLibrary('./sludger.so')
import scrypt 
import Queue
import sys
import os
import errno
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
        print("hashed hazmat")
        inq.task_done()
        

class Sender(Process):
    def __init__(self, queue):
        super(Sender, self).__init__()
        self.queue = queue
        self.header = Header()
        
    def run(self):
        # TODO set time limit for checking
        while(True):
            #set up connection
            count = 0
            hazmat = []
            while(count < 100):
                hazmat.append(self.queue.get_nowait())
                count += 1
            sludge_outgoing = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
            sludge_outgoing.connect(("downstream", 4444))
            self.header.size = 6408
            sludge_outgoing.send(self.header.serialize())
            while (count > 0):
              #  print("hazmatin sending")
                sludge_outgoing.send(
                    bytes(hazmat[count], 'utf-8'))
                count -= 1
            sludge_outgoing.close()
            
            
            

def main():

    request_queue = Queue.Queue(maxsize=0)
    output_queue = Queue.Queue(maxsize=0)

    BUFFER_SIZE = 4
    
    for i in range(4):
        worker = Thread(target=encrypt, args=(request_queue, output_queue))
        worker.setDaemon(True)
        worker.start()
    Sender(output_queue)
    
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
                print(unpack('!I', buffer))
                request_queue.put( unpack('!I', buffer) ) 
        
if __name__ == "__main__":
    main();