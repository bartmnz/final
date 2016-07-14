package final_project;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.WritableByteChannel;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Set;
import java.util.concurrent.BlockingQueue;

import org.apache.commons.math3.primes.*;


public class TCPServer {
	
	public static void main(String args[]){
		try {
			int serverPort = 1111;
			ServerSocket listenServer = new ServerSocket(serverPort);
			System.out.println("Listening on " + serverPort);
			
			// TODO change to have stop flag 
			while (true){
				Socket clientSocket = listenServer.accept();
				//TODO check that connection is from valid range
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
	private static final String FIFO1 = "sludgePipe";
	private static final String FIFO2 = "chlorinePipe";
	private Socket downstream;
	private WritableByteChannel trash;
	BufferedWriter sludge;
	BufferedWriter chlorinator;
	DataInputStream input;
	DataOutputStream output;
	Socket clientSocket;
	WaterPayload data;
	
	public Worker(Socket aClientSocket) {
		try {
			clientSocket = aClientSocket;
			input = new DataInputStream(clientSocket.getInputStream());
			// set timeout for read call
			clientSocket.setSoTimeout(1);
			sludge = new BufferedWriter(new FileWriter(FIFO1));
			chlorinator = new BufferedWriter(new FileWriter(FIFO2));
			downstream = new Socket( "downstream", 4444);
			trash = Channels.newChannel(new DataOutputStream(downstream.getOutputStream()));
			this.start();

		}

		catch (IOException e) {

			System.out.println("Connection:" + e.getMessage());

		}

	}

	public void run() {

		try { // get header
			short type = input.readShort();
			short size = input.readShort();
			int custom = input.readInt();
			//TODO check that data is there
			//TODO check that type is water
			data = new WaterPayload(size);  
			
			for (int x = 0; x < size/8; x++){
				WaterMolocule myWater = new WaterMolocule();
				myWater.data = input.readInt();
				myWater.left = input.readShort();
				myWater.right = input.readShort();
				data.add(myWater);
			}
			// TODO run checks on everything
			
			//send it to chlorinator to be sent along the way
			for (int x = 0; x < data.size/8; x++){
				if (data.myList[x].data != 0 ){
					chlorinator.write(data.myList[x].data);
				}				
			}
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

			}

			catch (IOException e) {
				/* close failed */}

		}

	}
	//not gonna do it this way right now -- using fifo pipes to minimize overhead
	/*private void sludge(int data){
		ProcessBuilder sludgify = new ProcessBuilder("./sludger", Integer.toString(data));
		try {
			sludgify.start();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}*/

	private void pooCatcher(){
		for (int x = 0; x < data.size/8; x++){
			if (Primes.isPrime(data.myList[x].data)){
				// send to sludgifier.c
				try {
					sludge.write(data.myList[x].data + "/n");
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				// set to zero
				data.myList[x].data = 0;
				data.myList[x].left = 0;
				data.myList[x].right = 0;
				
				//sludge(myWater.data);
				// TODO may have nodes pointing to poo that need to be corrected -- do i care?
			}
		}
	}

	private void debrisCatcher() {
		boolean isTrash = false;
		for (int x = 0; x < data.size/8; x++){
			if (data.myList[x].left >= data.size/8 || data.myList[x].left <= 0){
				isTrash = true;
				data.myList[x].left = 0xFFFF;
			}
			if (data.myList[x].right >= data.size/8 || data.myList[x].right <= 0 ){
				isTrash = true;
				data.myList[x].right = 0xFFFF;
			}
			// TODO compact trash and send to downstream:4444
			// whole packet is trash
		}
		
		if (isTrash){
			ByteBuffer header = ByteBuffer.allocate(data.size/8 + 8);
			header.putShort((short)1);
			header.putShort((short)(data.size/8));
			header.putInt(10211);
			for (int x = 0; x < data.size/8; x++){ 
				header.putInt(data.myList[x].data);
				header.putShort((short)data.myList[x].left);
				header.putShort((short)data.myList[x].right);
			}
			try {
				trash.write(header);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}
	
	private void ammoniaCatcher(){
		for (int x = 0; x < data.size/8; x++){
			int value = data.myList[x].data;
			int a = value % 10;
			value /= 10;
			int b = value % 10;
			if ( a > b ){
				value /= 10;
			}
			while( value > 99){
				a = value % 10;
				value /= 10;
				b = value % 10;
				value /= 10;
				int c = value % 10;
				// c will always loop
				if ( a > b || b < c ){
					// number is not undulating
					return;
				}
			}
			// finished loop number is undulating
			// send to sludgifier.c
			try {
				sludge.write(data.myList[x].data + "/n");
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
	
	// check for circularly linked list does not actually remove algorithm is implemented as defined, however does not guarantee that
	// list is not circular after
	private void seleniumCatcher(){

		int start = 0;
		while (start < (data.size/8 -1)){
			if (data.myList[start].data != 0){
				break;
			}
			start++;
		}
		//first molocule in chain or last in array
		int count = 0;
		Set<Integer> nodes = new HashSet<Integer>();
		do{
			if (data.myList[start].left != data.myList[start].right){
				return;
			}
			nodes.add(start);
			start = data.myList[start].left;
			count ++;
		} while (count < data.size/8 && count > 0);
		
		if ( count == nodes.size() && count > 1){
			// TODO have selinium 
		}
		
	}
	
	private void phosphateCatcher(){
		int cur = 0;
		int prev = 0;
		while (cur < (data.size/8 -1)){
			if (data.myList[cur].data != 0){
				if (data.myList[cur].left == 0 ^ data.myList[cur].right == 0){
					prev = cur;
					cur = Math.max(data.myList[cur].left, data.myList[cur].right);
					// TODO is it faster to check max manually?
					break;
				}
			}
			cur++;
		}
		//first molocule in chain or last in array
		int count = 1;
		Set<Integer> nodes = new HashSet<Integer>();
		nodes.add(prev);
		do{
			if (!( data.myList[cur].left == prev ^ data.myList[cur].right == prev)){
				return;
			}
			nodes.add(cur);
			cur = data.myList[cur].left == prev ? data.myList[cur].left : data.myList[cur].right;
			count ++;
		} while (count < data.size/8 && count > 0);
		
		if ( count == nodes.size() && count > 1){
			// TODO have phosphate 
		}
		
	}
	
	private void mercuryCatcher(){
		int count = 0;
		Set<Integer> nodes = new HashSet<Integer>();
		nodes.add(0); // make sure it will be in the set -- depend on it later
		for (int x = 0; x < (data.size/8 -1); x++){
			if (data.myList[x].data != 0 ){			
			// only valid nodes will have children -- trash has been removed
			nodes.add(data.myList[x].right);
			nodes.add(data.myList[x].left);
			count++;
			}
		}
		if ( count > nodes.size() ){
			// TODO have mercury
		}
	}
	
	
		// TODO when it matters actually try to keep data structure intact
		/*int count = 0;
		ArrayList<Integer> nodes = new ArrayList<Integer>();
		ArrayList<Integer> tofix = new ArrayList<Integer>();
		for (int x = 0; x < (data.size/8 -1); x++){
			if (data.myList[x].data == 0){
				continue;
			}
			count++;
			if (data.myList[x].left != 0 && data.myList[x].left == data.myList[x].right ){
				nodes.add(x);
				continue;
			}
			tofix.add(x);
		}
		int max = (int)(count * .05);
		int min = (int)(count * .03);
		count = 0;
		int fix = nodes.size();
		while ( fix > max){
			data.myList[nodes.get(count)].right = 0;
			count++;
			fix--;
		}
		count = 0;
		while ( fix < min ){
			
		}
		*/
	
}
