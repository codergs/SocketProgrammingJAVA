package udpSend.Receive;

import java.util.Random;
public class SenderFunctions {
	
		
	private static final byte[] S=new byte[256];				// State vector
	private static final byte[] T=new byte[256];				// Temporary vector
	private static byte[] SendBuffer=new byte[6];			    // Byte buffer holding the INIT packet to be sent
	private static byte[] integrityCheckValue=new byte[2];		// Byte array to hold the value of Integrity check field
	private static byte[] variable=new byte[2];
	//Setting up INIT packet
	public static byte[] initPacketBuffer(){	
		Random randomObj=new Random();							// Object of random class
		int a1=randomObj.nextInt(100);							// Generates random number from 0-99, (for positive values)
		int a2=randomObj.nextInt(100);							// For setting an INITIAL SEQUENCE NUMBER
		
		SendBuffer[0]=0x00;	
		SendBuffer[1]=0x00;										// Putting the value of "PACKET TYPE" in the INIT packet to be sent
		
		SendBuffer[2]=(byte)a1;			 
		SendBuffer[3]=(byte)a2;									// Setting the "INITIAL SEQUENCE NUMBER"
	
		integrityCheckValue=integrityCheckValue(SendBuffer); 	// Calling function to receive integrity check value for INIT packet
		SendBuffer[4]=integrityCheckValue[0];
		SendBuffer[5]=integrityCheckValue[1];					// Appending received integrity check value at the end of INIT packet
		
		return SendBuffer;
	}
	
    //Integrity check value for INIT packet
	public static byte[] integrityCheckValue(byte[] arrSendBuffer){
	
		variable[0]=0x00;
		variable[1]=0x00;			// Initializing variable to 0
		
		for(int i=0;i<2;i++){
			variable[i]=(byte) (variable[i]^ arrSendBuffer[i]);	//Bit-wise X-OR of SendBuffer with the 16-bits "variable"
			variable[i]=(byte) (variable[i]^ arrSendBuffer[i+2]);
		}																//EXCLUDING the Integrity check value field in the received packet
		
		return variable;	//returns the INTEGRITY CHECK VALUE to be appended in the packet to be sent
	}
		
	//Integrity check function for the received IACK
	public static boolean integrityCheckReceived(byte[] arrReceiveBuffer){
		
		variable[0]=0x00;
		variable[1]=0x00;	// Initializing variable to 0
		
		for(int i=0;i<2;i++){
			variable[i]=(byte)(variable[i] ^ arrReceiveBuffer[i]);		// Bit-wise X-OR of ReceivedBuffer with the 16-bits "variable", 	
			for(int j=2;j<13;j+=2)											// including the Integrity check value field in the received packet
				variable[i]^=arrReceiveBuffer[i+j];
		}
		
		if(variable[0]==0x00 && variable[1]==0x00)
			return true;			// Return true if integrity check value=0 (INTEGRITY CHECK=PASS)
		else 
			return false;			// Return False if integrity check value!=0 (INTEGRITY CHECK=FAIL)
	}
	
	// S byte state vector initialization
	public static void RC4Initialization(byte[] arKeys){
			
		// Initialization of the S State and T vector
		for(int s=0;s<256;s++){ 
			S[s]=(byte)s;								// Byte is signed type!
			T[s] = arKeys[s % arKeys.length];
		}		
		
		int j = 0; 
		byte temp;										// Temporary variable used for swapping
		if (arKeys.length >0 && arKeys.length < 257){	// Checking the length of the key
	    
			for (int i = 0; i < 256; i++){
				j = (j +positive(S[i]) + positive(T[i]))% 256; 
				temp=S[i];								// Swapping S[i] with another byte in S according to a scheme 
				S[i]=S[j];									//dictated by the current configuration of S
				S[j]=temp;
			}   
		}
		else{
		          System.out.println("Invalid Length for key"); // Print error message and throw exception as below
		          throw new IllegalArgumentException("Key length should be greater than 0 and less than 256 bytes");
		} 
			  
	}
	
	//Function to encrypt payload data with 128 bit key	
	public static byte[] RC4encryption(byte[] arPlainTexts) {
		 
		byte[] cipherTexts = new byte[arPlainTexts.length]; 	// Creating an array of bytes for Plain Text
		
		int i = 0, j = 0, k, t;
		byte temp;
	
		for (int z = 0; z < arPlainTexts.length; z++){
			i = (i + 1) % 256;
			j = (j + positive(S[i])) % 256;
			temp=S[i];
			S[i]=S[j];
			S[j]=temp;
			t = (positive(S[i]) + positive(S[j])) % 256;	   // Taking modulus i.e obtaining positive values
			k = S[t];
			cipherTexts[z] = (byte) (arPlainTexts[z] ^ k);	   // Plain text XOR k= Cipher text
			System.out.print("CipherText "+(z+1)+"= "+cipherTexts[z]+" ");	 // Printing the Encrypted payload
		}	
		return cipherTexts;
	}
	
	//Function to obtain positive values	
	public static int positive(byte temp){
			
		if(temp>=0)
			return temp;
		else 
			return 256+temp;	// Returns a positive value that results from the addition if the number is negative
	}
		
    //Integrity check value for DATA packet
	public static byte[] integrityCheckValueData(byte[] arSendDataBuffer){
			
		variable[0]=0x00;
		variable[1]=0x00;												// Initializing variable to 0
		
		for(int i=0;i<2;i++){
			variable[i]=(byte) (variable[i]^arSendDataBuffer[i]) ;		// Bit-wise X-OR of DATA to be sent with the 16-bits "variable" 
			variable[i]=(byte) (variable[i]^ arSendDataBuffer[i+2]);
			variable[i]^=arSendDataBuffer[i+4];								// Doesn't include the INTEGRITY CHECK VALUE FIELD
			for(int j=0;j<40;j+=2)
				variable[i]=(byte) (variable[i]^arSendDataBuffer[i+j+6]);
		}
		
		return variable;		// Returns the INTEGRITY CHECK VALUE to be appended in the DATA packet to be sent
	}

	//Integrity check function for the received DACK   
	public static boolean integrityCheckReceivedData(byte[] a){

		variable[0]=0x00;
		variable[1]=0x00;								// Initializing variable to 0
		
		for(int i=0;i<2;i++){
			variable[i]=(byte) (variable[i]^a[i]); 		// Bit-wise X-OR of DATA to be sent with the 16-bits "variable" 
			variable[i]=(byte) (variable[i]^ a[i+2]);
			variable[i]=(byte) (variable[i] ^ a[i+4]);		// includes the INTEGRITY CHECK VALUE FIELD
		}			
		
		if(variable[0]==0x00 && variable[1]==0x00)
			return true;			// Return true if integrity check value=0 (INTEGRITY CHECK=PASS)
		else
			return false;			// Return False if integrity check value!=0 (INTEGRITY CHECK=FAIL)
	}
	
	//Display function for the transmitted bytes
	public static void display(byte[] arrData){
		System.out.println("\nTransmitted Data:");
		for(int len=0;len<arrData.length;len++){
			System.out.println("Byte["+(len+1)+"] ="+arrData[len]);		// Display the transmitted data Bytes
		}
		System.out.println("____________PROGRAM END_______________");
	}
}
