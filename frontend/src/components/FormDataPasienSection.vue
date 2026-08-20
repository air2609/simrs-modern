<script setup>
import { computed, onMounted, ref } from 'vue';

const props = defineProps({
  apiBaseUrl: { type: String, required: true }
});

const emit = defineEmits(['session-expired', 'close']);

const loading = ref(false);
const error = ref('');

// master dropdown data
const masters = ref(null);

// form fields
const mrCode = ref('');
const namaPasien = ref('');
const jenisKelamin = ref('M');
const tglLahir = ref('');
const umur = ref('');
const agama = ref('');
const wargaNegara = ref('');
const statusKawin = ref('');
const alamat = ref('');
const rt = ref('');
const rw = ref('');
const kelurahanId = ref(null);
const kecamatanId = ref(null);
const kabupatenId = ref(null);
const propinsiId = ref(null);
const noTelp = ref('');
const alamatAlternatif = ref('');
const rt1 = ref('');
const rw1 = ref('');
const noTelpAlt = ref('');
const pendidikan = ref('');
const jenisPekerjaan = ref('');
const tipePasienId = ref(null);
const prioritas = ref('');

// patient search modal
const patientDialog = ref({ visible: false });
const searchMr = ref('');
const searchNama = ref('');
const searchTgl = ref('');
const searchAlamat = ref('');
const patientResults = ref([]);
const patientLoading = ref(false);
const patientPageSize = 15;
const patientPage = ref(1);
const pagedPatients = computed(() => {
  const start = (patientPage.value - 1) * patientPageSize;
  return patientResults.value.slice(start, start + patientPageSize);
});
const patientTotalPages = computed(() => Math.max(1, Math.ceil(patientResults.value.length / patientPageSize)));
function patientGoToPage(page) {
  patientPage.value = Math.min(Math.max(1, page), patientTotalPages.value);
}

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
    masters.value = await request('/master/patient/masters');
  } catch (requestError) {
    error.value = requestError.message;
  }
});

// UMUR otomatis: "{thn} thn {bln} bln {hr} hr" (pola legacy convertAgeToString)
function computeAge(dobIso) {
  if (!dobIso) return '';
  const dob = new Date(dobIso + 'T00:00:00');
  const now = new Date();
  let years = now.getFullYear() - dob.getFullYear();
  let months = now.getMonth() - dob.getMonth();
  let days = now.getDate() - dob.getDate();
  if (days < 0) {
    months -= 1;
    const prevMonth = new Date(now.getFullYear(), now.getMonth(), 0);
    days += prevMonth.getDate();
  }
  if (months < 0) {
    years -= 1;
    months += 12;
  }
  return `${years} thn ${months} bln ${days} hr`;
}

function onTglLahirChange() {
  umur.value = computeAge(tglLahir.value);
}

async function openPatientSearch() {
  searchMr.value = '';
  searchNama.value = '';
  searchTgl.value = '';
  searchAlamat.value = '';
  patientResults.value = [];
  patientPage.value = 1;
  patientDialog.value.visible = true;
}

async function searchPatient() {
  if (!searchMr.value && !searchNama.value && !searchTgl.value && !searchAlamat.value) {
    await showAlert('Salah satu field harus diisi');
    return;
  }
  patientLoading.value = true;
  try {
    patientResults.value = await request(`/master/patient/patients${qs({
      mrCode: searchMr.value,
      name: searchNama.value,
      address: searchAlamat.value,
      dob: searchTgl.value
    })}`);
    patientPage.value = 1;
  } catch (requestError) {
    await showAlert(requestError.message);
  } finally {
    patientLoading.value = false;
  }
}

async function loadDetail(mrCodeInput) {
  loading.value = true;
  error.value = '';
  try {
    const d = await request(`/master/patient/detail${qs({ mrCode: mrCodeInput })}`);
    mrCode.value = d.mrCode;
    namaPasien.value = d.namaPasien;
    jenisKelamin.value = d.jenisKelamin === 'M' ? 'M' : 'F';
    tglLahir.value = d.tglLahir ? d.tglLahir.split('/').reverse().join('-') : '';
    umur.value = computeAge(tglLahir.value);
    agama.value = d.agama || '';
    wargaNegara.value = d.wargaNegara || '';
    statusKawin.value = d.statusKawin || '';
    alamat.value = d.alamat || '';
    rt.value = d.rt || '';
    rw.value = d.rw || '';
    kelurahanId.value = d.kelurahanId;
    kecamatanId.value = d.kecamatanId;
    kabupatenId.value = d.kabupatenId;
    propinsiId.value = d.propinsiId;
    noTelp.value = d.noTelp || '';
    alamatAlternatif.value = d.alamatAlternatif || '';
    rt1.value = d.rt1 || '';
    rw1.value = d.rw1 || '';
    noTelpAlt.value = d.noTelpAlt || '';
    pendidikan.value = d.pendidikan || '';
    jenisPekerjaan.value = d.jenisPekerjaan || '';
    tipePasienId.value = d.tipePasienId;
    prioritas.value = d.prioritas || '';
    patientDialog.value.visible = false;
  } catch (requestError) {
    error.value = requestError.message;
  } finally {
    loading.value = false;
  }
}

function choosePatient(patient) {
  patientDialog.value.visible = false;
  loadDetail(patient.mrCode);
}

// NO. MR manual (ketik langsung)
function onMrManualInput(event) {
  const value = event.target.value.trim();
  if (value) loadDetail(value);
}

// BARU — reset form (migrasi doCancel)
function resetForm() {
  mrCode.value = '';
  namaPasien.value = '';
  jenisKelamin.value = 'M';
  tglLahir.value = '';
  umur.value = '';
  agama.value = '';
  wargaNegara.value = '';
  statusKawin.value = '';
  alamat.value = '';
  rt.value = '';
  rw.value = '';
  kelurahanId.value = null;
  kecamatanId.value = null;
  kabupatenId.value = null;
  propinsiId.value = null;
  noTelp.value = '';
  alamatAlternatif.value = '';
  rt1.value = '';
  rw1.value = '';
  noTelpAlt.value = '';
  pendidikan.value = '';
  jenisPekerjaan.value = '';
  tipePasienId.value = null;
  prioritas.value = '';
  error.value = '';
}

// SIMPAN
async function savePatient() {
  if (!namaPasien.value.trim()) {
    await showAlert('NAMA HARUS DI ISI!');
    return;
  }
  if (!tglLahir.value) {
    await showAlert('TANGGAL LAHIR HARUS DI ISI!');
    return;
  }
  if (!alamat.value.trim()) {
    await showAlert('ALAMAT HARUS DI ISI!');
    return;
  }
  loading.value = true;
  error.value = '';
  try {
    const result = await request('/master/patient/save', {
      method: 'POST',
      body: JSON.stringify({
        mrCode: mrCode.value || undefined,
        namaPasien: namaPasien.value,
        jenisKelamin: jenisKelamin.value,
        tglLahir: tglLahir.value,
        agama: agama.value || undefined,
        wargaNegara: wargaNegara.value || undefined,
        statusKawin: statusKawin.value || undefined,
        alamat: alamat.value,
        rt: rt.value || undefined,
        rw: rw.value || undefined,
        kelurahanId: kelurahanId.value ?? undefined,
        kecamatanId: kecamatanId.value ?? undefined,
        kabupatenId: kabupatenId.value ?? undefined,
        propinsiId: propinsiId.value ?? undefined,
        noTelp: noTelp.value || undefined,
        alamatAlternatif: alamatAlternatif.value || undefined,
        rt1: rt1.value || undefined,
        rw1: rw1.value || undefined,
        noTelpAlt: noTelpAlt.value || undefined,
        pendidikan: pendidikan.value || undefined,
        jenisPekerjaan: jenisPekerjaan.value || undefined,
        tipePasienId: tipePasienId.value ?? undefined,
        prioritas: prioritas.value || undefined
      })
    });
    mrCode.value = result.mrCode;
    showToast(result.modify ? 'DATA PASIEN BERHASIL DIUBAH' : 'DATA PASIEN BERHASIL DISIMPAN', 'success');
  } catch (requestError) {
    error.value = requestError.message;
  } finally {
    loading.value = false;
  }
}
</script>

<template>
  <div class="screen-page">
    <div class="page-header">
      <h2>🧑‍⚕️ FORM DATA PASIEN</h2>
    </div>

    <p v-if="error" class="status-banner status-banner--error">{{ error }}</p>

    <div class="card">
      <div class="form-grid">
        <div class="form-row">
          <span class="label">NO. MR</span>
          <input class="mr-input" :value="mrCode" placeholder="Cari pasien..." readonly @click="openPatientSearch" />
          <button class="small-button" type="button" @click="openPatientSearch">🔍 CARI</button>
          <input class="manual-input" type="text" placeholder="atau ketik NO. MR" @change="onMrManualInput" />
        </div>

        <div class="form-row">
          <span class="label">NAMA</span>
          <input v-model="namaPasien" type="text" class="wide" />
          <span class="label">JENIS KELAMIN</span>
          <div class="radio-group">
            <label class="radio-option"><input type="radio" value="M" v-model="jenisKelamin" /> PRIA</label>
            <label class="radio-option"><input type="radio" value="F" v-model="jenisKelamin" /> WANITA</label>
          </div>
        </div>

        <div class="form-row">
          <span class="label">TANGGAL LAHIR</span>
          <input v-model="tglLahir" type="date" @change="onTglLahirChange" />
          <span class="label">UMUR</span>
          <input v-model="umur" type="text" class="wide" readonly />
        </div>

        <div class="form-row">
          <span class="label">AGAMA</span>
          <select v-model="agama" class="wide">
            <option value="">- Pilih -</option>
            <option v-for="o in masters?.religions || []" :key="o.label" :value="o.label">{{ o.label }}</option>
          </select>
          <span class="label">WARGA NEGARA</span>
          <select v-model="wargaNegara" class="wide">
            <option value="">- Pilih -</option>
            <option v-for="o in masters?.nationalities || []" :key="o.label" :value="o.label">{{ o.label }}</option>
          </select>
        </div>

        <div class="form-row">
          <span class="label">STATUS KAWIN</span>
          <select v-model="statusKawin" class="wide">
            <option value="">- Pilih -</option>
            <option v-for="o in masters?.maritalStatuses || []" :key="o.label" :value="o.label">{{ o.label }}</option>
          </select>
        </div>

        <div class="form-row">
          <span class="label">ALAMAT UTAMA</span>
          <input v-model="alamat" type="text" class="wide" />
          <span class="label">RT / RW</span>
          <input v-model="rt" type="text" class="rt-input" /> / <input v-model="rw" type="text" class="rt-input" />
        </div>

        <div class="form-row">
          <span class="label">KELURAHAN / DESA</span>
          <select v-model="kelurahanId" class="wide">
            <option :value="null">- Pilih -</option>
            <option v-for="o in masters?.villages || []" :key="o.id" :value="o.id">{{ o.label }}</option>
          </select>
          <span class="label">KECAMATAN</span>
          <select v-model="kecamatanId" class="wide">
            <option :value="null">- Pilih -</option>
            <option v-for="o in masters?.subDistricts || []" :key="o.id" :value="o.id">{{ o.label }}</option>
          </select>
        </div>

        <div class="form-row">
          <span class="label">KABUPATEN</span>
          <select v-model="kabupatenId" class="wide">
            <option :value="null">- Pilih -</option>
            <option v-for="o in masters?.regencies || []" :key="o.id" :value="o.id">{{ o.label }}</option>
          </select>
          <span class="label">PROPINSI</span>
          <select v-model="propinsiId" class="wide">
            <option :value="null">- Pilih -</option>
            <option v-for="o in masters?.provinces || []" :key="o.id" :value="o.id">{{ o.label }}</option>
          </select>
        </div>

        <div class="form-row">
          <span class="label">NO. TELP / NO. HP</span>
          <input v-model="noTelp" type="text" class="wide" maxlength="20" />
        </div>

        <div class="form-row">
          <span class="label">ALAMAT ALTERNATIF</span>
          <input v-model="alamatAlternatif" type="text" class="wide" />
          <span class="label">RT / RW</span>
          <input v-model="rt1" type="text" class="rt-input" /> / <input v-model="rw1" type="text" class="rt-input" />
        </div>

        <div class="form-row">
          <span class="label">NO. TELP / NO. HP</span>
          <input v-model="noTelpAlt" type="text" class="wide" maxlength="20" />
        </div>

        <div class="form-row">
          <span class="label">PENDIDIKAN</span>
          <select v-model="pendidikan" class="wide">
            <option value="">- Pilih -</option>
            <option v-for="o in masters?.educations || []" :key="o.label" :value="o.label">{{ o.label }}</option>
          </select>
          <span class="label">JENIS PEKERJAAN</span>
          <select v-model="jenisPekerjaan" class="wide">
            <option value="">- Pilih -</option>
            <option v-for="o in masters?.jobTypes || []" :key="o.label" :value="o.label">{{ o.label }}</option>
          </select>
        </div>

        <div class="form-row">
          <span class="label">TIPE PASIEN</span>
          <select v-model="tipePasienId" class="wide">
            <option :value="null">- Pilih -</option>
            <option v-for="o in masters?.patientTypes || []" :key="o.id" :value="o.id">{{ o.label }}</option>
          </select>
          <span class="label">PRIORITAS PASIEN</span>
          <select v-model="prioritas" class="wide">
            <option value="">- Pilih -</option>
            <option v-for="o in masters?.priorities || []" :key="o.label" :value="o.label">{{ o.label }}</option>
          </select>
        </div>
      </div>

      <div class="action-bar">
        <button class="small-button primary" type="button" :disabled="loading" @click="savePatient">💾 SIMPAN</button>
        <button class="small-button" type="button" @click="resetForm">🆕 BARU</button>
        <button class="small-button" type="button" @click="openPatientSearch">✏️ UBAH</button>
        <button class="small-button" type="button" @click="emit('close')">✅ SELESAI</button>
      </div>
    </div>

    <!-- ==================== MODAL CARI PASIEN ==================== -->
    <transition name="dialog-fade">
      <div v-if="patientDialog.visible" class="modal-overlay" @click.self="patientDialog.visible = false">
        <div class="dialog-box patient-box">
          <div class="dialog-title">PENCARIAN DATA PASIEN</div>
          <div class="patient-search">
            <div class="search-row">
              <span class="filter-label">NO. MR</span>
              <input v-model="searchMr" type="text" placeholder="No. MR" />
            </div>
            <div class="search-row">
              <span class="filter-label">NAMA</span>
              <input v-model="searchNama" type="text" placeholder="Nama pasien" />
            </div>
            <div class="search-row">
              <span class="filter-label">TGL.LAHIR</span>
              <input v-model="searchTgl" type="date" />
            </div>
            <div class="search-row">
              <span class="filter-label">ALAMAT</span>
              <input v-model="searchAlamat" type="text" placeholder="Alamat" />
            </div>
            <button class="small-button primary" type="button" :disabled="patientLoading" @click="searchPatient">🔍 CARI</button>
          </div>
          <div class="table-wrap patient-table">
            <table class="table">
              <thead>
                <tr>
                  <th>NO. MR</th>
                  <th>NAMA</th>
                  <th>TGL.LAHIR</th>
                  <th>ALAMAT</th>
                </tr>
              </thead>
              <tbody>
                <tr v-for="(p, index) in pagedPatients" :key="index" class="clickable" @click="choosePatient(p)">
                  <td class="strong">{{ p.mrCode }}</td>
                  <td>{{ p.name }}</td>
                  <td>{{ p.dob }}</td>
                  <td>{{ p.address }}</td>
                </tr>
                <tr v-if="!pagedPatients.length">
                  <td colspan="4" class="empty-state">Tidak ada pasien. Tekan CARI.</td>
                </tr>
              </tbody>
            </table>
          </div>
          <div class="pagination" v-if="patientResults.length > patientPageSize">
            <button class="page-btn" type="button" :disabled="patientPage <= 1" @click="patientGoToPage(1)">⏮</button>
            <button class="page-btn" type="button" :disabled="patientPage <= 1" @click="patientGoToPage(patientPage - 1)">◀</button>
            <span class="page-info">Halaman {{ patientPage }} / {{ patientTotalPages }}</span>
            <button class="page-btn" type="button" :disabled="patientPage >= patientTotalPages" @click="patientGoToPage(patientPage + 1)">▶</button>
            <button class="page-btn" type="button" :disabled="patientPage >= patientTotalPages" @click="patientGoToPage(patientTotalPages)">⏭</button>
          </div>
          <div class="dialog-buttons">
            <button class="small-button" type="button" @click="patientDialog.visible = false">TUTUP</button>
          </div>
        </div>
      </div>
    </transition>

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
.loading { padding: 24px; text-align: center; color: #9ca3af; }

.card { background: #fff; border-radius: 12px; padding: 16px; box-shadow: 0 1px 3px rgba(0,0,0,0.08); margin-bottom: 16px; }

.form-grid { display: flex; flex-direction: column; gap: 10px; }
.form-row { display: flex; align-items: center; gap: 8px; flex-wrap: wrap; }
.label { font-weight: 700; color: #304b73; font-size: 12px; min-width: 130px; }
.form-row input[type="text"], .form-row input[type="date"], .form-row select {
  padding: 7px 9px; border: 1px solid #d1d9e6; border-radius: 6px; font-size: 13px;
}
.wide { width: 220px; }
.rt-input { width: 55px; }
.mr-input { width: 180px; background: #f3f5f8; cursor: pointer; }
.manual-input { width: 170px; }
.radio-group { display: inline-flex; gap: 12px; }
.radio-option { font-weight: 700; font-size: 13px; color: #304b73; display: inline-flex; align-items: center; gap: 4px; cursor: pointer; }

.action-bar { display: flex; flex-wrap: wrap; gap: 10px; justify-content: center; margin-top: 16px; }
.small-button { padding: 8px 14px; border-radius: 8px; border: 1px solid #d1d5db; background: #fff; cursor: pointer; font-weight: 600; font-size: 13px; }
.small-button.primary { background: #304b73; color: #fff; border-color: #304b73; }
.small-button:disabled { opacity: 0.5; cursor: default; }

.modal-overlay { position: fixed; inset: 0; background: rgba(15,23,42,0.45); display: flex; align-items: center; justify-content: center; z-index: 50; }
.dialog-box { background: #fff; border-radius: 16px; width: 380px; max-width: 92vw; padding: 26px 28px; text-align: center; box-shadow: 0 24px 50px rgba(15,23,42,0.3); border-top: 5px solid #5f83c2; }
.dialog-box--warning { border-top-color: #e6a23c; }
.dialog-box--error { border-top-color: #d64567; }
.dialog-box--success { border-top-color: #1f9d5c; }
.dialog-box--confirm { border-top-color: #5f83c2; }
.dialog-icon { font-size: 44px; margin-bottom: 8px; }
.dialog-title { font-size: 17px; font-weight: 800; color: #304b73; margin-bottom: 8px; }
.dialog-message { font-size: 14px; color: #4b5563; line-height: 1.5; margin-bottom: 18px; white-space: pre-line; }
.dialog-buttons { display: flex; justify-content: center; gap: 12px; margin-top: 12px; }
.dialog-buttons .small-button { min-width: 110px; }
.patient-box { width: 760px; max-width: 95vw; }
.patient-search { display: flex; flex-direction: column; align-items: center; gap: 8px; margin-bottom: 10px; }
.search-row { display: flex; align-items: center; gap: 8px; width: 100%; max-width: 480px; }
.search-row .filter-label { width: 80px; text-align: right; font-weight: 700; color: #304b73; font-size: 12px; }
.search-row input { flex: 1; padding: 7px 9px; border: 1px solid #d1d9e6; border-radius: 6px; font-size: 13px; }
.patient-search .small-button { min-width: 120px; }
.patient-table { max-height: 320px; }

.table-wrap { overflow: auto; margin: 10px 0; }
.table { width: 100%; border-collapse: collapse; font-size: 13px; }
.table th, .table td { padding: 7px 9px; border-bottom: 1px solid #eef2f7; text-align: left; white-space: nowrap; }
.table th { background: #f6f8fb; color: #304b73; position: sticky; top: 0; }
.table tbody tr:hover { background: #f6f8fb; }
.table tbody tr.clickable { cursor: pointer; }
.strong { font-weight: 700; }
.empty-state { color: #9ca3af; text-align: center; padding: 16px; }

.pagination { display: flex; align-items: center; justify-content: center; gap: 10px; margin: 12px 0 4px; flex-wrap: wrap; }
.page-btn { padding: 6px 12px; border-radius: 6px; border: 1px solid #d1d5db; background: #fff; cursor: pointer; font-weight: 700; font-size: 13px; color: #304b73; }
.page-btn:hover:not(:disabled) { background: #eef3fa; }
.page-btn:disabled { opacity: 0.4; cursor: default; }
.page-info { font-weight: 700; color: #304b73; font-size: 13px; }

.dialog-fade-enter-active, .dialog-fade-leave-active { transition: all 0.25s ease; }
.dialog-fade-enter-from, .dialog-fade-leave-to { opacity: 0; transform: scale(0.92); }

.toast { position: fixed; bottom: 24px; right: 24px; z-index: 100; display: flex; align-items: center; gap: 10px; padding: 14px 18px; border-radius: 12px; font-weight: 700; font-size: 14px; color: #fff; box-shadow: 0 10px 30px rgba(15,23,42,0.25); max-width: 420px; }
.toast--success { background: linear-gradient(135deg, #177245, #1f9d5c); }
.toast--error { background: linear-gradient(135deg, #a32943, #d64567); }
.toast--info { background: linear-gradient(135deg, #304b73, #5f83c2); }
.toast-icon { font-size: 18px; }
.toast-message { flex: 1; }
.toast-fade-enter-active, .toast-fade-leave-active { transition: all 0.3s ease; }
.toast-fade-enter-from, .toast-fade-leave-to { opacity: 0; transform: translateY(16px); }
</style>
