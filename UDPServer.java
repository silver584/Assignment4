import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;


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
            }


        }
    }
}
