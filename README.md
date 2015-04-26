###SocketProgramming (Java), Networks and Protocols I
####University of Maryland Sep 2013-­Dec 2013

A distributed networking application in Java consisting of a transmitter and a receiver that can
ensure reliable data transfer. The application uses Java’s UDP sockets (classes DatagramPacket and
DatagramSocket and their methods) and provide the necessary reliable data transfer functionality on the
top of UDP’s unreliable communication services by implementing the data transfer protocol. The data transfer is one-directional with data bytes flowing from the transmitter to the
receiver. To prevent unauthorized access to the data, the payload sent to the receiver is encrypted by
the RC4 cipher. We will assume that the transmitter and the receiver have a shared secret (a 64-bit key),
which will be used for encryption and decryption and will also automatically authenticate the
communicating parties.

UDPSender.java and SenderFunction.java are at the client side, and UDPReceiver.java and ReceiverFucnction.java is at serer side. You can test the script using to local instance if Eclipse running on localhost.
