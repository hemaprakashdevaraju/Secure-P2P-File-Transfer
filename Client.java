import java.io.*;
import java.net.*;

// Usage:
// java Client <loadBalancerHost> <loadBalancerPort> <outputFile> <chunkCommaList>
public class Client {
    public static void main(String[] args) throws Exception {
        if (args.length < 4) {
            System.out.println("Usage: java Client <lbHost> <lbPort> <outputFile> <chunk1,chunk2,...>");
            return;
        }
        String lbHost = args[0];
        int lbPort = Integer.parseInt(args[1]);
        String outputFile = args[2];
        String chunkList = args[3];

        try (Socket s = new Socket(lbHost, lbPort);
             PrintWriter pw = new PrintWriter(s.getOutputStream(), true);
             InputStream in = s.getInputStream();
             FileOutputStream fos = new FileOutputStream(outputFile)) {
            String[] chunks = chunkList.split(",");
            pw.println("DOWNLOAD " + outputFile + " " + chunks.length + " " + chunkList);
            // Read until LB closes connection
            byte[] buffer = new byte[4096];
            int r;
            while ((r = in.read(buffer)) != -1) {
                fos.write(buffer, 0, r);
            }
            System.out.println("[Client] Received file saved to " + outputFile);
        } catch (Exception e) {
            System.err.println("[Client] error: " + e.getMessage());
        }
    }
}
