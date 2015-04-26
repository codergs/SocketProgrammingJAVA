package udpSend.Receive;

import java.net.*;
import java.io.*;
import java.util.Random;

public class UDPSender 
{
	// ------------- fields ------------- //
	// Static fields
		private static final int LENGTH=0;							// The  MSB Byte for length in DATA Packet set to zero.
		private  static final int SERVER_PORT=1300;					// Initializing server port number, same as the one the server is listening on
		private static final byte[] KEYS={12,13,14,15,16,17,18,19}; // The shared 64-bit secret key 
		private static final int ALLOWED_ATTEMPTS=4;				// No of allowed transmission trials 
		private static byte[] newKey=new byte[16];					// 16 Bytes new Key after interleaving nonce bytes and shared secret key
		public static void main(String[] args)  throws IOException{
			
			// Instance fields
			int dataPacketNo=0;
			
			Random randomObj=new Random();					 // Object of random class			
			
			DatagramSocket socket=new DatagramSocket();	     // Creating a socket for the transmitter end			
			DatagramPacket INITpacket,IACKpacket,DATApacket,DACKpacket; 
			
			byte[] integrityCheckValue=new byte[2];			// Byte array to hold the value of Integrity check field
			
			byte[] sendBuffer=new byte[6];					// Byte buffer holding the INIT packet to be sent
			byte[] receiveBuffer=new byte[14];				// Byte buffer holding the IACK packet received
			
			byte[] sendDataBuffer=new byte[48];				// Byte buffer holding the DATA packet to be sent
			
			
			byte[] payload=new byte[40];					// Byte array holding the payload of the data
			byte[] cipher=new byte[40];						// Encrypted payload
					
			byte[] sequenceNumber=new byte[2];				// Stores the sequence# of the packets(2Bytes)
			byte[] packetType=new byte[2]; 					// Stores the packet type(2Bytes)
			
			byte[] data=new byte[351];						// Byte array to hold the 351-bytes of data
			sendBuffer=SenderFunctions.initPacketBuffer();
			packetType[0]=sendBuffer[0];
			packetType[1]=(byte) (sendBuffer[1]+1);
			
		// Loop back method is used to send request to the same machine, Providing Sever's IP address and Server's Host name approach can also be used.
			InetAddress localHostAddress= InetAddress.getLocalHost();
			System.out.println(localHostAddress);
				   
			
		// Creating the UDP INIT packet to be sent			
			INITpacket= new DatagramPacket(sendBuffer, sendBuffer.length,localHostAddress,SERVER_PORT);
			
	
		// Creating the UDP IACK packet to be received		
			IACKpacket = new DatagramPacket(receiveBuffer, receiveBuffer.length);
		
			int i=0; // Counts the number of times INIT is sent 
			int j=1; // Temporary variable set to double the timeout after each INIT sent
			
			System.out.println("Preparing to establish a connection with server......");
			
		// Setting count so as to send INIT and expect IACK for 4 times(considering timeout) 
			while(i<ALLOWED_ATTEMPTS){
				try{
					System.out.println("Sending INIT packet");
					socket.send(INITpacket);				// Sending INIT packet to the receiver
					socket.setSoTimeout(j*1000);			// Timeout value in milliseconds
					j=j*2;									// Timeout value DOUBLED after sending INIT each time
					i++;									// Incrementing the count of number of times INIT is sent 
					
					socket.receive(IACKpacket);				// Receiving an IACK packet from the receiver
					
					// Checks if the transmitter should accept the received IACK if the following checks are passed
					if(SenderFunctions.integrityCheckReceived(receiveBuffer)){ 
						// Calling function for "INTEGRITY CHECK" on the received IACK packet
						System.out.println("Integrity Check Passed: Receiving IACK");
						
					    if(receiveBuffer[0]==packetType[0] && receiveBuffer[1]==packetType[1] && receiveBuffer[2]==sendBuffer[2] && receiveBuffer[3]==sendBuffer[3]){  
					    	// Checks for the correct PACKET TYPE for the IACK 	
					    		keyGeneration(receiveBuffer);
								packetType[1]++;						//Incrementing & Setting PACKET TYPE for DATA
								sequenceNumber[0]=receiveBuffer[2]; 	//The received sequence#=sequence# of the first data packet
								sequenceNumber[1]=receiveBuffer[3];
								System.out.println("Two way handshake complete between Client & Server");
								break; 	//Exit the loop if valid IACK received before timeout and declare as handshake to be completed.
						}
					}
					else{
						if(i==ALLOWED_ATTEMPTS){	
							System.out.println("Integrity Check Failed: Discarding the IACK");
						    throw new Exception("Communication Failure- Server not responding");
						    // Discard IACK & throw exception, if Integrity check fails for INIT sent for the 4th time	
						}
						else{	
							System.out.println("Integrity Check Failed: Discarding the IACK");	
						    // Discard IACK & Flash error, if Integrity check fails for INIT sent less than 4 times
						}
					}
				}catch (Exception e){
					if(i==ALLOWED_ATTEMPTS){			// If Transmitter doesn't receive an IACK even after sending INIT packet 4 times
						System.out.println("\n"+e.getMessage());
					    socket.close();				// Closing Transmitter's socket
					    System.out.println("\nClient's socket closed");
						System.exit(0);				// Exit
					}
					else							// If INIT is sent less than 4 times, print "Timeout" (and re-transmit)
						System.out.println("Client's Socket TimeOut -"+e.getMessage());
				}
			} // while loop ends
			
			if(i<=ALLOWED_ATTEMPTS){	
				// Random bytes generated as DATA
				randomObj.nextBytes(data);	
				
				dataPacketNo=(data.length/40)+1;	// Determining the number of packets to be sent
				System.out.println("No. of Data packets to be sent "+dataPacketNo);
				System.out.println("------------------------------------------------------------------------------------\n");
				
				int positionCounter=0,k; // positionCounter= counter on (351 bytes) data array
				
				// Creating DATA packet to be sent
				DATApacket = new DatagramPacket(sendDataBuffer,sendDataBuffer.length,localHostAddress,SERVER_PORT);
				
				// Creating DACK packet that will be received 
				DACKpacket= new DatagramPacket(receiveBuffer,receiveBuffer.length);
				int packetCount=0;							// Counts the number of packet sent
				int numberPadded=(40*dataPacketNo)%351;		// # of padded (zero) bits
				
				SenderFunctions.RC4Initialization(newKey);		// Calling function for S byte state vector initialization
					while(dataPacketNo>0){	
						k=0;
						int g=0;							// g:Counter for DATA packet transmission
							while(k<40){
								if(dataPacketNo>1){			// If it is not the last packet to be sent, no zero padding
									payload[k]=data[k+positionCounter];		// Copy contents from data array to payload in the DATA packet
									System.out.print("Pay Load "+(k+1)+"= "+payload[k]+"  ");
								    k++;
								}
								else{						// If last packet to be transmitted
									if(k<40-numberPadded){	// Copy contents from data array to payload in the DATA packet
										payload[k]=data[k+positionCounter];
										System.out.print("Pay Load "+(k+1)+"= "+payload[k]+"  ");
										k++;}
									else{
										payload[k]=0;	// Padding the remaining bytes in the payload with zero 
										System.out.print("Pay Load "+(k+1)+"= "+payload[k]+"  ");
										k++;}
								}
							}
						
							if(dataPacketNo==1)				// If last packet to be sent
								packetType[1]++;			// Increment packet type for the last DATA packet
							
							System.out.println("");
							
							cipher=SenderFunctions.RC4encryption(payload);	// Calling Function to encrypt the payload data with 128 bit key
							
							System.out.println("");
							
							sendDataBuffer[0]=packetType[0];
							sendDataBuffer[1]=packetType[1];						  // Putting the value of the PACKET TYPE in the DATA packet
							
							sendDataBuffer[2]=sequenceNumber[0];
							sendDataBuffer[3]=sequenceNumber[1];					  // Putting the value of the SEQUENCE NUMBER in the DATA packet
							
							sendDataBuffer[4]=(byte)LENGTH;
							
							if(dataPacketNo>1)
								sendDataBuffer[5]=(byte)cipher.length;                // Putting the length as 5th Byte in the DATA packet
							else
								sendDataBuffer[5]=(byte)(cipher.length-numberPadded); // For last packet, putting the unpadded payload length 
																					  // as the 5th byte in the DATA packet
							
							for(int d=0;d<40;d++)
								sendDataBuffer[d+6]=cipher[d];						  // Coping  Encrypted payload into the DATA packet
							
							integrityCheckValue=SenderFunctions.integrityCheckValueData(sendDataBuffer); // Calling the function to obtain the integrity check value 
																						 // To be appended as the last two Bytes of the DATA packet
							sendDataBuffer[46]=integrityCheckValue[0];	// Inserting the INTEGRITY CHECK VALUE obtained in the DATA packet(Last two bytes)
							sendDataBuffer[47]=integrityCheckValue[1];
							
							for(int d=0;d<sendDataBuffer.length;d++){
								System.out.print("Encrypted Data "+(d+1)+"= "+sendDataBuffer[d]+" ");
								// Displaying the encrypted data(that is stored in the send data buffer)
							}
							
							int f=1; // Used to double the timeout interval after each unsuccessful DATA packet transmission
							int local=0;// Used to distinguish whether the thrown exception is because of integrity check failure or time out
							while(g<ALLOWED_ATTEMPTS)
							{
							try{																// Try block begins
							System.out.print("\n\nSending DATA packet "+(packetCount+1)+"\n");
							
							socket.send(DATApacket);		// Send DATA packet	
							System.out.println("");
							socket.setSoTimeout(f*1000);	// Start and set timer(in milliseconds)
							f=f*2;							// Doubled after each transmission of the same DATA packet 
							g++;							// Increments the # of times the DATA packet is sent
							socket.receive(DACKpacket);		// Receive the DACK 
							
							if(SenderFunctions.integrityCheckReceivedData(receiveBuffer)) // Function call for "INTEGRITY CHECK" on the received DACK
							{
								if(receiveBuffer[0]==0x00 && receiveBuffer[1]==0x04 && receiveBuffer[2]==sendDataBuffer[2] && receiveBuffer[3]==sendDataBuffer[3]) // Checks the received DACK for packet type
								{
									
													System.out.println("Integrity check passed: Receiving DACK "+(packetCount+1)); 	
																		// Increments packet# on successful reception of  DACK
													System.out.println("------------------------------------------------------------------------------------\n");
												    
													sequenceNumber[0]=receiveBuffer[2];		// Putting the value of Sequence# from the received DACK
													sequenceNumber[1]=receiveBuffer[3];	    // into the sequenceNumber temporary variable 
													
													positionCounter+=k;				// Saving the state of the counter on data array containing 351 bytes to be sent
												    dataPacketNo--;		// Decrements the packet counter on successful transmission
												    					// of DATA packet and reception of DACK for the same
													packetCount++;		// Increment DATA packet count- On Successful transmission and reception of DACK 
													break;
									
								}
							}
							else
							{
								if(g<ALLOWED_ATTEMPTS)   // If Integrity check fails for the received DACK
									System.out.println("Integrity check failed for DACK- Discarding DACK"); 	
								else{
									local=1;
									throw new Exception(); // If Integrity check for the received DACK fails for the 4th time. 
								}
							}
							
							}catch (Exception e){
							
							if(g==ALLOWED_ATTEMPTS ){ 
								if(local==1)
									System.out.println("Integrity check failed for DACK- Discarding DACK");
								else
									System.out.println("Client's Socket Timeout");
								System.out.println("\nError: Communication Failure");
								System.out.println("Socket Close");
								socket.close();			//Closes error if DACK i sn't received even after sending DATA 4 times
								System.exit(0);			//exit
							}
							else
								System.out.println("Client's Socket Timeout");	//if DATA is sent less than 4 times, then display timeout
						  }
						 }// while loop ends here
					
						if(g<ALLOWED_ATTEMPTS)			
							sequenceNumber[1]++;	
					}
			}
			SenderFunctions.display(data);	
	}
	public static void keyGeneration(byte[] arrReceiveBuffer)
	{
		for(int m=0,l=0;l<16;m++,l+=2)
		{
			newKey[l]=KEYS[m];
			newKey[l+1]=arrReceiveBuffer[m+4];		// Interleaving SHARED SECRET KEY AND NONCE Bytes		  			
		}
	}
		
}		





