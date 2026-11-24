package com.resto.pemesanan.controller;

import com.resto.pemesanan.model.Detail_pesanan;
import com.resto.pemesanan.model.Meja;
import com.resto.pemesanan.model.Menu;
import com.resto.pemesanan.model.Pengguna;
import com.resto.pemesanan.model.Pesanan;
import com.resto.pemesanan.repository.Detail_repository;
import com.resto.pemesanan.repository.Meja_repository;
import com.resto.pemesanan.repository.Menu_repository;
import com.resto.pemesanan.repository.Pesanan_repository;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class ApiController {

    @Autowired
    private Pesanan_repository pesananRepository;

    @Autowired
    private Detail_repository detailRepository;

    @Autowired
    private Menu_repository menuRepository;

    @Autowired
    private Meja_repository mejaRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private String getWaktuSekarang() {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        return LocalDateTime.now().format(formatter);
    }

    private boolean isTokoBuka() {
        try {
            jdbcTemplate.execute("CREATE TABLE IF NOT EXISTS status_operasional (id INT PRIMARY KEY, is_buka BOOLEAN)");
            jdbcTemplate.execute("INSERT IGNORE INTO status_operasional (id, is_buka) VALUES (1, TRUE)");
            Boolean status = jdbcTemplate.queryForObject("SELECT is_buka FROM status_operasional WHERE id = 1", Boolean.class);
            return Boolean.TRUE.equals(status);
        } catch (Exception e) {
            return true;
        }
    }

    public static class ItemRequest {
        public Integer id_menu;
        public Integer jumlah;
        public Integer subtotal;
        public String catatan;
    }

    public static class PesananRequest {
        public Integer id_meja;
        public String nama_pelanggan;
        public Integer total_harga;
        public String metode_pembayaran;
        public String catatan_pesanan;
        public List<ItemRequest> items;
    }

    // 1. Simpan Pesanan Baru + Cek Status Buka + Metode Pembayaran + Catatan + Potong Stok
    @PostMapping("/pesanan")
    public ResponseEntity buatPesanan(@RequestBody PesananRequest req) {
        if (!isTokoBuka()) {
            return ResponseEntity.badRequest().body(Map.of("message", "Maaf, pemesanan saat ini sedang tutup."));
        }

        if (req.items == null || req.items.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("message", "Keranjang pesanan kosong!"));
        }

        for (ItemRequest item : req.items) {
            Menu menu = menuRepository.findById(item.id_menu)
                    .orElseThrow(() -> new RuntimeException("Menu tidak ditemukan"));

            int stokSekarang = (menu.getStok() != null) ? menu.getStok() : 0;
            if (!Boolean.TRUE.equals(menu.getTersedia()) || stokSekarang < item.jumlah) {
                return ResponseEntity.badRequest().body(Map.of(
                        "message", "Stok " + menu.getNama_menu() + " tidak mencukupi (sisa: " + stokSekarang + ")"
                ));
            }
        }

        String waktuSekarang = getWaktuSekarang();

        Pesanan pesanan = new Pesanan();
        pesanan.setId_meja(req.id_meja);
        pesanan.setNama_pelanggan(req.nama_pelanggan);
        pesanan.setTotal_harga(req.total_harga);
        pesanan.setStatus("baru");
        pesanan.setWaktu_pesan(waktuSekarang);
        pesanan.setWaktu_update(waktuSekarang);
        pesanan.setMetode_pembayaran(req.metode_pembayaran != null ? req.metode_pembayaran : "Tunai");
        pesanan.setCatatan_pesanan(req.catatan_pesanan != null ? req.catatan_pesanan : "");

        Pesanan savedPesanan = pesananRepository.save(pesanan);

        for (ItemRequest item : req.items) {
            Menu menu = menuRepository.findById(item.id_menu)
                    .orElseThrow(() -> new RuntimeException("Menu tidak ditemukan"));

            Detail_pesanan detail = new Detail_pesanan();
            detail.setId_pesanan(savedPesanan.getId_pesanan());
            detail.setId_menu(item.id_menu);
            detail.setJumlah(item.jumlah);
            detail.setHarga_satuan(menu.getHarga());
            detail.setHarga(menu.getHarga());
            detail.setCatatan(item.catatan != null ? item.catatan : "");
            detailRepository.save(detail);

            int sisaStok = menu.getStok() - item.jumlah;
            menu.setStok(Math.max(sisaStok, 0));
            menu.setTersedia(menu.getStok() > 0);
            menuRepository.save(menu);
        }

        HashMap res = new HashMap();
        res.put("message", "Pesanan berhasil dikirim!");
        res.put("id_pesanan", savedPesanan.getId_pesanan());
        res.put("waktu_pesan", waktuSekarang);
        res.put("metode_pembayaran", pesanan.getMetode_pembayaran());
        res.put("catatan_pesanan", pesanan.getCatatan_pesanan());
        return ResponseEntity.ok(res);
    }

    // 2. Update Status Pesanan di Kanban
    @PutMapping("/pesanan/{id}/status")
    public ResponseEntity updateStatusPesanan(@PathVariable Integer id,
                                              @RequestBody Map<String, String> body,
                                              HttpSession session) {
        Pesanan pesanan = pesananRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Pesanan tidak ditemukan"));

        pesanan.setStatus(body.get("status"));
        pesanan.setWaktu_update(getWaktuSekarang());

        Pengguna staf = (Pengguna) session.getAttribute("stafLogin");
        if (staf != null) {
            pesanan.setId_pengguna(staf.getId_pengguna());
        }

        pesananRepository.save(pesanan);
        return ResponseEntity.ok(Map.of(
                "message", "Status pesanan diperbarui",
                "waktu_update", pesanan.getWaktu_update()
        ));
    }

    // 3. Update Jumlah Stok Menu dari Dashboard Staf
    @PutMapping("/menu/{id}/stok")
    public ResponseEntity updateStatusMenu(@PathVariable Integer id, @RequestBody Map<String, Integer> body) {
        Menu menu = menuRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Menu tidak ditemukan"));

        int stokBaru = Math.max(body.getOrDefault("stok", 0), 0);
        menu.setStok(stokBaru);
        menu.setTersedia(stokBaru > 0);
        menuRepository.save(menu);

        return ResponseEntity.ok(Map.of(
                "message", "Stok menu diperbarui",
                "stok", menu.getStok(),
                "tersedia", menu.getTersedia()
        ));
    }

    // 4. Ambil Daftar & Status Pesanan Terbaru Berdasarkan Kode Unik Meja (kode_qr)
    @GetMapping("/meja/{kodeQr}/pesanan")
    public ResponseEntity getPesananByMeja(@PathVariable String kodeQr) {
        Meja meja = mejaRepository.findAll().stream()
                .filter(m -> m.getKode_qr() != null && m.getKode_qr().equalsIgnoreCase(kodeQr))
                .findFirst()
                .orElse(null);

        if (meja == null) {
            return ResponseEntity.notFound().build();
        }

        List<Menu> semuaMenu = menuRepository.findAll();
        HashMap mapNamaMenu = new HashMap();
        for (Menu m : semuaMenu) {
            mapNamaMenu.put(m.getId_menu(), m.getNama_menu());
        }

        List<Detail_pesanan> semuaDetail = detailRepository.findAll();
        List<Pesanan> pesananMeja = pesananRepository.findAll().stream()
                .filter(p -> p.getId_meja() != null && p.getId_meja().equals(meja.getId_meja()))
                .sorted((a, b) -> Integer.compare(b.getId_pesanan(), a.getId_pesanan()))
                .toList();

        ArrayList hasil = new ArrayList();
        for (Pesanan p : pesananMeja) {
            ArrayList rincian = new ArrayList();
            for (Detail_pesanan d : semuaDetail) {
                if (d.getId_pesanan() == p.getId_pesanan()) {
                    HashMap itemMap = new HashMap();
                    itemMap.put("nama_menu", mapNamaMenu.getOrDefault(d.getId_menu(), "Menu #" + d.getId_menu()));
                    itemMap.put("jumlah", d.getJumlah());
                    itemMap.put("catatan", d.getCatatan() != null ? d.getCatatan() : "");
                    rincian.add(itemMap);
                }
            }

            HashMap pesananMap = new HashMap();
            pesananMap.put("id_pesanan", p.getId_pesanan());
            pesananMap.put("nama_pelanggan", p.getNama_pelanggan());
            pesananMap.put("status", p.getStatus());
            pesananMap.put("total_harga", p.getTotal_harga());
            pesananMap.put("metode_pembayaran", p.getMetode_pembayaran() != null ? p.getMetode_pembayaran() : "Tunai");
            pesananMap.put("catatan_pesanan", p.getCatatan_pesanan() != null ? p.getCatatan_pesanan() : "");
            pesananMap.put("waktu_pesan", p.getWaktu_pesan() != null ? p.getWaktu_pesan() : "-");
            pesananMap.put("waktu_update", p.getWaktu_update() != null ? p.getWaktu_update() : "-");
            pesananMap.put("items", rincian);
            hasil.add(pesananMap);
        }

        HashMap response = new HashMap();
        response.put("nomor_meja", meja.getNomor_meja());
        response.put("kode_qr", meja.getKode_qr());
        response.put("pesanan", hasil);

        return ResponseEntity.ok(response);
    }

    // 5A. Buka Layanan Pemesanan
    @RequestMapping(value = "/sesi/buka", method = {RequestMethod.POST, RequestMethod.GET})
    public ResponseEntity bukaSesi() {
        try {
            jdbcTemplate.execute("CREATE TABLE IF NOT EXISTS status_operasional (id INT PRIMARY KEY, is_buka BOOLEAN)");
            jdbcTemplate.execute("INSERT IGNORE INTO status_operasional (id, is_buka) VALUES (1, TRUE)");
            jdbcTemplate.update("UPDATE status_operasional SET is_buka = TRUE WHERE id = 1");

            return ResponseEntity.ok(Map.of(
                    "message", "Layanan berhasil dibuka! Pelanggan sekarang dapat memesan."
            ));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of(
                    "message", "Gagal membuka layanan: " + e.getMessage()
            ));
        }
    }

    // 5B. Tutup Layanan Pemesanan (Ditolak jika masih ada pesanan belum selesai)
    @RequestMapping(value = "/sesi/tutup", method = {RequestMethod.POST, RequestMethod.DELETE, RequestMethod.GET})
    public ResponseEntity tutupSesiShift() {
        try {
            long pesananBelumSelesai = pesananRepository.findAll().stream()
                    .filter(p -> p.getStatus() == null || !p.getStatus().equalsIgnoreCase("selesai"))
                    .count();

            if (pesananBelumSelesai > 0) {
                return ResponseEntity.badRequest().body(Map.of(
                        "message", "Tidak dapat menutup! Masih ada " + pesananBelumSelesai + " pesanan yang belum selesai diproses dapur."
                ));
            }

            jdbcTemplate.execute("CREATE TABLE IF NOT EXISTS status_operasional (id INT PRIMARY KEY, is_buka BOOLEAN)");
            jdbcTemplate.execute("INSERT IGNORE INTO status_operasional (id, is_buka) VALUES (1, FALSE)");
            jdbcTemplate.update("UPDATE status_operasional SET is_buka = FALSE WHERE id = 1");

            jdbcTemplate.execute("DELETE FROM detail_pesanan");
            jdbcTemplate.execute("DELETE FROM pesanan");
            jdbcTemplate.execute("ALTER TABLE pesanan AUTO_INCREMENT = 1");

            return ResponseEntity.ok(Map.of(
                    "message", "Layanan berhasil ditutup! Seluruh riwayat pesanan telah dibersihkan."
            ));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().body(Map.of(
                    "message", "Gagal menutup layanan: " + e.getMessage()
            ));
        }
    }
}