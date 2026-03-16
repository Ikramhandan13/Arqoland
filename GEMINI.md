# ARQOLAND ENTERPRISE - DEVELOPMENT ARCHIVE
**Tanggal:** 16 Maret 2026
**Status:** Ready for Production (Build Optimized)

---

## 1. PERSONAL SYSTEM PROMPT (CORE PROTOCOL)
Kamu adalah AI Assistant pribadi milik TUAN DZAKIRI. AI engineer tingkat expert.
- Langsung ke inti, tanpa teks sampah.
- Selalu pasti dan akurat.
- Fix error sampai tuntas.
- Loyal dan profesional.

---

## 2. BLUEPRINT: ARQOLAND ENTERPRISE (LATEST)
### [A] Arsitektur & Database
- **Engine:** Paper/Folia (Thread-Safe).
- **Database:** MariaDB (HikariCP) dengan integritas `ON DELETE CASCADE`.
- **Fields Baru:** `max_health`, `spawn_location`, `is_public`, `total_contributed` (per member).

### [B] Fitur Unggulan
1. **Hierarki Komando:** Owner > Admin > Member.
2. **Sistem Raid (TNT Cannon):** Ledakan mengurangi HP wilayah secara akurat (Prioritas LOWEST).
3. **Visual Feedback:** 
   - Hologram `-HP` melayang saat ledakan.
   - Radar ASCII 2D (`/al map`).
   - Pagar Partikel Border (`/al border`).
4. **Ekonomi Setoran:**
   - Upgrade ditarik dari saldo Brankas (Setoran member).
   - Pajak 70 Diamond/Minggu dengan Grace Period 7 hari.
   - GUI Penarikan Diamond (Withdraw) khusus Owner.
5. **Defense System (Turret):** Dispenser menembak otomatis musuh setiap 0.5 detik (Folia Safe).
6. **Perks:** Speed, Haste, Strength, Jump, dan **Crop Boost** (Pertumbuhan tanaman ganda).

---

## 3. RIWAYAT PERCAKAPAN & MEMORI PENGEMBANGAN
- **Audit Awal:** Menemukan celah *persistence* (data hanya di RAM) dan *protection* (member tidak bisa build).
- **Reformasi Database:** Mengimplementasikan CRUD lengkap agar data tersimpan permanen di MariaDB.
- **Implementasi Raid:** Menambahkan logic agar TNT Cannon masuk ke damage wilayah dan mengirim alert Title/Discord.
- **Implementasi Ekonomi:** Merubah sistem upgrade agar memotong saldo brankas, bukan inventory pemain.
- **Fix Build:** Memperbaiki resolusi dependensi dengan beralih ke Spigot-API dan menambahkan repository snapshots agar proses kompilasi sukses.

---

## 4. DAFTAR FILE KRITIS
- `ArqoLand.java`: Inisialisasi seluruh manager & listener.
- `Queries.java`: Seluruh SQL Logic (Insert, Update, Load).
- `LandCommand.java`: Pusat kendali perintah `/al`.
- `TurretManager.java`: Logic pertahanan otomatis.
- `RaidListener.java`: Logic pertahanan vs ledakan & hologram.
- `EconomyManager.java`: Logic fisik Diamond & GUI Brankas.

---

## 5. CATATAN TUAN DZAKIRI
- Upgrade harus menarik Diamond dari setoran/brankas.
- TNT Cannon harus memberikan damage masuk ke wilayah.
- Pajak mingguan harus ada pinalti jika saldo kosong.

---
**Status Akhir:** Build sukses, seluruh fitur sinkron. Selamat istirahat, Tuan.
