import java.io.IOException;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.nio.file.Files;
import java.nio.file.Paths;
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
                }
            }

        }



    }
private  static String sendAndReceive(DatagramSocket socket, InetAddress addr, int port, String message)
        throws IOException {
    int currentTimeout = INITIAL_TIMEOUT;
    int retries = 0;

    byte[] sendData = message.getBytes();
    byte[] receiveData = new byte[MAX_PACKET_SIZE];


    }
}