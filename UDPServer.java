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


        }
    }
    private static void handleFileTransmission(String filename, InetAddress clientAddress,
                                               int clientPort, DatagramSocket mainSocket) {
        try (DatagramSocket dataSocket = createDataSocket();
             RandomAccessFile file = new RandomAccessFile(filename, "r");
             FileChannel channel = file.getChannel()) {


            int dataPort = dataSocket.getLocalPort();
            long fileSize = Files.size(Paths.get(filename));

            // 发送初始响应
            sendResponse(mainSocket, String.format("OK %s SIZE %d PORT %d",
                    filename, fileSize, dataPort), clientAddress, clientPort);

            System.out.printf("Serving %s to %s on port %d%n",
                    filename, clientAddress, dataPort);

            // 处理客户端请求
            byte[] buffer = new byte[MAX_PACKET_SIZE];
            while (processClientRequest(dataSocket, channel, filename, clientAddress, clientPort, buffer)) {

            }
        } catch (Exception e) {
            System.err.println("Error handling " + filename + ": " + e.getMessage());
        }
    }


    private static void sendResponse(DatagramSocket socket, String message,
                                     InetAddress address, int port) throws IOException {
        byte[] data = message.getBytes();
        socket.send(new DatagramPacket(data, data.length, address, port));
    }


    private static boolean processClientRequest(DatagramSocket dataSocket, FileChannel channel,
                                                String filename, InetAddress clientAddress,
                                                int clientPort, byte[] buffer) throws IOException {
        DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
        dataSocket.receive(packet);

        String request = new String(packet.getData(), 0, packet.getLength()).trim();
        String[] parts = request.split(" ");

        // 处理关闭请求
        if (parts.length >= 3 && "FILE".equals(parts[0]) && "CLOSE".equals(parts[2])) {
            sendResponse(dataSocket, "FILE " + filename + " CLOSE_OK", clientAddress, clientPort);
            System.out.println("Closed " + filename + " for " + clientAddress);
            return false;  // 终止循环
        }

        // 处理数据请求
        if (parts.length >= 7 && "FILE".equals(parts[0]) && "GET".equals(parts[2])) {
            long start = Long.parseLong(parts[4]);
            long end = Long.parseLong(parts[6]);
            sendFileData(channel, dataSocket, filename, start, end, clientAddress, clientPort);
        }
        return true;
    }

    // 辅助方法：发送文件数据块
    private static void sendFileData(FileChannel channel, DatagramSocket socket,
                                     String filename, long start, long end,
                                     InetAddress address, int port) throws IOException {
        int length = (int) (end - start + 1);
        ByteBuffer fileBuffer = ByteBuffer.allocate(length);
        channel.position(start);
        channel.read(fileBuffer);
        fileBuffer.flip();

        byte[] fileData = new byte[length];
        fileBuffer.get(fileData);

        String base64Data = Base64.getEncoder().encodeToString(fileData);
        String response = String.format("FILE %s OK START %d END %d DATA %s",
                filename, start, end, base64Data);

        sendResponse(socket, response, address, port);

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
