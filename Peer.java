import java.io.*;
import java.net.*;
import java.nio.file.*;
import java.util.*;

// Usage:
// java Peer <peerHost> <peerPort> <trackerHost> <trackerPort> <chunkDirectory>
public class Peer {
    public static void main(String[] args) throws Exception {
        if (args.length < 5) {
            System.out.println("Usage: java Peer <peerHost> <peerPort> <trackerHost> <trackerPort> <chunkDirectory>");
            return;
        }
        String peerHost = args[0];
        int peerPort = Integer.parseInt(args[1]);
        String trackerHost = args[2];
        int trackerPort = Integer.parseInt(args[3]);
        String chunkDir = args[4];

        // Build list of chunk filenames in directory
        File dir = new File(chunkDir);
        if (!dir.exists() || !dir.isDirectory()) {
            System.out.println("Chunk directory not found: " + chunkDir);
            return;
        }
        StringJoiner sj = new StringJoiner(",");
        for (File f : dir.listFiles()) {
            if (f.isFile()) sj.add(f.getName());
        }
        String chunksCsv = sj.toString();
        // Register with tracker
        try (Socket ts = new Socket(trackerHost, trackerPort);
             PrintWriter pw = new PrintWriter(ts.getOutputStream(), true);
             BufferedReader br = new BufferedReader(new InputStreamReader(ts.getInputStream()))) {
            pw.println("REGISTER " + chunksCsv + " " + peerHost + ":" + peerPort);
            String resp = br.readLine();
            System.out.println("[Peer] Registered with tracker: " + resp);
        } catch (Exception e) {
            System.err.println("[Peer] cannot register: " + e.getMessage());
        }

        ServerSocket ss = new ServerSocket(peerPort);
        System.out.println("[Peer] Listening on " + peerPort + " serving: " + chunksCsv);
        while (true) {
            Socket client = ss.accept();
            new Thread(() -> handleClient(client, chunkDir)).start();
        }
    }

    private static void handleClient(Socket s, String chunkDir) {
        try (BufferedReader br = new BufferedReader(new InputStreamReader(s.getInputStream()));
             OutputStream out = s.getOutputStream();
             PrintWriter pw = new PrintWriter(out, true)) {
            String line = br.readLine();
            if (line == null) return;
            String[] parts = line.split(" ", 2);
            String cmd = parts[0];
            if ("GETCHUNK".equalsIgnoreCase(cmd)) {
                String chunkId = parts[1].trim();
                File f = new File(chunkDir, chunkId);
                if (!f.exists()) {
                    pw.println("ERR");
                    return;
                }
                byte[] chunkData = Files.readAllBytes(f.toPath());
                byte[] encrypted = CryptoUtils.encrypt(chunkData);
                String md5 = CryptoUtils.getMD5Hash(chunkData);
                // Send header: OK <encryptedSize> <md5>
                pw.println("OK " + encrypted.length + " " + md5);
                // Send raw bytes
                out.write(encrypted);
                out.flush();
                System.out.println("[Peer] Sent chunk " + chunkId + " size=" + encrypted.length);
            } else {
                pw.println("ERR");
            }
        } catch (Exception e) {
            System.err.println("[Peer] error: " + e.getMessage());
        } finally {
            try { s.close(); } catch (IOException ignore) {}
        }
    }
}
