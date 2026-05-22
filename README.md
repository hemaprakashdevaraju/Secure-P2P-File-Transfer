P2P Review 3 Project (AES encryption + MD5 integrity)

Provided files:
- Tracker.java
- LoadBalancer.java
- Peer.java
- Client.java
- CryptoUtils.java
- sample_chunks/peer1_chunks/chunk_0
- sample_chunks/peer1_chunks/chunk_1
- sample_chunks/peer2_chunks/chunk_2

Build:
1. Open a terminal/CMD in this folder.
2. javac *.java

Run (open separate terminal windows for each component):
1) Start Tracker (tracker listens on port 6000):
   java Tracker 6000

2) Start Peer 1 (arguments):
   java Peer <peerHost> <peerPort> <trackerHost> <trackerPort> <chunkDirectory>
   Example:
   java Peer 127.0.0.1 7101 127.0.0.1 6000 sample_chunks/peer1_chunks

3) Start Peer 2:
   java Peer 127.0.0.1 7102 127.0.0.1 6000 sample_chunks/peer2_chunks

4) Start LoadBalancer:
   java LoadBalancer <trackerHost> <trackerPort> <listenPort>
   Example:
   java LoadBalancer 127.0.0.1 6000 7000

5) Run Client (requests multiple chunks, receives merged file):
   java Client <loadBalancerHost> <loadBalancerPort> <outputFile> <chunkCommaList>
   Example:
   java Client 127.0.0.1 7000 output.txt chunk_0,chunk_1,chunk_2

Protocol summary:
- Peer registers chunks with tracker: "REGISTER <chunk1>,<chunk2> <host>:<port>"
- LoadBalancer asks tracker: "GETPEERS <chunkId>"
- LoadBalancer requests chunk from peer: "GETCHUNK <chunkId>"
  Peer replies: "OK <encryptedSize> <md5hash>"
  Then peer sends raw encrypted bytes.
- LoadBalancer decrypts, verifies MD5, streams to client.

Notes:
- AES key is hardcoded in CryptoUtils (for demo). For production use a secure key exchange.
- If integrity fails for a chunk, the LoadBalancer will try another peer (if available).
- Sample chunks are small text files; you can replace them with your own chunk files.
