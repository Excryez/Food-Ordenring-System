package com.resto.pemesanan.repository;

import com.resto.pemesanan.model.Meja;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

public interface Meja_repository extends JpaRepository<Meja, Long> {
}