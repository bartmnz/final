package final_project;

import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.channels.WritableByteChannel;


public class Chlorinator {

	

	@SuppressWarnings("resource")
	public static void main(String[] argv){
		final int MAX_SIZE = 800 + 8 + 8; // 100 ints + header + air
		final String FIFO2 = "/home/sbartholomew/chlorinePipe";
		ByteBuffer dataOut = ByteBuffer.allocate(MAX_SIZE);
		byte[] dataIn = new byte[400]; // 100 ints
		int actuallyRead;
		WritableByteChannel waterOut;
		Socket downstream;
		FileInputStream in = null;
		
		try {
			in = new FileInputStream(FIFO2);
		} catch (FileNotFoundException e2) {
			// TODO Auto-generated catch block
			e2.printStackTrace();
		}
		System.out.println("Chlorinator Listening");
		while(true){
			try{
				
				int x = in.available();
				if (x > 400){ // 400 bytes in buffer I have ~100 unique ids
					waterOut = null;
					downstream = null;
					try{
						downstream = new Socket( "downstream", 1111);
						waterOut = Channels.newChannel(new DataOutputStream(downstream.getOutputStream()));
					}
					catch(IOException e){
						System.out.println("socket Exception");
						File file = new File("ChlorineOut");
						waterOut = new FileOutputStream( file, false).getChannel();
					}
					dataOut.clear();
					actuallyRead = in.read(dataIn, 0, 400);
					System.out.format("Actualy read %d bytes", actuallyRead);
					// TODO ABC
					dataOut.putShort((short)0);
					int headerSize = (actuallyRead*2) + 16;
					dataOut.putShort((short)(headerSize)); 
					dataOut.putInt(10211);
					
					ByteBuffer wrapper = ByteBuffer.wrap(dataIn);
					int count = 0;
					while (count < 4){ 
						dataOut.putInt(wrapper.getInt()); // get data point
						dataOut.putShort((short)(count+1));  // double link = chlorine
						dataOut.putShort((short)(count+1)); // double link = chlorine
						count++;
					}
					int max = actuallyRead/4;
					while (count < max ){
						dataOut.putInt(wrapper.getInt()); // get data point
						dataOut.putShort((short)(count+1)); 
						dataOut.putShort((short)(0));
						count ++;
					}
					//dataOut.putInt(wrapper.getInt()); // get data point
					dataOut.putInt(0);
					dataOut.putShort((short)(0)); 
					dataOut.putShort((short)(0));
					dataOut.position(0);
//					File file = new File("Chlorine out");
//					FileChannel wChannel = new FileOutputStream( file, false).getChannel();
//					
					System.out.println("sending water downstream");
					waterOut.write(dataOut);
					waterOut.close();
					downstream.close();
					
					
					}
				
				Thread.sleep(5);
			}
			
			catch (Exception e){
				try {
					in.close();
				} catch (IOException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				}
				System.out.println(e.getMessage());
				e.printStackTrace();
				break;
			}
			
		
			
			
			
//		} catch (IOException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		}
		
	}
	}
}