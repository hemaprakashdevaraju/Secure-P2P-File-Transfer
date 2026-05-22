import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;

// Tracker: keeps mapping chunkId -> list of peer endpoints (host:port)
public class Tracker {
    private static final Map<String, List<String>> chunkPeers = new ConcurrentHashMap<>();

    public static void main(String[] args) throws Exception {
        int port = 6000;
        if (args.length >= 1) port = Integer.parseInt(args[0]);
        ServerSocket ss = new ServerSocket(port);
        System.out.println("[Tracker] running on port " + port);

        while (true) {
            Socket s = ss.accept();
            new Thread(() -> handle(s)).start();
        }
    }

    private static void handle(Socket s) {
        try (BufferedReader br = new BufferedReader(new InputStreamReader(s.getInputStream()));
             PrintWriter pw = new PrintWriter(s.getOutputStream(), true)) {
            String line = br.readLine();
            if (line == null) return;
            String[] parts = line.split(" ", 3);
            String cmd = parts[0];
            if ("REGISTER".equalsIgnoreCase(cmd)) {
                // REGISTER chunkA,chunkB host:port
                String[] chunks = parts[1].split(",");
                String peer = parts[2].trim();
                for (String c : chunks) {
                    chunkPeers.putIfAbsent(c, Collections.synchronizedList(new ArrayList<>()));
                    List<String> list = chunkPeers.get(c);
                    if (!list.contains(peer)) list.add(peer);
                }
                pw.println("OK");
                System.out.println("[Tracker] Registered " + parts[1] + " -> " + peer);
            } else if ("GETPEERS".equalsIgnoreCase(cmd)) {
                String chunkId = parts[1].trim();
                List<String> peers = chunkPeers.getOrDefault(chunkId, new ArrayList<>());
                pw.println(String.join(",", peers));
                System.out.println("[Tracker] GETPEERS " + chunkId + " -> " + peers);
            } else {
                pw.println("ERR Unknown");
            }
        } catch (Exception e) {
            System.err.println("[Tracker] error: " + e.getMessage());
        } finally {
            try { s.close(); } catch (IOException ignore) {}
        }
    }
}
