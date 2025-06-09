import java.io.IOException;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

public class UDPClient {
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





            }







        }




        catch (IOException e) {

        }

    }

}