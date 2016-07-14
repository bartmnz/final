#!/usr/local/bin/python3.5

from ctypes import CDLL
lib = cdll.LoadLibrary('./sludger.so')

class Header:
    def __init__(self, blob):
        (self.type, self.size, self.custom) = struct.unpack("!HHL", blob)

    def serialize(self):
        return struct.pack("!HHL", self.type, self.size, self.custom)

class Worker(Process):
    def __init__(self, data, in_queue, out_queue):
        super(Worker, self).__init__()
        self.data = data
        self.in_queue = in_queue
        self.out_queue = out_queue
    
    def run(self):
        for data in iter( self.in_queue.get, None ):
            self.out_queue.put(lib.sludge(data))
        

class Sender(Process):
    def __init__(self, queue):
        super(Sender, self).__init__()
        self.queue = queue
        self.header = Header()
        
    def run(self):
        # TODO set time limit for checking
        while(true):
            #set up connection
            count = 0
            hazmat = []
            while(count < 100):
                hazmat.append(self.queue.get_nowait())
                count += 1
            sludge_outgoing = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
            sludge_outgoing.connect(("downstream", 4444))
            self.header.size = 6400
            sludge_outgoing.send(header.serialize())
            while (count > 0):
                sludge_outgoing.send(
                    bytes(hazmat[count], 'utf-8'))
                count -= 1
            sludge_outgoing.close()
            
            
            

def main():
    fifo = open('sludgePipe', 'r')
    request_queue = Queue()
    output_queue = Queue()
    for i in range(4):
        Worker( request_queue, output_queue ).start()
    Sender(output_queue)
    for data in the_real_source:
        request_queue.put( data )
# Sentinel objects to allow clean shutdown: 1 per worker.
    for i in range(4):
        request_queue.put( None ) 
    
