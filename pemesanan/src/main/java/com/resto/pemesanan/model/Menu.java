package com.resto.pemesanan.model;

import jakarta.persistence.*;

@Entity
@Table(name = "menu")
public class Menu {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id_menu;

    private String nama_menu;
    private String kategori;
    private Integer harga;
    private Integer stok;
    private String deskripsi;
    private Boolean tersedia;
    private String gambar;

    public Integer getId_menu() { return id_menu; }
    public void setId_menu(Integer id_menu) { this.id_menu = id_menu; }

    public String getNama_menu() { return nama_menu; }
    public void setNama_menu(String nama_menu) { this.nama_menu = nama_menu; }

    public String getKategori() { return kategori; }
    public void setKategori(String kategori) { this.kategori = kategori; }

    public Integer getHarga() { return harga; }
    public void setHarga(Integer harga) { this.harga = harga; }

    public Integer getStok() { return stok; }
    public void setStok(Integer stok) { this.stok = stok; }

    public String getDeskripsi() { return deskripsi; }
    public void setDeskripsi(String deskripsi) { this.deskripsi = deskripsi; }

    public Boolean getTersedia() { return tersedia; }
    public void setTersedia(Boolean tersedia) { this.tersedia = tersedia; }

    public String getGambar() { return gambar; }
    public void setGambar(String gambar) { this.gambar = gambar; }
}