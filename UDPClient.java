import java.io.IOException;
import java.io.RandomAccessFile;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketTimeoutException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Base64;
import java.util.List;

public class UDPClient {
    private static final int MAX_RETRIES = 5;
    private static final int INITIAL_TIMEOUT = 1000; // 初始超时时间
    private static final int BLOCK_SIZE = 1000; // 每个数据块的大小
    private static final int MAX_PACKET_SIZE = 65536; // 最大数据包大小



    public static void main(String[] args) {
        if (args.length != 3) {
            System.out.println("Usage: java UDPClient <server_host> <server_port> <file_list>");
            return;
        }

        String serverHost = args[0];
        int serverPort = Integer.parseInt(args[1]);
        String fileList = args[2];
        try {
            // 读取文件列表
            List<String> filenames = Files.readAllLines(Paths.get(fileList));

            //Udp
            DatagramSocket socket = new DatagramSocket();
            InetAddress serverAddr = InetAddress.getByName(serverHost);

            for (String filename : filenames) {
                if (filename.trim().isEmpty()) continue;
                try {
                    //下载请求

                    String request = "下载" + filename.trim();
                    String response = sendAndReceive(socket, serverAddr, serverPort, request);

                    String[] parts = response.split(" ");
                    if (parts[0].equals("ERR")) {
                        System.out.println("File not found: " + filename);
                        continue;
                    }

                    if (!parts[0].equals("OK") || parts.length < 6) {
                        System.out.println("Invalid response: " + response);
                        continue;
                    }

                    // 解析响应
                    long fileSize = Long.parseLong(parts[3]);
                    int dataPort = Integer.parseInt(parts[5]);
                    // 下载文件
                    if (downloadFile(socket, serverAddr, dataPort, filename, fileSize)) {
                        System.out.println("Successfully downloaded " + filename);
                    } else {
                        System.out.println("Failed to download " + filename);


                    }


                }

                catch (Exception e) {
                    System.out.println("Error processing " + filename + ": " + e.getMessage());
                }

            }

            socket.close();
        }


        catch (Exception e) {
        System.err.println("Fatal error: " + e.getMessage());
        }

    }
private  static String sendAndReceive(DatagramSocket socket, InetAddress addr, int port, String message)
        throws IOException {
    int currentTimeout = INITIAL_TIMEOUT;
    int retries = 0;

    byte[] sendData = message.getBytes();
    byte[] receiveData = new byte[MAX_PACKET_SIZE];
    while (retries < MAX_RETRIES) {
        try {
            // 发送消息
            DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, addr, port);
            socket.send(sendPacket);

            //
            socket.setSoTimeout(currentTimeout);

            // 接收响应
            DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
            socket.receive(receivePacket);

            return new String(receivePacket.getData(), 0, receivePacket.getLength());
        } catch (SocketTimeoutException e) {
            retries++;
            System.out.println("Timeout (attempt " + retries + "/" + MAX_RETRIES + "), retrying...");
            currentTimeout *= 2;
        }


    }
    throw new IOException("Max retries exceeded, giving up");


    }

    private static boolean downloadFile(DatagramSocket socket, InetAddress addr, int port,
                                        String filename, long fileSize) throws IOException {

        try (RandomAccessFile file = new RandomAccessFile(filename, "rw");
             FileChannel channel = file.getChannel()) {

            System.out.print("Downloading " + filename + " (" + fileSize + " bytes): ");

            long downloaded = 0;
            while (downloaded < fileSize) {
                String request = String.format("FILE %s GET START %d END %d", filename, start, end);


                String response = sendAndReceive(socket, addr, port, request);
                String[] parts = response.split(" ");
                if (parts.length < 8 || !parts[0].equals("FILE") || !parts[2].equals("OK")) {
                    System.out.println("\nInvalid response: " + response);
                    return false;

            }
                // 提取Base64编码的数据
            int dataIndex = response.indexOf("DATA") + 5;
            String base64Data = response.substring(dataIndex).trim();

            // 解码数据
            byte[] fileData = Base64.getDecoder().decode(base64Data);


            channel.position(start);
            ByteBuffer buffer = ByteBuffer.wrap(fileData);
            channel.write(buffer);
            downloaded += fileData.length;

            // 显示进度


        }

        System.out.println("\nhave finish");

        // 发送关闭请求
        String closeRequest = "FILE " + filename + " CLOSE";
        String closeResponse = sendAndReceive(socket, addr, port, closeRequest);

        if (closeResponse.contains("CLOSE_OK")) {
            System.out.println("File " + filename + " closed successfully");
            return true;
        }

        System.out.println("Close confirmation failed: " + closeResponse);
        return false;

    }


}

}