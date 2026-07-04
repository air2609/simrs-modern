<script setup>
import { computed, onMounted, reactive, ref } from 'vue';

const props = defineProps({
  apiBaseUrl: {
    type: String,
    required: true
  }
});

const emit = defineEmits(['session-expired']);

const loading = ref(true);
const saving = ref(false);
const searching = ref(false);
const message = ref('');
const error = ref('');
const searchError = ref('');
const saveError = ref('');
const searchResults = ref([]);
const doctors = ref([]);
const locationOptions = reactive({
  provinces: [],
  regencies: [],
  districts: [],
  villages: []
});
const masters = reactive({
  units: [],
  patientTypes: []
});

const searchForm = reactive({
  mrCode: '',
  patientName: '',
  nik: '',
  birthDate: '',
  address: ''
});

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
const formTitle = computed(() => (form.existingMrCode ? 'Pasien Lama' : 'Pasien Baru'));

async function request(path, options = {}) {
  const response = await fetch(`${props.apiBaseUrl}${path}`, {
    credentials: 'include',
    headers: {
      'Content-Type': 'application/json',
      ...(options.headers || {})
    },
    ...options
  });
  const payload = await response.json().catch(() => null);

  if (response.status === 401) {
    emit('session-expired', payload?.message || 'Your session has been expired. You need to login again.');
    throw new Error(payload?.message || 'Unauthorized');
  }

  if (!response.ok) {
    throw new Error(payload?.message || `HTTP ${response.status}`);
  }

  return payload.data;
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
    const params = new URLSearchParams();
    Object.entries(searchForm).forEach(([key, value]) => {
      if (value) {
        params.set(key, value);
      }
    });

    searchResults.value = await request(`/admission/registration/patients/search?${params.toString()}`);
  } catch (requestError) {
    searchError.value = requestError.message;
  } finally {
    searching.value = false;
  }
}

async function selectPatient(mrCode) {
  message.value = '';
  saveError.value = '';

  try {
    const detail = await request(`/admission/registration/patients/${encodeURIComponent(mrCode)}`);
    assignPatientDetail(detail);
    await loadRegencies(detail.provinceCode, false);
    await loadDistricts(detail.cityCode, false);
    await loadVillages(detail.districtCode, false);
    form.subdistrictCode = detail.subdistrictCode || '';
    if (form.unitId) {
      await loadDoctors(form.unitId);
    }
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
  if (!unitId) {
    return;
  }

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
  if (!provinceCode) {
    return;
  }

  locationOptions.regencies = await request(`/admission/registration/provinces/${encodeURIComponent(provinceCode)}/regencies`);
}

async function loadDistricts(regencyCode, resetChildren = true) {
  locationOptions.districts = [];
  if (resetChildren) {
    form.districtCode = '';
    form.subdistrictCode = '';
    locationOptions.villages = [];
  }
  if (!regencyCode) {
    return;
  }

  locationOptions.districts = await request(`/admission/registration/regencies/${encodeURIComponent(regencyCode)}/districts`);
}

async function loadVillages(districtCode, resetChildren = true) {
  locationOptions.villages = [];
  if (resetChildren) {
    form.subdistrictCode = '';
  }
  if (!districtCode) {
    return;
  }

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

    const result = await request('/admission/registration', {
      method: 'POST',
      body: JSON.stringify(payload)
    });

    form.existingMrCode = result.mrCode;
    form.currentRegistrationCode = result.registrationCode;
    message.value = `Registrasi sukses: ${result.registrationCode} / Nota ${result.noteNumber}`;
  } catch (requestError) {
    saveError.value = requestError.message;
  } finally {
    saving.value = false;
  }
}

onMounted(() => {
  loadMasters();
});
</script>

<template>
  <section class="registration-wrapper">
    <header class="section-header">
      <div>
        <p class="section-kicker">Admisi</p>
        <h2>Form Pendaftaran Pasien Rawat Jalan</h2>
        <p class="section-copy">Cari pasien lama atau buat registrasi pasien baru langsung ke database existing.</p>
      </div>
      <div class="header-actions">
        <button class="secondary-button" type="button" @click="resetForNewPatient">Pasien Baru</button>
      </div>
    </header>

    <p v-if="error" class="status-banner status-banner--error">{{ error }}</p>
    <p v-else-if="message" class="status-banner status-banner--success">{{ message }}</p>
    <p v-if="saveError" class="status-banner status-banner--error">{{ saveError }}</p>

    <div class="layout-grid">
      <section class="panel-card">
        <h3>Pencarian Pasien Lama</h3>
        <div class="search-grid">
          <label>
            <span>No. MR</span>
            <input v-model="searchForm.mrCode" type="text" />
          </label>
          <label>
            <span>Nama</span>
            <input v-model="searchForm.patientName" type="text" />
          </label>
          <label>
            <span>No KTP / NIK</span>
            <input v-model="searchForm.nik" type="text" />
          </label>
          <label>
            <span>Tgl. Lahir</span>
            <input v-model="searchForm.birthDate" type="date" />
          </label>
          <label class="search-grid__wide">
            <span>Alamat</span>
            <input v-model="searchForm.address" type="text" />
          </label>
        </div>

        <div class="button-row">
          <button class="primary-button" type="button" :disabled="searching" @click="searchPatients">
            {{ searching ? 'Mencari...' : 'Cari Pasien' }}
          </button>
        </div>

        <p v-if="searchError" class="inline-error">{{ searchError }}</p>

        <div class="table-wrap">
          <table class="result-table">
            <thead>
              <tr>
                <th>No. MR</th>
                <th>Nama</th>
                <th>NIK</th>
                <th>Tgl. Lahir</th>
                <th>Alamat</th>
                <th />
              </tr>
            </thead>
            <tbody>
              <tr v-for="patient in searchResults" :key="patient.mrCode">
                <td>{{ patient.mrCode }}</td>
                <td>{{ patient.patientName }}</td>
                <td>{{ patient.nik }}</td>
                <td>{{ patient.birthDate }}</td>
                <td>{{ patient.address }}</td>
                <td>
                  <button class="link-button" type="button" @click="selectPatient(patient.mrCode)">Pilih</button>
                </td>
              </tr>
              <tr v-if="!searchResults.length">
                <td colspan="6" class="empty-cell">Belum ada hasil pencarian.</td>
              </tr>
            </tbody>
          </table>
        </div>
      </section>

      <section class="panel-card">
        <h3>Data Pasien - {{ formTitle }}</h3>

        <div class="badge-row">
          <span class="info-badge">MR: {{ form.existingMrCode || '-' }}</span>
          <span class="info-badge">Registrasi aktif: {{ form.currentRegistrationCode || '-' }}</span>
          <span class="info-badge">IHS: {{ form.ihsNumber || '-' }}</span>
        </div>

        <div class="form-grid">
          <label>
            <span>Nama</span>
            <input v-model="form.patientName" type="text" @input="form.patientName = form.patientName.toUpperCase()" />
          </label>
          <label>
            <span>Jenis Kelamin</span>
            <select v-model="form.gender">
              <option value="M">PRIA</option>
              <option value="F">WANITA</option>
            </select>
          </label>
          <label>
            <span>Tanggal Lahir</span>
            <input v-model="form.birthDate" type="date" />
          </label>
          <label>
            <span>No KTP / NIK</span>
            <input v-model="form.nik" type="text" />
          </label>
          <label class="form-grid__wide">
            <span>Alamat Utama</span>
            <textarea v-model="form.mainAddress" rows="2" @input="form.mainAddress = form.mainAddress.toUpperCase()" />
          </label>
          <label>
            <span>No. Telp / HP</span>
            <input v-model="form.mainPhone" type="text" />
          </label>
          <label>
            <span>RT</span>
            <input v-model="form.mainRt" type="text" />
          </label>
          <label>
            <span>RW</span>
            <input v-model="form.mainRw" type="text" />
          </label>
          <label>
            <span>Tipe Pasien</span>
            <select v-model="form.patientTypeId">
              <option value="">-</option>
              <option v-for="item in masters.patientTypes" :key="item.patientTypeId" :value="String(item.patientTypeId)">
                {{ item.patientTypeCode }} - {{ item.patientTypeDescription }}
              </option>
            </select>
          </label>
          <label>
            <span>Etnis / Suku</span>
            <select v-model="form.etnis">
              <option v-for="item in staticOptions.etnis" :key="item" :value="item">{{ item }}</option>
            </select>
          </label>
          <label>
            <span>Bahasa</span>
            <select v-model="form.language">
              <option v-for="item in staticOptions.language" :key="item" :value="item">{{ item }}</option>
            </select>
          </label>
          <label>
            <span>Status Kawin</span>
            <select v-model="form.maritalStatus">
              <option v-for="item in staticOptions.maritalStatus" :key="item" :value="item">{{ item }}</option>
            </select>
          </label>
          <label>
            <span>Warga Negara</span>
            <select v-model="form.nationality">
              <option v-for="item in staticOptions.nationality" :key="item" :value="item">{{ item }}</option>
            </select>
          </label>
          <label>
            <span>Agama</span>
            <select v-model="form.religion">
              <option v-for="item in staticOptions.religion" :key="item" :value="item">{{ item }}</option>
            </select>
          </label>
          <label>
            <span>Pendidikan</span>
            <select v-model="form.education">
              <option v-for="item in staticOptions.education" :key="item" :value="item">{{ item }}</option>
            </select>
          </label>
          <label>
            <span>Jenis Pekerjaan</span>
            <select v-model="form.jobType">
              <option v-for="item in staticOptions.jobType" :key="item" :value="item">{{ item }}</option>
            </select>
          </label>
          <label>
            <span>Prioritas</span>
            <select v-model="form.priority">
              <option v-for="item in staticOptions.priority" :key="item" :value="item">{{ item }}</option>
            </select>
          </label>
          <label>
            <span>Propinsi</span>
            <select v-model="form.provinceCode" @change="loadRegencies(form.provinceCode)">
              <option value="">-</option>
              <option v-for="item in locationOptions.provinces" :key="item.value" :value="item.value">{{ item.label }}</option>
            </select>
          </label>
          <label>
            <span>Kabupaten</span>
            <select v-model="form.cityCode" @change="loadDistricts(form.cityCode)">
              <option value="">-</option>
              <option v-for="item in locationOptions.regencies" :key="item.value" :value="item.value">{{ item.label }}</option>
            </select>
          </label>
          <label>
            <span>Kecamatan</span>
            <select v-model="form.districtCode" @change="loadVillages(form.districtCode)">
              <option value="">-</option>
              <option v-for="item in locationOptions.districts" :key="item.value" :value="item.value">{{ item.label }}</option>
            </select>
          </label>
          <label>
            <span>Kelurahan / Desa</span>
            <select v-model="form.subdistrictCode">
              <option value="">-</option>
              <option v-for="item in locationOptions.villages" :key="item.value" :value="item.value">{{ item.label }}</option>
            </select>
          </label>
          <label class="form-grid__wide">
            <span>Alamat Alternatif</span>
            <textarea v-model="form.altAddress" rows="2" @input="form.altAddress = form.altAddress.toUpperCase()" />
          </label>
          <label>
            <span>Telp Alternatif</span>
            <input v-model="form.altPhone" type="text" />
          </label>
          <label>
            <span>RT Alternatif</span>
            <input v-model="form.altRt" type="text" />
          </label>
          <label>
            <span>RW Alternatif</span>
            <input v-model="form.altRw" type="text" />
          </label>
          <label>
            <span>Unit Layanan</span>
            <select v-model="form.unitId" @change="loadDoctors(form.unitId)">
              <option value="">-</option>
              <option v-for="item in masters.units" :key="item.unitId" :value="String(item.unitId)">
                {{ item.unitCode }} - {{ item.unitName }}
              </option>
            </select>
          </label>
          <label>
            <span>Dokter Pemeriksa</span>
            <select v-model="form.doctorStaffId">
              <option value="">-</option>
              <option v-for="item in doctors" :key="item.staffId" :value="String(item.staffId)">
                {{ item.staffName }}
              </option>
            </select>
          </label>
        </div>

        <div v-if="selectedUnit" class="unit-meta">
          <strong>Unit terpilih:</strong> {{ selectedUnit.unitName }} - Biaya registrasi {{ selectedUnit.registrationCharge }}
        </div>

        <div class="button-row">
          <button class="primary-button" type="button" :disabled="saving || loading" @click="submit">
            {{ saving ? 'Menyimpan...' : 'Simpan Registrasi' }}
          </button>
        </div>
      </section>
    </div>
  </section>
</template>

<style scoped>
.registration-wrapper {
  display: grid;
  gap: 20px;
}

.section-header {
  display: flex;
  justify-content: space-between;
  gap: 20px;
  align-items: start;
}

.section-kicker {
  margin: 0 0 6px;
  text-transform: uppercase;
  letter-spacing: 0.08em;
  font-size: 12px;
  color: #5f83c2;
}

.section-header h2,
.panel-card h3 {
  margin: 0;
}

.section-copy {
  margin: 10px 0 0;
  color: #4b5565;
}

.layout-grid {
  display: grid;
  grid-template-columns: minmax(300px, 460px) 1fr;
  gap: 20px;
}

.panel-card {
  padding: 20px;
  background: rgba(255, 255, 255, 0.95);
  border: 1px solid rgba(150, 136, 117, 0.35);
  box-shadow: 0 12px 24px rgba(53, 64, 84, 0.08);
}

.status-banner {
  margin: 0;
  padding: 12px 14px;
  border-left: 4px solid #5f83c2;
  background: #eef4ff;
}

.status-banner--success {
  border-left-color: #3a8f5f;
  background: #ecf8f0;
}

.status-banner--error,
.inline-error {
  border-left-color: #b53e3e;
  background: #fff1f1;
}

.search-grid,
.form-grid {
  display: grid;
  gap: 14px;
}

.search-grid {
  grid-template-columns: repeat(2, minmax(0, 1fr));
  margin-top: 16px;
}

.form-grid {
  grid-template-columns: repeat(3, minmax(0, 1fr));
  margin-top: 16px;
}

.search-grid__wide,
.form-grid__wide {
  grid-column: 1 / -1;
}

label {
  display: grid;
  gap: 6px;
  font-size: 13px;
  font-weight: 700;
  color: #304b73;
}

input,
select,
textarea {
  width: 100%;
  min-height: 38px;
  padding: 8px 10px;
  border: 1px solid #968875;
  background: #f2f1ee;
  font: inherit;
  color: #222;
}

textarea {
  resize: vertical;
}

.button-row {
  display: flex;
  gap: 12px;
  margin-top: 16px;
}

.primary-button,
.secondary-button,
.link-button {
  border: 1px solid #4d6ba0;
  background: #5f83c2;
  color: #fff;
  font-weight: 700;
  cursor: pointer;
}

.primary-button,
.secondary-button {
  min-height: 40px;
  padding: 0 18px;
}

.secondary-button {
  background: #6b7280;
  border-color: #6b7280;
}

.link-button {
  min-height: 28px;
  padding: 0 10px;
  font-size: 12px;
}

.table-wrap {
  overflow: auto;
  margin-top: 14px;
}

.result-table {
  width: 100%;
  border-collapse: collapse;
  font-size: 13px;
}

.result-table th,
.result-table td {
  border: 1px solid #d8d3cb;
  padding: 8px;
  text-align: left;
  vertical-align: top;
}

.result-table th {
  background: #e6edf9;
}

.empty-cell {
  text-align: center;
  color: #6b7280;
}

.badge-row {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  margin-top: 14px;
}

.info-badge {
  display: inline-flex;
  align-items: center;
  min-height: 32px;
  padding: 0 12px;
  border-radius: 999px;
  background: #edf2fa;
  color: #304b73;
  font-size: 12px;
  font-weight: 700;
}

.unit-meta {
  margin-top: 14px;
  padding: 10px 12px;
  background: #f7f5f1;
  border: 1px solid #ddd4c9;
}

@media (max-width: 1100px) {
  .layout-grid {
    grid-template-columns: 1fr;
  }

  .form-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 760px) {
  .search-grid,
  .form-grid {
    grid-template-columns: 1fr;
  }

  .section-header {
    flex-direction: column;
  }
}
</style>
