package com.resto.pemesanan.repository;

import com.resto.pemesanan.model.Pesanan;
import org.springframework.data.jpa.repository.JpaRepository;

public interface Pesanan_repository extends JpaRepository<Pesanan, Integer> {
}