<script setup>
import { computed, onMounted, ref } from 'vue';

const props = defineProps({
  apiBaseUrl: { type: String, required: true }
});

const emit = defineEmits(['session-expired', 'close']);

const loading = ref(false);
const error = ref('');
const treatmentClasses = ref([]);

// form mutasi kamar (SC0002 — MutasiKamar.zul)
const form = ref({
  mrCode: '',
  regNo: '',
  patientName: '',
  classId: null,
  hallId: null,
  hallName: '',
  bedId: null,
  bedDesc: ''
});

// mode ubah: d_whn_create baris history terpilih (null = mutasi baru)
const editingCreatedDate = ref(null);

// history mutasi bed
const history = ref([]);
const selectedHistoryIndex = ref(-1);

// modal cari pasien
const showPatientModal = ref(false);
const patientSearch = ref({ mrCode: '', name: '', address: '' });
const patientResults = ref([]);

// modal ruangan & bed
const showBedModal = ref(false);
const halls = ref([]);
const beds = ref([]);

// dialog/toast
const toast = ref({ visible: false, message: '', type: 'success' });
const dialog = ref({ visible: false, mode: 'alert', type: 'warning', title: '', message: '', resolve: null });
let toastTimer = null;

function showToast(message, type = 'success') {
  toast.value = { visible: true, message, type };
  if (toastTimer) clearTimeout(toastTimer);
  toastTimer = setTimeout(() => { toast.value.visible = false; }, 3500);
}

function showAlert(message, options = {}) {
  return new Promise((resolve) => {
    dialog.value = { visible: true, mode: 'alert', type: options.type || 'warning',
      title: options.title || 'PERHATIAN', message, resolve };
  });
}

function closeDialog(result) {
  const resolve = dialog.value.resolve;
  dialog.value.visible = false;
  if (resolve) resolve(result);
}

const dialogIcon = computed(() => ({
  warning: '⚠️', info: 'ℹ️', error: '❌', success: '✅', confirm: '❓'
}[dialog.value.type] || 'ℹ️'));

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

function qs(params) {
  const parts = [];
  Object.entries(params).forEach(([key, value]) => {
    if (value !== null && value !== undefined && value !== '') {
      parts.push(`${key}=${encodeURIComponent(value)}`);
    }
  });
  return parts.length ? `?${parts.join('&')}` : '';
}

onMounted(async () => {
  try {
    const data = await request('/admission/ranap/masters');
    treatmentClasses.value = data.treatmentClasses;
  } catch (requestError) {
    error.value = requestError.message;
  }
});

function classLabel(id) {
  const found = treatmentClasses.value.find((c) => c.value === String(id));
  return found ? found.label : '';
}

// ================= PASIEN =================

async function searchPatient() {
  const s = patientSearch.value;
  if (!s.mrCode && !s.name && !s.address) {
    await showAlert('Salah satu field pencarian pasien harus diisi!');
    return;
  }
  loading.value = true;
  error.value = '';
  try {
    patientResults.value = await request(`/admission/bed-mutations/patients${qs(s)}`);
  } catch (requestError) {
    error.value = requestError.message;
  } finally {
    loading.value = false;
  }
}

async function selectPatient(result) {
  showPatientModal.value = false;
  loading.value = true;
  error.value = '';
  try {
    const detail = await request(`/admission/bed-mutations/patients/${encodeURIComponent(result.mrCode)}`);
    form.value.mrCode = detail.mrCode;
    form.value.regNo = detail.regNo || '';
    form.value.patientName = detail.patientName;
    history.value = detail.history || [];
    resetBedSelection();
    selectedHistoryIndex.value = -1;
    editingCreatedDate.value = null;
    // tandai baris bed aktif (belum check-out) agar mudah diubah
    const activeIndex = history.value.findIndex((h) => !h.checkOutTime);
    if (activeIndex >= 0) {
      selectedHistoryIndex.value = activeIndex;
    }
  } catch (requestError) {
    error.value = requestError.message;
  } finally {
    loading.value = false;
  }
}

// ================= RUANGAN & BED =================

async function openBedModal() {
  if (!form.value.classId) {
    await showAlert('PILIH KELAS TARIF TERLEBIH DAHULU!');
    return;
  }
  loading.value = true;
  error.value = '';
  try {
    halls.value = await request(`/admission/bed-mutations/halls${qs({ classId: form.value.classId })}`);
    beds.value = [];
    showBedModal.value = true;
  } catch (requestError) {
    error.value = requestError.message;
  } finally {
    loading.value = false;
  }
}

async function selectHall(hall) {
  form.value.hallId = hall.hallId;
  form.value.hallName = hall.name;
  loading.value = true;
  error.value = '';
  try {
    beds.value = await request(`/admission/bed-mutations/halls/${hall.hallId}/beds`);
  } catch (requestError) {
    error.value = requestError.message;
  } finally {
    loading.value = false;
  }
}

function selectBed(bed) {
  if (bed.status === '1' || bed.availableStatus === 'B' || bed.availableStatus === 'C') {
    showAlert(`${bed.label} tidak tersedia.`);
    return;
  }
  form.value.bedId = bed.bedId;
  form.value.bedDesc = bed.bedDesc;
  showBedModal.value = false;
}

// ================= HISTORY MUTASI =================

function selectHistory(index) {
  selectedHistoryIndex.value = index;
}

// ================= UBAH / SIMPAN / BARU =================

async function editSelectedBed() {
  if (selectedHistoryIndex.value < 0 || !history.value[selectedHistoryIndex.value]) {
    await showAlert('Mohon Pilih Data Mutasi Bed Terlebih Dahulu..!');
    return;
  }
  const row = history.value[selectedHistoryIndex.value];
  if (row.checkOutTime) {
    await showAlert('Maaf..Data Bed Tidak Bisa Diubah..!');
    return;
  }
  // isi form dari baris history bed aktif
  form.value.classId = row.classId;
  form.value.hallId = row.hallId;
  form.value.hallName = row.hallName;
  form.value.bedId = row.bedId;
  form.value.bedDesc = row.bedDesc;
  editingCreatedDate.value = row.createdDate;

  // muat daftar ruangan & bed dari kelas/ruangan bed tersebut
  loading.value = true;
  error.value = '';
  try {
    halls.value = await request(`/admission/bed-mutations/halls${qs({ classId: row.classId })}`);
    beds.value = await request(`/admission/bed-mutations/halls/${row.hallId}/beds`);
    showBedModal.value = true;
  } catch (requestError) {
    error.value = requestError.message;
  } finally {
    loading.value = false;
  }
}

async function save() {
  if (!form.value.regNo) {
    await showAlert('NO. MR HARUS DI ISI!');
    return;
  }
  if (!form.value.bedId) {
    await showAlert('BED MUTASI HARUS DI ISI!');
    return;
  }
  loading.value = true;
  error.value = '';
  try {
    const result = await request('/admission/bed-mutations/save', {
      method: 'POST',
      body: JSON.stringify({
        regNo: form.value.regNo,
        bedId: form.value.bedId,
        createdDate: editingCreatedDate.value
      })
    });
    history.value = result.history || [];
    editingCreatedDate.value = null;
    selectedHistoryIndex.value = -1;
    showToast(result.message || 'Pengubahan Data Berhasil..!', 'success');
  } catch (requestError) {
    error.value = requestError.message;
  } finally {
    loading.value = false;
  }
}

function resetBedSelection() {
  form.value.classId = null;
  form.value.hallId = null;
  form.value.hallName = '';
  form.value.bedId = null;
  form.value.bedDesc = '';
  editingCreatedDate.value = null;
}

function createNew() {
  form.value.mrCode = '';
  form.value.regNo = '';
  form.value.patientName = '';
  history.value = [];
  selectedHistoryIndex.value = -1;
  resetBedSelection();
  error.value = '';
}
</script>

<template>
  <div class="screen-page">
    <div class="page-header">
      <h2>🛏️ FORM MUTASI KAMAR</h2>
      <p class="page-subtitle">SC0002 — MutasiKamar.zul (Pemindahan / Perubahan Bed Pasien Rawat Inap)</p>
    </div>

    <p v-if="error" class="status-banner status-banner--error">{{ error }}</p>

    <div class="card">
      <div class="section-title">DATA PASIEN</div>
      <div class="patient-grid">
        <div class="field">
          <label>NO. MR *</label>
          <div class="input-row">
            <input v-model="form.mrCode" readonly placeholder="-" />
            <button class="mini primary" type="button" @click="showPatientModal = true">CARI PASIEN</button>
          </div>
        </div>
        <div class="field">
          <label>NO. REGISTRASI</label>
          <input :value="form.regNo" readonly placeholder="-" />
        </div>
        <div class="field">
          <label>NAMA</label>
          <input :value="form.patientName" readonly placeholder="-" />
        </div>
        <div class="field">
          <label>KELAS TARIF</label>
          <select
            v-model="form.classId"
            @change="form.hallId = null; form.hallName = ''; form.bedId = null; form.bedDesc = ''"
          >
            <option :value="null">--PILIH--</option>
            <option v-for="c in treatmentClasses" :key="c.value" :value="Number(c.value)">
              {{ c.value }}. {{ c.label }}
            </option>
          </select>
        </div>
        <div class="field">
          <label>RUANGAN</label>
          <div class="input-row">
            <input :value="form.hallName" readonly placeholder="-" />
            <button class="mini primary" type="button" @click="openBedModal">CARI</button>
          </div>
        </div>
        <div class="field">
          <label>BED MUTASI *</label>
          <input :value="form.bedDesc" readonly placeholder="-" />
        </div>
      </div>

      <div class="section-title">DAFTAR HISTORY MUTASI PASIEN</div>
      <div class="table-wrap">
        <table class="table">
          <thead>
            <tr>
              <th>BED ASAL</th>
              <th>TANGGAL MASUK</th>
              <th>TANGGAL KELUAR</th>
              <th>DURASI</th>
            </tr>
          </thead>
          <tbody>
            <tr
              v-for="(row, index) in history"
              :key="row.createdDate"
              :class="{ selected: index === selectedHistoryIndex }"
              @click="selectHistory(index)"
            >
              <td class="strong">{{ row.bedDesc }}</td>
              <td>{{ row.checkInTime || '-' }}</td>
              <td>{{ row.checkOutTime || '-' }}</td>
              <td>{{ row.duration }}</td>
            </tr>
            <tr v-if="!history.length">
              <td colspan="4" class="empty-state">Pilih pasien rawat inap untuk melihat riwayat mutasi bed.</td>
            </tr>
          </tbody>
        </table>
      </div>

      <div class="action-bar">
        <button class="small-button primary" type="button" :disabled="loading" @click="save">💾 SIMPAN</button>
        <button class="small-button" type="button" :disabled="loading" @click="editSelectedBed">✏️ UBAH</button>
        <button class="small-button" type="button" @click="createNew">🆕 BARU</button>
      </div>
    </div>

    <!-- ==================== MODAL: CARI PASIEN ==================== -->
    <div v-if="showPatientModal" class="modal-overlay" @click.self="showPatientModal = false">
      <div class="modal">
        <div class="modal-header">CARI DATA PASIEN <span class="modal-note">*) KHUSUS PASIEN RAWAT INAP</span></div>
        <div class="modal-body">
          <div class="field"><label>NO. MR</label><input v-model="patientSearch.mrCode" @keyup.enter="searchPatient" /></div>
          <div class="field"><label>NAMA</label><input v-model="patientSearch.name" @keyup.enter="searchPatient" /></div>
          <div class="field"><label>ALAMAT</label><input v-model="patientSearch.address" @keyup.enter="searchPatient" /></div>
          <button class="small-button primary" type="button" :disabled="loading" @click="searchPatient">🔍 CARI</button>
          <div class="table-wrap modal-list">
            <table class="table">
              <thead><tr><th>NO. MR</th><th>NAMA</th><th>ALAMAT</th></tr></thead>
              <tbody>
                <tr v-for="r in patientResults" :key="r.mrId" @click="selectPatient(r)">
                  <td class="strong">{{ r.mrCode }}</td>
                  <td>{{ r.patientName }}</td>
                  <td>{{ r.address }}</td>
                </tr>
                <tr v-if="!patientResults.length">
                  <td colspan="3" class="empty-state">Tidak ada pasien rawat inap ditemukan.</td>
                </tr>
              </tbody>
            </table>
          </div>
        </div>
        <div class="modal-footer">
          <button class="small-button" type="button" @click="showPatientModal = false">TUTUP</button>
        </div>
      </div>
    </div>

    <!-- ==================== MODAL: RUANGAN & BED ==================== -->
    <div v-if="showBedModal" class="modal-overlay" @click.self="showBedModal = false">
      <div class="modal modal--wide">
        <div class="modal-header">CARI DATA RUANGAN &amp; PENEMPATAN BED</div>
        <div class="modal-body">
          <div class="section-title">RUANGAN (kelas {{ classLabel(form.classId) }})</div>
          <div class="table-wrap modal-list">
            <table class="table">
              <thead><tr><th>RUANGAN</th><th class="num">BED TERSISA</th></tr></thead>
              <tbody>
                <tr
                  v-for="h in halls"
                  :key="h.hallId"
                  :class="{ selected: form.hallId === h.hallId }"
                  @click="selectHall(h)"
                >
                  <td class="strong">{{ h.name }}</td>
                  <td class="num">{{ h.availableBeds }}</td>
                </tr>
                <tr v-if="!halls.length"><td colspan="2" class="empty-state">Tidak ada ruangan untuk kelas ini.</td></tr>
              </tbody>
            </table>
          </div>
          <div v-if="beds.length" class="section-title">BED — {{ form.hallName }}</div>
          <div v-if="beds.length" class="bed-grid">
            <button
              v-for="b in beds"
              :key="b.bedId"
              type="button"
              class="bed-btn"
              :class="{
                'bed-btn--used': b.status === '1',
                'bed-btn--booked': b.availableStatus === 'B',
                'bed-btn--broken': b.availableStatus === 'C',
                'bed-btn--current': b.bedId === form.bedId
              }"
              :disabled="b.status === '1' || b.availableStatus === 'B' || b.availableStatus === 'C'"
              :title="b.patientName ? (b.mrCode + ' - ' + b.patientName) : ''"
              @click="selectBed(b)"
            >
              {{ b.label }}
            </button>
          </div>
          <div v-if="form.bedId && !beds.length" class="empty-state">
            Bed terpilih: {{ form.bedDesc }}
          </div>
        </div>
        <div class="modal-footer">
          <button class="small-button" type="button" @click="showBedModal = false">TUTUP</button>
        </div>
      </div>
    </div>

    <!-- ==================== DIALOG / TOAST ==================== -->
    <transition name="dialog-fade">
      <div v-if="dialog.visible" class="modal-overlay" @click.self="closeDialog(false)">
        <div class="dialog-box" :class="'dialog-box--' + dialog.type">
          <div class="dialog-icon">{{ dialogIcon }}</div>
          <div class="dialog-title">{{ dialog.title }}</div>
          <div class="dialog-message">{{ dialog.message }}</div>
          <div class="dialog-buttons">
            <button class="small-button primary" type="button" @click="closeDialog(true)">✔ OK</button>
          </div>
        </div>
      </div>
    </transition>

    <transition name="toast-fade">
      <div v-if="toast.visible" class="toast" :class="'toast--' + toast.type">
        <span class="toast-icon">{{ toast.type === 'success' ? '✅' : toast.type === 'error' ? '❌' : 'ℹ️' }}</span>
        <span class="toast-message">{{ toast.message }}</span>
      </div>
    </transition>
  </div>
</template>

<style scoped>
.screen-page { padding: 16px; }
.page-header { margin-bottom: 16px; display: flex; flex-direction: column; gap: 4px; }
.page-header h2 { margin: 0; color: #304b73; font-size: 20px; }
.page-subtitle { margin: 0; color: #6b7280; font-size: 14px; }

.status-banner { padding: 12px 16px; border-radius: 10px; font-weight: 600; margin-bottom: 12px; }
.status-banner--error { background: #fde8ea; color: #a32943; }

.card { background: #fff; border-radius: 12px; padding: 16px; box-shadow: 0 1px 3px rgba(0,0,0,0.08); margin-bottom: 16px; }
.section-title { font-weight: 800; color: #304b73; font-size: 14px; margin: 14px 0 10px; padding-bottom: 6px; border-bottom: 1px solid #eef2f7; }
.section-title:first-child { margin-top: 0; }

.patient-grid { display: grid; grid-template-columns: repeat(3, 1fr); gap: 10px 18px; }
.field { display: flex; flex-direction: column; gap: 4px; }
.field label { font-size: 11px; font-weight: 700; color: #6b7280; text-transform: uppercase; letter-spacing: 0.03em; }
.field input, .field select { padding: 7px 9px; border: 1px solid #d1d9e6; border-radius: 6px; font-size: 13px; width: 100%; box-sizing: border-box; }
.field input[readonly] { background: #f3f5f8; color: #4b5563; }
.input-row { display: flex; gap: 6px; align-items: center; }
.input-row input { flex: 1; }
.mini { padding: 6px 10px; border-radius: 6px; border: 1px solid #d1d5db; background: #fff; cursor: pointer; font-weight: 600; font-size: 12px; white-space: nowrap; }
.mini.primary { background: #304b73; color: #fff; border-color: #304b73; }

.table-wrap { overflow: auto; margin: 10px 0; }
.table { width: 100%; border-collapse: collapse; font-size: 13px; }
.table th, .table td { padding: 7px 9px; border-bottom: 1px solid #eef2f7; text-align: left; white-space: nowrap; }
.table th { background: #f6f8fb; color: #304b73; }
.table tbody tr { cursor: pointer; }
.table tbody tr:hover { background: #f6f8fb; }
.table tbody tr.selected { background: #e8eef8; }
.num { text-align: right; }
.strong { font-weight: 700; }
.empty-state { color: #9ca3af; text-align: center; padding: 16px; }

.bed-grid { display: flex; flex-wrap: wrap; gap: 8px; margin: 8px 0; }
.bed-btn { padding: 8px 12px; border-radius: 8px; border: 1px solid #177245; background: #e7f6ec; color: #177245; cursor: pointer; font-weight: 700; font-size: 12px; }
.bed-btn--used { background: #fde8ea; color: #a32943; border-color: #a32943; cursor: default; }
.bed-btn--booked { background: #fef3d6; color: #b7791f; border-color: #b7791f; cursor: default; }
.bed-btn--broken { background: #e5e7eb; color: #6b7280; border-color: #9ca3af; cursor: default; }
.bed-btn--current { box-shadow: 0 0 0 2px #304b73; }
.bed-btn:disabled { opacity: 0.9; }

.action-bar { display: flex; flex-wrap: wrap; gap: 10px; justify-content: center; margin-top: 12px; }
.small-button { padding: 8px 14px; border-radius: 8px; border: 1px solid #d1d5db; background: #fff; cursor: pointer; font-weight: 600; font-size: 13px; }
.small-button.primary { background: #304b73; color: #fff; border-color: #304b73; }
.small-button:disabled { opacity: 0.5; cursor: default; }

.modal-overlay { position: fixed; inset: 0; background: rgba(15,23,42,0.45); display: flex; align-items: center; justify-content: center; z-index: 50; }
.modal { background: #fff; border-radius: 12px; width: 640px; max-width: 94vw; max-height: 88vh; display: flex; flex-direction: column; box-shadow: 0 20px 40px rgba(0,0,0,0.2); }
.modal--wide { width: 860px; }
.modal-header { padding: 14px 18px; background: #304b73; color: #fff; font-weight: 800; border-radius: 12px 12px 0 0; display: flex; justify-content: space-between; align-items: center; gap: 10px; }
.modal-note { font-size: 11px; font-weight: 600; color: #ffd9a0; }
.modal-body { padding: 14px 18px; overflow-y: auto; display: flex; flex-direction: column; gap: 10px; }
.modal-list { max-height: 250px; }
.modal-footer { padding: 12px 18px; border-top: 1px solid #eef2f7; display: flex; justify-content: flex-end; gap: 10px; border-radius: 0 0 12px 12px; }

.toast { position: fixed; bottom: 24px; right: 24px; z-index: 100; display: flex; align-items: center; gap: 10px; padding: 14px 18px; border-radius: 12px; font-weight: 700; font-size: 14px; color: #fff; box-shadow: 0 10px 30px rgba(15,23,42,0.25); max-width: 420px; }
.toast--success { background: linear-gradient(135deg, #177245, #1f9d5c); }
.toast--error { background: linear-gradient(135deg, #a32943, #d64567); }
.toast--info { background: linear-gradient(135deg, #304b73, #5f83c2); }
.toast-icon { font-size: 18px; }
.toast-message { flex: 1; }
.toast-fade-enter-active, .toast-fade-leave-active { transition: all 0.3s ease; }
.toast-fade-enter-from, .toast-fade-leave-to { opacity: 0; transform: translateY(16px); }

.dialog-fade-enter-active, .dialog-fade-leave-active { transition: all 0.25s ease; }
.dialog-fade-enter-from, .dialog-fade-leave-to { opacity: 0; transform: scale(0.92); }
.dialog-box { background: #fff; border-radius: 16px; width: 380px; max-width: 92vw; padding: 26px 28px; text-align: center; box-shadow: 0 24px 50px rgba(15,23,42,0.3); border-top: 5px solid #5f83c2; }
.dialog-box--warning { border-top-color: #e6a23c; }
.dialog-box--error { border-top-color: #d64567; }
.dialog-box--success { border-top-color: #1f9d5c; }
.dialog-icon { font-size: 44px; margin-bottom: 8px; }
.dialog-title { font-size: 17px; font-weight: 800; color: #304b73; margin-bottom: 8px; }
.dialog-message { font-size: 14px; color: #4b5563; line-height: 1.5; margin-bottom: 18px; white-space: pre-line; }
.dialog-buttons { display: flex; justify-content: center; gap: 12px; }
.dialog-buttons .small-button { min-width: 110px; }

@media (max-width: 960px) {
  .patient-grid { grid-template-columns: 1fr 1fr; }
}
</style>
