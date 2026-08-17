<script setup>
import { onMounted, ref } from 'vue';

const props = defineProps({
  apiBaseUrl: { type: String, required: true }
});

const emit = defineEmits(['session-expired', 'close']);

const loading = ref(false);
const saving = ref(false);
const error = ref('');
const dfrom = ref(todayIso());
const dto = ref(todayIso());
const rows = ref([]);

// Dialog hasil simpan
const resultDialogOpen = ref(false);
const resultState = ref('success'); // success | error
const resultMessage = ref('');

function fmtDate(iso) {
  if (!iso) return '';
  const [y, m, d] = iso.split('-');
  return `${d}/${m}/${y}`;
}

function todayIso() {
  const d = new Date();
  const pad = (n) => String(n).padStart(2, '0');
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}`;
}

async function request(path, options = {}) {
  const response = await fetch(`${props.apiBaseUrl}${path}`, {
    credentials: 'include',
    headers: { 'Content-Type': 'application/json', ...(options.headers || {}) },
    ...options
  });
  const payload = await response.json().catch(() => null);
  if (response.status === 401) {
    emit('session-expired', payload?.message || 'Your session has been expired. You need to login again.');
    throw new Error(payload?.message || 'Unauthorized');
  }
  if (!response.ok) throw new Error(payload?.message || `HTTP ${response.status}`);
  return payload.data;
}

async function loadList() {
  loading.value = true;
  error.value = '';
  try {
    rows.value = await request('/accounting/rekap-gl');
  } catch (requestError) {
    error.value = requestError.message;
  } finally {
    loading.value = false;
  }
}

async function save() {
  error.value = '';
  if (!dfrom.value || !dto.value) {
    alert('TANGGAL DARI DAN SAMPAI WAJIB DIISI!');
    return;
  }
  saving.value = true;
  try {
    await request('/accounting/rekap-gl', {
      method: 'POST',
      body: JSON.stringify({ from: dfrom.value, to: dto.value })
    });
    await loadList();
    resultState.value = 'success';
    resultMessage.value = 'Data Successfully Saved!';
    resultDialogOpen.value = true;
  } catch (requestError) {
    resultState.value = 'error';
    resultMessage.value = requestError.message || 'Terjadi kesalahan saat menyimpan data.';
    resultDialogOpen.value = true;
  } finally {
    saving.value = false;
  }
}

function closeResultDialog() {
  resultDialogOpen.value = false;
}

async function downloadFile(item) {
  error.value = '';
  try {
    const response = await fetch(`${props.apiBaseUrl}/accounting/rekap-gl/${item.id}/download`, {
      credentials: 'include'
    });
    if (response.status === 401) {
      emit('session-expired', 'Your session has been expired. You need to login again.');
      return;
    }
    if (!response.ok) {
      const payload = await response.json().catch(() => null);
      throw new Error(payload?.message || `HTTP ${response.status}`);
    }
    const blob = await response.blob();
    const url = URL.createObjectURL(blob);
    const link = document.createElement('a');
    link.href = url;
    link.download = `rekap_gl_${item.from.replace(/\//g, '')}_${item.to.replace(/\//g, '')}.xlsx`;
    document.body.appendChild(link);
    link.click();
    document.body.removeChild(link);
    URL.revokeObjectURL(url);
  } catch (requestError) {
    error.value = requestError.message;
  }
}

onMounted(loadList);
</script>

<template>
  <div class="screen-page">
    <div class="page-header">
      <h2>📋 REKAP GL</h2>
      <p class="page-subtitle">Migrasi screen legacy SC0176 — rekapGl.zul (Rekapitulasi General Ledger)</p>
    </div>

    <p v-if="error" class="status-banner status-banner--error">{{ error }}</p>

    <div class="card">
      <div class="period-bar">
        <span class="period-label">DARI</span>
        <input v-model="dfrom" type="date" />
        <span class="period-label">SAMPAI</span>
        <input v-model="dto" type="date" />
        <button class="small-button primary" type="button" :disabled="saving" @click="save">💾 SIMPAN</button>
      </div>
    </div>

    <div class="card">
      <h3 class="card-title">LIST REKAP GL</h3>
      <div v-if="loading" class="loading">Memuat data...</div>
      <div v-else class="table-wrap">
        <table class="table">
          <thead>
            <tr>
              <th>DARI</th>
              <th>SAMPAI</th>
              <th>FILE</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="item in rows" :key="item.id">
              <td>{{ item.from }}</td>
              <td>{{ item.to }}</td>
              <td>
                <a v-if="item.hasFile" class="download-link" href="javascript:void(0)" @click="downloadFile(item)">DOWNLOAD</a>
                <span v-else class="muted">-</span>
              </td>
            </tr>
            <tr v-if="!rows.length">
              <td colspan="3" class="empty-state">Belum ada data rekap GL. Pilih rentang tanggal lalu tekan SIMPAN.</td>
            </tr>
          </tbody>
        </table>
      </div>
      <div class="action-bar">
        <button class="small-button" type="button" @click="emit('close')">✅ SELESAI</button>
      </div>
    </div>
  </div>

  <!-- MODAL HASIL SIMPAN -->
  <div v-if="resultDialogOpen" class="modal-overlay" @click.self="closeResultDialog">
    <div class="result-modal" role="dialog" aria-modal="true">

      <!-- State: Sukses -->
      <template v-if="resultState === 'success'">
        <div class="result-icon success"><span>✓</span></div>
        <h3 class="result-title">Berhasil!</h3>
        <p class="result-text">{{ resultMessage }}</p>

        <div class="result-summary">
          <div class="summary-row">
            <span>DARI</span>
            <strong>{{ fmtDate(dfrom) }}</strong>
          </div>
          <div class="summary-row">
            <span>SAMPAI</span>
            <strong>{{ fmtDate(dto) }}</strong>
          </div>
          <div class="summary-row">
            <span>FILE</span>
            <strong class="status-ok">Dapat diunduh dari daftar REKAP GL</strong>
          </div>
        </div>

        <div class="result-actions">
          <button class="modal-btn primary" type="button" @click="closeResultDialog">
            SELESAI
          </button>
        </div>
      </template>

      <!-- State: Error -->
      <template v-else>
        <div class="result-icon error"><span>!</span></div>
        <h3 class="result-title">Simpan Gagal</h3>
        <p class="result-text">{{ resultMessage }}</p>

        <div class="result-actions">
          <button class="modal-btn secondary" type="button" @click="closeResultDialog">
            TUTUP
          </button>
        </div>
      </template>

    </div>
  </div>
</template>

<style scoped>
.screen-page { padding: 16px; }
.page-header { margin-bottom: 16px; display: flex; flex-direction: column; gap: 4px; }
.page-header h2 { margin: 0; color: #304b73; font-size: 20px; }
.page-subtitle { margin: 0; color: #6b7280; font-size: 14px; }

.status-banner { padding: 12px 16px; border-radius: 10px; font-weight: 600; margin-bottom: 12px; }
.status-banner--error { background: #fde8ea; color: #a32943; }
.loading { padding: 24px; text-align: center; color: #9ca3af; }

.card { background: #fff; border-radius: 12px; padding: 16px; box-shadow: 0 1px 3px rgba(0,0,0,0.08); margin-bottom: 16px; }
.card-title { margin: 0 0 12px; color: #304b73; font-size: 15px; text-align: center; }

.period-bar { display: flex; align-items: center; justify-content: center; gap: 10px; flex-wrap: wrap; }
.period-label { font-weight: 700; color: #304b73; font-size: 13px; }
.period-bar input { padding: 8px 10px; border: 1px solid #d1d9e6; border-radius: 6px; font-size: 14px; }

.table-wrap { overflow: auto; margin: 10px 0; }
.table { width: 100%; border-collapse: collapse; font-size: 13px; }
.table th, .table td { padding: 8px 10px; border-bottom: 1px solid #eef2f7; text-align: left; }
.table th { background: #f6f8fb; color: #304b73; }
.table tbody tr:hover { background: #f6f8fb; }

.download-link { color: #2563eb; font-weight: 700; cursor: pointer; text-decoration: underline; }
.muted { color: #9ca3af; }
.empty-state { color: #9ca3af; text-align: center; padding: 16px; }

.action-bar { display: flex; flex-wrap: wrap; gap: 10px; justify-content: center; }
.small-button { padding: 8px 14px; border-radius: 8px; border: 1px solid #d1d5db; background: #fff; cursor: pointer; font-weight: 600; }
.small-button.primary { background: #304b73; color: #fff; border-color: #304b73; }
.small-button:disabled { opacity: 0.5; cursor: default; }

/* ===== Modal Hasil Simpan ===== */
.modal-overlay {
  position: fixed;
  inset: 0;
  background: rgba(15, 23, 42, 0.55);
  backdrop-filter: blur(3px);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 50;
  animation: fade-in 0.18s ease;
}

.result-modal {
  background: #fff;
  border-radius: 18px;
  padding: 28px 30px 24px;
  width: 420px;
  max-width: 94vw;
  box-shadow: 0 24px 60px rgba(15, 23, 42, 0.35);
  text-align: center;
  animation: pop-in 0.22s cubic-bezier(0.2, 0.9, 0.3, 1.2);
}

.result-icon {
  width: 64px;
  height: 64px;
  margin: 0 auto 14px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 28px;
  font-weight: 900;
  color: #fff;
}

.result-icon.success {
  background: linear-gradient(135deg, #10b981, #059669);
  box-shadow: 0 6px 16px rgba(5, 150, 105, 0.4);
  animation: success-pop 0.5s cubic-bezier(0.2, 0.9, 0.3, 1.3);
}

.result-icon.error {
  background: linear-gradient(135deg, #ef4444, #b91c1c);
  box-shadow: 0 6px 16px rgba(185, 28, 28, 0.4);
}

.result-title { margin: 0 0 6px; font-size: 20px; color: #1f2937; font-weight: 800; }
.result-text { margin: 0 0 16px; font-size: 14px; color: #6b7280; }

.result-summary {
  border: 1px solid #e5eaf1;
  border-radius: 12px;
  background: #f8fafc;
  padding: 4px 14px;
  margin-bottom: 20px;
  text-align: left;
}

.summary-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding: 9px 0;
  border-bottom: 1px dashed #e2e8f0;
}

.summary-row:last-child { border-bottom: none; }

.summary-row span {
  font-size: 12px;
  font-weight: 700;
  color: #6b7280;
  text-transform: uppercase;
  letter-spacing: 0.04em;
  white-space: nowrap;
}

.summary-row strong { font-size: 13px; color: #1f2937; text-align: right; word-break: break-word; }
.summary-row strong.status-ok { color: #059669; font-weight: 800; }

.result-actions { display: flex; gap: 10px; justify-content: center; }

.modal-btn {
  min-width: 120px;
  padding: 10px 18px;
  border-radius: 10px;
  border: 1px solid #d1d5db;
  background: #fff;
  font-weight: 700;
  font-size: 13px;
  cursor: pointer;
  transition: transform 0.1s ease, box-shadow 0.15s ease;
}

.modal-btn:hover { transform: translateY(-1px); }

.modal-btn.primary {
  background: linear-gradient(135deg, #10b981, #059669);
  color: #fff;
  border-color: transparent;
  box-shadow: 0 4px 12px rgba(5, 150, 105, 0.3);
}

.modal-btn.secondary { color: #374151; }

@keyframes fade-in {
  from { opacity: 0; }
  to { opacity: 1; }
}

@keyframes pop-in {
  from { opacity: 0; transform: scale(0.88) translateY(10px); }
  to { opacity: 1; transform: scale(1) translateY(0); }
}

@keyframes success-pop {
  0% { transform: scale(0.4); opacity: 0; }
  60% { transform: scale(1.15); opacity: 1; }
  100% { transform: scale(1); }
}
</style>
