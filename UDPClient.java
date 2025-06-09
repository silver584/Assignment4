public class UDPClient {
    public static void main(String[] args) {
        if (args.length != 3) {
            System.out.println("Usage: java UDPClient <server_host> <server_port> <file_list>");
            return;
        }

        String serverHost = args[0];
        int serverPort = Integer.parseInt(args[1]);
        String fileList = args[2];
    }
}