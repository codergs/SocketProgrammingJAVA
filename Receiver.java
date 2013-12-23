package udpReceive;
  
import java.io.IOException;
import java.net.*;
  
public class Receiver {
  
    // ------------- fields ------------- //
    // Static fields
      
        private  static final int SERVER_PORT=1300;                 // Initializing server port number, same as the one the server is listening on
        private static final byte[] KEYS={12,13,14,15,16,17,18,19}; // The shared 64-bit secret key 
        private static final int ALLOWED_ATTEMPTS=4;                // No of allowed transmission trials 
        private static byte[] newKey=new byte[16];                  // 16 Bytes new Key after interleaving nonce bytes and shared secret key
          
    
        public static void main(String[] args) throws IOException {
              
        DatagramSocket listener=new DatagramSocket(SERVER_PORT);    // Listens for incoming connections.
        DatagramPacket INITpacket,IACKpacket,DATApacket,DACKpacket; // Creating Datagram object
  
        int packetCounter=ReceiverFunctions.getPacketCount();       // Counts the # of DATA packet
        int initPacketCount=0,dataPacketCount=0,temp=0,length=0;    // Initialization count for # of tries to send INIT, DATA
                                                                            //Temporary variable and length
        byte[] sequenceCheck=new byte[1];                           // Holds the value of the sequence # received in the INIT packet
        byte[] sequenceNumber=new byte[2];                          // Holds the sequence # to be put in the packet to be sent
          
        byte[] packetType=new byte[2];                              // Stores the packet type (2 Bytes)
          
        byte[] receiveBuffer= new  byte[48];                        // Byte array to hold the INIT packet received 
        byte[] sendBuffer= new  byte[14];                           // Byte array to hold the IACK packet to be sent
        byte[] receiveDataBuffer=new byte[48];                      // Byte array to hold the DATA packet received 
        byte[] sendDataBuffer=new byte[6];                          // Byte array to hold the DACK packet to be sent
              
        InetAddress clientAddress= null;                            // Creating object for client's IP address
        int portAddress=SERVER_PORT;                                // Initialized server port number assigned to the port address
              
        System.out.println("Listening on port: "+SERVER_PORT);      // Waiting for the INIT packet 
          
        while(initPacketCount!=ALLOWED_ATTEMPTS){                  // For less than 4 tries
                  
                INITpacket = new DatagramPacket(receiveBuffer,receiveBuffer.length);
                                                                    // Creating INIT packet to be received
                listener.receive(INITpacket);                       // Receiving INIT packet
                clientAddress=INITpacket.getAddress();              // Obtaining client's IP address
                portAddress=INITpacket.getPort();                   // Obtaining Client's port address
                  
                // Checks for the correct PACKET TYPE for the received INIT packet
                if(receiveBuffer[1]==0x00){							
                    if(ReceiverFunctions.integrityCheckReceived(receiveBuffer)){	
                    		// Checking the integrity check value of INIT packet received
                        System.out.println("Getting request from the host: "+clientAddress);
                        System.out.println("Integrity Check Passed for INIT packet: Receiving packet");
                          
                        sendBuffer=ReceiverFunctions.iackPacketBuffer(receiveBuffer);
                        sequenceNumber[0]=sendBuffer[2];	// Stores the sequence number from the received INIT
                        sequenceNumber[1]=sendBuffer[3];
                        packetType[0]=sendBuffer[0];		// Packet Type is set
                        packetType[1]=sendBuffer[1];
                        IACKpacket = new DatagramPacket(sendBuffer,sendBuffer.length, clientAddress, portAddress);
                        listener.send(IACKpacket);			// Transmission of IACK packet
                        System.out.println("IACK Sent");
                        keyGeneration(sendBuffer);
                    }
                    else{
                            System.out.println("Getting request from the host: "+clientAddress);
                            System.out.println("Integrity Check fails: Packet Discarded");
                            if(initPacketCount!=3)
                                System.out.println("Please Resend the INIT packet\n");
                        }
                  }
                  
                if(receiveBuffer[1]==0x02){                        // If its a DATA packet
                    System.out.println("\nTwo-way handshake completed between Client & Server");
                                                                // Handshake is complete when DATA is received after the IACK is successfully received at transmitter end
                    System.out.println("\n------------------------------------------------------------------------------------\n");
                    System.out.println("Receiving Packet "+(dataPacketCount+1));
                    for(int k=0;k<receiveBuffer.length;k++)
                        receiveDataBuffer[k]=receiveBuffer[k];  // Store the DATA received in the receiverDataBuffer
                    break;
                }
                initPacketCount++;
             }
            if(initPacketCount<ALLOWED_ATTEMPTS){
                // Creating DATA packet
            	DATApacket = new DatagramPacket(receiveDataBuffer,receiveDataBuffer.length);
                packetType[1]++;
                sequenceCheck[0]=sequenceNumber[1];
              
                int dackCount=0,transmissionType=1;				// Transmission type tells if the packet received is a new DATA Packet or a previous DATA packet  
                length+=receiveDataBuffer[5];   
              
                ReceiverFunctions.RC4Initialization(newKey);	// RC4 initialization to decrypt the received DATA packet
              
                while(dataPacketCount<packetCounter){
                  
                  
                    if(dataPacketCount==(packetCounter-1))
                        packetType[1]++;
                  
                    if(temp>0){  						
                        listener.receive(DATApacket);			// Receiving DATA packet
                        if(receiveDataBuffer[3]==sequenceCheck[0]){    
                            if(transmissionType==1)
                                dataPacketCount--;				// Decrement the number of packets expected
                                System.out.println("Receiving Packet "+(dataPacketCount+1)+" Again");
                        }
                    else{
                            dackCount=0;
                            transmissionType=1;
                            
                            sequenceNumber[1]++;
                            sequenceCheck[0]=sequenceNumber[1];
                            
                            length+=receiveDataBuffer[5];   	// Counts the number of DATA bytes received
                        }
                }
                if(ReceiverFunctions.integrityCheckData(receiveDataBuffer)){	//Checks integrity check value for the received DATA packet
                    if(ReceiverFunctions.dataPacketCheck(sequenceNumber,packetType,dataPacketCount,receiveDataBuffer)){
                                sendDataBuffer=ReceiverFunctions.getSendDataBuffer();
                                
                                DACKpacket = new DatagramPacket(sendDataBuffer,sendDataBuffer.length, clientAddress,portAddress);
                                listener.send(DACKpacket);		// Send DACK packet
                                dataPacketCount++;
                            }
                }
                else{
                    System.out.println("Integrity check failed\n");		// Print error message if integrity check fails
                    transmissionType=0;
                    if (dackCount==ALLOWED_ATTEMPTS){
                        System.out.println("\nCommunication error in the Network");
                        listener.close(); // Receiver Socket Close
                        System.out.println("Server socket closed");
                        System.exit(0);
                    }
                  }
                  
                dackCount++;
                temp++;
                if (dackCount==ALLOWED_ATTEMPTS){   
                    System.out.println("Communication error in the Network");	// Print error message if integrity check fails
                    listener.close();	// Receiver Socket Close
                    System.out.println("Server socket closed");
                    System.exit(0);
                }
             }// while loop ends here
                          
            if(dataPacketCount==packetCounter)
                ReceiverFunctions.display(length);
          }
        else{
                System.out.println("Communication Error- Client is not Responding"); // Print error message if allowed attempts are exhausted 
                listener.close();
                System.out.println("\nServer socket closed");
                System.exit(0);
            }
        }// Main ends here 
          
        // 128 bit key generation 
        public static void keyGeneration(byte[] arrSenderBuffer){
            for(int m=0,l=0;l<16;m++,l+=2){
                newKey[l]=KEYS[m];
                newKey[l+1]=arrSenderBuffer[m+4];       // Interleaving SHARED SECRET KEY AND NONCE Bytes                   
            }
        }
}