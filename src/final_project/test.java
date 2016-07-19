package final_project;
import java.io.BufferedWriter;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Collections;
import java.util.Comparator;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.WritableByteChannel;
import java.util.HashSet;
import java.util.Set;


import org.apache.commons.math3.primes.Primes;


public class test {
	//protected static ExecutorService threadPool = Executors.newFixedThreadPool(10);
	
	public static void main(String args[]){
		try {
			int serverPort = 1111;
			ServerSocket listenServer = new ServerSocket(serverPort);
			System.out.println("Listening on " + serverPort);
			
			// TODO change to have stop flag 
			while (true){
				
				Socket clientSocket = listenServer.accept();
				//TODO check that connection is from valid range
				System.out.format("connection on + %s/n", clientSocket.getInetAddress());
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
	private Socket downstream;
	private WritableByteChannel trash; // goes directly downstream
	DataOutputStream sludge;
	DataOutputStream chlorinator;
	DataOutputStream hazmatter;
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
			
			sludge = new DataOutputStream (new FileOutputStream(FIFO1));
			chlorinator = new DataOutputStream (new FileOutputStream(FIFO2));
			hazmatter = new DataOutputStream (new FileOutputStream(FIFO3));
			downstream = new Socket( "downstream", 4444);
			trash = Channels.newChannel(new DataOutputStream(downstream.getOutputStream()));
			clientSocket = aClientSocket;
			input = new DataInputStream(clientSocket.getInputStream());
			// set timeout for read call
			clientSocket.setSoTimeout(1);
			this.start();
		}

		catch (IOException e) {

			System.out.println("Connection:" + e.getMessage());

		}

	}
// TODO move socket connection here
	public void run() {
		try { // get header
			
			short type = input.readShort();
			short size = input.readShort();
			int custom = input.readInt();
			size = (short) ((size /8) -1);
			//TODO check that data is there
			//TODO check that type is water
			data = new WaterPayload(size);  
			
			for (int x = 0; x < size; x++){
				WaterMolocule myWater = new WaterMolocule();
				myWater.data = input.readInt();
				myWater.left = input.readShort();
				myWater.right = input.readShort();
				data.add(myWater);
			}
			
			boolean keepRunning = debrisCatcher();
			if (keepRunning) keepRunning = mercuryCatcher(true);
			if (keepRunning) leadCatcher();
			if (keepRunning) seleniumCatcher();
			if (keepRunning) pooCatcher();
			if (keepRunning) ammoniaCatcher();
			//if (keepRunning) phosphateCatcher(); //don't even need to run this at the moment as treatment is just chlorinate it

			chlorineSender();
			
		}

		catch (EOFException e) {

			System.out.println("EOF:" + e.getMessage());
		}

		catch (IOException e) {

			System.out.println("IO:" + e.getMessage());
		}

		finally {

			try {

				clientSocket.close();
				downstream.close();

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
			//		System.out.println("sending water");
					chlorinator.writeInt(data.myList[x].data);
					
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}				
		}
	}

	private void pooCatcher(){
		for (int x = 0; x < data.size; x++){
			if (Primes.isPrime(data.myList[x].data)){
				// send to sludgifier.c
				try {
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

	
	private void leadCatcher(){
		int first = 0;
		for (int x = 0; x < data.size; x++){
			first = (int)((Math.sqrt(1 + 8*data.myList[x].data)-1)/2);
			if ( first*first + first  == data.myList[x].data * 2){
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
			ByteBuffer header = ByteBuffer.allocate(data.size*8 + 8);
			header.putShort((short)1);
			header.putShort((short)(data.size));
			header.putInt(10211);
			for (int x = 0; x < data.size; x++){ 
				header.putInt(data.myList[x].data);
				header.putShort((short)data.myList[x].left);
				header.putShort((short)data.myList[x].right);
			}
			try {				
				trash.write(header);
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			return false;
		}
		return true;
	}
	
	
	private boolean undulatingLower(int data){
		int a, b, c, value = data;
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
	
	private boolean undulatingHigher(int data){
		int a, b, c, value = data;
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
		int a, b, value;
		boolean sludgey;
		for (int x = 0; x < data.size; x++){
			value = data.myList[x].data;
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
	
	// check for circularly linked list does not actually remove algorithm is implemented as defined, however does not guarantee that
	// list is not circular after
	private void seleniumCatcher(){

		// TODO make sure that we are on the first node in a potential list
		Set<Integer> nodes = new HashSet<Integer>();
		nodes.add(0);
		int count = 0;
		int cur = 1;
		int prev = 1;
		int next = 0;
		int reverse = (data.myList[count].left != 0 && data.myList[count].right !=0) ? 1 : 0;
		while( count < data.size){
			if(data.myList[cur-1].left != prev){
				next = data.myList[cur-1].left;
			} else reverse ++;
			if (data.myList[cur-1].right != prev ){
				if( next != 0 && data.myList[cur-1].right != next){
					return;// two forward nodes
				}
				next = data.myList[cur-1].right;
			} else reverse ++;
			prev = cur;
			cur = next;
			count++;
			if ( ! nodes.add(next-1) || next == 0) break; // nowhere to go
			//if (reverse != count ){
				//return;
			//}
		}
		if ( next == 1 && count != 0){
			// have selinium
			int index = Collections.max(nodes, checkIndex);
			try {
				hazmatter.writeInt(data.myList[index].data);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
//			for ( int x = 0 ; x < data.size; x++){
//				if ( data.myList[x].data > max){
//					max = data.myList[x].data;
//					index = x;
//				}
//			}
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
		for (int x = 0; x < (data.size); x++){
			//if (data.myList[x].data != 0 ){ may not need since air counts towards structure 			
			// only valid nodes will have children -- trash has been removed
			nodes.add(data.myList[x].right-1); // index as counting numbers 
			nodes.add(data.myList[x].left-1);
			
		}
		if ( (data.size -1) > nodes.size() ){
			Set<Integer> mercury = new HashSet<Integer>();
			for ( int x = -1; x< (data.size); x++){
				mercury.add(x);
			}
			mercury.removeAll(nodes);
			int index = 0;
			mercury.remove(Collections.max(mercury, checkIndex));
			for ( int node : mercury ){
				try {
					if (data.myList[node].data != 0){
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