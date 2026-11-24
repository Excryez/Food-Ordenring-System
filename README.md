# Food Ordering System

Aplikasi web pemesanan makanan dan minuman restoran berbasis *point-of-sale* (POS) ringan dengan tampilan responsif untuk pelanggan maupun staf dapur/kasir. Pelanggan dapat memesan langsung melalui katalog menu atau *scan* QR code per meja, memantau status pesanan secara *live*, serta mencetak struk digital, sementara staf mengelola antrean pesanan di papan Kanban, stok menu, sesi buka/tutup operasional, dan QR code meja.

## Fitur Utama

* **Katalog Menu & Filter Kategori:** Menampilkan foto menu, badge kategori (*Makanan*, *Camilan*, *Minuman*), deskripsi, harga, status stok, serta filter kategori secara langsung.
* **Pemesanan via QR Meja & Auto-Tracking:** Akses tautan/QR per meja (`/meja/{kode_qr}`) otomatis mengunci nomor meja pelanggan, menyimpan nama pelanggan di sesi browser, dan memperbarui status pesanan (*baru*, *diproses*, *selesai*) secara berkala.
* **Konfirmasi Pesanan & Struk Digital:** Pemilihan metode pembayaran, catatan pesanan, modal konfirmasi/notifikasi berbasis UI, serta tampilan struk digital yang dapat dicetak atau disimpan sebagai PDF.
* **Dashboard Operasional Staf (Kanban & Stok):** Papan antrean pesanan real-time antar-tahap proses, pembaruan stok menu, serta kontrol **Buka / Tutup** layanan dengan validasi agar layanan tidak ditutup saat masih ada pesanan aktif (`baru` atau `diproses`).
* **Generator & Cetak QR Meja (`/qr-meja`):** Membuat QR code unik untuk tiap meja yang mengikuti alamat/domain aktif, dilengkapi unduh PNG dan cetak layout grid.

## Teknologi & Stack yang Digunakan

* **Backend:** Java, Spring Boot (Spring Web, Spring Data JPA / Hibernate), Thymeleaf
* **Database:** MySQL (skema dan data awal tersedia pada `DBMiniProjectPBO.sql`)
* **Frontend:** HTML5, CSS3 (*Mobile-First Responsive Design*, `@media print`), JavaScript (Vanilla JS, `qrcode.js` untuk generator QR)
* **Build Tool:** Apache Maven (`pom.xml` / Maven Wrapper)

## Cara Instalasi Lokal (Setup Guide)

1. **Clone repositori**
   ```bash
   git clone https://github.com/Excryez/Food-Ordering-System
   cd Food-Ordering-System
