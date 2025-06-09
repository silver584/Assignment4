import java.io.IOException;
import java.io.RandomAccessFile;
import java.net.*;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Base64;
import java.util.Random;


public class UDPServer {


    private static final int MIN_DATA_PORT = 50000;
    private static final int MAX_DATA_PORT = 51000;
    private static final int MAX_PACKET_SIZE = 65536;

    public static void main(String[] args) {
        if (args.length == 1){
            System.out.println("Usage: java UDPServer <port>");
        }
        String parameter = args[0]; // 获取第一个参数
        System.out.println("The provided parameter is: " + parameter);

        int port = Integer.parseInt(args[0]);

        try (DatagramSocket serverSocket = new DatagramSocket(port)) {
            System.out.println("Server started on port " + port);
            byte[] buffer = new byte[MAX_PACKET_SIZE];


            while (true) {
                // 接收下载请求
                DatagramPacket requestPacket = new DatagramPacket(buffer, buffer.length);
                serverSocket.receive(requestPacket);

                // 处理请求
                String request = new String(requestPacket.getData(), 0, requestPacket.getLength());
                String[] parts = request.trim().split(" ");


                if (parts.length < 2 || !parts[0].equals("DOWNLOAD")) {
                    continue;
                }

                String filename = parts[1];
                InetAddress clientAddress = requestPacket.getAddress();

                int clientPort = requestPacket.getPort();

                //检查文件
                Path filePath = Paths.get(filename);
                if (!Files.exists(filePath) )
                {
                    String response = "ERR " + filename + " NOT FOUND";
                    byte[] responseData = response.getBytes();
                    DatagramPacket responsePacket = new DatagramPacket(
                            responseData, responseData.length, clientAddress, clientPort);
                    serverSocket.send(responsePacket);
                    System.out.println("File not found: " + filename);
                    continue;
                }
                new Thread(() -> {
                    handleFileTransmission(filename, clientAddress, clientPort, serverSocket);
                }).start();
            }
        }
        catch (Exception e) {
            System.err.println("Server error: " + e.getMessage());
        }
    }


    private static void handleFileTransmission(String filename, InetAddress clientAddress,
                                           int clientPort, DatagramSocket mainSocket) {
    try {

        DatagramSocket dataSocket = createDataSocket();
        int dataPort = dataSocket.getLocalPort();

        // 获取文件大小
        long fileSize = Files.size(Paths.get(filename));

        // 发送OK响应
        String response = String.format("OK %s SIZE %d PORT %d", filename, fileSize, dataPort);
        byte[] responseData = response.getBytes();


        System.out.println("Serving " + filename + " to " + clientAddress +
                " on port " + dataPort);

        // 打开文件
        try (RandomAccessFile file = new RandomAccessFile(filename, "r");
             FileChannel channel = file.getChannel()) {

            byte[] buffer = new byte[MAX_PACKET_SIZE];

            while (true) {
                // 接收客户端请求
                DatagramPacket requestPacket = new DatagramPacket(buffer, buffer.length);
                dataSocket.receive(requestPacket);

                String request = new String(requestPacket.getData(), 0, requestPacket.getLength());
                String[] parts = request.trim().split(" ");

                // 检查关闭请求
                if (parts.length >= 3 && parts[0].equals("FILE") && parts[2].equals("CLOSE")) {
                    String closeResponse = "FILE " + filename + " CLOSE_OK";
                    byte[] closeData = closeResponse.getBytes();
                    DatagramPacket closePacket = new DatagramPacket(
                            closeData, closeData.length, clientAddress, clientPort);
                    dataSocket.send(closePacket);
                    System.out.println("Closed " + filename + " for " + clientAddress);
                    break;
                }

                // 处理数据请求
                if (parts.length < 7 || !parts[0].equals("FILE") || !parts[2].equals("GET")) {
                    continue;
                }

                // 解析字节范围
                long start = Long.parseLong(parts[4]);
                long end = Long.parseLong(parts[6]);

                // 读取文件数据
                int length = (int) (end - start + 1);
                ByteBuffer fileBuffer = ByteBuffer.allocate(length);

                channel.read(fileBuffer);
                fileBuffer.flip();
                byte[] fileData = new byte[length];
                fileBuffer.get(fileData);

                // 编码并发送数据
                String base64Data = Base64.getEncoder().encodeToString(fileData);
                String dataResponse = String.format(
                        "FILE %s OK START %d END %d DATA %s", filename, start, end, base64Data);

                byte[] responseDataBytes = dataResponse.getBytes();
                DatagramPacket dataPacket = new DatagramPacket(
                        responseDataBytes, responseDataBytes.length, clientAddress, clientPort);
                dataSocket.send(dataPacket);
            }
        }
    } catch (Exception e) {
        System.err.println("Error handling " + filename + ": " + e.getMessage());
    }
}




    private static DatagramSocket createDataSocket() throws SocketException {
        Random random = new Random();
        int attempts = 0;

        while (attempts < 100) {
            try {
                int port = MIN_DATA_PORT + random.nextInt(MAX_DATA_PORT - MIN_DATA_PORT + 1);
                return new DatagramSocket(port);
            } catch (BindException e) {
                attempts++;
            }
        }

        throw new SocketException("Could not find available port in range");
    }



}
