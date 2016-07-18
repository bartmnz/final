package final_project;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.WritableByteChannel;

public class Hazmatter {

	private static final int MAX_SIZE = 400 + 8 ; // 100 ints + header 
	private static final String FIFO2 = "chlorinePipe";
	private static ByteBuffer dataOut = ByteBuffer.allocate(MAX_SIZE);
	private static byte[] dataIn = new byte[400]; // 100 ints
	private static int actuallyRead;
	private static WritableByteChannel waterOut;
	private static Socket downstream;

	public static void main(String[] argv){
		
		try {
			
			DataInputStream in = new DataInputStream(new FileInputStream(FIFO2));
			
			while(true){
				try{
					if (in.available() > 400){ // 400 bytes in buffer I have ~100 unique ids
//						downstream = new Socket( "downstream", 8888);
//						waterOut = Channels.newChannel(new DataOutputStream(downstream.getOutputStream()));
						
						dataOut.clear();
						actuallyRead = in.read(dataIn, 0, 400);
						// TODO ABC
						dataOut.putShort((short)1);
						dataOut.putShort((short)(actuallyRead*2));
						dataOut.putInt(10211);
						
						ByteBuffer wrapper = ByteBuffer.wrap(dataIn);
						int count = 0;
						while (count < actuallyRead/4){
							dataOut.putInt(wrapper.getInt()); // get data point
							dataOut.putInt((count+1)); 
						}
						System.out.println("Sending hazmat out");
//						waterOut.write(dataOut);
//						waterOut.close();
//						downstream.close();
						}
				}
				catch (Exception e){
					break;
				}
					
			}
			in.close();
			
			
			
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}
}