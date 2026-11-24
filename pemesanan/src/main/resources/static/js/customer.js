document.addEventListener("DOMContentLoaded", () => {
    const menuItems = document.querySelectorAll(".menu-item");
    const totalItemEl = document.getElementById("total-item");
    const totalHargaEl = document.getElementById("total-harga");
    const btnPesan = document.getElementById("btn-pesan");
    const selectMeja = document.getElementById("select-meja");
    const inputNama = document.getElementById("input-nama");
    const trackingSection = document.getElementById("tracking-section");
    const daftarStatusEl = document.getElementById("daftar-status-pesanan");

    const uiAlertModal = document.getElementById("ui-alert-modal");
    const uiAlertIcon = document.getElementById("ui-alert-icon");
    const uiAlertTitle = document.getElementById("ui-alert-title");
    const uiAlertMessage = document.getElementById("ui-alert-message");
    const uiAlertOk = document.getElementById("ui-alert-ok");
    const toastContainer = document.getElementById("ui-toast-container");

    function showUiAlert(title, message, type = "warning", onClose = null) {
        uiAlertTitle.textContent = title;
        uiAlertMessage.textContent = message;
        uiAlertIcon.className = "ui-modal-icon " + type;
        uiAlertIcon.textContent = type === "danger" ? "✕" : (type === "success" ? "✓" : "!");
        uiAlertModal.style.display = "flex";

        uiAlertOk.onclick = () => {
            uiAlertModal.style.display = "none";
            if (typeof onClose === "function") onClose();
        };
    }

    function showToast(message, type = "success") {
        const toast = document.createElement("div");
        toast.className = `ui-toast ${type}`;
        toast.innerHTML = `<span>${type === "error" ? "⚠️" : "✅"}</span><span>${message}</span>`;
        toastContainer.appendChild(toast);

        setTimeout(() => {
            toast.style.opacity = "0";
            toast.style.transition = "opacity 0.25s ease";
            setTimeout(() => toast.remove(), 250);
        }, 3200);
    }

    // Elemen Modal Konfirmasi
    const modalKonfirmasi = document.getElementById("modal-konfirmasi");
    const konfirmasiInfoEl = document.getElementById("konfirmasi-info-pemesan");
    const konfirmasiListEl = document.getElementById("konfirmasi-list-item");
    const selectPembayaran = document.getElementById("select-pembayaran");
    const inputCatatanUmum = document.getElementById("input-catatan-umum");
    const konfirmasiTotalEl = document.getElementById("konfirmasi-total-harga");
    const btnBatalKonfirmasi = document.getElementById("btn-batal-konfirmasi");
    const btnSubmitKonfirmasi = document.getElementById("btn-submit-konfirmasi");

    // Elemen Modal Struk
    const modalStruk = document.getElementById("modal-struk");
    const strukMetaEl = document.getElementById("struk-meta");
    const strukItemsEl = document.getElementById("struk-items");
    const strukFooterEl = document.getElementById("struk-footer");
    const btnCetakStruk = document.getElementById("btn-cetak-struk");
    const btnTutupStruk = document.getElementById("btn-tutup-struk");

    let draftPesanan = null;

    function getStorageKeyNama(kodeQrOverride = "") {
        const kodeQr = kodeQrOverride || getKodeQrAktif();
        return kodeQr ? `nama_pelanggan_${kodeQr}` : "nama_pelanggan_umum";
    }

    function pulihkanNamaPelanggan(kodeQrOverride = "") {
        const savedNama = sessionStorage.getItem(getStorageKeyNama(kodeQrOverride))
            || sessionStorage.getItem("nama_pelanggan_umum")
            || "";
        if (savedNama && !inputNama.value.trim()) {
            inputNama.value = savedNama;
        }
    }

    inputNama.addEventListener("input", () => {
        const val = inputNama.value.trim();
        const key = getStorageKeyNama();
        if (val) {
            sessionStorage.setItem(key, val);
            sessionStorage.setItem("nama_pelanggan_umum", val);
        }
    });

    function formatRupiah(angka) {
        return "Rp " + angka.toLocaleString("id-ID");
    }

    function hitungTotal() {
        let totalItem = 0;
        let totalHarga = 0;

        menuItems.forEach(item => {
            const harga = parseInt(item.dataset.harga, 10) || 0;
            const qty = parseInt(item.querySelector(".qty-val").textContent, 10) || 0;
            totalItem += qty;
            totalHarga += harga * qty;
        });

        totalItemEl.textContent = totalItem;
        totalHargaEl.textContent = formatRupiah(totalHarga);
    }

    // Kontrol tombol
    menuItems.forEach(item => {
        const btnMinus = item.querySelector(".btn-minus");
        const btnPlus = item.querySelector(".btn-plus");
        const qtyVal = item.querySelector(".qty-val");
        const stokMax = parseInt(item.dataset.stok, 10) || 0;

        if (stokMax <= 0) {
            btnPlus.disabled = true;
            btnMinus.disabled = true;
        }

        btnPlus.addEventListener("click", () => {
            let qty = parseInt(qtyVal.textContent, 10) || 0;
            if (qty < stokMax) {
                qty++;
                qtyVal.textContent = qty;
                hitungTotal();
                if (qty >= stokMax) btnPlus.disabled = true;
            }
        });

        btnMinus.addEventListener("click", () => {
            let qty = parseInt(qtyVal.textContent, 10) || 0;
            if (qty > 0) {
                qty--;
                qtyVal.textContent = qty;
                hitungTotal();
                if (qty < stokMax) btnPlus.disabled = false;
            }
        });
    });

    // Filter Kategori Menu
    const filterButtons = document.querySelectorAll(".btn-filter-kat");
    filterButtons.forEach(btn => {
        btn.addEventListener("click", () => {
            filterButtons.forEach(b => b.classList.remove("active"));
            btn.classList.add("active");

            const kategoriDipilih = btn.dataset.kategori.toLowerCase();
            menuItems.forEach(item => {
                const katItem = (item.dataset.kategori || "").toLowerCase();
                if (kategoriDipilih === "semua" || katItem === kategoriDipilih) {
                    item.style.display = "flex";
                } else {
                    item.style.display = "none";
                }
            });
        });
    });

    // Live Tracking Status Pesanan Meja
    function getKodeQrAktif() {
        if (trackingSection && trackingSection.dataset.kodeQr) {
            return trackingSection.dataset.kodeQr;
        }
        const selectedOpt = selectMeja.options[selectMeja.selectedIndex];
        return selectedOpt ? (selectedOpt.dataset.kode || "") : "";
    }

    function formatLabelStatus(status) {
        if (status === "baru") return { teks: "Menunggu Konfirmasi Dapur", warna: "#d97706", bg: "#fef3c7" };
        if (status === "diproses") return { teks: "Sedang Dimasak Dapur", warna: "#2563eb", bg: "#dbeafe" };
        if (status === "selesai") return { teks: "Selesai / Siap Diantar", warna: "#16a34a", bg: "#dcfce7" };
        return { teks: status, warna: "#4b5563", bg: "#f3f4f6" };
    }

    async function muatStatusPesanan() {
        const kodeQr = getKodeQrAktif();
        if (!kodeQr) {
            daftarStatusEl.innerHTML = `<p style="color: #6b7280; font-size: 14px;">Pilih nomor meja di atas untuk melihat status pesanan Anda.</p>`;
            return;
        }

        try {
            const res = await fetch(`/api/meja/${kodeQr}/pesanan`);
            if (!res.ok) return;

            const data = await res.json();

            if (!data.pesanan || data.pesanan.length === 0) {
                daftarStatusEl.innerHTML = `<p style="color: #6b7280; font-size: 14px;">Belum ada pesanan aktif untuk Meja ${data.nomor_meja} pada sesi ini.</p>`;
                return;
            }

            daftarStatusEl.innerHTML = data.pesanan.map(p => {
                const badge = formatLabelStatus(p.status);
                const listItems = p.items.map(i =>
                    `<li>${i.nama_menu} <strong>x${i.jumlah}</strong></li>`
                ).join("");

                return `
                    <div style="border: 1px solid #e5e7eb; border-radius: 8px; padding: 12px; margin-bottom: 10px; background: #fff;">
                        <div style="display: flex; justify-content: space-between; align-items: center; margin-bottom: 6px; flex-wrap: wrap; gap: 6px;">
                            <strong>Order #${p.id_pesanan} - ${p.nama_pelanggan}</strong>
                            <span style="padding: 4px 10px; border-radius: 999px; font-size: 12px; font-weight: 600; color: ${badge.warna}; background: ${badge.bg};">
                                ${badge.teks}
                            </span>
                        </div>
                        <ul style="margin: 6px 0 8px 18px; font-size: 14px;">${listItems}</ul>
                        <div style="font-size: 12px; color: #374151; margin-bottom: 4px;">
                            Pembayaran: <strong>${p.metode_pembayaran}</strong>
                            ${p.catatan_pesanan ? ` • Catatan: <em>"${p.catatan_pesanan}"</em>` : ""}
                        </div>
                        <div style="font-size: 12px; color: #6b7280;">
                            Dipesan: ${p.waktu_pesan} • Update: ${p.waktu_update}
                        </div>
                    </div>
                `;
            }).join("");
        } catch (err) {
            console.error("Gagal memuat status pesanan:", err);
        }
    }

    pulihkanNamaPelanggan();
    selectMeja.addEventListener("change", () => {
        pulihkanNamaPelanggan();
        muatStatusPesanan();
    });
    muatStatusPesanan();
    setInterval(muatStatusPesanan, 5000);

    btnPesan.addEventListener("click", () => {
        const idMeja = selectMeja.value;
        const namaPelanggan = inputNama.value.trim();

        if (!idMeja) {
            showUiAlert("Nomor Meja Belum Dipilih", "Silakan pilih nomor meja Anda terlebih dahulu sebelum memesan.", "warning", () => {
                selectMeja.focus();
            });
            return;
        }

        if (!namaPelanggan) {
            showUiAlert("Nama Pelanggan Kosong", "Silakan masukkan nama Anda terlebih dahulu.", "warning", () => {
                inputNama.focus();
            });
            return;
        }

        const items = [];
        let totalHarga = 0;

        menuItems.forEach(item => {
            const qty = parseInt(item.querySelector(".qty-val").textContent, 10) || 0;
            if (qty > 0) {
                const idMenu = parseInt(item.dataset.id, 10);
                const namaMenu = item.dataset.nama;
                const harga = parseInt(item.dataset.harga, 10);
                const subtotal = harga * qty;

                totalHarga += subtotal;
                items.push({
                    id_menu: idMenu,
                    nama_menu: namaMenu,
                    harga: harga,
                    jumlah: qty,
                    subtotal: subtotal,
                    catatan: ""
                });
            }
        });

        if (items.length === 0) {
            showUiAlert("Keranjang Masih Kosong", "Pilih minimal 1 menu makanan atau minuman terlebih dahulu.", "warning");
            return;
        }

        const selectedOpt = selectMeja.options[selectMeja.selectedIndex];
        const teksMeja = selectedOpt.text;
        const kodeQrMeja = selectedOpt.dataset.kode || getKodeQrAktif();

        draftPesanan = {
            id_meja: parseInt(idMeja, 10),
            teks_meja: teksMeja,
            kode_qr: kodeQrMeja,
            nama_pelanggan: namaPelanggan,
            total_harga: totalHarga,
            items: items
        };

        konfirmasiInfoEl.innerHTML = `Pemesan: <strong>${namaPelanggan}</strong> • <strong>${teksMeja}</strong>`;
        konfirmasiListEl.innerHTML = items.map(i => `
            <li style="margin-bottom: 6px;">
                <strong>${i.nama_menu}</strong> x${i.jumlah} — ${formatRupiah(i.subtotal)}
            </li>
        `).join("");
        konfirmasiTotalEl.textContent = formatRupiah(totalHarga);

        modalKonfirmasi.style.display = "flex";
    });

    btnBatalKonfirmasi.addEventListener("click", () => {
        modalKonfirmasi.style.display = "none";
    });

    // Langkah 2: Submit dari Modal Konfirmasi -> Simpan ke DB & Munculkan Struk
    btnSubmitKonfirmasi.addEventListener("click", async () => {
        if (!draftPesanan) return;

        const metodePembayaran = selectPembayaran.value;
        const catatanPesanan = inputCatatanUmum.value.trim();

        btnSubmitKonfirmasi.disabled = true;
        btnSubmitKonfirmasi.textContent = "Memproses...";

        try {
            const response = await fetch("/api/pesanan", {
                method: "POST",
                headers: { "Content-Type": "application/json" },
                body: JSON.stringify({
                    id_meja: draftPesanan.id_meja,
                    nama_pelanggan: draftPesanan.nama_pelanggan,
                    total_harga: draftPesanan.total_harga,
                    metode_pembayaran: metodePembayaran,
                    catatan_pesanan: catatanPesanan,
                    items: draftPesanan.items
                })
            });

            const result = await response.json();

            if (response.ok) {
                sessionStorage.setItem(getStorageKeyNama(draftPesanan.kode_qr), draftPesanan.nama_pelanggan);
                sessionStorage.setItem("nama_pelanggan_umum", draftPesanan.nama_pelanggan);

                modalKonfirmasi.style.display = "none";
                showToast("Pesanan berhasil dikirim ke dapur!", "success");

                strukMetaEl.innerHTML = `
                    <div>No. Order : <strong>#${result.id_pesanan}</strong></div>
                    <div>Waktu     : ${result.waktu_pesan}</div>
                    <div>Meja      : ${draftPesanan.teks_meja}</div>
                    <div>Pelanggan : ${draftPesanan.nama_pelanggan}</div>
                `;

                strukItemsEl.innerHTML = draftPesanan.items.map(i => `
                    <div style="margin-bottom: 4px;">
                        <div style="display: flex; justify-content: space-between;">
                            <span>${i.nama_menu} x${i.jumlah}</span>
                            <span>${formatRupiah(i.subtotal)}</span>
                        </div>
                    </div>
                `).join("");

                strukFooterEl.innerHTML = `
                    <div style="display: flex; justify-content: space-between; font-weight: bold; font-size: 14px;">
                        <span>TOTAL</span>
                        <span>${formatRupiah(draftPesanan.total_harga)}</span>
                    </div>
                    <div>Pembayaran: <strong>${result.metode_pembayaran}</strong></div>
                    ${catatanPesanan ? `<div>Catatan   : ${catatanPesanan}</div>` : ""}
                `;

                modalStruk.style.display = "flex";
                muatStatusPesanan();
            } else {
                showUiAlert("Pesanan Gagal", result.message || "Gagal menyimpan pesanan.", "danger");
            }
        } catch (error) {
            console.error(error);
            showUiAlert("Kesalahan Koneksi", "Tidak dapat terhubung ke server.", "danger");
        } finally {
            btnSubmitKonfirmasi.disabled = false;
            btnSubmitKonfirmasi.textContent = "Konfirmasi & Buat Struk";
        }
    });

    btnCetakStruk.addEventListener("click", () => {
        window.print();
    });

    btnTutupStruk.addEventListener("click", () => {
        modalStruk.style.display = "none";
        if (draftPesanan && draftPesanan.kode_qr && !window.location.pathname.includes("/meja/")) {
            window.location.href = `/meja/${draftPesanan.kode_qr}`;
        } else {
            window.location.reload();
        }
    });
});