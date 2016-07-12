package final_project;
import java.io.BufferedWriter;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.FileWriter;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import org.apache.commons.math3.primes.*;

import com.sun.org.glassfish.external.probe.provider.annotations.Probe;

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
		}
	}
	
	private void ammoniaCatcher(){
		for (int x = 0; x < data.size/8; x++){
			WaterMolocule myWater = data.myList[x];
			String value = Integer.toString(myWater.data);
			int[] array = new int[value.length()];
			for (x = 0; x < value.length(); x++){
				array[x] = Integer.parseInt(String.valueOf(value.charAt(x)));
				
				
			}
		}	
	}
}