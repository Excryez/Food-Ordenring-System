-- =====================================================================
--  FOOD RESERVATION SYSTEM - Kelompok 6 (PBO)
--  Database : MySQL 8.x (Laragon)   |   Charset : utf8mb4
--  Cara pakai: import file ini SEKALI -> database + tabel + data dummy jadi.
--  PERHATIAN: file ini menghapus lalu membuat ulang database
--             'food_reservation' (berguna untuk reset data dummy).
-- =====================================================================

DROP DATABASE IF EXISTS food_reservation;
CREATE DATABASE food_reservation
  CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE food_reservation;

-- ---------------------------------------------------------------------
-- 1. PENGGUNA  : staf restoran (Kasir/Admin & Pihak Dapur).
--    Pelanggan TIDAK login, cukup scan QR meja.
-- ---------------------------------------------------------------------
CREATE TABLE pengguna (
  id_pengguna   INT AUTO_INCREMENT PRIMARY KEY,
  nama          VARCHAR(100) NOT NULL,
  username      VARCHAR(50)  NOT NULL UNIQUE,
  password_hash CHAR(64)     NOT NULL,                 -- SHA-256 (hex)
  role          ENUM('admin','dapur') NOT NULL,
  dibuat_pada   DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB;

-- ---------------------------------------------------------------------
-- 2. MEJA : tiap meja punya kode unik yang ditanam di QR Code
--    contoh isi QR: http://alamat-web/menu?meja=MEJA01-7f3a9c
-- ---------------------------------------------------------------------
CREATE TABLE meja (
  id_meja     INT AUTO_INCREMENT PRIMARY KEY,
  nomor_meja  VARCHAR(10) NOT NULL UNIQUE,
  kode_qr     VARCHAR(64) NOT NULL UNIQUE
) ENGINE=InnoDB;

-- ---------------------------------------------------------------------
-- 3. MENU : stok & ketersediaan dikelola Kasir/Admin
--    tersedia = saklar manual admin; otomatis 0 kalau stok habis
-- ---------------------------------------------------------------------
CREATE TABLE menu (
  id_menu    INT AUTO_INCREMENT PRIMARY KEY,
  nama_menu  VARCHAR(100) NOT NULL,
  deskripsi  VARCHAR(255) NULL,
  kategori   ENUM('Makanan','Minuman','Camilan') NOT NULL DEFAULT 'Makanan',
  harga      INT NOT NULL,
  stok       INT NOT NULL DEFAULT 0,
  tersedia   TINYINT(1) NOT NULL DEFAULT 1,
  gambar     VARCHAR(255) NULL,
  CONSTRAINT chk_menu_harga CHECK (harga > 0),
  CONSTRAINT chk_menu_stok  CHECK (stok >= 0)
) ENGINE=InnoDB;

-- ---------------------------------------------------------------------
-- 4. PESANAN : satu pesanan = satu kali kirim dari satu meja
--    Alur status: Pending -> Diproses -> Siap Saji -> Selesai
-- ---------------------------------------------------------------------
CREATE TABLE pesanan (
  id_pesanan     INT AUTO_INCREMENT PRIMARY KEY,
  id_meja        INT NOT NULL,
  id_pengguna    INT NULL,                -- petugas dapur yang menangani
  nama_pelanggan VARCHAR(50) NULL,
  status         ENUM('Pending','Diproses','Siap Saji','Selesai')
                 NOT NULL DEFAULT 'Pending',
  total_harga    INT NOT NULL DEFAULT 0,  -- diisi otomatis oleh trigger
  waktu_pesan    DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  waktu_update   DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
                 ON UPDATE CURRENT_TIMESTAMP,
  CONSTRAINT fk_pesanan_meja     FOREIGN KEY (id_meja)
    REFERENCES meja(id_meja)         ON UPDATE CASCADE ON DELETE RESTRICT,
  CONSTRAINT fk_pesanan_pengguna FOREIGN KEY (id_pengguna)
    REFERENCES pengguna(id_pengguna) ON UPDATE CASCADE ON DELETE SET NULL,
  INDEX idx_pesanan_status (status)
) ENGINE=InnoDB;

-- ---------------------------------------------------------------------
-- 5. DETAIL_PESANAN : isi pesanan (menu apa, berapa porsi)
--    harga_satuan diisi otomatis dari tabel menu (trigger), jadi Java
--    cukup INSERT (id_pesanan, id_menu, jumlah, catatan).
-- ---------------------------------------------------------------------
CREATE TABLE detail_pesanan (
  id_detail    INT AUTO_INCREMENT PRIMARY KEY,
  id_pesanan   INT NOT NULL,
  id_menu      INT NOT NULL,
  jumlah       INT NOT NULL,
  harga_satuan INT NOT NULL DEFAULT 0,
  subtotal     INT GENERATED ALWAYS AS (jumlah * harga_satuan) STORED,
  catatan      VARCHAR(100) NULL,
  CONSTRAINT chk_detail_jumlah CHECK (jumlah > 0),
  CONSTRAINT fk_detail_pesanan FOREIGN KEY (id_pesanan)
    REFERENCES pesanan(id_pesanan) ON UPDATE CASCADE ON DELETE CASCADE,
  CONSTRAINT fk_detail_menu    FOREIGN KEY (id_menu)
    REFERENCES menu(id_menu)       ON UPDATE CASCADE ON DELETE RESTRICT
) ENGINE=InnoDB;

-- ---------------------------------------------------------------------
-- TRIGGER : stok & total dihitung otomatis oleh database
-- ---------------------------------------------------------------------
DELIMITER $$

CREATE TRIGGER trg_detail_before_insert
BEFORE INSERT ON detail_pesanan
FOR EACH ROW
BEGIN
  DECLARE v_harga    INT;
  DECLARE v_stok     INT;
  DECLARE v_tersedia TINYINT(1);

  SELECT harga, stok, tersedia INTO v_harga, v_stok, v_tersedia
  FROM menu WHERE id_menu = NEW.id_menu;

  IF v_tersedia = 0 THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Menu sedang tidak tersedia';
  END IF;
  IF v_stok < NEW.jumlah THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Stok menu tidak cukup';
  END IF;

  SET NEW.harga_satuan = v_harga;
END$$

CREATE TRIGGER trg_detail_after_insert
AFTER INSERT ON detail_pesanan
FOR EACH ROW
BEGIN
  -- kurangi stok; kalau jadi 0, menu otomatis ditandai tidak tersedia
  UPDATE menu
     SET stok     = stok - NEW.jumlah,
         tersedia = IF(stok > 0, tersedia, 0)
   WHERE id_menu = NEW.id_menu;

  -- tambah ke total pesanan
  UPDATE pesanan
     SET total_harga = total_harga + (NEW.jumlah * NEW.harga_satuan)
   WHERE id_pesanan = NEW.id_pesanan;
END$$

DELIMITER ;

-- ---------------------------------------------------------------------
-- VIEW : query siap pakai untuk sisi pelanggan & dapur
-- ---------------------------------------------------------------------
CREATE VIEW v_menu_tersedia AS
SELECT id_menu, nama_menu, deskripsi, kategori, harga, stok, gambar
FROM menu
WHERE tersedia = 1 AND stok > 0;

CREATE VIEW v_pesanan_dapur AS
SELECT p.id_pesanan, m.nomor_meja, p.nama_pelanggan, p.status,
       p.waktu_pesan, d.id_detail, mn.nama_menu, d.jumlah, d.catatan
FROM pesanan p
JOIN meja m            ON m.id_meja    = p.id_meja
JOIN detail_pesanan d  ON d.id_pesanan = p.id_pesanan
JOIN menu mn           ON mn.id_menu   = d.id_menu
WHERE p.status <> 'Selesai'
ORDER BY p.waktu_pesan, p.id_pesanan;

-- ---------------------------------------------------------------------
-- DATA DUMMY
-- ---------------------------------------------------------------------
-- Password dummy:  admin / admin123   |   dapur / dapur123
INSERT INTO pengguna (nama, username, password_hash, role) VALUES
 ('Admin Restoran', 'admin', SHA2('admin123', 256), 'admin'),
 ('Koki Dapur',     'dapur', SHA2('dapur123', 256), 'dapur');

INSERT INTO meja (nomor_meja, kode_qr) VALUES
 ('01', 'MEJA01-7f3a9c'),
 ('02', 'MEJA02-b81d42'),
 ('03', 'MEJA03-c5e07a'),
 ('04', 'MEJA04-1d9f36'),
 ('05', 'MEJA05-e24b8d');

-- 7 menu (Es Teh Manis sengaja stok 0 untuk menguji status "habis")
INSERT INTO menu (nama_menu, deskripsi, kategori, harga, stok, tersedia) VALUES
 ('Tinutuan',            'Bubur Manado dengan sayuran, ikan asin, dan sambal roa', 'Makanan', 18000, 20, 1),
 ('Ayam Rica-Rica',      'Ayam bumbu rica pedas khas Manado',                      'Makanan', 35000, 15, 1),
 ('Cakalang Fufu',       'Ikan cakalang asap dengan sambal dabu-dabu',             'Makanan', 38000, 10, 1),
 ('Nasi Kuning Manado',  'Nasi kuning dengan lauk ikan dan sambal',                'Makanan', 25000, 12, 1),
 ('Pisang Goroho Goreng','Pisang goroho goreng dengan sambal roa',                 'Camilan', 15000, 25, 1),
 ('Es Kacang Merah',     'Es serut dengan kacang merah dan susu',                  'Minuman', 15000, 30, 1),
 ('Es Teh Manis',        'Teh manis dingin',                                       'Minuman',  8000,  0, 0);

-- 2 pesanan contoh (harga, total, dan stok dihitung otomatis oleh trigger)
INSERT INTO pesanan (id_meja, id_pengguna, nama_pelanggan, status) VALUES
 (1, NULL, 'Budi', 'Pending'),
 (3, 2,    'Sari', 'Diproses');

INSERT INTO detail_pesanan (id_pesanan, id_menu, jumlah, catatan) VALUES
 (1, 1, 2, 'Tanpa cabai'),
 (1, 6, 2, NULL),
 (2, 2, 1, 'Pedas'),
 (2, 5, 1, NULL);
