// Java program to illustrate Server side
// Implementation using DatagramSocket
import java.io.*;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Date;

public class Server
{
    public static void main(String[] args) throws IOException
    {
        File output_file;
        FileOutputStream fp = null;
        Double probability;
        Integer port_number;
        if(args.length == 3) {
            port_number = Integer.parseInt(args[0]);
            output_file = new File(args[1]);
            probability = Double.parseDouble(args[2]);

            if(port_number != 7735) {
                System.out.println("Port number for this project has to be 7735");
                return;
            }

            if(output_file.exists()) {
                if(!output_file.delete()) {
                    System.out.println("Could not delete old output file with same name");
                    return;
                }
            }

            if(!output_file.createNewFile()) {
                System.out.println("Can not create new output file");
                return;
            } else {
                fp = new FileOutputStream(output_file);
            }


            if(probability < 0 || probability > 1) {
                System.out.println("Probability has to be between 0 and 1 (both including)");
                return;
            }
        } else {
            System.out.println("java Server port# output-file-name probability(0 <= p <= 1)");
            return;
        }

        final long startTime = System.nanoTime();

        // Step 1 : Create a socket to listen at port 7735
        DatagramSocket serverSocket = new DatagramSocket(7735);
        byte[] receive = new byte[65535];
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        int nextExpectedSeqNumber = 0;

        DatagramPacket DpReceive = null;
        boolean isPacketLost = false;
        int lostPacketSeqNum = -1;
        int totalPacketsLost = 0;
        while (true)
        {

            // Step 2 : create a DatgramPacket to receive the data.
            DpReceive = new DatagramPacket(receive, receive.length);

            // Step 3 : revieve the data in byte buffer.
            serverSocket.receive(DpReceive);

            InetAddress clientIP = DpReceive.getAddress();
            Integer clientPort = DpReceive.getPort();

            StringBuilder receivedPacket = P2_Utils.data(receive);
            Integer receivedSequenceNumber = getReceivedSequenceNumber(receivedPacket);
            Integer receivedCheckSum = getReceivedCheckSum(receivedPacket);
            String receivedPadding = getReceivedPadding(receivedPacket);
            String receivedData = getReceivedData(receivedPacket);

            Double r = Math.random();

            if(Integer.parseInt(receivedPadding, 2) == 0) {
                out.writeTo(fp);
                System.out.println("Terminating connection as instructed by client");
                System.out.println("Total Packets lost: " + totalPacketsLost);
                serverSocket.close();
                final long duration = System.nanoTime() - startTime;
                System.out.println("Server Time: " + duration/1000000000 + " s");
                System.exit(0);
            }

            if(receivedSequenceNumber == lostPacketSeqNum) {
                isPacketLost = false;
                lostPacketSeqNum = -1;
            }

            if(r <= probability && !isPacketLost) {
                System.out.println("Packet loss, sequence number = " + receivedSequenceNumber);
                isPacketLost = true;
                lostPacketSeqNum = receivedSequenceNumber;
                totalPacketsLost++;
            } else {
                if(verifyReceivedData(receivedData, receivedCheckSum)) {
                    if(receivedPadding.equals("0101010101010101") && nextExpectedSeqNumber == receivedSequenceNumber) {
//                        saveDataToFile(receivedData, output_file);
                        sendACK(receivedSequenceNumber, clientIP, serverSocket, clientPort);
                        nextExpectedSeqNumber++;
                        out.write(receivedData.getBytes());
                    } else {
//                        System.out.println("Dropping Packet: " + receivedSequenceNumber);
                        continue;
                    }
                }
            }

            // Clear the buffer after every message.
            receive = new byte[65535];
        }
    }

    private static void saveDataToFile(String receivedData, File output_file) throws IOException {
        BufferedWriter writer = new BufferedWriter(new FileWriter(output_file, true));
        writer.write(receivedData);
        writer.close();
    }

    private static void sendACK(Integer receivedSequenceNumber, InetAddress clientIP, DatagramSocket serverSocket, Integer clientPort) throws IOException {
//        System.out.println("Sending ACK for Packet: " + receivedSequenceNumber);
        String binSqNum = Long.toBinaryString( Integer.toUnsignedLong(receivedSequenceNumber) | 0x100000000L ).substring(1);

        String ackData = binSqNum + "0000000000000000" +
                "1010101010101010";
        byte[] buf = ackData.getBytes();
        DatagramPacket ackPacket = new DatagramPacket(buf, buf.length, clientIP, clientPort);
        serverSocket.send(ackPacket);
    }

    private static boolean verifyReceivedData(String receivedData, Integer receivedCheckSum) {
        int generatedCheckSum = Integer.parseInt(String.valueOf(P2_Utils.getCheckSum(new StringBuilder(receivedData))), 2);
        generatedCheckSum = Integer.parseInt("FFFF", 16) - generatedCheckSum;

        int syn = generatedCheckSum + receivedCheckSum;
        syn = Integer.parseInt("FFFF", 16) - syn;
        return syn == 0;
    }

    private static String getReceivedData(StringBuilder receivedPacket) {
        return String.valueOf(receivedPacket.substring(64));
    }

    private static String getReceivedPadding(StringBuilder receivedPacket) {
        return String.valueOf(receivedPacket.substring(48, 64));
    }

    private static Integer getReceivedCheckSum(StringBuilder receivedPacket) {
        return Integer.parseInt(String.valueOf(receivedPacket.substring(32, 48)),2);
    }

    private static Integer getReceivedSequenceNumber(StringBuilder receivedPacket) {
        return Integer.parseInt(String.valueOf(receivedPacket.substring(0, 32)),2);
    }
}
