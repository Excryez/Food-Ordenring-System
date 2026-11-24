package com.resto.pemesanan.controller;

import com.resto.pemesanan.model.Detail_pesanan;
import com.resto.pemesanan.model.Meja;
import com.resto.pemesanan.model.Menu;
import com.resto.pemesanan.model.Pengguna;
import com.resto.pemesanan.model.Pesanan;
import com.resto.pemesanan.repository.*;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.*;

@Controller
public class PageController {

    @Autowired
    private Menu_repository menuRepository;

    @Autowired
    private Meja_repository mejaRepository;

    @Autowired
    private Pesanan_repository pesananRepository;

    @Autowired
    private Detail_repository detailRepository;

    @Autowired
    private Pengguna_repository penggunaRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

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

    private String hashSHA256(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder();
            for (byte b : hashBytes) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @GetMapping("/")
    public String halamanPelanggan(Model model) {
        model.addAttribute("daftarMenu", menuRepository.findAll());
        model.addAttribute("daftarMeja", mejaRepository.findAll());
        model.addAttribute("mejaTerpilih", null);
        model.addAttribute("isBuka", isTokoBuka());
        return "index";
    }

    @GetMapping("/meja/{kodeQr}")
    public String halamanPelangganByQr(@PathVariable String kodeQr, Model model) {
        Meja meja = mejaRepository.findAll().stream()
                .filter(m -> m.getKode_qr() != null && m.getKode_qr().equalsIgnoreCase(kodeQr))
                .findFirst()
                .orElse(null);

        if (meja == null) {
            return "redirect:/";
        }

        model.addAttribute("daftarMenu", menuRepository.findAll());
        model.addAttribute("daftarMeja", mejaRepository.findAll());
        model.addAttribute("mejaTerpilih", meja);
        model.addAttribute("isBuka", isTokoBuka());
        return "index";
    }

    @GetMapping("/login")
    public String halamanLogin(HttpSession session) {
        if (session.getAttribute("stafLogin") != null) {
            return "redirect:/dashboard";
        }
        return "login";
    }

    @PostMapping("/login")
    public String prosesLogin(@RequestParam String username,
                              @RequestParam String password,
                              HttpSession session,
                              Model model) {
        String hashedInput = hashSHA256(password);

        Pengguna staf = penggunaRepository.findAll().stream()
                .filter(u -> u.getUsername().equals(username) && u.getPassword_hash().equals(hashedInput))
                .findFirst()
                .orElse(null);

        if (staf != null) {
            session.setAttribute("stafLogin", staf);
            return "redirect:/dashboard";
        }

        model.addAttribute("error", "Username atau password salah!");
        return "login";
    }

    @GetMapping("/logout")
    public String logout(HttpSession session) {
        session.invalidate();
        return "redirect:/login";
    }

    @GetMapping("/dashboard")
    public String halamanAdmin(HttpSession session, Model model) {
        Pengguna staf = (Pengguna) session.getAttribute("stafLogin");
        if (staf == null) {
            return "redirect:/login";
        }

        List<Pesanan> semuaPesanan = pesananRepository.findAll();
        List<Detail_pesanan> semuaDetail = detailRepository.findAll();
        List<Menu> semuaMenu = menuRepository.findAll();
        List<Meja> semuaMeja = mejaRepository.findAll();

        Map<Integer, String> mapNamaMenu = new HashMap<>();
        for (Menu m : semuaMenu) {
            mapNamaMenu.put(m.getId_menu(), m.getNama_menu());
        }

        Map<Integer, Integer> mapNomorMeja = new HashMap<>();
        for (Meja mj : semuaMeja) {
            mapNomorMeja.put(mj.getId_meja(), mj.getNomor_meja());
        }

        Map<Integer, List<Detail_pesanan>> mapDetailPesanan = new HashMap<>();
        for (Detail_pesanan d : semuaDetail) {
            mapDetailPesanan.computeIfAbsent(d.getId_pesanan(), k -> new ArrayList<>()).add(d);
        }

        model.addAttribute("staf", staf);
        model.addAttribute("daftarPesanan", semuaPesanan);
        model.addAttribute("daftarMenu", semuaMenu);
        model.addAttribute("mapNamaMenu", mapNamaMenu);
        model.addAttribute("mapNomorMeja", mapNomorMeja);
        model.addAttribute("mapDetailPesanan", mapDetailPesanan);
        model.addAttribute("isBuka", isTokoBuka());

        return "dashboard";
    }
    @GetMapping("/qr-meja")
    public String halamanQrMeja(HttpSession session, Model model) {
        Pengguna staf = (Pengguna) session.getAttribute("stafLogin");
        if (staf == null) {
            return "redirect:/login";
        }

        List<Meja> semuaMeja = mejaRepository.findAll().stream()
                .sorted(Comparator.comparing(Meja::getNomor_meja))
                .toList();

        model.addAttribute("staf", staf);
        model.addAttribute("daftarMeja", semuaMeja);
        return "qr-meja";
    }
}