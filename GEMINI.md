--- Tanggal: 19 Maret 2026 (Sesi Perbaikan Build & Runtime Lamp v4) ---
Status: Proyek ArqoLand berhasil dikompilasi (Build Success) dan melewati tahap inisialisasi awal di server (Runtime Fix).

Masalah yang Diselesaikan:
1. NoClassDefFoundError (Shading Fix):
   - Masalah: Dependensi lokal di folder libs/ tidak ikut terbungkus (shaded) ke JAR final karena menggunakan system scope.
   - Solusi: Membuat repositori Maven lokal (local-repo/) di dalam proyek, menginstal JAR secara manual ke sana, dan mengubah scope menjadi 'compile' di pom.xml agar Shade Plugin bisa memprosesnya.
2. Missing Module (Lamp Brigadier):
   - Masalah: Lamp v4 Bukkit membutuhkan modul brigadier saat runtime/integrasi command.
   - Solusi: Menambahkan dependensi 'lamp.brigadier' v4.0.0-rc.16.
3. StringIndexOutOfBoundsException (Parser Fix):
   - Masalah: Konflik Lamp v4 parser akibat duplikasi @Command dan @Subcommand("").
   - Solusi: Menggabungkan perintah admin ke LandCommand dan menggunakan @Command pada metode help.
4. Database Reversion: Dikembalikan ke MariaDB/MySQL.

File Penting:
- pom.xml: Shading & local-repo.
- LandCommand.java: Command central /al.
- mc/plugins/ArqoLand.jar: Build terbaru.

--- Tanggal: 20 Maret 2026 (Persiapan Pengujian Command & Bot) ---
Status: Persiapan lingkungan untuk pengujian fungsional plugin ArqoLand.

Aktivitas Sesi:
1. Analisa LandCommand.java: Memetakan fitur (claim, ally, admin, economy) untuk skenario pengujian.
2. Konfigurasi Server:
   - mc/server.properties: Mengubah online-mode=false untuk mendukung bot testing.
3. Strategi Testing:
   - Menggunakan Mineflayer (Node.js) sebagai bot klien untuk mensimulasikan aktivitas pemain dan memvalidasi command /al.
   - Mengidentifikasi dependensi mineflayer sudah tersedia di node_modules.

Tujuan Berikutnya:
- Membuat script bot_test.js untuk otomatisasi pengujian perintah /al claimland, status, dll.
- Menjalankan server Minecraft secara background dan memantau log.
---
