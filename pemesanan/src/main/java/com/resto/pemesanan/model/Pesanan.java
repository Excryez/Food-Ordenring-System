package com.resto.pemesanan.model;

import jakarta.persistence.*;

@Entity
@Table(name = "pengguna")
public class Pesanan {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)

    private Integer id_pesanan;
    private Integer id_meja;
    private Integer id_pengguna;
    private String nama_pelanggan;
    private String status;
    private Integer total_harga;
    private String waktu_pesan;
    private String waktu_update;

    public Integer getId_pesanan() {
        return id_pesanan;
    }

    public void setId_pesanan(Integer id_pesanan) {
        this.id_pesanan = id_pesanan;
    }

    public Integer getId_meja() {
        return id_meja;
    }

    public void setId_meja(Integer id_meja) {
        this.id_meja = id_meja;
    }

    public Integer getId_pengguna() {
        return id_pengguna;
    }

    public void setId_pengguna(Integer id_pengguna) {
        this.id_pengguna = id_pengguna;
    }

    public String getNama_pelanggan() {
        return nama_pelanggan;
    }

    public void setNama_pelanggan(String nama_pelanggan) {
        this.nama_pelanggan = nama_pelanggan;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Integer getTotal_harga() {
        return total_harga;
    }

    public void setTotal_harga(Integer total_harga) {
        this.total_harga = total_harga;
    }

    public String getWaktu_pesan() {
        return waktu_pesan;
    }

    public void setWaktu_pesan(String waktu_pesan) {
        this.waktu_pesan = waktu_pesan;
    }

    public String getWaktu_update() {
        return waktu_update;
    }

    public void setWaktu_update(String waktu_update) {
        this.waktu_update = waktu_update;
    }
}
