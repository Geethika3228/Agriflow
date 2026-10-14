package agriflow;

import agriflow.server.AgriFlowServer;

public class AgriFlowApp {
    public static void main(String[] args) throws Exception {
        int port = args.length > 0 ? Integer.parseInt(args[0]) : 8080;
        new AgriFlowServer(port).start();
        System.out.println("AgriFlow is live  ->  http://localhost:" + port);
    }
}
