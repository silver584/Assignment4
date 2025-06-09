import java.net.DatagramSocket;




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

        }

    }
}
