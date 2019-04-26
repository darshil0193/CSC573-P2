// Java program to illustrate Client side
// Implementation using DatagramSocket
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.*;



public class Client
{
    public static void main(String args[]) throws IOException
    {
        String server_host_name;
        Integer port_number;
        File input_file;
        Integer window_size_n;
        Integer MSS;

        if(args.length == 5) {
            server_host_name = args[0];
            port_number = Integer.parseInt(args[1]);
            input_file = new File(args[2]);
            window_size_n = Integer.parseInt(args[3]);
            MSS = Integer.parseInt(args[4]);

            if(port_number != 7735) {
                System.out.println("Port number for this project has to be 7735");
                return;
            }

            if(!input_file.exists()) {
                System.out.println("Input file not found");
                return;
            }
        } else {
            System.out.println("java Client server-host-name server-port# input-file-name window-size MSS");
            return;
        }

        FileInputStream fin = null;
        fin = new FileInputStream(input_file);

        byte fileContent[] = new byte[(int)input_file.length()];
        fin.read(fileContent);

        DatagramSocket clientSocket = new DatagramSocket();

        int numberOfPackets = (int)Math.ceil((double)fileContent.length / MSS);

        Packet[] allPackets = generatePackets(fileContent, numberOfPackets, MSS);
        addHeaders(allPackets, false);

        Integer numberOfPacketsToSend = 0;
        int waitingForACKFrom = 0;
        if(window_size_n <= numberOfPackets) {
            numberOfPacketsToSend = window_size_n;
        } else {
            numberOfPacketsToSend = numberOfPackets;
        }

        Packet[] packetsToSend = new Packet[numberOfPacketsToSend];

        if (numberOfPacketsToSend >= 0) System.arraycopy(allPackets, 0, packetsToSend, 0, numberOfPacketsToSend);

        sendPackets(packetsToSend, 0, numberOfPackets, clientSocket, server_host_name);

        receiveACK(clientSocket, allPackets, window_size_n, numberOfPackets, server_host_name, waitingForACKFrom);
    }

    private static void receiveACK(DatagramSocket clientSocket, Packet[] allPackets, Integer window_size_n, int numberOfPackets, String server_host_name, int waitingForACKFrom) throws IOException {
        try {
            byte[] receive = new byte[65535];

            while(true) {
                DatagramPacket ackReceive = new DatagramPacket(receive, receive.length);
                clientSocket.setSoTimeout(1000);
                clientSocket.receive(ackReceive);

                StringBuilder receivedACK = P2_Utils.data(receive);
                int receivedACKSeqNum = Integer.parseInt(String.valueOf(receivedACK.substring(0, 32)),2);

                if(waitingForACKFrom == receivedACKSeqNum) {
                    if(receivedACKSeqNum == numberOfPackets - 1) {
                        System.out.println("Total number of packets sent (without retransmissions): " + numberOfPackets);
                        sendEndConnectionPacket(numberOfPackets, clientSocket, server_host_name);
                        break;
                    }
                    waitingForACKFrom++;
                }
            }
        } catch (SocketTimeoutException e) {
            System.out.println("Timeout, sequence number = " + waitingForACKFrom);
//            System.out.println("Timeout for Packet: " + waitingForACKFrom);
//            System.out.println("numberOfPackets: " + numberOfPackets);
            Integer lastPacketSeqNumToSend = 0;
            Integer numberOfPacketsToSend = 0;
            if(waitingForACKFrom + window_size_n <= numberOfPackets) {
                lastPacketSeqNumToSend = waitingForACKFrom + window_size_n;
                numberOfPacketsToSend = window_size_n;
            } else {
                lastPacketSeqNumToSend = numberOfPackets;
                numberOfPacketsToSend = numberOfPackets - waitingForACKFrom;
            }

            Packet[] packetsToSend = new Packet[numberOfPacketsToSend];

            for(int i=waitingForACKFrom; i<lastPacketSeqNumToSend; ++i) {
                packetsToSend[i-waitingForACKFrom] = allPackets[i];
            }
            sendPackets(packetsToSend, waitingForACKFrom, numberOfPackets, clientSocket, server_host_name);
            receiveACK(clientSocket, allPackets, window_size_n, numberOfPackets, server_host_name, waitingForACKFrom);
        }
    }

    private static void sendEndConnectionPacket(int sequenceNumberOfEnd, DatagramSocket clientSocket, String server_host_name) throws IOException {
        Packet[] endConnectionPacket = new Packet[1];
        endConnectionPacket[0] = new Packet(new StringBuilder(), false, sequenceNumberOfEnd, 0);
//        addHeaders(endConnectionPacket, true);
//        sendPackets(endConnectionPacket, sequenceNumberOfEnd, sequenceNumberOfEnd, clientSocket, server_host_name);
    }

    private static void sendPackets(Packet[] packetsToSend, Integer startSequenceNumber, Integer maximumSequenceNumber, DatagramSocket clientSocket, String server_host_name) throws IOException {
//        System.out.println("Sending packets from " + startSequenceNumber + " to " + (startSequenceNumber + packetsToSend.length - 1));
        for(int i=0; i<packetsToSend.length; ++i) {
            InetAddress serverIp = null;
            serverIp = InetAddress.getByName(server_host_name);

            StringBuilder dataToSend = new StringBuilder();
            dataToSend.append(packetsToSend[i].getHeader());
            dataToSend.append(packetsToSend[i].getData());

//            System.out.println("Sending Packet: " + Integer.parseInt(packetsToSend[i].getHeader().substring(0,32), 2));

            byte[] buf = dataToSend.toString().getBytes();

            DatagramPacket DpSend = new DatagramPacket(buf, buf.length, serverIp, 7735);
            clientSocket.send(DpSend);
        }
    }

    private static void addHeaders(Packet[] allPackets, boolean isTerminationPacket) {
        if(isTerminationPacket && allPackets.length == 1) {
//            StringBuilder binSeqNumber = new StringBuilder(Long.toBinaryString( Integer.toUnsignedLong(allPackets[0].getSequenceNumber()) | 0x100000000L ).substring(1));
//            StringBuilder binCheckSum = new StringBuilder("0000000000000000");
//            StringBuilder padding = new StringBuilder("0000000000000000");
        } else {
            for(int i=0; i<allPackets.length; ++i) {
                if(i == allPackets.length - 1) {
                    allPackets[i].setHeader(new StringBuilder("0000000000000000000000000000000000000000000000000000000000000000"));
                } else {
                    StringBuilder binSeqNumber = new StringBuilder(Long.toBinaryString( Integer.toUnsignedLong(allPackets[i].getSequenceNumber()) | 0x100000000L ).substring(1));
                    StringBuilder binCheckSum = P2_Utils.getCheckSum(allPackets[i].getData());
                    StringBuilder padding = new StringBuilder("0101010101010101");

                    StringBuilder header = new StringBuilder();
                    header.append(binSeqNumber);
                    header.append(binCheckSum);
                    header.append(padding);

                    allPackets[i].setHeader(header);
                }
            }
        }
    }

    private static Packet[] generatePackets(byte[] fileContent, int numberOfPackets, Integer MSS) {
        StringBuilder packetData = new StringBuilder();
        int packetsCreated = 0;
        Packet[] allPackets = new Packet[numberOfPackets + 1];
        for(int i=0; i<fileContent.length; ++i) {
            packetData.append((char)fileContent[i]);
            if(packetData.length() == MSS) {
                Packet p = new Packet(packetData, packetsCreated == 0, packetsCreated, -1);
                allPackets[packetsCreated] = p;
                packetData = new StringBuilder();
                packetsCreated++;
            }
        }

        Packet p = new Packet(packetData, packetsCreated == 0, packetsCreated, -1);
        allPackets[packetsCreated] = p;
        packetData = new StringBuilder();
        packetsCreated++;

        p = new Packet(packetData, packetsCreated == 0, packetsCreated, -1);
        allPackets[packetsCreated] = p;
        packetData = new StringBuilder();
        packetsCreated++;

        if(packetsCreated != numberOfPackets + 1) {
            System.out.println("Error in Packet Creation");
            System.exit(0);
        }

        return allPackets;
    }



}