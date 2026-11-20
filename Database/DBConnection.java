package config; // sesuaikan dengan nama package project temanmu

import java.io.FileInputStream;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

/**
 * Koneksi ke database food_reservation (MySQL via JDBC).
 *
 * Pengaturan host/user/password dibaca dari file "db.properties" di folder
 * root project, jadi tiap anggota tim bisa beda setting TANPA ubah kode.
 * Kalau file tidak ada, dipakai default Laragon (localhost, root, tanpa password).
 *
 * Pemakaian:
 *   try (Connection conn = DBConnection.getConnection()) {
 *       // ... query di sini
 *   }
 */
public final class DBConnection {

    private static final Properties PROPS = new Properties();

    static {
        try (FileInputStream in = new FileInputStream("db.properties")) {
            PROPS.load(in);
        } catch (IOException e) {
            System.out.println("[DB] db.properties tidak ditemukan, pakai default localhost.");
        }
    }

    private DBConnection() { }

    public static Connection getConnection() throws SQLException {
        String host = PROPS.getProperty("db.host", "localhost");
        String port = PROPS.getProperty("db.port", "3306");
        String name = PROPS.getProperty("db.name", "food_reservation");
        String user = PROPS.getProperty("db.user", "root");
        String pass = PROPS.getProperty("db.password", "");

        String url = "jdbc:mysql://" + host + ":" + port + "/" + name
                + "?sslMode=DISABLED&allowPublicKeyRetrieval=true&serverTimezone=Asia/Makassar";

        return DriverManager.getConnection(url, user, pass);
    }

    /** Jalankan file ini untuk mengetes koneksi. */
    public static void main(String[] args) {
        try (Connection conn = getConnection()) {
            System.out.println("[DB] Koneksi BERHASIL ke: " + conn.getCatalog());
        } catch (SQLException e) {
            System.out.println("[DB] Koneksi GAGAL: " + e.getMessage());
        }
    }
}
