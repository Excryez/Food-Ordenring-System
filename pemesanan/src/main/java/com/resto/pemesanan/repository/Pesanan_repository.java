package com.resto.pemesanan.repository;


import com.resto.pemesanan.model.Pesanan;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

public interface Pesanan_repository extends JpaRepository<Pesanan, Long> {
}