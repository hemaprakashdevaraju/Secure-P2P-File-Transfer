# Secure P2P File Transfer System

A Java-based peer-to-peer distributed file transfer system implementing AES encryption, MD5 integrity verification, tracker-based peer discovery, and load-balanced chunk transfer.

---

## Features

- Peer-to-peer chunk transfer
- AES encrypted communication
- MD5 integrity verification
- Tracker-based peer discovery
- Load balancing
- Distributed file reconstruction
- Socket-based communication

---

## Project Structure

```text
Tracker.java
Peer.java
Client.java
LoadBalancer.java
CryptoUtils.java

sample_chunks/
```

---

## Technologies Used

- Java
- Socket Programming
- Computer Networks
- AES Encryption
- MD5 Hashing

---

## How to Run

### Compile

```bash
javac *.java
```

### Start Tracker

```bash
java Tracker 6000
```

### Start Peer 1

```bash
java Peer 127.0.0.1 7101 127.0.0.1 6000 sample_chunks/peer1_chunks
```

### Start Peer 2

```bash
java Peer 127.0.0.1 7102 127.0.0.1 6000 sample_chunks/peer2_chunks
```

### Start Load Balancer

```bash
java LoadBalancer 127.0.0.1 6000 7000
```

### Run Client

```bash
java Client 127.0.0.1 7000 output.txt chunk_0,chunk_1,chunk_2
```

---

## Protocol Flow

1. Peers register available chunks with Tracker
2. Client requests chunks through LoadBalancer
3. LoadBalancer obtains peer details from Tracker
4. Chunks are transferred securely using AES encryption
5. MD5 hashes verify integrity
6. Client receives reconstructed output file

---

## Security Features

- AES encrypted chunk transfer
- MD5 integrity verification
- Peer validation through tracker

---

## Future Improvements

- Dynamic chunking
- GUI interface
- Better fault tolerance
- Real-time peer discovery
- Distributed hash table implementation
