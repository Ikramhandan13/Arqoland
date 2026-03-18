--- Tanggal: 18 Maret 2026 (Sesi Penutupan: ArqoLand Build Fix) ---
Status: Proyek ArqoLand dalam proses migrasi ke Java 21, Lamp v4.0.0-rc.16, dan API 1.21.1.
Dependensi: Library Lamp (common & bukkit) telah diunduh manual ke folder libs/ dan dikonfigurasi di pom.xml.
Masalah Utama: 
1. Transitive dependencies (Adventure API, BungeeCord) hilang di classpath compiler.
2. Inkompatibilitas kode lama (API 1.21.1, Lamp v4 syntax).
Catatan: Sesi diakhiri dengan rencana penyederhanaan fitur sementara untuk mencapai target Build Successful.
---
