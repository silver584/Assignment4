public class UDPServer {
    public static void main(String[] args) {
        if (args.length == 1){
            System.out.println("Usage: java UDPServer <port>");
        }
        String parameter = args[0]; // 获取第一个参数
        System.out.println("The provided parameter is: " + parameter);


    }
}
