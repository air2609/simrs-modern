<script setup>
import { computed, onMounted, reactive, ref } from 'vue';

const props = defineProps({
  apiBaseUrl: { type: String, required: true }
});

const emit = defineEmits(['session-expired']);

const loading = ref(false);
const saving = ref(false);
const searching = ref(false);
const error = ref('');
const searchError = ref('');
const saveError = ref('');
const message = ref('');
const searchResults = ref([]);
const doctors = ref([]);
const locationOptions = reactive({ provinces: [], regencies: [], districts: [], villages: [] });
const masters = reactive({ units: [], patientTypes: [] });

const showSearchModal = ref(false);

const searchForm = reactive({ mrCode: '', patientName: '', nik: '', birthDate: '', address: '' });

const form = reactive({
  existingMrCode: '',
  patientTypeId: '',
  patientName: '',
  gender: 'M',
  birthDate: '',
  nik: '',
  mainAddress: '',
  mainPhone: '',
  mainRt: '',
  mainRw: '',
  altAddress: '',
  altPhone: '',
  altRt: '',
  altRw: '',
  maritalStatus: '',
  nationality: '',
  religion: '',
  education: '',
  jobType: '',
  priority: '',
  etnis: '',
  language: '',
  provinceCode: '',
  cityCode: '',
  districtCode: '',
  subdistrictCode: '',
  unitId: '',
  doctorStaffId: '',
  ihsNumber: '',
  currentRegistrationCode: ''
});

const staticOptions = {
  etnis: ['', 'BATAK', 'BASEMAH', 'CINA', 'JAWA', 'LEMBAK', 'MADURA', 'MELAYU', 'MINANG', 'PEKAL', 'REJANG', 'SERAWAI', 'SUNDA'],
  language: ['', 'BAHASA INDONESIA', 'BAHASA DAERAH', 'BAHASA INGGRIS', 'BAHASA CHINA'],
  maritalStatus: ['', 'Belum Menikah', 'Menikah', 'Duda', 'Janda'],
  nationality: ['', 'WNI', 'WNA'],
  religion: ['', 'ISLAM', 'PROTESTAN', 'KATOLIK', 'HINDU', 'BUDHA', 'KONGHUCU'],
  education: ['', 'TIDAK SEKOLAH', 'BELUM SEKOLAH', 'SD/Sederajat', 'SMP/Sederajat', 'SMA/Sederajat', 'D1', 'D2', 'D3', 'S1', 'S2', 'S3'],
  jobType: ['', 'Pegawai Negri', 'Swasta', 'Pensiunan', 'Ibu Rumah Tangga'],
  priority: ['', 'REGULER', 'PRIORITAS']
};

const selectedUnit = computed(() => masters.units.find((item) => String(item.unitId) === String(form.unitId)) || null);

// UMUR dihitung otomatis dari TANGGAL LAHIR (migrasi generateAge legacy)
const age = computed(() => {
  if (!form.birthDate) return '';
  const dob = new Date(form.birthDate);
  if (isNaN(dob.getTime())) return '';
  const now = new Date();
  let years = now.getFullYear() - dob.getFullYear();
  let months = now.getMonth() - dob.getMonth();
  let days = now.getDate() - dob.getDate();
  if (days < 0) { months--; days += new Date(now.getFullYear(), now.getMonth(), 0).getDate(); }
  if (months < 0) { years--; months += 12; }
  return `${years} thn ${months} bln ${days} hr`;
});

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

function showConfirm(message, options = {}) {
  return new Promise((resolve) => {
    dialog.value = { visible: true, mode: 'confirm', type: options.type || 'confirm',
      title: options.title || 'KONFIRMASI', message, resolve };
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

async function loadMasters() {
  loading.value = true;
  error.value = '';
  try {
    const data = await request('/admission/registration/masters');
    masters.units = data.units;
    masters.patientTypes = data.patientTypes;
    locationOptions.provinces = data.provinces;
  } catch (requestError) {
    error.value = requestError.message;
  } finally {
    loading.value = false;
  }
}

async function searchPatients() {
  searching.value = true;
  searchError.value = '';
  searchResults.value = [];
  try {
    searchResults.value = await request(`/admission/registration/patients/search${qs(searchForm)}`);
  } catch (requestError) {
    searchError.value = requestError.message;
  } finally {
    searching.value = false;
  }
}

async function selectPatient(mrCode) {
  message.value = '';
  saveError.value = '';
  showSearchModal.value = false;
  try {
    const detail = await request(`/admission/registration/patients/${encodeURIComponent(mrCode)}`);
    assignPatientDetail(detail);
    await loadRegencies(detail.provinceCode, false);
    await loadDistricts(detail.cityCode, false);
    await loadVillages(detail.districtCode, false);
    form.subdistrictCode = detail.subdistrictCode || '';
    if (form.unitId) await loadDoctors(form.unitId);
  } catch (requestError) {
    searchError.value = requestError.message;
  }
}

function assignPatientDetail(detail) {
  form.existingMrCode = detail.mrCode || '';
  form.patientTypeId = detail.patientTypeId ? String(detail.patientTypeId) : '';
  form.patientName = detail.patientName || '';
  form.gender = detail.gender || 'M';
  form.birthDate = detail.birthDate || '';
  form.nik = detail.nik || '';
  form.mainAddress = detail.mainAddress || '';
  form.mainPhone = detail.mainPhone || '';
  form.mainRt = detail.mainRt || '';
  form.mainRw = detail.mainRw || '';
  form.altAddress = detail.altAddress || '';
  form.altPhone = detail.altPhone || '';
  form.altRt = detail.altRt || '';
  form.altRw = detail.altRw || '';
  form.maritalStatus = detail.maritalStatus || '';
  form.nationality = detail.nationality || '';
  form.religion = detail.religion || '';
  form.education = detail.education || '';
  form.jobType = detail.jobType || '';
  form.priority = detail.priority || '';
  form.etnis = detail.etnis || '';
  form.language = detail.language || '';
  form.provinceCode = detail.provinceCode || '';
  form.cityCode = detail.cityCode || '';
  form.districtCode = detail.districtCode || '';
  form.subdistrictCode = detail.subdistrictCode || '';
  form.ihsNumber = detail.ihsNumber || '';
  form.currentRegistrationCode = detail.activeRegistrationCode || '';
}

async function loadDoctors(unitId) {
  doctors.value = [];
  form.doctorStaffId = '';
  if (!unitId) return;
  doctors.value = await request(`/admission/registration/units/${unitId}/doctors`);
}

async function loadRegencies(provinceCode, resetChildren = true) {
  locationOptions.regencies = [];
  if (resetChildren) {
    form.cityCode = '';
    form.districtCode = '';
    form.subdistrictCode = '';
    locationOptions.districts = [];
    locationOptions.villages = [];
  }
  if (!provinceCode) return;
  locationOptions.regencies = await request(`/admission/registration/provinces/${encodeURIComponent(provinceCode)}/regencies`);
}

async function loadDistricts(regencyCode, resetChildren = true) {
  locationOptions.districts = [];
  if (resetChildren) {
    form.districtCode = '';
    form.subdistrictCode = '';
    locationOptions.villages = [];
  }
  if (!regencyCode) return;
  locationOptions.districts = await request(`/admission/registration/regencies/${encodeURIComponent(regencyCode)}/districts`);
}

async function loadVillages(districtCode, resetChildren = true) {
  locationOptions.villages = [];
  if (resetChildren) form.subdistrictCode = '';
  if (!districtCode) return;
  locationOptions.villages = await request(`/admission/registration/districts/${encodeURIComponent(districtCode)}/villages`);
}

function resetForNewPatient() {
  message.value = '';
  saveError.value = '';
  searchError.value = '';
  form.existingMrCode = '';
  form.patientTypeId = '';
  form.patientName = '';
  form.gender = 'M';
  form.birthDate = '';
  form.nik = '';
  form.mainAddress = '';
  form.mainPhone = '';
  form.mainRt = '';
  form.mainRw = '';
  form.altAddress = '';
  form.altPhone = '';
  form.altRt = '';
  form.altRw = '';
  form.maritalStatus = '';
  form.nationality = '';
  form.religion = '';
  form.education = '';
  form.jobType = '';
  form.priority = '';
  form.etnis = '';
  form.language = '';
  form.provinceCode = '';
  form.cityCode = '';
  form.districtCode = '';
  form.subdistrictCode = '';
  form.unitId = '';
  form.doctorStaffId = '';
  form.ihsNumber = '';
  form.currentRegistrationCode = '';
  doctors.value = [];
  locationOptions.regencies = [];
  locationOptions.districts = [];
  locationOptions.villages = [];
}

function openPatientSearch() {
  showSearchModal.value = true;
}

async function submit() {
  saving.value = true;
  saveError.value = '';
  message.value = '';
  try {
    const payload = {
      existingMrCode: form.existingMrCode || null,
      patientTypeId: form.patientTypeId ? Number(form.patientTypeId) : null,
      patientName: form.patientName,
      gender: form.gender,
      birthDate: form.birthDate,
      nik: form.nik,
      mainAddress: form.mainAddress,
      mainPhone: form.mainPhone || null,
      mainRt: form.mainRt || null,
      mainRw: form.mainRw || null,
      altAddress: form.altAddress || null,
      altPhone: form.altPhone || null,
      altRt: form.altRt || null,
      altRw: form.altRw || null,
      maritalStatus: form.maritalStatus || null,
      nationality: form.nationality || null,
      religion: form.religion || null,
      education: form.education || null,
      jobType: form.jobType || null,
      priority: form.priority || null,
      etnis: form.etnis || null,
      language: form.language || null,
      provinceCode: form.provinceCode || null,
      cityCode: form.cityCode || null,
      districtCode: form.districtCode || null,
      subdistrictCode: form.subdistrictCode || null,
      unitId: Number(form.unitId),
      doctorStaffId: Number(form.doctorStaffId)
    };
    const result = await request('/admission/registration', { method: 'POST', body: JSON.stringify(payload) });
    form.existingMrCode = result.mrCode;
    form.currentRegistrationCode = result.registrationCode;
    showToast(`Registrasi sukses: ${result.registrationCode} / Nota ${result.noteNumber}`, 'success');
  } catch (requestError) {
    saveError.value = requestError.message;
  } finally {
    saving.value = false;
  }
}

async function cancelRegistration() {
  if (!form.currentRegistrationCode) {
    await showAlert('TIDAK ADA REGISTRASI YANG AKAN DIBATALKAN!');
    return;
  }
  const ok = await showConfirm(`Batalkan registrasi ${form.currentRegistrationCode}?`, { title: 'BATAL REGISTRASI' });
  if (!ok) return;
  loading.value = true;
  saveError.value = '';
  try {
    const result = await request('/admission/registration/cancel', {
      method: 'POST',
      body: JSON.stringify({ registrationCode: form.currentRegistrationCode })
    });
    showToast(result, 'success');
    resetForNewPatient();
  } catch (requestError) {
    saveError.value = requestError.message;
  } finally {
    loading.value = false;
  }
}

function printSticker() {
  showToast('Cetak sticker belum tersedia di versi modern.', 'info');
}

function printCard() {
  showToast('Cetak kartu pasien belum tersedia di versi modern.', 'info');
}

onMounted(() => {
  loadMasters();
});
</script>

<template>
  <div>
    <p v-if="error" class="status-banner status-banner--error">{{ error }}</p>
    <p v-if="saveError" class="status-banner status-banner--error">{{ saveError }}</p>

    <!-- ==================== DATA PASIEN ==================== -->
    <div class="section-title">DATA PASIEN</div>
    <div class="patient-grid">
      <div class="field">
        <label>NO. MR</label>
        <div class="input-row">
          <input :value="form.existingMrCode" readonly placeholder="-" />
          <button class="mini primary" type="button" @click="openPatientSearch">CARI PASIEN</button>
        </div>
      </div>
      <div class="field">
        <label>NO. REGISTRASI</label>
        <input :value="form.currentRegistrationCode" readonly placeholder="-" />
      </div>
      <div class="field">
        <label>NOMOR IHS</label>
        <input :value="form.ihsNumber" readonly placeholder="-" />
      </div>
      <div class="field">
        <label>NAMA <span class="req">*</span></label>
        <input v-model="form.patientName" @input="form.patientName = form.patientName.toUpperCase()" />
      </div>
      <div class="field">
        <label>JENIS KELAMIN <span class="req">*</span></label>
        <select v-model="form.gender">
          <option value="M">PRIA</option>
          <option value="F">WANITA</option>
        </select>
      </div>
      <div class="field">
        <label>TANGGAL LAHIR <span class="req">*</span></label>
        <input v-model="form.birthDate" type="date" />
      </div>
      <div class="field">
        <label>UMUR</label>
        <input :value="age" readonly />
      </div>
      <div class="field">
        <label>NOMOR KTP / NIK <span class="req">*</span></label>
        <input v-model="form.nik" />
      </div>
      <div class="field">
        <label>ALAMAT UTAMA <span class="req">*</span></label>
        <input v-model="form.mainAddress" @input="form.mainAddress = form.mainAddress.toUpperCase()" />
      </div>
      <div class="field">
        <label>ETNIS / SUKU</label>
        <select v-model="form.etnis">
          <option v-for="item in staticOptions.etnis" :key="item" :value="item">{{ item }}</option>
        </select>
      </div>
      <div class="field">
        <label>BAHASA</label>
        <select v-model="form.language">
          <option v-for="item in staticOptions.language" :key="item" :value="item">{{ item }}</option>
        </select>
      </div>
      <div class="field">
        <label>STATUS KAWIN</label>
        <select v-model="form.maritalStatus">
          <option v-for="item in staticOptions.maritalStatus" :key="item" :value="item">{{ item }}</option>
        </select>
      </div>
      <div class="field">
        <label>NO. TELP / NO. HP</label>
        <input v-model="form.mainPhone" />
      </div>
      <div class="field">
        <label>WARGA NEGARA</label>
        <select v-model="form.nationality">
          <option v-for="item in staticOptions.nationality" :key="item" :value="item">{{ item }}</option>
        </select>
      </div>
      <div class="field">
        <label>RT / RW</label>
        <div class="rt-rw">
          <input v-model="form.mainRt" class="rt-input" placeholder="RT" />
          <span class="slash">/</span>
          <input v-model="form.mainRw" class="rt-input" placeholder="RW" />
        </div>
      </div>
      <div class="field">
        <label>PROPINSI</label>
        <select v-model="form.provinceCode" @change="loadRegencies(form.provinceCode)">
          <option value="">-</option>
          <option v-for="item in locationOptions.provinces" :key="item.value" :value="item.value">{{ item.label }}</option>
        </select>
      </div>
      <div class="field">
        <label>KABUPATEN</label>
        <select v-model="form.cityCode" @change="loadDistricts(form.cityCode)">
          <option value="">-</option>
          <option v-for="item in locationOptions.regencies" :key="item.value" :value="item.value">{{ item.label }}</option>
        </select>
      </div>
      <div class="field">
        <label>KECAMATAN</label>
        <select v-model="form.districtCode" @change="loadVillages(form.districtCode)">
          <option value="">-</option>
          <option v-for="item in locationOptions.districts" :key="item.value" :value="item.value">{{ item.label }}</option>
        </select>
      </div>
      <div class="field">
        <label>KELURAHAN / DESA</label>
        <select v-model="form.subdistrictCode">
          <option value="">-</option>
          <option v-for="item in locationOptions.villages" :key="item.value" :value="item.value">{{ item.label }}</option>
        </select>
      </div>
      <div class="field">
        <label>ALAMAT ALTERNATIF</label>
        <input v-model="form.altAddress" @input="form.altAddress = form.altAddress.toUpperCase()" />
      </div>
      <div class="field">
        <label>RT / RW (ALT)</label>
        <div class="rt-rw">
          <input v-model="form.altRt" class="rt-input" placeholder="RT" />
          <span class="slash">/</span>
          <input v-model="form.altRw" class="rt-input" placeholder="RW" />
        </div>
      </div>
      <div class="field">
        <label>NO. TELP / HP (ALT)</label>
        <input v-model="form.altPhone" />
      </div>
      <div class="field">
        <label>AGAMA</label>
        <select v-model="form.religion">
          <option v-for="item in staticOptions.religion" :key="item" :value="item">{{ item }}</option>
        </select>
      </div>
      <div class="field">
        <label>PENDIDIKAN</label>
        <select v-model="form.education">
          <option v-for="item in staticOptions.education" :key="item" :value="item">{{ item }}</option>
        </select>
      </div>
      <div class="field">
        <label>JENIS PEKERJAAN</label>
        <select v-model="form.jobType">
          <option v-for="item in staticOptions.jobType" :key="item" :value="item">{{ item }}</option>
        </select>
      </div>
      <div class="field">
        <label>TIPE PASIEN</label>
        <select v-model="form.patientTypeId">
          <option value="">-</option>
          <option v-for="item in masters.patientTypes" :key="item.patientTypeId" :value="String(item.patientTypeId)">
            {{ item.patientTypeCode }} - {{ item.patientTypeDescription }}
          </option>
        </select>
      </div>
      <div class="field">
        <label>PRIORITAS PASIEN</label>
        <select v-model="form.priority">
          <option v-for="item in staticOptions.priority" :key="item" :value="item">{{ item }}</option>
        </select>
      </div>
      <div class="field">
        <label>DIVISI <span class="req">*</span></label>
        <select v-model="form.unitId" @change="loadDoctors(form.unitId)">
          <option value="">-</option>
          <option v-for="item in masters.units" :key="item.unitId" :value="String(item.unitId)">
            {{ item.unitCode }} - {{ item.unitName }}
          </option>
        </select>
      </div>
      <div class="field">
        <label>DOKTER PEMERIKSA <span class="req">*</span></label>
        <select v-model="form.doctorStaffId">
          <option value="">-</option>
          <option v-for="item in doctors" :key="item.staffId" :value="String(item.staffId)">
            {{ item.staffName }}
          </option>
        </select>
      </div>
      <div v-if="selectedUnit" class="field">
        <label>UNIT TERPILIH</label>
        <input :value="`${selectedUnit.unitName} - Biaya registrasi ${selectedUnit.registrationCharge}`" readonly />
      </div>
    </div>

    <div class="action-bar">
      <button class="small-button primary" type="button" :disabled="saving || loading" @click="submit">💾 SIMPAN</button>
      <button class="small-button" type="button" @click="resetForNewPatient">🆕 PASIEN BARU</button>
      <button class="small-button" type="button" @click="openPatientSearch">👤 PASIEN LAMA</button>
      <button class="small-button danger" type="button" :disabled="!form.currentRegistrationCode" @click="cancelRegistration">🚫 BATAL REGISTRASI</button>
      <button class="small-button" type="button" @click="printSticker">🏷️ CETAK STICKER</button>
      <button class="small-button" type="button" @click="printCard">🖨️ CETAK</button>
    </div>

    <!-- ==================== MODAL: CARI PASIEN ==================== -->
    <div v-if="showSearchModal" class="modal-overlay" @click.self="showSearchModal = false">
      <div class="modal">
        <div class="modal-header">CARI DATA PASIEN</div>
        <div class="modal-body">
          <div class="field"><label>NO. MR</label><input v-model="searchForm.mrCode" /></div>
          <div class="field"><label>NAMA</label><input v-model="searchForm.patientName" /></div>
          <div class="field"><label>NO KTP / NIK</label><input v-model="searchForm.nik" /></div>
          <div class="field"><label>TGL. LAHIR</label><input v-model="searchForm.birthDate" type="date" /></div>
          <div class="field"><label>ALAMAT</label><input v-model="searchForm.address" /></div>
          <button class="small-button primary" type="button" :disabled="searching" @click="searchPatients">🔍 CARI</button>
          <p v-if="searchError" class="status-banner status-banner--error">{{ searchError }}</p>
          <div class="table-wrap modal-list">
            <table class="table">
              <thead>
                <tr><th>NO. MR</th><th>NAMA</th><th>NIK</th><th>TGL</th><th>ALAMAT</th><th></th></tr>
              </thead>
              <tbody>
                <tr v-for="patient in searchResults" :key="patient.mrCode">
                  <td class="strong">{{ patient.mrCode }}</td>
                  <td>{{ patient.patientName }}</td>
                  <td>{{ patient.nik }}</td>
                  <td>{{ patient.birthDate }}</td>
                  <td>{{ patient.address }}</td>
                  <td><button class="mini primary" type="button" @click="selectPatient(patient.mrCode)">PILIH</button></td>
                </tr>
                <tr v-if="!searchResults.length">
                  <td colspan="6" class="empty-state">Belum ada hasil pencarian.</td>
                </tr>
              </tbody>
            </table>
          </div>
        </div>
        <div class="modal-footer">
          <button class="small-button" type="button" @click="showSearchModal = false">TUTUP</button>
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
            <template v-if="dialog.mode === 'confirm'">
              <button class="small-button danger" type="button" @click="closeDialog(false)">✖ TIDAK</button>
              <button class="small-button primary" type="button" @click="closeDialog(true)">✔ YA</button>
            </template>
            <template v-else>
              <button class="small-button primary" type="button" @click="closeDialog(true)">✔ OK</button>
            </template>
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
.section-title { font-weight: 800; color: #304b73; font-size: 14px; margin: 14px 0 10px; padding-bottom: 6px; border-bottom: 1px solid #eef2f7; }

.status-banner { padding: 12px 16px; border-radius: 10px; font-weight: 600; margin-bottom: 12px; }
.status-banner--error { background: #fde8ea; color: #a32943; }

.patient-grid { display: grid; grid-template-columns: repeat(3, 1fr); gap: 10px 18px; }
.field { display: flex; flex-direction: column; gap: 4px; }
.field label { font-size: 11px; font-weight: 700; color: #6b7280; text-transform: uppercase; letter-spacing: 0.03em; }
.field input, .field select { padding: 7px 9px; border: 1px solid #d1d9e6; border-radius: 6px; font-size: 13px; width: 100%; box-sizing: border-box; }
.field input[readonly] { background: #f3f5f8; color: #4b5563; }
.req { color: #a32943; }
.rt-rw { display: flex; align-items: center; gap: 6px; }
.rt-input { flex: 1; }
.slash { color: #6b7280; font-weight: 700; }
.input-row { display: flex; gap: 6px; align-items: center; }
.input-row input { flex: 1; }
.mini { padding: 6px 10px; border-radius: 6px; border: 1px solid #d1d5db; background: #fff; cursor: pointer; font-weight: 600; font-size: 12px; white-space: nowrap; }
.mini.primary { background: #304b73; color: #fff; border-color: #304b73; }

.table-wrap { overflow: auto; margin: 10px 0; }
.table { width: 100%; border-collapse: collapse; font-size: 13px; }
.table th, .table td { padding: 7px 9px; border-bottom: 1px solid #eef2f7; text-align: left; white-space: nowrap; }
.table th { background: #f6f8fb; color: #304b73; }
.table tbody tr:hover { background: #f6f8fb; }
.strong { font-weight: 700; }
.empty-state { color: #9ca3af; text-align: center; padding: 16px; }

.action-bar { display: flex; flex-wrap: wrap; gap: 10px; justify-content: center; margin-top: 14px; }
.small-button { padding: 8px 14px; border-radius: 8px; border: 1px solid #d1d5db; background: #fff; cursor: pointer; font-weight: 600; font-size: 13px; }
.small-button.primary { background: #304b73; color: #fff; border-color: #304b73; }
.small-button.danger { background: #fde8ea; color: #a32943; border-color: #a32943; }
.small-button:disabled { opacity: 0.5; cursor: default; }

.modal-overlay { position: fixed; inset: 0; background: rgba(15,23,42,0.45); display: flex; align-items: center; justify-content: center; z-index: 50; }
.modal { background: #fff; border-radius: 12px; width: 680px; max-width: 94vw; max-height: 88vh; display: flex; flex-direction: column; box-shadow: 0 20px 40px rgba(0,0,0,0.2); }
.modal-header { padding: 14px 18px; background: #304b73; color: #fff; font-weight: 800; border-radius: 12px 12px 0 0; }
.modal-body { padding: 14px 18px; overflow-y: auto; display: flex; flex-direction: column; gap: 10px; }
.modal-list { max-height: 300px; }
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
.dialog-box--confirm { border-top-color: #5f83c2; }
.dialog-icon { font-size: 44px; margin-bottom: 8px; }
.dialog-title { font-size: 17px; font-weight: 800; color: #304b73; margin-bottom: 8px; }
.dialog-message { font-size: 14px; color: #4b5563; line-height: 1.5; margin-bottom: 18px; white-space: pre-line; }
.dialog-buttons { display: flex; justify-content: center; gap: 12px; }
.dialog-buttons .small-button { min-width: 110px; }

@media (max-width: 960px) {
  .patient-grid { grid-template-columns: 1fr; }
}
</style>
