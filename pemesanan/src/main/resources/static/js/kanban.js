document.addEventListener("DOMContentLoaded", () => {
    // 1. Update Status Pesanan di Papan Kanban (Baru -> Diproses -> Selesai)
    document.querySelectorAll(".btn-status").forEach(btn => {
        btn.addEventListener("click", async () => {
            const idPesanan = btn.dataset.id;
            const statusBaru = btn.dataset.status;

            btn.disabled = true;
            btn.textContent = "Memproses...";

            try {
                const res = await fetch(`/api/pesanan/${idPesanan}/status`, {
                    method: "PUT",
                    headers: { "Content-Type": "application/json" },
                    body: JSON.stringify({ status: statusBaru })
                });

                if (res.ok) {
                    window.location.reload();
                } else {
                    alert("Gagal memperbarui status pesanan.");
                    btn.disabled = false;
                }
            } catch (err) {
                console.error(err);
                alert("Terjadi kesalahan koneksi ke server.");
                btn.disabled = false;
            }
        });
    });

    document.querySelectorAll(".btn-simpan-stok").forEach(btn => {
        btn.addEventListener("click", async () => {
            const idMenu = btn.dataset.id;
            const inputStok = document.getElementById(`stok-${idMenu}`);
            const stokBaru = parseInt(inputStok.value, 10);

            if (isNaN(stokBaru) || stokBaru < 0) {
                alert("Masukkan jumlah stok yang valid (minimal 0).");
                return;
            }

            try {
                const response = await fetch(`/api/menu/${idMenu}/stok`, {
                    method: "PUT",
                    headers: { "Content-Type": "application/json" },
                    body: JSON.stringify({ stok: stokBaru })
                });

                if (response.ok) {
                    window.location.reload();
                } else {
                    alert("Gagal memperbarui stok menu.");
                }
            } catch (error) {
                console.error(error);
                alert("Terjadi kesalahan koneksi ke server.");
            }
        });
    });

    const btnTutupSesi = document.getElementById("btn-tutup-sesi");
    if (btnTutupSesi) {
        btnTutupSesi.addEventListener("click", async () => {
            const yakin = confirm(
                "Tutup sesi / shift sekarang?\n\nPERINGATAN: Seluruh riwayat pesanan pada sesi ini akan dihapus permanen untuk memulai sesi baru."
            );
            if (!yakin) return;

            btnTutupSesi.disabled = true;
            btnTutupSesi.textContent = "Menutup Sesi...";

            try {
                const res = await fetch("/api/sesi/tutup", {
                    method: "POST", // Menggunakan POST agar tidak diblokir browser/server
                    headers: { "Content-Type": "application/json" }
                });

                const data = await res.json().catch(() => ({}));

                if (res.ok) {
                    alert(data.message || "Sesi / Shift berhasil ditutup!");
                    window.location.reload();
                } else if (res.status === 401) {
                    alert("Sesi login Anda sudah habis. Silakan login ulang.");
                    window.location.href = "/login";
                } else {
                    alert("Gagal menutup sesi: " + (data.message || `Error ${res.status}`));
                    btnTutupSesi.disabled = false;
                    btnTutupSesi.textContent = "Tutup Sesi / Shift";
                }
            } catch (err) {
                console.error(err);
                alert("Terjadi kesalahan koneksi ke server.");
                btnTutupSesi.disabled = false;
                btnTutupSesi.textContent = "Tutup Sesi / Shift";
            }
        });
    }
});