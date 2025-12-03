# Next steps to implement full messenger with P2P and AI

- Implement local encryption (use ECDH for key exchange + AES-GCM for message encryption)
- Implement P2P discovery over Wi-Fi (multicast, local hotspot or WebRTC P2P)
- Message storage: use Room database
- Add media (photo) sending with content provider & encrypted storage
- Integrate local AI models or connectors (placeholder currently)
- Implement decentralized token and payment integration as separate service
- CI: Add GitHub Actions workflow to build debug/release APKs (requires secrets and Android SDK)
