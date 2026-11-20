package com.resto.pemesanan.controller;

import com.resto.pemesanan.repository.Menu_repository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class PageController {

    @Autowired
    private Menu_repository  Menu_repository;

    @GetMapping("/")
    public String halamanPelanggan(Model model) {
        // Mengambil seluruh data dari tabel menu MySQL ke variabel "daftarMenu"
        model.addAttribute("daftarMenu",  Menu_repository.findAll());

        // Membuka file src/main/resources/templates/Index.html
        return "index";
    }

    @GetMapping("/login")
    public String halamanLogin() {
        return "login";
    }

    @GetMapping("/dashboard")
    public String halamanAdmin(Model model) {
        model.addAttribute("daftarMenu", Menu_repository.findAll());
        return "dashboard";
    }
}