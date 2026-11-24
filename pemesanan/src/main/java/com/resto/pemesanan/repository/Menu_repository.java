package com.resto.pemesanan.repository;

import com.resto.pemesanan.model.Menu;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;


public interface Menu_repository extends JpaRepository<Menu, Integer> {
}