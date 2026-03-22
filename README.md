<div align="center">

# 🏰 ArqoLand

### Enterprise-Grade Land Claim & Territory Management

[![Version](https://img.shields.io/badge/version-1.1.0--ENTERPRISE-gold.svg)](https://github.com/Ikramhandan13/ArqoLand)
[![Minecraft](https://img.shields.io/badge/minecraft-1.21.x-green.svg)](https://www.minecraft.net/)
[![License](https://img.shields.io/badge/license-MIT-red.svg)](LICENSE)
[![Java](https://img.shields.io/badge/java-21-orange.svg)](https://www.java.com/)

**Sistem manajemen wilayah tercanggih dengan proteksi dinamis, ekonomi berbasis berlian, dan pertahanan otomatis (Turret) untuk server Minecraft modern.**

[Fitur Utama](#-fitur-utama) • [Instalasi](#-instalasi) • [Perintah](#-perintah) • [Konfigurasi](#%EF%B8%8F-konfigurasi) • [Pembangunan](#-pembangunan-dari-source) • [Lisensi](#-lisensi)

---

### 🛠️ Didukung Oleh (Powered By)

[![PacketEvents](https://img.shields.io/badge/PacketEvents-API-blue?style=flat&logo=github)](https://github.com/retrooper/packetevents)
[![PlaceholderAPI](https://img.shields.io/badge/PlaceholderAPI-Integration-green?style=flat&logo=spigotmc)](https://github.com/PlaceholderAPI/PlaceholderAPI)
[![Lamp](https://img.shields.io/badge/Lamp-Command_Framework-orange?style=flat&logo=java)](https://github.com/Revxrsal/Lamp)
[![HikariCP](https://img.shields.io/badge/HikariCP-Performance-red?style=flat&logo=java)](https://github.com/brettwooldridge/HikariCP)
[![Folia](https://img.shields.io/badge/Folia-Supported-lightgrey?style=flat&logo=minecraft)](https://github.com/PaperMC/Folia)

</div>

---

## 📋 Ikhtisar (Overview)

**ArqoLand** adalah plugin manajemen wilayah (Land Claim) ultra-lightweight (2.2MB) yang dirancang khusus untuk performa tinggi pada environment **Folia** dan **Paper**. Berbeda dengan plugin klaim tradisional, ArqoLand memperkenalkan sistem ekonomi berbasis **Brankas Berlian** (Diamond Bank) dan mekanik **Raid TNT** yang seimbang dengan HP wilayah.

### 🎯 Mengapa ArqoLand?

- ⚡ **Ultra-Lightweight** - Ukuran JAR minimalis berkat teknologi Paper Library Loader.
- 💎 **Diamond Economy** - Klaim dan perawatan wilayah menggunakan item Diamond asli.
- 🛡️ **Smart Turret** - Pertahanan otomatis dengan deteksi Ray-tracing dan efek debuff bertingkat.
- ⚔️ **Balanced Raiding** - Sistem HP wilayah yang bisa hancur oleh ledakan TNT dari luar.
- 📊 **Leaderboard Terintegrasi** - Pantau wilayah terkaya dan kontributor terbaik secara real-time.
- 🚀 **Folia Optimized** - Thread-safe dan siap digunakan di server dengan regionalized multithreading.

---

## ✨ Fitur Utama

### 🗺️ Manajemen Wilayah & Proteksi
- **Klaim Berbasis Chunk** - Gunakan `/al claimland` untuk mengamankan area.
- **Sistem Aliansi (Ally)** - Berbagi izin akses dan pembangunan dengan wilayah rekanan.
- **Visual Border & Notifikasi** - Partikel batas wilayah yang dapat dinyalakan/matikan dan notifikasi masuk (Title, Action Bar, Sound).
- **Self-Healing** - Wilayah memulihkan 2 HP setiap 5 menit secara otomatis.

### 💰 Ekonomi & Brankas (Brankas)
- **GUI Setoran Berlian** - Interface `/al setoran` yang intuitif untuk menabung Diamond.
- **Perk Wilayah** - Efek Jump Boost, Speed, dan lainnya aktif saat berada di dalam wilayah sendiri.
- **Sistem Upkeep** - Pastikan saldo brankas cukup untuk mempertahankan proteksi.

### 🏹 Sistem Pertahanan Turret (Enterprise)
- **Smart Targeting** - Menyerang Monster agresif dan pemain asing (bukan anggota/ally).
- **Ray-Tracing Collision** - Panah tidak menembus dinding; sistem memberi peringatan jika moncong turret terhalang.
- **Upgrade Efek (Lvl 1-5)**:
  - **Lvl 1-2**: Efek Slowness & Weakness.
  - **Lvl 3-4**: Mining Fatigue & Wither.
  - **Lvl 5**: **Freeze State** (Slowness V), Blindness, dan Wither II.

### 🛡️ Mekanik Raid & TNT
- **Anti-Griefing** - Pemain luar tidak bisa meletakkan/menghancurkan blok di dalam wilayah.
- **TNT Cannon Support** - Ledakan TNT dari luar wilayah akan mengurangi HP wilayah (2 HP/blok).
- **Intruder Alert** - Pemain musuh yang masuk akan terkena efek Slowness dan Poison secara otomatis (Turret Upgrade).

---

## 📦 Instalasi

### Persyaratan Sistem
- **Server**: Paper, Purpur, atau Folia (1.21+)
- **Java**: Version 21
- **Dependensi Wajib**: [PacketEvents](https://github.com/retrooper/packetevents) (Harus terpasang sebagai plugin terpisah)

### Langkah Instalasi
1. Unduh `ArqoLand.jar`.
2. Masukkan ke folder `plugins/`.
3. Restart server (Paper akan otomatis mengunduh library pendukung seperti HikariCP dan Database Drivers).
4. Konfigurasi `plugins/ArqoLand/config.yml` dengan detail MariaDB/MySQL Anda.

---

## 🛠️ Pembangunan dari Source

Jika Anda ingin membangun plugin ini sendiri dari kode sumber:

```bash
# Clone repository
git clone https://github.com/Ikramhandan13/ArqoLand.git

# Masuk ke direktori
cd ArqoLand

# Build menggunakan Maven
mvn clean package
```
JAR hasil build akan tersedia di folder `target/ArqoLand.jar`.

---

## 💻 Developer API

Integrasikan **ArqoLand** ke dalam project Anda untuk mengakses data wilayah dan sistem pertahanan.

### Maven
```xml
<dependency>
    <groupId>dev.arqo</groupId>
    <artifactId>ArqoLand</artifactId>
    <version>1.1.0-ENTERPRISE</version>
    <scope>provided</scope>
</dependency>
```

### Gradle
```kotlin
dependencies {
    compileOnly("dev.arqo:ArqoLand:1.1.0-ENTERPRISE")
}
```

### Java Example
Dapatkan informasi wilayah di lokasi pemain:

```java
import dev.arqo.land.core.LandManager;
import dev.arqo.land.model.Land;

public void checkLand(Player player) {
    Land land = LandManager.getLandAt(player.getLocation());
    if (land != null) {
        player.sendMessage("Anda berada di wilayah: " + land.getDisplayName());
        player.sendMessage("Pemilik: " + land.getOwnerName());
        player.sendMessage("HP: " + land.getHp());
    }
}
```

---

## 🎮 Perintah & Izin (Permissions)

| Perintah | Deskripsi | Izin (Permission) |
|----------|-----------|-------------------|
| `/al claimland` | Klaim chunk tempat Anda berdiri | `arqoland.player` |
| `/al status` | Lihat HP, Pemilik, dan Saldo Brankas | `arqoland.player` |
| `/al setoran` | Buka GUI untuk setor Diamond | `arqoland.player` |
| `/al border` | Toggle partikel batas wilayah | `arqoland.player` |
| `/al spawn` | Teleportasi ke wilayah sendiri | `arqoland.player` |
| `/al topBrankas` | Lihat 10 wilayah terkaya | `arqoland.player` |
| `/al rename <id>` | Ubah ID internal wilayah | `arqoland.admin` |

---

## ⚙️ Konfigurasi Default

```yaml
database:
  type: "MARIADB" # Pilih: MARIADB atau SQLITE
  host: "localhost"
  port: 3306
  name: "arqoland"
  user: "root"
  pass: "password"

land:
  default-hp: 100
  max-hp: 500
  claim-cost: 64 # Diamond
  healing-rate: 2 # HP per 5 menit
```

---

## 📊 Performa & Optimasi

| Aspek | Standar Lain | ArqoLand |
|-------|--------------|----------|
| **Ukuran File** | ~25MB | **2.2MB** |
| **Manajemen Memori** | Heavy Shading | **Library Loading** |
| **Database** | Synchronous | **Async HikariCP** |
| **Kesiapan Folia** | Terbatas | **Native Support** |

---

## 💎 Sponsor & Dukungan

Proyek ini didukung penuh oleh **[Arqonara Hosting](https://arqonara.com)**.  
Dapatkan performa server Minecraft terbaik dengan optimasi Enterprise.

---

## 🌟 Credits

### **Developed By:**
- **[Arqonara Hosting](https://arqonara.com)** (Official Maintainer)
- **DZ** (Lead Architect)

### **Special Thanks to:**
- **[PaperMC Team](https://papermc.io)** - Untuk API server berkinerja tinggi (Paper & Folia).
- **[Lamp Framework](https://github.com/Revxrsal/Lamp)** - Command framework modern yang elegan.
- **[PacketEvents](https://github.com/retrooper/packetevents)** - Library packet handling tingkat lanjut.
- **[PlaceholderAPI](https://github.com/PlaceholderAPI/PlaceholderAPI)** - Standar integrasi variabel global.
- **[InventoryFramework](https://github.com/stefvanschie/InventoryFramework)** - Solusi GUI inventory yang intuitif.
- **[HikariCP](https://github.com/brettwooldridge/HikariCP)** - Koneksi database tercepat di kelasnya.
- **Minecraft Server Community** - Untuk inspirasi dan feedback berharga.
- **Beta Testers & Contributors** - Yang telah membantu memvalidasi stabilitas plugin.

---

## 📜 Lisensi
Proyek ini dilisensikan di bawah **MIT License** - lihat file [LICENSE](LICENSE) untuk detail lebih lanjut.

---

## 💬 Dukungan & Kontak
- **Lapor Bug**: [GitHub Issues](https://github.com/Ikramhandan13/ArqoLand/issues)
- **Documentation**: [arqonara.com/docs](https://arqonara.com)

<div align="center">

**⚡ Built for performance. Designed for competition.**

</div>
