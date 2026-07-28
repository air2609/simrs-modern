<script setup>
import { computed, onMounted, reactive, ref } from 'vue';

const props = defineProps({
  apiBaseUrl: { type: String, required: true }
});

const emit = defineEmits(['session-expired']);

const loading = ref(true);
const saving = ref(false);
const message = ref('');
const error = ref('');

// Groups (categories)
const groups = ref([]);
const selectedGroupId = ref('');

// Treatments
const treatments = ref([]);
const selectedTreatmentId = ref('');

// Detail panel (tampil di samping, bukan popup)
const showDetailPanel = ref(false);
const details = ref([]);
const selectedDetailId = ref('');
const isEditing = ref(false);

const detailForm = reactive({
  detailName: '',
  quantify: '',
  normalRange: ''
});

const currentTreatmentLabel = computed(() => {
  const t = treatments.value.find(item => String(item.treatmentId) === String(selectedTreatmentId.value));
  return t ? `${t.code} - ${t.name}` : '-';
});

async function request(path, options = {}) {
  const response = await fetch(`${props.apiBaseUrl}${path}`, {
    credentials: 'include',
    headers: { 'Content-Type': 'application/json', ...(options.headers || {}) },
    ...options
  });
  const payload = await response.json().catch(() => null);
  if (response.status === 401) {
    emit('session-expired', payload?.message || 'Sesi habis.');
    throw new Error(payload?.message || 'Unauthorized');
  }
  if (!response.ok) throw new Error(payload?.message || `HTTP ${response.status}`);
  return payload;
}

async function loadGroups() {
  try {
    const res = await request('/master/lab-treatment/groups');
    groups.value = res.data || [];
    if (groups.value.length > 0 && !selectedGroupId.value) {
      selectedGroupId.value = String(groups.value[0].groupId);
      await loadTreatments();
    }
  } catch (e) {
    error.value = e.message;
  }
}

function onGroupChange() {
  showDetailPanel.value = false;
  loadTreatments();
}

async function loadTreatments() {
  if (!selectedGroupId.value) {
    treatments.value = [];
    return;
  }
  try {
    const res = await request(`/master/lab-treatment/groups/${selectedGroupId.value}/treatments`);
    treatments.value = res.data || [];
    if (treatments.value.length > 0) {
      selectedTreatmentId.value = String(treatments.value[0].treatmentId);
    } else {
      selectedTreatmentId.value = '';
    }
  } catch (e) {
    error.value = e.message;
  }
}

async function loadDetails() {
  if (!selectedTreatmentId.value) {
    details.value = [];
    return;
  }
  try {
    const res = await request(`/master/lab-treatment/treatments/${selectedTreatmentId.value}/details`);
    details.value = res.data || [];
  } catch (e) {
    error.value = e.message;
  }
}

function openDetailPanel() {
  if (!selectedTreatmentId.value) {
    error.value = 'Pilih treatment terlebih dahulu.';
    return;
  }
  showDetailPanel.value = true;
  isEditing.value = false;
  selectedDetailId.value = '';
  resetDetailForm();
  loadDetails();
}

function closeDetailPanel() {
  showDetailPanel.value = false;
  resetDetailForm();
}

function selectDetailForEdit(detail) {
  selectedDetailId.value = String(detail.detailId);
  detailForm.detailName = detail.detailName;
  detailForm.quantify = detail.quantify;
  detailForm.normalRange = detail.normalRange;
  isEditing.value = true;
}

function resetDetailForm() {
  detailForm.detailName = '';
  detailForm.quantify = '';
  detailForm.normalRange = '';
  isEditing.value = false;
  selectedDetailId.value = '';
}

async function saveDetail() {
  if (!detailForm.detailName.trim() || !detailForm.quantify.trim() || !detailForm.normalRange.trim()) {
    error.value = 'Semua field (Jenis Pemeriksaan, Satuan, Normal Range) wajib diisi.';
    return;
  }

  saving.value = true;
  error.value = '';
  message.value = '';

  try {
    const body = {
      treatmentId: Number(selectedTreatmentId.value),
      detailName: detailForm.detailName.trim(),
      quantify: detailForm.quantify.trim(),
      normalRange: detailForm.normalRange.trim()
    };

    if (isEditing.value && selectedDetailId.value) {
      await request(`/master/lab-treatment/details/${selectedDetailId.value}`, {
        method: 'PUT',
        body: JSON.stringify(body)
      });
      message.value = 'Detail berhasil diubah.';
    } else {
      await request('/master/lab-treatment/details', {
        method: 'POST',
        body: JSON.stringify(body)
      });
      message.value = 'Detail berhasil disimpan.';
    }

    resetDetailForm();
    await loadDetails();
  } catch (e) {
    error.value = e.message;
  } finally {
    saving.value = false;
  }
}

async function deleteDetail(detailId) {
  if (!confirm('Yakin akan menghapus detail ini?')) return;

  saving.value = true;
  error.value = '';
  message.value = '';

  try {
    await request(`/master/lab-treatment/details/${detailId}`, { method: 'DELETE' });
    message.value = 'Detail berhasil dihapus.';
    if (selectedDetailId.value === String(detailId)) {
      resetDetailForm();
    }
    await loadDetails();
  } catch (e) {
    error.value = e.message;
  } finally {
    saving.value = false;
  }
}

onMounted(async () => {
  loading.value = true;
  await loadGroups();
  loading.value = false;
});
</script>

<template>
  <div class="lab-treatment-master">
    <!-- Header -->
    <div class="section-header">
      <div>
        <p class="eyebrow">MASTER DATA</p>
        <h2>🔬 Master Tindakan Laboratorium</h2>
        <p class="subcopy">Form master data tindakan laboratorium</p>
      </div>
    </div>

    <!-- Loading -->
    <div v-if="loading" class="flash neutral">Memuat data master...</div>

    <!-- Messages -->
    <p v-if="message" class="flash success">{{ message }}</p>
    <p v-if="error" class="flash error">{{ error }}</p>

    <!-- Main Content: Groups + Treatments (kiri) | Detail (kanan) -->
    <div v-if="!loading" class="panel-grid">
      <!-- Left Panel: Category + Treatment List -->
      <div class="form-card">
        <h3>KATEGORI</h3>
        <label>
          Pilih Kategori
          <select v-model="selectedGroupId" @change="onGroupChange">
            <option v-for="g in groups" :key="g.groupId" :value="String(g.groupId)">
              {{ g.code }} - {{ g.name }}
            </option>
          </select>
        </label>

        <div class="treatments-section">
          <h3>DAFTAR TINDAKAN</h3>
          <div v-if="!treatments.length" class="empty-state">
            Tidak ada tindakan untuk kategori ini.
          </div>
          <table v-else class="table">
            <thead>
              <tr>
                <th>KODE</th>
                <th>NAMA</th>
                <th>Aksi</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="t in treatments" :key="t.treatmentId"
                :class="{ selected: String(t.treatmentId) === selectedTreatmentId }"
                @click="selectedTreatmentId = String(t.treatmentId)">
                <td><strong>{{ t.code }}</strong></td>
                <td>{{ t.name }}</td>
                <td>
                  <button class="small-button" type="button" @click.stop="selectedTreatmentId = String(t.treatmentId); openDetailPanel()">
                    ISI DETAIL
                  </button>
                </td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>

      <!-- Right Panel: Detail (atau info jika belum dibuka) -->
      <div class="detail-card">
        <template v-if="!showDetailPanel">
          <h3>Informasi</h3>
          <p>Pilih kategori dan tindakan, lalu klik <strong>ISI DETAIL</strong> untuk mengelola detail pemeriksaan laboratorium.</p>
          <ul class="info-list">
            <li><strong>Jenis Pemeriksaan</strong> — Nama detail item tes lab</li>
            <li><strong>Satuan</strong> — Unit quantify (misal: MG/DL, %)</li>
            <li><strong>Normal Range</strong> — Rentang nilai normal</li>
          </ul>
        </template>

        <template v-else>
          <div class="detail-panel-header">
            <h3>FORM INPUT DETAIL TREATMENT</h3>
            <span class="badge">{{ currentTreatmentLabel }}</span>
            <button class="close-button" type="button" @click="closeDetailPanel" title="Tutup panel detail">&times;</button>
          </div>

          <!-- Form Fields -->
          <div class="detail-form">
            <label>
              JENIS PEMERIKSAAN
              <input v-model="detailForm.detailName" placeholder="Nama jenis pemeriksaan" @keyup.enter="saveDetail" />
            </label>
            <label>
              SATUAN
              <input v-model="detailForm.quantify" placeholder="Contoh: MG/DL, %" @keyup.enter="saveDetail" />
            </label>
            <label>
              NORMAL RANGE
              <input v-model="detailForm.normalRange" placeholder="Rentang nilai normal" @keyup.enter="saveDetail" />
            </label>

            <div class="action-row">
              <button class="primary-button" :disabled="saving" @click="saveDetail">
                {{ saving ? 'Menyimpan...' : (isEditing ? 'UBAH' : 'SIMPAN') }}
              </button>
              <button class="secondary-button" type="button" @click="resetDetailForm" :disabled="!isEditing && !selectedDetailId">
                BATAL
              </button>
              <button class="danger-button" type="button" @click="deleteDetail(selectedDetailId)" :disabled="!selectedDetailId || saving">
                HAPUS
              </button>
            </div>
          </div>

          <!-- Details Table -->
          <div class="details-section">
            <h4>DETAIL TREATMENT</h4>
            <div v-if="!details.length" class="empty-state">
              Belum ada detail untuk treatment ini.
            </div>
            <div v-else class="table-scroll">
              <table class="table">
                <thead>
                  <tr>
                    <th>JENIS PEMERIKSAAN</th>
                    <th>SATUAN</th>
                    <th>NORMAL RANGE</th>
                    <th>Aksi</th>
                  </tr>
                </thead>
                <tbody>
                  <tr v-for="d in details" :key="d.detailId"
                    :class="{ selected: String(d.detailId) === selectedDetailId }"
                    @click="selectDetailForEdit(d)">
                    <td><strong>{{ d.detailName }}</strong></td>
                    <td>{{ d.quantify }}</td>
                    <td>{{ d.normalRange }}</td>
                    <td>
                      <button class="small-button" type="button" @click.stop="selectDetailForEdit(d)">
                        Pilih
                      </button>
                    </td>
                  </tr>
                </tbody>
              </table>
            </div>
          </div>
        </template>
      </div>
    </div>
  </div>
</template>

<style scoped>
.lab-treatment-master {
  display: grid;
  gap: 18px;
}

.section-header,
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
.detail-card h3 {
  margin: 0;
}

.subcopy {
  color: #5a667b;
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
  grid-template-columns: 1fr 1fr;
  gap: 18px;
  align-items: start;
}

.form-card,
.detail-card {
  padding: 20px;
  background: rgba(255, 255, 255, 0.94);
  border: 1px solid rgba(150, 136, 117, 0.35);
  box-shadow: 0 12px 24px rgba(53, 64, 84, 0.08);
}

.form-card label {
  display: grid;
  gap: 6px;
  font-weight: 700;
  color: #304b73;
  margin-bottom: 16px;
}

.form-card select,
.form-card input {
  height: 38px;
  border: 1px solid rgba(95, 131, 194, 0.35);
  padding: 0 10px;
  font: inherit;
}

.treatments-section {
  margin-top: 20px;
}

.treatments-section h3 {
  margin-bottom: 10px;
}

.table {
  width: 100%;
  border-collapse: collapse;
  font-size: 13px;
}

.table th,
.table td {
  padding: 8px 10px;
  border-bottom: 1px solid rgba(150, 136, 117, 0.18);
  text-align: left;
}

.table th {
  background: #f6f8fb;
  color: #304b73;
  white-space: nowrap;
}

.table tbody tr {
  cursor: pointer;
}

.table tbody tr.selected {
  background: rgba(95, 131, 194, 0.12);
}

.small-button {
  padding: 6px 12px;
  font-size: 12px;
  background: #5f83c2;
  border: 0;
  color: #fff;
  font-weight: 700;
  border-radius: 4px;
  cursor: pointer;
}

.small-button:hover {
  background: #4d6ba0;
}

.info-list {
  padding-left: 18px;
}

.info-list li {
  margin-bottom: 8px;
}

.empty-state {
  padding: 24px;
  text-align: center;
  color: #9ca3af;
}

/* Detail Panel (di samping, bukan modal) */
.detail-panel-header {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 16px;
  flex-wrap: wrap;
}

.detail-panel-header h3 {
  font-size: 16px;
  color: #304b73;
}

.detail-panel-header .badge {
  padding: 4px 10px;
  background: #eef3fb;
  border-radius: 999px;
  font-size: 11px;
  font-weight: 700;
  color: #304b73;
  white-space: nowrap;
}

.close-button {
  margin-left: auto;
  width: 28px;
  height: 28px;
  border: 1px solid #d1d9e6;
  background: #fff;
  border-radius: 50%;
  font-size: 18px;
  line-height: 1;
  cursor: pointer;
  color: #6b7280;
  display: flex;
  align-items: center;
  justify-content: center;
}

.close-button:hover {
  background: #f3f4f6;
  color: #304b73;
}

.detail-form {
  display: grid;
  gap: 12px;
  margin-bottom: 20px;
  padding: 16px;
  border: 1px solid rgba(150, 136, 117, 0.2);
  border-radius: 8px;
  background: #fafaf8;
}

.detail-form label {
  display: grid;
  gap: 4px;
  font-weight: 700;
  color: #304b73;
  font-size: 13px;
}

.detail-form input {
  height: 36px;
  border: 1px solid #d1d9e6;
  padding: 0 10px;
  font: inherit;
  border-radius: 6px;
}

.action-row {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-top: 4px;
  justify-content: center;
}

.primary-button,
.secondary-button,
.danger-button {
  min-height: 36px;
  padding: 0 16px;
  border: 0;
  font-weight: 700;
  cursor: pointer;
  border-radius: 6px;
}

.primary-button {
  background: #5f83c2;
  color: #fff;
}

.primary-button:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.secondary-button {
  background: #fff;
  border: 1px solid #d1d9e6;
  color: #3d4b63;
}

.danger-button {
  background: #b84747;
  color: #fff;
}

.details-section {
  margin-top: 8px;
}

.details-section h4 {
  margin: 0 0 10px;
  color: #304b73;
}

.table-scroll {
  max-height: 300px;
  overflow-y: auto;
}

@media (max-width: 1080px) {
  .panel-grid {
    grid-template-columns: 1fr;
  }
}
</style>
