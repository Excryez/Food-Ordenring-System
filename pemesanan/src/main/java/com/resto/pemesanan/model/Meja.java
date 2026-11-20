package com.resto.pemesanan.model;

import jakarta.persistence.*;

@Entity
@Table(name = "meja")
public class Meja {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)

    private Integer id_meja;
    private Integer nomor_meja;
    private String kode_qr;

    public Integer getId_meja() {
        return id_meja;
    }

    public void setId_meja(Integer id_meja) {
        this.id_meja = id_meja;
    }

    public Integer getNomor_meja() {
        return nomor_meja;
    }

    public void setNomor_meja(Integer nomor_meja) {
        this.nomor_meja = nomor_meja;
    }

    public String getKode_qr() {
        return kode_qr;
    }

    public void setKode_qr(String kode_qr) {
        this.kode_qr = kode_qr;
    }
}
