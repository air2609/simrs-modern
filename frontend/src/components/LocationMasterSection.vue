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
const error = ref('');
const message = ref('');
const activeTab = ref('province');

const provinces = ref([]);
const regencies = ref([]);
const districts = ref([]);
const villages = ref([]);

const selectedProvinceId = ref('');
const selectedRegencyId = ref('');
const selectedDistrictId = ref('');
const selectedVillageId = ref('');

const provinceForm = reactive({
  id: '',
  code: '',
  name: ''
});

const regencyForm = reactive({
  id: '',
  parentId: '',
  code: '',
  name: ''
});

const districtForm = reactive({
  id: '',
  parentId: '',
  code: '',
  name: ''
});

const villageForm = reactive({
  id: '',
  parentId: '',
  code: '',
  name: ''
});

const currentProvince = computed(() => {
  return provinces.value.find((item) => String(item.provinceId) === String(selectedProvinceId.value)) || null;
});

const currentRegency = computed(() => {
  return regencies.value.find((item) => String(item.regencyId) === String(selectedRegencyId.value)) || null;
});

const currentDistrict = computed(() => {
  return districts.value.find((item) => String(item.districtId) === String(selectedDistrictId.value)) || null;
});

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

function resetProvinceForm() {
  provinceForm.id = '';
  provinceForm.code = '';
  provinceForm.name = '';
}

function resetRegencyForm() {
  regencyForm.id = '';
  regencyForm.parentId = selectedProvinceId.value ? String(selectedProvinceId.value) : '';
  regencyForm.code = '';
  regencyForm.name = '';
}

function resetDistrictForm() {
  districtForm.id = '';
  districtForm.parentId = selectedRegencyId.value ? String(selectedRegencyId.value) : '';
  districtForm.code = '';
  districtForm.name = '';
}

function resetVillageForm() {
  villageForm.id = '';
  villageForm.parentId = selectedDistrictId.value ? String(selectedDistrictId.value) : '';
  villageForm.code = '';
  villageForm.name = '';
}

function applyProvince(row) {
  selectedProvinceId.value = String(row.provinceId);
  provinceForm.id = String(row.provinceId);
  provinceForm.code = row.provinceCode || '';
  provinceForm.name = row.provinceName || '';
  regencyForm.parentId = String(row.provinceId);
}

function applyRegency(row) {
  selectedRegencyId.value = String(row.regencyId);
  regencyForm.id = String(row.regencyId);
  regencyForm.parentId = String(row.provinceId);
  regencyForm.code = row.regencyCode || '';
  regencyForm.name = row.regencyName || '';
  districtForm.parentId = String(row.regencyId);
}

function applyDistrict(row) {
  selectedDistrictId.value = String(row.districtId);
  districtForm.id = String(row.districtId);
  districtForm.parentId = String(row.regencyId);
  districtForm.code = row.districtCode || '';
  districtForm.name = row.districtName || '';
  villageForm.parentId = String(row.districtId);
}

function applyVillage(row) {
  selectedVillageId.value = String(row.villageId);
  villageForm.id = String(row.villageId);
  villageForm.parentId = String(row.districtId);
  villageForm.code = row.villageCode || '';
  villageForm.name = row.villageName || '';
}

async function loadProvinces(selectId = '') {
  provinces.value = await request('/master/locations/provinces');

  if (!selectId) {
    const currentExists = provinces.value.some((item) => String(item.provinceId) === String(selectedProvinceId.value));
    if (!currentExists) {
      selectedProvinceId.value = '';
      resetProvinceForm();
    }
    return;
  }

  const row = provinces.value.find((item) => String(item.provinceId) === String(selectId));
  if (row) {
    applyProvince(row);
  }
}

async function loadRegencies(provinceId, selectId = '') {
  selectedProvinceId.value = provinceId ? String(provinceId) : '';
  regencies.value = [];
  districts.value = [];
  villages.value = [];
  selectedRegencyId.value = '';
  selectedDistrictId.value = '';
  selectedVillageId.value = '';
  resetRegencyForm();
  resetDistrictForm();
  resetVillageForm();

  if (!provinceId) {
    return;
  }

  regencies.value = await request(`/master/locations/provinces/${provinceId}/regencies`);
  regencyForm.parentId = String(provinceId);

  if (selectId) {
    const row = regencies.value.find((item) => String(item.regencyId) === String(selectId));
    if (row) {
      applyRegency(row);
    }
  }
}

async function loadDistricts(regencyId, selectId = '') {
  selectedRegencyId.value = regencyId ? String(regencyId) : '';
  districts.value = [];
  villages.value = [];
  selectedDistrictId.value = '';
  selectedVillageId.value = '';
  resetDistrictForm();
  resetVillageForm();

  if (!regencyId) {
    return;
  }

  districts.value = await request(`/master/locations/regencies/${regencyId}/districts`);
  districtForm.parentId = String(regencyId);

  if (selectId) {
    const row = districts.value.find((item) => String(item.districtId) === String(selectId));
    if (row) {
      applyDistrict(row);
    }
  }
}

async function loadVillages(districtId, selectId = '') {
  selectedDistrictId.value = districtId ? String(districtId) : '';
  villages.value = [];
  selectedVillageId.value = '';
  resetVillageForm();

  if (!districtId) {
    return;
  }

  villages.value = await request(`/master/locations/districts/${districtId}/villages`);
  villageForm.parentId = String(districtId);

  if (selectId) {
    const row = villages.value.find((item) => String(item.villageId) === String(selectId));
    if (row) {
      applyVillage(row);
    }
  }
}

async function initialize() {
  loading.value = true;
  error.value = '';

  try {
    await loadProvinces();
  } catch (requestError) {
    error.value = requestError.message;
  } finally {
    loading.value = false;
  }
}

async function chooseProvince(row) {
  error.value = '';
  message.value = '';
  applyProvince(row);
  await loadRegencies(row.provinceId);
}

async function chooseRegency(row) {
  error.value = '';
  message.value = '';
  if (String(selectedProvinceId.value) !== String(row.provinceId)) {
    selectedProvinceId.value = String(row.provinceId);
    await loadProvinces(row.provinceId);
  }
  applyRegency(row);
  await loadDistricts(row.regencyId);
}

async function chooseDistrict(row) {
  error.value = '';
  message.value = '';
  if (String(selectedRegencyId.value) !== String(row.regencyId)) {
    selectedRegencyId.value = String(row.regencyId);
    await loadDistricts(row.regencyId);
  }
  applyDistrict(row);
  await loadVillages(row.districtId);
}

function chooseVillage(row) {
  error.value = '';
  message.value = '';
  applyVillage(row);
}

async function submitProvince() {
  saving.value = true;
  error.value = '';
  message.value = '';

  try {
    const isUpdate = Boolean(provinceForm.id);
    const method = provinceForm.id ? 'PUT' : 'POST';
    const path = provinceForm.id
      ? `/master/locations/provinces/${provinceForm.id}`
      : '/master/locations/provinces';
    const province = await request(path, {
      method,
      body: JSON.stringify({
        code: provinceForm.code,
        name: provinceForm.name
      })
    });
    await loadProvinces(province.provinceId);
    await loadRegencies(province.provinceId);
    message.value = isUpdate ? 'Data propinsi berhasil diubah.' : 'Data propinsi berhasil ditambahkan.';
  } catch (requestError) {
    error.value = requestError.message;
  } finally {
    saving.value = false;
  }
}

async function submitRegency() {
  saving.value = true;
  error.value = '';
  message.value = '';

  try {
    const isUpdate = Boolean(regencyForm.id);
    const method = regencyForm.id ? 'PUT' : 'POST';
    const path = regencyForm.id
      ? `/master/locations/regencies/${regencyForm.id}`
      : '/master/locations/regencies';
    const regency = await request(path, {
      method,
      body: JSON.stringify({
        parentId: regencyForm.parentId ? Number(regencyForm.parentId) : null,
        code: regencyForm.code,
        name: regencyForm.name
      })
    });
    await loadRegencies(regency.provinceId, regency.regencyId);
    message.value = isUpdate ? 'Data kabupaten berhasil diubah.' : 'Data kabupaten berhasil ditambahkan.';
  } catch (requestError) {
    error.value = requestError.message;
  } finally {
    saving.value = false;
  }
}

async function submitDistrict() {
  saving.value = true;
  error.value = '';
  message.value = '';

  try {
    const isUpdate = Boolean(districtForm.id);
    const method = districtForm.id ? 'PUT' : 'POST';
    const path = districtForm.id
      ? `/master/locations/districts/${districtForm.id}`
      : '/master/locations/districts';
    const district = await request(path, {
      method,
      body: JSON.stringify({
        parentId: districtForm.parentId ? Number(districtForm.parentId) : null,
        code: districtForm.code,
        name: districtForm.name
      })
    });
    await loadDistricts(district.regencyId, district.districtId);
    message.value = isUpdate ? 'Data kecamatan berhasil diubah.' : 'Data kecamatan berhasil ditambahkan.';
  } catch (requestError) {
    error.value = requestError.message;
  } finally {
    saving.value = false;
  }
}

async function submitVillage() {
  saving.value = true;
  error.value = '';
  message.value = '';

  try {
    const isUpdate = Boolean(villageForm.id);
    const method = villageForm.id ? 'PUT' : 'POST';
    const path = villageForm.id
      ? `/master/locations/villages/${villageForm.id}`
      : '/master/locations/villages';
    const village = await request(path, {
      method,
      body: JSON.stringify({
        parentId: villageForm.parentId ? Number(villageForm.parentId) : null,
        code: villageForm.code,
        name: villageForm.name
      })
    });
    await loadVillages(village.districtId, village.villageId);
    message.value = isUpdate ? 'Data kelurahan berhasil diubah.' : 'Data kelurahan berhasil ditambahkan.';
  } catch (requestError) {
    error.value = requestError.message;
  } finally {
    saving.value = false;
  }
}

async function removeProvince() {
  if (!provinceForm.id || !window.confirm('Hapus data propinsi terpilih?')) {
    return;
  }

  saving.value = true;
  error.value = '';
  message.value = '';

  try {
    await request(`/master/locations/provinces/${provinceForm.id}`, { method: 'DELETE' });
    await loadProvinces();
    regencies.value = [];
    districts.value = [];
    villages.value = [];
    selectedProvinceId.value = '';
    resetProvinceForm();
    resetRegencyForm();
    resetDistrictForm();
    resetVillageForm();
    message.value = 'Data propinsi berhasil dihapus.';
  } catch (requestError) {
    error.value = requestError.message;
  } finally {
    saving.value = false;
  }
}

async function removeRegency() {
  if (!regencyForm.id || !window.confirm('Hapus data kabupaten terpilih?')) {
    return;
  }

  saving.value = true;
  error.value = '';
  message.value = '';

  try {
    const provinceId = regencyForm.parentId;
    await request(`/master/locations/regencies/${regencyForm.id}`, { method: 'DELETE' });
    await loadRegencies(provinceId);
    message.value = 'Data kabupaten berhasil dihapus.';
  } catch (requestError) {
    error.value = requestError.message;
  } finally {
    saving.value = false;
  }
}

async function removeDistrict() {
  if (!districtForm.id || !window.confirm('Hapus data kecamatan terpilih?')) {
    return;
  }

  saving.value = true;
  error.value = '';
  message.value = '';

  try {
    const regencyId = districtForm.parentId;
    await request(`/master/locations/districts/${districtForm.id}`, { method: 'DELETE' });
    await loadDistricts(regencyId);
    message.value = 'Data kecamatan berhasil dihapus.';
  } catch (requestError) {
    error.value = requestError.message;
  } finally {
    saving.value = false;
  }
}

async function removeVillage() {
  if (!villageForm.id || !window.confirm('Hapus data kelurahan terpilih?')) {
    return;
  }

  saving.value = true;
  error.value = '';
  message.value = '';

  try {
    const districtId = villageForm.parentId;
    await request(`/master/locations/villages/${villageForm.id}`, { method: 'DELETE' });
    await loadVillages(districtId);
    message.value = 'Data kelurahan berhasil dihapus.';
  } catch (requestError) {
    error.value = requestError.message;
  } finally {
    saving.value = false;
  }
}

onMounted(() => {
  initialize();
});
</script>

<template>
  <section class="location-master">
    <header class="section-header">
      <div>
        <p class="eyebrow">Master</p>
        <h2>Master Wilayah</h2>
        <p class="subcopy">
          Satu layar untuk memelihara data propinsi, kabupaten, kecamatan, dan kelurahan pada tabel existing.
        </p>
      </div>
      <div class="tab-strip">
        <button type="button" :class="['tab-button', { active: activeTab === 'province' }]" @click="activeTab = 'province'">Propinsi</button>
        <button type="button" :class="['tab-button', { active: activeTab === 'regency' }]" @click="activeTab = 'regency'">Kabupaten</button>
        <button type="button" :class="['tab-button', { active: activeTab === 'district' }]" @click="activeTab = 'district'">Kecamatan</button>
        <button type="button" :class="['tab-button', { active: activeTab === 'village' }]" @click="activeTab = 'village'">Kelurahan</button>
      </div>
    </header>

    <p v-if="message" class="flash success">{{ message }}</p>
    <p v-if="error" class="flash error">{{ error }}</p>
    <p v-if="loading" class="flash neutral">Memuat master wilayah...</p>

    <div v-else class="panel-shell">
      <section v-show="activeTab === 'province'" class="panel-card">
        <div class="panel-grid">
          <div class="form-card">
            <h3>Form Propinsi</h3>
            <label>
              Kode
              <input v-model="provinceForm.code" type="text" />
            </label>
            <label>
              Nama
              <input v-model="provinceForm.name" type="text" />
            </label>
            <div class="action-row">
              <button type="button" class="primary" :disabled="saving" @click="submitProvince">Simpan</button>
              <button type="button" :disabled="saving" @click="resetProvinceForm">Batal</button>
              <button type="button" class="danger" :disabled="saving || !provinceForm.id" @click="removeProvince">Hapus</button>
            </div>
          </div>
          <div class="table-card">
            <h3>Data Propinsi</h3>
            <table>
              <thead>
                <tr>
                  <th>Kode</th>
                  <th>Nama</th>
                </tr>
              </thead>
              <tbody>
                <tr
                  v-for="row in provinces"
                  :key="row.provinceId"
                  :class="{ selected: String(row.provinceId) === String(selectedProvinceId) }"
                  @click="chooseProvince(row)"
                >
                  <td>{{ row.provinceCode }}</td>
                  <td>{{ row.provinceName }}</td>
                </tr>
              </tbody>
            </table>
          </div>
        </div>
      </section>

      <section v-show="activeTab === 'regency'" class="panel-card">
        <div class="panel-grid">
          <div class="form-card">
            <h3>Form Kabupaten</h3>
            <label>
              Propinsi
              <select v-model="regencyForm.parentId" @change="loadRegencies(regencyForm.parentId)">
                <option value="">Pilih propinsi</option>
                <option v-for="item in provinces" :key="item.provinceId" :value="String(item.provinceId)">
                  {{ item.provinceCode }} - {{ item.provinceName }}
                </option>
              </select>
            </label>
            <label>
              Kode
              <input v-model="regencyForm.code" type="text" />
            </label>
            <label>
              Nama
              <input v-model="regencyForm.name" type="text" />
            </label>
            <div class="action-row">
              <button type="button" class="primary" :disabled="saving" @click="submitRegency">Simpan</button>
              <button type="button" :disabled="saving" @click="resetRegencyForm">Batal</button>
              <button type="button" class="danger" :disabled="saving || !regencyForm.id" @click="removeRegency">Hapus</button>
            </div>
          </div>
          <div class="table-card">
            <h3>Data Kabupaten</h3>
            <p class="caption">{{ currentProvince ? `Propinsi aktif: ${currentProvince.provinceName}` : 'Pilih propinsi untuk memuat kabupaten.' }}</p>
            <table>
              <thead>
                <tr>
                  <th>Kode</th>
                  <th>Nama</th>
                </tr>
              </thead>
              <tbody>
                <tr
                  v-for="row in regencies"
                  :key="row.regencyId"
                  :class="{ selected: String(row.regencyId) === String(selectedRegencyId) }"
                  @click="chooseRegency(row)"
                >
                  <td>{{ row.regencyCode }}</td>
                  <td>{{ row.regencyName }}</td>
                </tr>
              </tbody>
            </table>
          </div>
        </div>
      </section>

      <section v-show="activeTab === 'district'" class="panel-card">
        <div class="panel-grid">
          <div class="form-card">
            <h3>Form Kecamatan</h3>
            <label>
              Kabupaten
              <select v-model="districtForm.parentId" @change="loadDistricts(districtForm.parentId)">
                <option value="">Pilih kabupaten</option>
                <option v-for="item in regencies" :key="item.regencyId" :value="String(item.regencyId)">
                  {{ item.regencyCode }} - {{ item.regencyName }}
                </option>
              </select>
            </label>
            <label>
              Kode
              <input v-model="districtForm.code" type="text" />
            </label>
            <label>
              Nama
              <input v-model="districtForm.name" type="text" />
            </label>
            <div class="action-row">
              <button type="button" class="primary" :disabled="saving" @click="submitDistrict">Simpan</button>
              <button type="button" :disabled="saving" @click="resetDistrictForm">Batal</button>
              <button type="button" class="danger" :disabled="saving || !districtForm.id" @click="removeDistrict">Hapus</button>
            </div>
          </div>
          <div class="table-card">
            <h3>Data Kecamatan</h3>
            <p class="caption">{{ currentRegency ? `Kabupaten aktif: ${currentRegency.regencyName}` : 'Pilih kabupaten untuk memuat kecamatan.' }}</p>
            <table>
              <thead>
                <tr>
                  <th>Kode</th>
                  <th>Nama</th>
                </tr>
              </thead>
              <tbody>
                <tr
                  v-for="row in districts"
                  :key="row.districtId"
                  :class="{ selected: String(row.districtId) === String(selectedDistrictId) }"
                  @click="chooseDistrict(row)"
                >
                  <td>{{ row.districtCode }}</td>
                  <td>{{ row.districtName }}</td>
                </tr>
              </tbody>
            </table>
          </div>
        </div>
      </section>

      <section v-show="activeTab === 'village'" class="panel-card">
        <div class="panel-grid">
          <div class="form-card">
            <h3>Form Kelurahan</h3>
            <label>
              Kecamatan
              <select v-model="villageForm.parentId" @change="loadVillages(villageForm.parentId)">
                <option value="">Pilih kecamatan</option>
                <option v-for="item in districts" :key="item.districtId" :value="String(item.districtId)">
                  {{ item.districtCode }} - {{ item.districtName }}
                </option>
              </select>
            </label>
            <label>
              Kode
              <input v-model="villageForm.code" type="text" />
            </label>
            <label>
              Nama
              <input v-model="villageForm.name" type="text" />
            </label>
            <div class="action-row">
              <button type="button" class="primary" :disabled="saving" @click="submitVillage">Simpan</button>
              <button type="button" :disabled="saving" @click="resetVillageForm">Batal</button>
              <button type="button" class="danger" :disabled="saving || !villageForm.id" @click="removeVillage">Hapus</button>
            </div>
          </div>
          <div class="table-card">
            <h3>Data Kelurahan</h3>
            <p class="caption">{{ currentDistrict ? `Kecamatan aktif: ${currentDistrict.districtName}` : 'Pilih kecamatan untuk memuat kelurahan.' }}</p>
            <table>
              <thead>
                <tr>
                  <th>Kode</th>
                  <th>Nama</th>
                </tr>
              </thead>
              <tbody>
                <tr
                  v-for="row in villages"
                  :key="row.villageId"
                  :class="{ selected: String(row.villageId) === String(selectedVillageId) }"
                  @click="chooseVillage(row)"
                >
                  <td>{{ row.villageCode }}</td>
                  <td>{{ row.villageName }}</td>
                </tr>
              </tbody>
            </table>
          </div>
        </div>
      </section>
    </div>
  </section>
</template>

<style scoped>
.location-master {
  display: grid;
  gap: 18px;
}

.section-header,
.panel-card,
.flash {
  background: rgba(255, 255, 255, 0.94);
  border: 1px solid rgba(150, 136, 117, 0.35);
  box-shadow: 0 12px 24px rgba(53, 64, 84, 0.08);
}

.section-header {
  padding: 24px;
  display: flex;
  justify-content: space-between;
  gap: 20px;
}

.eyebrow {
  margin: 0 0 6px;
  text-transform: uppercase;
  letter-spacing: 0.08em;
  font-size: 12px;
}

.section-header h2,
.form-card h3,
.table-card h3 {
  margin: 0;
}

.subcopy,
.caption {
  color: #5a667b;
}

.tab-strip {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  align-content: flex-start;
}

.tab-button {
  min-height: 36px;
  padding: 0 14px;
  border: 1px solid rgba(95, 131, 194, 0.35);
  background: #fff;
  color: #304b73;
  font-weight: 700;
  cursor: pointer;
}

.tab-button.active {
  background: #304b73;
  color: #fff;
}

.flash {
  margin: 0;
  padding: 14px 16px;
}

.flash.success {
  border-color: rgba(38, 125, 65, 0.35);
  color: #267d41;
}

.flash.error {
  border-color: rgba(173, 58, 58, 0.35);
  color: #ad3a3a;
}

.flash.neutral {
  color: #304b73;
}

.panel-grid {
  display: grid;
  grid-template-columns: 320px 1fr;
}

.form-card,
.table-card {
  padding: 20px;
}

.form-card {
  border-right: 1px solid rgba(150, 136, 117, 0.2);
  display: grid;
  gap: 12px;
}

.form-card label {
  display: grid;
  gap: 6px;
  font-weight: 700;
  color: #304b73;
}

.form-card input,
.form-card select {
  height: 38px;
  border: 1px solid rgba(95, 131, 194, 0.35);
  padding: 0 10px;
  font: inherit;
}

.action-row {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  margin-top: 4px;
}

.action-row button {
  min-width: 96px;
  height: 38px;
  border: 1px solid rgba(95, 131, 194, 0.35);
  background: #fff;
  color: #304b73;
  font-weight: 700;
  cursor: pointer;
}

.action-row button.primary {
  background: #5f83c2;
  color: #fff;
  border-color: #5f83c2;
}

.action-row button.danger {
  border-color: rgba(173, 58, 58, 0.35);
  color: #ad3a3a;
}

.action-row button:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.table-card table {
  width: 100%;
  border-collapse: collapse;
  margin-top: 12px;
}

.table-card th,
.table-card td {
  padding: 10px 12px;
  border-bottom: 1px solid rgba(150, 136, 117, 0.18);
  text-align: left;
}

.table-card tbody tr {
  cursor: pointer;
}

.table-card tbody tr.selected {
  background: rgba(95, 131, 194, 0.12);
}

@media (max-width: 1080px) {
  .section-header,
  .panel-grid {
    grid-template-columns: 1fr;
    display: grid;
  }

  .form-card {
    border-right: 0;
    border-bottom: 1px solid rgba(150, 136, 117, 0.2);
  }
}
</style>
