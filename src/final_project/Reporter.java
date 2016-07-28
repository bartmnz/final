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
import java.nio.charset.StandardCharsets;


public class Reporter {

	

	@SuppressWarnings("resource")
	public static void main(String[] argv){
		final int MAX_SIZE = 1416; // 100 ints + header + air
		final String FIFO2 = "/home/sbartholomew/reportPipe";
		ByteBuffer dataOut = ByteBuffer.allocate(MAX_SIZE);
		byte[] dataIn = new byte[88]; // 100 ints
		int actuallyRead;
		WritableByteChannel waterOut;
		Socket downstream;
		FileChannel wChannel;
		FileInputStream in = null;
		
		try {
			in = new FileInputStream(FIFO2);
		} catch (FileNotFoundException e2) {
			// TODO Auto-generated catch block
			e2.printStackTrace();
		}
		System.out.println("Reporter Listening");
		while(true){
			try{
				
				int x = in.available();
				if (x > 88){ // 88 bytes in buffer I have ~22 unique reports
					waterOut = null;
					downstream = null;
					wChannel = null;
					try{
						downstream = new Socket( "downstream", 9999);
						waterOut = Channels.newChannel(new DataOutputStream(downstream.getOutputStream()));
					}
					catch(IOException e){
						System.out.println("socket Exception");
						File file = new File("ReportsOut");
						wChannel = new FileOutputStream( file, false).getChannel();
						wChannel.write(dataOut);
					}
					dataOut.clear();
					actuallyRead = in.read(dataIn, 0, 88); // will store the last octet of ip and type 
					System.out.format("Actualy read %d bytes", actuallyRead);
					// TODO ABC
					// size of report = 56 + 2 + 2 + 4 == 64
					dataOut.putShort((short)8);
					int headerSize = (actuallyRead* 32) + 8;
					dataOut.putShort((short)(headerSize)); 
					dataOut.putInt(10211);
					
					ByteBuffer wrapper = ByteBuffer.wrap(dataIn);
					int count = 0;
					int max = actuallyRead/4;
					while (count < max ){
						short type = wrapper.getShort();
						// add the type of error
						if (type == 0 ){
							dataOut.putShort((short) 8);
						}
						else {
							dataOut.putShort((short) 16);
						}
						dataOut.putShort((short)(0));
						short ipAddr = wrapper.getShort(); // add the last octet
						dataOut.putInt((int)ipAddr); // silly Liam ip address can't fit in an int
						int index = dataOut.position();
						if ( type == 0){
							// not enough data
							dataOut.put("Recieved incomplete packet".getBytes(StandardCharsets.US_ASCII));
						}
						else{
							String material;
							switch ( type ){
							case 1: 
								material = "Trash";
								break;
							case 2: 
								material = "Mercury";
								break;
							case 3: 
								material = "Lead";
								break;
							case 4: 
								material = "Selenium";
								break;
							case 5:
								material = "Poop";
								break;
							case 6:
								material = "Pee";
								break;
							default:
								material = "idk but it's bad";
							}
							dataOut.put("Someone is illegal dumping ".getBytes(StandardCharsets.US_ASCII));
							dataOut.put(material.getBytes(StandardCharsets.US_ASCII));
							dataOut.put((byte) 0); // null terminate
						}
						dataOut.position(index + 56);
						count ++;
					}
					dataOut.position(0);
//					File file = new File("Chlorine out");
//					FileChannel wChannel = new FileOutputStream( file, false).getChannel();
//					
					System.out.println("sending reports downstream");
					if( waterOut != null){
						waterOut.write(dataOut);
						waterOut.close();
						downstream.close();
					}else{
						wChannel.write(dataOut);
						wChannel.close();
					}
					
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
