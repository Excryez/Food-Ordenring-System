DROP DATABASE IF EXISTS food_reservation;
CREATE DATABASE food_reservation
  CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE food_reservation;

CREATE TABLE pengguna (
  id_pengguna   INT AUTO_INCREMENT PRIMARY KEY,
  nama          VARCHAR(100) NOT NULL,
  username      VARCHAR(50)  NOT NULL UNIQUE,
  password_hash CHAR(64)     NOT NULL,
  role          ENUM('admin','dapur') NOT NULL,
  dibuat_pada   DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB;

CREATE TABLE meja (
  id_meja     INT AUTO_INCREMENT PRIMARY KEY,
  nomor_meja  VARCHAR(10) NOT NULL UNIQUE,
  kode_qr     VARCHAR(64) NOT NULL UNIQUE
) ENGINE=InnoDB;

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

CREATE TABLE pesanan (
  id_pesanan        INT AUTO_INCREMENT PRIMARY KEY,
  id_meja           INT NOT NULL,
  id_pengguna       INT NULL,
  nama_pelanggan    VARCHAR(50) NULL,
  metode_pembayaran VARCHAR(50) NULL DEFAULT 'Tunai',
  catatan_pesanan   VARCHAR(255) NULL,
  status            ENUM('Pending','Diproses','Siap Saji','Selesai')
                    NOT NULL DEFAULT 'Pending',
  total_harga       INT NOT NULL DEFAULT 0,
  waktu_pesan       DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  waktu_update      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
                    ON UPDATE CURRENT_TIMESTAMP,
  CONSTRAINT fk_pesanan_meja     FOREIGN KEY (id_meja)
    REFERENCES meja(id_meja)         ON UPDATE CASCADE ON DELETE RESTRICT,
  CONSTRAINT fk_pesanan_pengguna FOREIGN KEY (id_pengguna)
    REFERENCES pengguna(id_pengguna) ON UPDATE CASCADE ON DELETE SET NULL,
  INDEX idx_pesanan_status (status)
) ENGINE=InnoDB;

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
  UPDATE menu
     SET stok     = stok - NEW.jumlah,
         tersedia = IF(stok > 0, tersedia, 0)
   WHERE id_menu = NEW.id_menu;

  UPDATE pesanan
     SET total_harga = total_harga + (NEW.jumlah * NEW.harga_satuan)
   WHERE id_pesanan = NEW.id_pesanan;
END$$

DELIMITER ;

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

INSERT INTO pengguna (nama, username, password_hash, role) VALUES
 ('Admin Restoran', 'admin', SHA2('admin123', 256), 'admin'),
 ('Koki Dapur',     'dapur', SHA2('dapur123', 256), 'dapur');

INSERT INTO meja (nomor_meja, kode_qr) VALUES
 ('01', 'MEJA01-7f3a9c'),
 ('02', 'MEJA02-b81d42'),
 ('03', 'MEJA03-c5e07a'),
 ('04', 'MEJA04-1d9f36'),
 ('05', 'MEJA05-e24b8d');

INSERT INTO menu (nama_menu, deskripsi, kategori, harga, stok, tersedia, gambar) VALUES
 ('Tinutuan',            'Bubur Manado dengan sayuran, ikan asin, dan sambal roa', 'Makanan', 18000, 20, 1, 'Tinutuan.jpg'),
 ('Ayam Rica-Rica',      'Ayam bumbu rica pedas khas Manado',                      'Makanan', 35000, 15, 1, 'ayam_rica.jpg'),
 ('Cakalang Fufu',       'Ikan cakalang asap dengan sambal dabu-dabu',             'Makanan', 38000, 10, 1, 'cakalang_fufu.jpg'),
 ('Nasi Kuning Manado',  'Nasi kuning dengan lauk ikan dan sambal',                'Makanan', 25000, 12, 1, 'Resep_Nasi_Kuning_Asli_Manado.jpg'),
 ('Pisang Goroho Goreng','Pisang goroho goreng dengan sambal roa',                 'Camilan', 15000, 25, 1, 'pisang_goroho.jpg'),
 ('Es Kacang Merah',     'Es serut dengan kacang merah dan susu',                  'Minuman', 15000, 30, 1, 'es_kacang_merah.png'),
 ('Es Teh Manis',        'Teh manis dingin',                                       'Minuman',  8000,  0, 0, 'es_teh_manis.jpg');