package com.resto.pemesanan.repository;

import com.resto.pemesanan.model.Detail_pesanan;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

public interface Detail_repository extends JpaRepository<Detail_pesanan, Long> {
}