package final_project;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.WritableByteChannel;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Set;

import org.apache.commons.math3.primes.Primes;


public class test {
	//protected static ExecutorService threadPool = Executors.newFixedThreadPool(10);
	
	public static void main(String args[]){
		try {
			int serverPort = 1111;
			@SuppressWarnings("resource")
			ServerSocket listenServer = new ServerSocket(serverPort);
			System.out.println("Listening on " + serverPort);
			
			
			// TODO change to have stop flag 
			while (true){
				
				Socket clientSocket = listenServer.accept();
				//TODO check that connection is from valid range
				System.out.format("connection on + %s\n", clientSocket.getInetAddress());
				@SuppressWarnings("unused")
				Worker c = new Worker(clientSocket);
			}
			
		}
		catch(IOException e){
			System.out.println("Listen :" +e.getMessage());
		}
	}

}
class WaterPayload{
	WaterMolocule[] myList;
	int index;
	int size;
	WaterPayload(int size){
		myList = new WaterMolocule[size];
		this.size = size;
		index = 0;
	}
	void add(WaterMolocule water){
		if (index < myList.length){
			myList[index] = water;
			index ++;
		}
		 
	}
}

class WaterMolocule{
	int data;
	int left;
	int right;
	
	WaterMolocule(){
		this.data = 0;
		this.left = 0;
		this.right = 0;
	}
	
}

class Worker extends Thread {
	private static final String FIFO1 = "/home/sbartholomew/sludgePipe";
	private static final String FIFO2 = "/home/sbartholomew/chlorinePipe";
	private static final String FIFO3 = "/home/sbartholomew/hazmatPipe";
	//private static final String FIFO4 = "/home/sbartholomew/reportPipe";
	private Socket downstream;
	private WritableByteChannel trash; // goes directly downstream
	Hashtable<Integer, Integer> fibSeq;
	DataOutputStream sludge;
	DataOutputStream chlorinator;
	DataOutputStream hazmatter;
	//DataOutputStream reporter;
	
	DataInputStream input;
	DataOutputStream output;
	Socket clientSocket;
	WaterPayload data;
	Comparator<Integer> checkIndex;
	
	public Worker(Socket aClientSocket) {
		try {
			checkIndex = new Comparator<Integer>(){
				@Override
				public int compare(Integer arg0, Integer arg1) {
					if( data.myList[arg0].data == data.myList[arg1].data) return 0;
					if( data.myList[arg0].data > data.myList[arg1].data) return 1;
					return -1;
				}
			};
			fibSeq = new Hashtable<Integer, Integer>(100);
			int a = 1;
			int b = 1;
			int n = 1;
			while ( n <= 50 ){
				fibSeq.put(b, b);
				a += b;
				b = a - b;
				n++;
			}
			sludge = new DataOutputStream (new FileOutputStream(FIFO1));
			chlorinator = new DataOutputStream (new FileOutputStream(FIFO2));
			hazmatter = new DataOutputStream (new FileOutputStream(FIFO3));
			//reporter = new DataOutputStream (new FileOutputStream(FIFO4));
			
			clientSocket = aClientSocket;
			input = new DataInputStream(clientSocket.getInputStream());
			// set timeout for read call
			//clientSocket.setSoTimeout(1);
			this.start();
		}

		catch (IOException e) {

			System.out.println("Connection:" + e.getMessage());

		}

	}
// TODO move socket connection here
	public void run() {
		try { // get header
			
			@SuppressWarnings("unused")
			short type = input.readShort();
			short size = input.readShort();
			@SuppressWarnings("unused")
			int custom = input.readInt();
			size = (short) ((size /8) -1);
			//TODO check that data is there
			//TODO check that type is water
			data = new WaterPayload(size);  
			
			for (int x = 0; x < size; x++){
				WaterMolocule myWater = new WaterMolocule();
				myWater.data = input.readInt();
				myWater.left = input.readUnsignedShort();
				myWater.right = input.readUnsignedShort();
				data.add(myWater);
			}
			fungusCatcher();
			boolean keepRunning = debrisCatcher();
			if (keepRunning){ 
				keepRunning = mercuryCatcher(true);
				leadCatcher(false);
				if (keepRunning) seleniumCatcher();
				pooCatcher();
				ammoniaCatcher();
				chlorineSender();
			}
			//if (keepRunning) phosphateCatcher(); //don't even need to run this at the moment as treatment is just chlorinate it
//			System.out.println("Sending Chlorine");
			
			
		}

		catch (EOFException e) {

			System.out.println("EOF:" + e.getMessage());
		}

		catch (IOException e) {

			System.out.println("IO:" + e.getMessage());
		}

		finally {

			try {
				System.out.format("closed + %s\n", clientSocket.getInetAddress());
				clientSocket.close();
				//downstream.close();

			}

			catch (IOException e) {
				/* close failed */}

		}

	}

	
	private void chlorineSender(){
		//System.out.println("here");
		for (int x = 0; x < data.size; x++){
			if (data.myList[x].data != 0 ){
				try {
//					System.out.println("sending water");
					chlorinator.writeInt(data.myList[x].data);
					
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}				
		}
	}

	private void fungusCatcher(){
		for ( int x = 0; x < data.size ; x++){
			if (fibSeq.contains(data.myList[x].data)){
				try {
					hazmatter.writeInt(data.myList[x].data);
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				data.myList[x].data = 0;
				data.myList[x].left = 0;
				data.myList[x].right = 0;
			}
		}
	}
	
	private void pooCatcher(){
		boolean isPrime;
		for (int x = 0; x < data.size; x++){
			if (data.myList[x].data < 0 ){
				long tooBig  = data.myList[x].data & 0x00000000ffffffffL;
				if (tooBig % 2 == 0 ) continue; // we can restart the loop
				isPrime = true;
				for(int y = 3; y <= Math.sqrt(tooBig); y+=2){
					if ( tooBig % y == 0 ){
						isPrime = false;
						break;	
					}
				}
				// all the way through the loop and no factor !!!
				if ( isPrime ){
					try {
						sludge.writeInt(data.myList[x].data);
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
					data.myList[x].data = 0;
					data.myList[x].left = 0;
					data.myList[x].right = 0;
					continue;
				}
			}
				
			if (Primes.isPrime(data.myList[x].data)){
				// send to sludgifier.c
				try {
//					System.out.println("Sending sludge -- Poo");
					// sendReport(5);
					sludge.writeInt(data.myList[x].data);
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				// set to zero
				data.myList[x].data = 0;
				data.myList[x].left = 0;
				data.myList[x].right = 0;
				
				// TODO may have nodes pointing to poo that need to be corrected -- do i care?
			}
		}
	}

	
	private void leadCatcher(boolean trash){
		long first = 0;
		for (int x = 0; x < data.size; x++){
			if ( data.myList[x].data != 0){
				long tooBig = data.myList[x].data & 0x00000000ffffffffL;
				first = (long)((Math.sqrt(1 + 8*tooBig)-1)/2);
				if ( first*first + first  == tooBig * 2){
					try {
						// sendReport(3);
						System.out.println("sending Hazmat -- Lead");
						hazmatter.writeInt(data.myList[x].data);
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
					data.myList[x].data = 0;
					if ( ! trash){
						data.myList[x].left = 0;
						data.myList[x].right = 0;
					}
					else{
						int temp = data.myList[x].left;
						data.myList[x].left = data.myList[x].right;
						data.myList[x].right = temp;
					}
				}
			}
		}
	}
	
/*	private void sendReport(int type){
		try {
			// put the short and the type in the report sending thingy
			reporter.writeShort((short)type);
			String address = clientSocket.getRemoteSocketAddress().toString();
			address = address.substring(address.lastIndexOf('.')+ 1, address.indexOf(':'));
			reporter.writeShort(Short.parseShort(address));
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		} 
	}*/ 
	
	private void trashCompactor(){
		//de-lead have to do separately because treatment is different for trash
		leadCatcher(true);
		//compact all indices already set to FFFF 
		
		Hashtable<Integer, Integer> dictionary = new Hashtable<Integer, Integer>(data.size*2);
		// dictionary will map the current index to a compressed index
		
		dictionary.put(0, 0); // add the null value
		dictionary.put(0xFFFF, 0xFFFF); // add the out of bounds index
		
		int compactedIndex = 1; // how many things we have
		// iterate over list and find all valid nodes
		for(int x = 0; x < data.size; x++){
			if (data.myList[x].data != 0){ // we have a valid node -- not air
				dictionary.put((x+1), compactedIndex); // <current index in list>, <nth (not air) thing in the list>
				compactedIndex++; // increment counter
			}
		}
		for(int x = 0; x < data.size; x++){
			// get the compressed index or null if we are pointing to an air molecule
			Integer newLeft = dictionary.get(data.myList[x].left);
			if ( newLeft == null){ // this means we are pointing to an air molecule
				// follow the pointer and get the index of the thing on its left
				data.myList[x].left = dictionary.get(data.myList[data.myList[x].left].left); 
			}
			// we are not pointing to an air molecule.
			else{
				data.myList[x].left = (int)newLeft;
			}
			Integer newRight = dictionary.get(data.myList[x].right);
			if ( newRight == null){ // this means we are pointing to an air molecule
				// follow the pointer and get the index of the thing on its right
				data.myList[x].right = dictionary.get(data.myList[data.myList[x].right].right); 
			}
			// we are not pointing to an air molecule
			else{
				data.myList[x].right = (int)newRight;
			}
		}
		// send data
		ByteBuffer header = ByteBuffer.allocate(compactedIndex*8 + 8);
		header.putShort((short)1);
		header.putShort((short)(compactedIndex));
		header.putInt(10211);
		for (int x = 0; x < data.size; x++){ 
			if ( data.myList[x].data != 0){
				header.putInt(data.myList[x].data);
				header.putShort((short)data.myList[x].left);
				header.putShort((short)data.myList[x].right);
			}
		}
		header.position(0);
		try {
			System.out.println("sending trash");
			downstream = new Socket( "downstream", 2222);
			trash = Channels.newChannel(new DataOutputStream(downstream.getOutputStream()));
			trash.write(header);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			System.out.println("ERROR: could not connect trash to downstream");
			e.printStackTrace();
		}
	}
	
	
	private boolean debrisCatcher() {
		boolean isTrash = false;
		for (int x = 0; x < data.size; x++){
			if (data.myList[x].left > data.size ){
				isTrash = true;
				data.myList[x].left = 0xFFFF;
			}
			if (data.myList[x].right > data.size ){
				isTrash = true;
				data.myList[x].right = 0xFFFF;
			}
			// whole packet is trash
		}
		
		if (isTrash){
			trashCompactor();
			return false;
		}
		return true;
	}
	
	
	private boolean undulatingLower(long data){
		long a, b, c, value = data;
		while( value > 99){
			a = value % 10;
			value /= 10;
			b = value % 10;
			value /= 10;
			c = value % 10;
			// c will always loop
			if ( a >= b || b <= c ){
				// number is not undulating
				return false;
			}
		}
		
		return true;
	}
	
	private boolean undulatingHigher(long data){
		long a, b, c, value = data;
		while( value > 99){
			a = value % 10;
			value /= 10;
			b = value % 10;
			value /= 10;
			c = value % 10;
			// c will always loop
			if ( a <= b || b >= c ){
				// number is not undulating
				return false;
			}
		}
		return true;
	}
	
	
	private void ammoniaCatcher(){
		long a, b, value;
		boolean sludgey;
		for (int x = 0; x < data.size; x++){
			value  = data.myList[x].data & 0x00000000ffffffffL;
			if (value == 0){
				continue;
			}
			a = value % 10;
			value /= 10;
			b = value % 10;
			value /= 10;
			if ( a == b ){
				sludgey = false;
			}
			else if ( a > b ){
				sludgey = undulatingHigher(data.myList[x].data);
			}else {
				sludgey = undulatingLower(data.myList[x].data);
			}
			if(sludgey){
				try {
//					System.out.println("sending Sludge -- Amonia");
					// sendReport(6);
					sludge.writeInt(data.myList[x].data);
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				// set to zero
				data.myList[x].data = 0;
				data.myList[x].left = 0;
				data.myList[x].right = 0;
			}
			
		}	

	}
	
	private void singleForwardNode(int start){
		// TODO make sure that we are on the first node in a potential list
				Set<Integer> nodes = new HashSet<Integer>();
				nodes.add(0);
				int count = 0;
				int cur = start;
				int next = 0;
				
				//int reverse = (data.myList[count].left != 0 && data.myList[count].right !=0) ? 1 : 0;
				while( count < data.size){
					next = 0;
					next = data.myList[cur-1].left;
					if (next == 0){
						next = data.myList[cur-1].right;
					} else if( data.myList[cur-1].right != next && data.myList[cur-1].right != 0){
						return;// two forward nodes
					}//else //reverse ++;
					cur = next;
					count++;
					if ( ! nodes.add(next-1) || next == 0) break; // nowhere to go
					//if (reverse != count ){
						//return;
					//}
				}
				if ( next == start && count != 0){
					// have selinium
					int index = Collections.max(nodes, checkIndex);
					try {
						System.out.println("sending Hazmat -- Selinium (single forward)");
						// sendReport(4);
						hazmatter.writeInt(data.myList[index].data);
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
//					for ( int x = 0 ; x < data.size; x++){
//						if ( data.myList[x].data > max){
//							max = data.myList[x].data;
//							index = x;
//						}
//					}
					data.myList[index].data = 0;
					data.myList[index].left = 0;
					data.myList[index].right = 0;
					for ( int x = 0 ; x < data.size; x++){
						if ( data.myList[x].left == index+1){
							data.myList[x].left = 0;
						}
						if (data.myList[x].right == index+1){
							data.myList[x].right = 0;
						}
					}
					

				}
	}
	private void doubleForwardNode(int start){
		// TODO make sure that we are on the first node in a potential list
				Set<Integer> nodes = new HashSet<Integer>();
				nodes.add(0);
				int count = 0;
				int cur = start;
				int prev = data.myList[start].left;
				int next = data.myList[start].right;
				
				//int reverse = (data.myList[count].left != 0 && data.myList[count].right !=0) ? 1 : 0;
				while( count < data.size){
					next = 0;
					// one of them MUST be previous
					if(data.myList[cur-1].left != prev ^ data.myList[cur-1].right != prev){
						next = data.myList[cur-1].left;
						if( next == prev){
							next = data.myList[cur-1].right;
					} else break;
					
					}//else //reverse ++;
					prev = cur;
					cur = next;
					count++;
					if ( ! nodes.add(next-1) || next == 0) break; // nowhere to go
					//if (reverse != count ){
						//return;
					//}
				}
				if ( next == start && count != 0){
					// have selinium
					int index = Collections.max(nodes, checkIndex);
					try {
						System.out.println("sending Hazmat -- Selinium (double kind)");
						// sendReport(4);
						hazmatter.writeInt(data.myList[index].data);
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
//					for ( int x = 0 ; x < data.size; x++){
//						if ( data.myList[x].data > max){
//							max = data.myList[x].data;
//							index = x;
//						}
//					}
					data.myList[index].data = 0;
					data.myList[index].left = 0;
					data.myList[index].right = 0;
					for ( int x = 0 ; x < data.size; x++){
						if ( data.myList[x].left == index+1){
							data.myList[x].left = 0;
						}
						if (data.myList[x].right == index+1){
							data.myList[x].right = 0;
						}
					}
					

				}
	}
	
	// check for circularly linked list does not actually remove algorithm is implemented as defined, however does not guarantee that
	// list is not circular after
	private void seleniumCatcher(){

		// TODO make sure that we are on the first node in a potential list
		Set<Integer> nodes = new HashSet<Integer>();
		nodes.add(0);
		int cur = 0;
		while (cur < (data.size )){
			//if (data.myList[cur].data != 0){
			if (data.myList[cur].left != 0 || data.myList[cur].right != 0){
				if (data.myList[cur].left != 0 && data.myList[cur].right != 0 && 
						data.myList[cur].left != data.myList[cur].right){
					doubleForwardNode(cur +1);
					return;// double linked list
				}
				singleForwardNode(cur + 1);
				return;
			}
			//}
			cur++;
		}
		
		
	}
	
	@SuppressWarnings("unused")
	private int phosphateCatcher(){
		int cur = 0;
		int prev = 0;
		int start = 0;
		// go until we find the start of the list 
		while (cur < (data.size -1)){
			//if (data.myList[cur].data != 0){
			if (data.myList[cur].left == 0 ^ data.myList[cur].right == 0){
				prev = cur;
				cur = Math.max(data.myList[cur].left, data.myList[cur].right);
				cur --; // make it have the appropriate index
				// TODO is it faster to check max manually?
				break;
			}
			//}
			cur++;
		}
		start = prev;
		//first molocule in chain or last in array
		int count = 1;
		int temp = 0;
		do{
			if (!( data.myList[cur].left == prev+1 ^ data.myList[cur].right == prev+1)){
				return 1;
			}
			//nodes.add(cur);
			temp = data.myList[cur].left == prev ? data.myList[cur].left : data.myList[cur].right;
			prev = cur;
			cur = temp -1; // make sure index is correct 
			count ++; 
		} while (count < data.size && count > 0);
		
		if ( count == data.size && count > 1){
			// have phosphate 
			return -1;
			// treatment = make chlorine --- its just water pass it allong
			 
//			if ( data.myList[start].data > data.myList[cur].data){
//				cur = start;
//			}
//			prev = cur;
//			cur = Math.max(data.myList[cur].left, data.myList[cur].right);
//			do{
//				cur = data.myList[cur].left == prev ? data.myList[cur].left : data.myList[cur].right;
//				count ++;
//			} while (count < data.size && count > 0);

		}
		return 1;
		
	}
	
	// if we have mercury we CANNOT have any other heavy metals
	private boolean mercuryCatcher(boolean returnValue){
		
		Set<Integer> nodes = new HashSet<Integer>();
		nodes.add(-1); // make sure it will be in the set -- depend on it later
		
		int size = 1; // size starts at 1 because we already added Null to nodes set
		for (int x = 0; x < (data.size); x++){
			//if (data.myList[x].data != 0 ){ may not need since air counts towards structure 			
			// only valid nodes will have children -- trash has been removed
			nodes.add(data.myList[x].right-1); // index as counting numbers 
			nodes.add(data.myList[x].left-1);
			if ( data.myList[x].left !=0 || data.myList[x].right !=0 || data.myList[x].data !=0){
				size ++;
			}

			
		}
		if ( (size - 1) > nodes.size() ){ // if there is more than one valid node without a parent
			
			try{
				File file = new File("SampleMercury");
				WritableByteChannel wChannel = Channels.newChannel(new FileOutputStream( file ));
				ByteBuffer dataOut = ByteBuffer.allocate((data.size*8)+8);
				dataOut.putShort((short)0);
				int headerSize = (data.size*8) + 8;
				dataOut.putShort((short)(headerSize)); 
				dataOut.putInt(10211);
				for (int x=0; x < data.size; x++){
					dataOut.putInt(data.myList[x].data); // get data point
					dataOut.putShort((short)data.myList[x].left); 
					dataOut.putShort((short)data.myList[x].right);
				}
				dataOut.position(0);
				wChannel.write(dataOut);
				wChannel.close();
			}
			catch (Exception e){
				System.out.println("Could not write sample to file");
			}
			Set<Integer> mercury = new HashSet<Integer>();
			for ( int x = -1; x< (data.size); x++){
				mercury.add(x);
			}
			mercury.removeAll(nodes);
			@SuppressWarnings("unused")
			int index = 0;
			mercury.remove(Collections.max(mercury, checkIndex));
			for ( int node : mercury ){
				try {
					if (data.myList[node].data != 0){
						System.out.println("sending Hazmat -- Mercury");
						hazmatter.writeInt(data.myList[node].data);
					}
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				data.myList[node].data =0;
				data.myList[node].left =0;
				data.myList[node].right=0;
				data.size -= 8;
			}
			return mercuryCatcher(false);
		}
		return returnValue;
		
	}
	
}