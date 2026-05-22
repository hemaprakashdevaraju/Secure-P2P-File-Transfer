import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;

// Usage:
// java LoadBalancer <trackerHost> <trackerPort> <listenPort>
public class LoadBalancer {
    private final String trackerHost;
    private final int trackerPort;
    private final int listenPort;
    private final ExecutorService fetchPool = Executors.newCachedThreadPool();
    private final Map<String, Integer> rrIndexMap = new ConcurrentHashMap<>();

    public LoadBalancer(String trackerHost, int trackerPort, int listenPort) {
        this.trackerHost = trackerHost;
        this.trackerPort = trackerPort;
        this.listenPort = listenPort;
    }

    public static void main(String[] args) throws Exception {
        if (args.length < 3) {
            System.out.println("Usage: java LoadBalancer <trackerHost> <trackerPort> <listenPort>");
            return;
        }
        String th = args[0];
        int tp = Integer.parseInt(args[1]);
        int lp = Integer.parseInt(args[2]);
        new LoadBalancer(th, tp, lp).start();
    }

    public void start() throws IOException {
        ServerSocket ss = new ServerSocket(listenPort);
        System.out.println("[LoadBalancer] Listening on " + listenPort);
        while (true) {
            Socket client = ss.accept();
            new Thread(() -> handleClient(client)).start();
        }
    }

    private void handleClient(Socket client) {
        try (BufferedReader br = new BufferedReader(new InputStreamReader(client.getInputStream()));
             OutputStream clientOs = client.getOutputStream()) {
            String request = br.readLine();
            if (request == null) return;
            String[] parts = request.split(" ", 4);
            if (!"DOWNLOAD".equalsIgnoreCase(parts[0])) return;
            String outFileName = parts[1];
            int numChunks = Integer.parseInt(parts[2]);
            String[] chunkIds = parts[3].split(",");

            PipedOutputStream pos = new PipedOutputStream();
            PipedInputStream pis = new PipedInputStream(pos);
            Thread writer = new Thread(() -> {
                try {
                    byte[] buffer = new byte[4096];
                    int r;
                    while ((r = pis.read(buffer)) != -1) {
                        clientOs.write(buffer, 0, r);
                    }
                    clientOs.flush();
                } catch (IOException e) {}
            });
            writer.start();

            List<Future<Void>> fetchFutures = new ArrayList<>();
            for (String chunkId : chunkIds) {
                Callable<Void> task = () -> {
                    List<String> peers = queryTrackerForChunk(chunkId.trim());
                    if (peers.isEmpty()) {
                        System.out.println("[LB] No peers for " + chunkId);
                        return null;
                    }
                    String chosenPeer = choosePeerRoundRobin(chunkId, peers);
                    fetchChunkFromPeerToStream(chosenPeer, chunkId, pos);
                    return null;
                };
                fetchFutures.add(fetchPool.submit(task));
            }
            for (Future<Void> f : fetchFutures) try { f.get(); } catch (Exception ex) {}
            pos.close();
            writer.join();
        } catch (Exception ex) {
            System.err.println("[LB] error: " + ex.getMessage());
        } finally {
            try { client.close(); } catch (IOException ignore) {}
        }
    }

    private List<String> queryTrackerForChunk(String chunkId) {
        try (Socket s = new Socket(trackerHost, trackerPort);
             PrintWriter pw = new PrintWriter(s.getOutputStream(), true);
             BufferedReader br = new BufferedReader(new InputStreamReader(s.getInputStream()))) {
            pw.println("GETPEERS " + chunkId);
            String line = br.readLine();
            if (line == null || line.trim().isEmpty()) return Collections.emptyList();
            String[] arr = line.split(",");
            List<String> peers = new ArrayList<>();
            for (String a : arr) if (!a.trim().isEmpty()) peers.add(a.trim());
            return peers;
        } catch (Exception ex) { return Collections.emptyList(); }
    }

    private String choosePeerRoundRobin(String chunkId, List<String> peers) {
        rrIndexMap.putIfAbsent(chunkId, 0);
        int idx = rrIndexMap.get(chunkId);
        String peer = peers.get(idx % peers.size());
        rrIndexMap.put(chunkId, (idx + 1) % peers.size());
        return peer;
    }

    private void fetchChunkFromPeerToStream(String peerEndpoint, String chunkId, OutputStream out) {
        try {
            String[] hp = peerEndpoint.split(":");
            String host = hp[0];
            int port = Integer.parseInt(hp[1]);
            try (Socket s = new Socket(host, port);
                 PrintWriter pw = new PrintWriter(s.getOutputStream(), true);
                 BufferedInputStream bis = new BufferedInputStream(s.getInputStream())) {
                pw.println("GETCHUNK " + chunkId);
                BufferedReader br = new BufferedReader(new InputStreamReader(bis));
                String resp = br.readLine();
                if (resp == null) return;
                if (resp.startsWith("OK")) {
                    String[] rp = resp.split(" ", 3);
                    int size = Integer.parseInt(rp[1]);
                    String expectedHash = rp[2];
                    byte[] buffer = new byte[size];
                    int read = 0;
                    while (read < size) {
                        int r = bis.read(buffer, read, size - read);
                        if (r == -1) break;
                        read += r;
                    }
                    byte[] decrypted = CryptoUtils.decrypt(buffer);
                    String actualHash = CryptoUtils.getMD5Hash(decrypted);
                    if (!actualHash.equals(expectedHash)) {
                        System.out.println("[LB] Integrity failed for " + chunkId + " from " + peerEndpoint);
                        return;
                    }
                    out.write(decrypted);
                    out.flush();
                    System.out.println("[LB] Fetched and verified " + chunkId + " from " + peerEndpoint);
                }
            }
        } catch (Exception ex) {
            System.err.println("[LB] fetch error: " + ex.getMessage());
        }
    }
}
