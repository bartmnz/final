package final_project;
import java.io.BufferedWriter;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.FileWriter;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Set;

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
				Connection c = new Connection(clientSocket);
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

class Connection extends Thread {
	DataInputStream input;
	DataOutputStream output;
	Socket clientSocket;
	WaterPayload data;

	public Connection(Socket aClientSocket) {
		try {
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
	private void sludge(int data){
		ProcessBuilder sludgify = new ProcessBuilder("./sludger", Integer.toString(data));
		try {
			sludgify.start();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	private void pooCatcher(){
		for (int x = 0; x < data.size/8; x++){
			WaterMolocule myWater = data.myList[x];
			if (Primes.isPrime(myWater.data)){
				myWater = new WaterMolocule();
				data.myList[x] = myWater;
				sludge(myWater.data);
				// TODO may have nodes pointing to poo that need to be corrected
			}
		}
	}

	private void debrisCatcher() {
		for (int x = 0; x < data.size/8; x++){
			WaterMolocule myWater = data.myList[x];
			if (myWater.left >= data.size/8 || myWater.left <= 0){
				myWater.left = 0xFFFF;
			}
			if (myWater.right >= data.size/8 || myWater.right <= 0 ){
				myWater.right = 0xFFFF;
			}
			// TODO compact trash and send to downstream:4444
			// whole packet is trash
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
			sludge(data.myList[x].data);				
		}	
	}
	
	// check for circularly linked list does not actually remove algorithm is implemented as defined, however does not guarantee that
	// list is not circular after
	private void seleniumCatcher(){
		// dictionary will store index in array, and value of data
		Hashtable dict = new Hashtable();
		Set<Integer> remo = new HashSet<Integer>();
		for (int x = 0; x < data.size/8; x++){
			if (data.myList[x].data != 0){
				dict.put(x, data.myList[x].data);
				// add all references to parent classes
				remo.add(data.myList[x].left);
				remo.add(data.myList[x].right);
			}
		}
		int max = 0;
		int index = 0;
		for ( int parent: remo){
			if (parent > max){
				max = parent;
				index = (int)dict.get(parent);
			}
			dict.remove(parent);
			
		}
		// iterate over list to remove reference to max value
		if( dict.isEmpty()){
			for (int x = 0; x < data.size/8; x++){
				if (x == index){
					// TODO send node to hazmat
					data.myList[x].data = 0;
					data.myList[x].left = 0;
					data.myList[x].right = 0;
				}
				if (data.myList[x].left == index){
					data.myList[x].left = 0;
				}
				if (data.myList[x].right == index){
					data.myList[x].right = 0;
				}
			}
	
		}
		// TODO send data to port 1111
	}
	
	private void phosphateCatcher(){
		
	}
}