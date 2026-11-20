package com.resto.pemesanan.repository;

import com.resto.pemesanan.model.Pengguna;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

public interface Pengguna_repository extends JpaRepository<Pengguna, Long> {
}