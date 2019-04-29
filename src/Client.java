// Java program to illustrate Client side
// Implementation using DatagramSocket
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.*;
import java.util.Arrays;


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

        final long startTime = System.nanoTime();

        FileInputStream fin = null;
        fin = new FileInputStream(input_file);

        byte fileContent[] = new byte[(int)input_file.length()];
        fin.read(fileContent);

        DatagramSocket clientSocket = new DatagramSocket();

        int numberOfPackets = (int)Math.ceil((double)fileContent.length / MSS);

        Packet[] allPackets = generatePackets(fileContent, numberOfPackets, MSS);
        addHeaders(allPackets, false);

        int startSeqNum = 0;

//        while(startSeqNum < numberOfPackets) {
        int lastSeqNum = sendPackets(clientSocket, allPackets, startSeqNum, window_size_n, server_host_name, false);
//        System.out.println("Sent Packets from " + startSeqNum + " to " + lastSeqNum);
        receiveACK(clientSocket, startSeqNum, lastSeqNum, server_host_name, startTime, allPackets, window_size_n);
//        }

//        Integer numberOfPacketsToSend = 0;
//        int waitingForACKFrom = 0;
//        if(window_size_n <= numberOfPackets) {
//            numberOfPacketsToSend = window_size_n;
//        } else {
//            numberOfPacketsToSend = numberOfPackets;
//        }
//
//        Packet[] packetsToSend = new Packet[numberOfPacketsToSend];
//
//        if (numberOfPacketsToSend >= 0) System.arraycopy(allPackets, 0, packetsToSend, 0, numberOfPacketsToSend);
//
//        sendPackets(packetsToSend, 0, numberOfPacketsToSend, clientSocket, server_host_name);
//
//        receiveACK(clientSocket, allPackets, window_size_n, numberOfPackets, server_host_name, waitingForACKFrom, startTime, numberOfPacketsToSend - 1);
    }

    private static int sendPackets(DatagramSocket clientSocket, Packet[] allPackets, int startSeqNum, Integer window_size_n, String server_host_name, boolean isEndConnection) throws IOException {
        int totalPackets = allPackets.length;
        int numberOfPacketsToSend = 0;
        int lastPacketSeqNum = 0;

        if(startSeqNum + window_size_n <= totalPackets) {
            numberOfPacketsToSend = window_size_n;
        } else {
            numberOfPacketsToSend = totalPackets - startSeqNum;
        }

        lastPacketSeqNum = startSeqNum + numberOfPacketsToSend - 1;

        Packet[] packetsToSend;
        if(!isEndConnection) {
            packetsToSend = Arrays.copyOfRange(allPackets, startSeqNum, lastPacketSeqNum + 1);
        } else {
            Packet[] endConnectionPacket = new Packet[1];
            endConnectionPacket[0] = new Packet(new StringBuilder(), false, startSeqNum, 0);
            addHeaders(endConnectionPacket, true);
            packetsToSend = endConnectionPacket;
        }

        for(int i=0; i<packetsToSend.length; ++i) {
            InetAddress serverIp = null;
            serverIp = InetAddress.getByName(server_host_name);

            StringBuilder dataToSend = new StringBuilder();
            dataToSend.append(packetsToSend[i].getHeader());
            dataToSend.append(packetsToSend[i].getData());

            byte[] buf = dataToSend.toString().getBytes();

            DatagramPacket DpSend = new DatagramPacket(buf, buf.length, serverIp, 7735);
            clientSocket.send(DpSend);
        }

        return lastPacketSeqNum;
    }

    private static void receiveACK(DatagramSocket clientSocket, int startSeqNum, int lastSeqNum, String server_host_name, long startTime, Packet[] allPackets, Integer window_size_n) throws IOException {
        byte[] receive = new byte[65535];
        int waitingForACK = startSeqNum;
        try {
            while (true) {
                DatagramPacket ackReceive = new DatagramPacket(receive, receive.length);
                clientSocket.setSoTimeout(1000);
                clientSocket.receive(ackReceive);

                StringBuilder receivedACK = P2_Utils.data(receive);
                int receivedACKSeqNum = Integer.parseInt(String.valueOf(receivedACK.substring(0, 32)), 2);
                waitingForACK++;

                if(receivedACKSeqNum == lastSeqNum) {
                    break;
                }

            }

            if(waitingForACK < allPackets.length) {
                int lastSeqNum_updated = sendPackets(clientSocket, allPackets, waitingForACK, window_size_n, server_host_name, false);
                receiveACK(clientSocket, waitingForACK, lastSeqNum_updated, server_host_name, startTime, allPackets, window_size_n);
            } else {
                System.out.println("Total number of packets sent (without retransmissions): " + allPackets.length);
                final long duration = System.nanoTime() - startTime;
                System.out.println("Client Time: " + duration/1000000000 + " s");
                sendPackets(clientSocket, allPackets, waitingForACK, window_size_n, server_host_name, true);
            }
        } catch (SocketTimeoutException e) {
            System.out.println("Timeout, sequence number = " + waitingForACK);
            int lastSeqNum_updated = sendPackets(clientSocket, allPackets, waitingForACK, window_size_n, server_host_name, false);
            receiveACK(clientSocket, waitingForACK, lastSeqNum_updated, server_host_name, startTime, allPackets, window_size_n);
        }
    }

//    private static void receiveACK(DatagramSocket clientSocket, Packet[] allPackets, Integer window_size_n, int numberOfPackets, String server_host_name, int waitingForACKFrom, long startTime, Integer lastPacketToACK) throws IOException {
//        try {
//            System.out.println("waitingForACKFrom: " + waitingForACKFrom);
//            System.out.println("lastPacketToACK: " + lastPacketToACK);
//            byte[] receive = new byte[65535];
//
//            while(true) {
//                DatagramPacket ackReceive = new DatagramPacket(receive, receive.length);
//                clientSocket.setSoTimeout(1000);
//                clientSocket.receive(ackReceive);
//
//                StringBuilder receivedACK = P2_Utils.data(receive);
//                int receivedACKSeqNum = Integer.parseInt(String.valueOf(receivedACK.substring(0, 32)),2);
//
//                if(receivedACKSeqNum == lastPacketToACK - 1) {
//                    break;
//                }
//
//                if(waitingForACKFrom == receivedACKSeqNum) {
//                    if(receivedACKSeqNum == numberOfPackets - 1) {
//                        System.out.println("Total number of packets sent (without retransmissions): " + numberOfPackets);
//                        sendEndConnectionPacket(numberOfPackets, clientSocket, server_host_name);
//                        final long duration = System.nanoTime() - startTime;
//                        System.out.println("Client Time: " + duration/1000000000 + " s");
//                        break;
//                    }
//                    waitingForACKFrom++;
//                }
//            }
//        } catch (SocketTimeoutException e) {
//            System.out.println("Timeout, sequence number = " + waitingForACKFrom);
//            Integer lastPacketSeqNumToSend = 0;
//            Integer numberOfPacketsToSend = 0;
//            if(waitingForACKFrom + window_size_n <= numberOfPackets) {
//                lastPacketSeqNumToSend = waitingForACKFrom + window_size_n;
//                numberOfPacketsToSend = window_size_n;
//            } else {
//                lastPacketSeqNumToSend = numberOfPackets;
//                numberOfPacketsToSend = numberOfPackets - waitingForACKFrom;
//            }
//
//            Packet[] packetsToSend = new Packet[numberOfPacketsToSend];
//
//            for(int i=waitingForACKFrom; i<lastPacketSeqNumToSend; ++i) {
//                packetsToSend[i-waitingForACKFrom] = allPackets[i];
//            }
//            sendPackets(packetsToSend, waitingForACKFrom, numberOfPackets, clientSocket, server_host_name);
//            receiveACK(clientSocket, allPackets, window_size_n, numberOfPackets, server_host_name, waitingForACKFrom, startTime, lastPacketSeqNumToSend);
//        }
//    }

    private static void sendEndConnectionPacket(DatagramSocket clientSocket, String server_host_name, int sequenceNumberOfEnd, Packet[] allPackets, int window_size_n) throws IOException {
//        Packet[] endConnectionPacket = new Packet[1];
//        endConnectionPacket[0] = new Packet(new StringBuilder(), false, sequenceNumberOfEnd, 0);
//        addHeaders(endConnectionPacket, true);

    }

//    private static void sendPackets(Packet[] packetsToSend, Integer startSequenceNumber, Integer maximumSequenceNumber, DatagramSocket clientSocket, String server_host_name) throws IOException {
//        System.out.println("sending packets: " + packetsToSend.length);
////        System.out.println("Sending packets from " + startSequenceNumber + " to " + (startSequenceNumber + packetsToSend.length - 1));
//        for(int i=0; i<packetsToSend.length; ++i) {
//            InetAddress serverIp = null;
//            serverIp = InetAddress.getByName(server_host_name);
//
//            StringBuilder dataToSend = new StringBuilder();
//            dataToSend.append(packetsToSend[i].getHeader());
//            dataToSend.append(packetsToSend[i].getData());
//
////            System.out.println("Sending Packet: " + Integer.parseInt(packetsToSend[i].getHeader().substring(0,32), 2));
//
//            byte[] buf = dataToSend.toString().getBytes();
//
//            DatagramPacket DpSend = new DatagramPacket(buf, buf.length, serverIp, 7735);
//            clientSocket.send(DpSend);
//        }
//    }

    private static void addHeaders(Packet[] allPackets, boolean isTerminationPacket) {
        StringBuilder binSeqNumber;
        StringBuilder binCheckSum;
        StringBuilder padding;
        if(isTerminationPacket && allPackets.length == 1) {
            binSeqNumber = new StringBuilder(Long.toBinaryString( Integer.toUnsignedLong(allPackets[0].getSequenceNumber()) | 0x100000000L ).substring(1));
            binCheckSum = new StringBuilder("0000000000000000");
            padding = new StringBuilder("0000000000000000");

            StringBuilder header = new StringBuilder();
            header.append(binSeqNumber);
            header.append(binCheckSum);
            header.append(padding);

            allPackets[0].setHeader(header);
        } else {
            for(int i=0; i<allPackets.length; ++i) {
                binSeqNumber = new StringBuilder(Long.toBinaryString( Integer.toUnsignedLong(allPackets[i].getSequenceNumber()) | 0x100000000L ).substring(1));
                binCheckSum = P2_Utils.getCheckSum(allPackets[i].getData());
                padding = new StringBuilder("0101010101010101");

                StringBuilder header = new StringBuilder();
                header.append(binSeqNumber);
                header.append(binCheckSum);
                header.append(padding);

                allPackets[i].setHeader(header);
            }
        }
    }

    private static Packet[] generatePackets(byte[] fileContent, int numberOfPackets, Integer MSS) {
        StringBuilder packetData = new StringBuilder();
        int packetsCreated = 0;
        Packet[] allPackets = new Packet[numberOfPackets];
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

        if(packetsCreated != numberOfPackets) {
            System.out.println("Error in Packet Creation");
            System.exit(0);
        }

        return allPackets;
    }



}