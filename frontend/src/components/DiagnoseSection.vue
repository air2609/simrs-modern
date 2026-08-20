<script setup>
import { computed, reactive, ref } from 'vue';
import DiagnoseHistoryTab from './DiagnoseHistoryTab.vue';
import LaboratoryResultSection from './LaboratoryResultSection.vue';

const props = defineProps({
  apiBaseUrl: {
    type: String,
    required: true
  }
});

const emit = defineEmits(['session-expired']);

const ATURAN_PAKAI_OPTIONS = [
  '(Setiap 24 Jam) sesudah makan',
  '(Setiap 12 Jam) sesudah makan',
  '(Setiap 8 Jam) sesudah makan',
  '(Setiap 6 Jam) sesudah makan',
  '(Setiap 24 Jam) sebelum makan',
  '(Setiap 12 Jam) sebelum makan',
  '(Setiap 8 Jam) sebelum makan',
  '(Setiap 6 Jam) sebelum makan'
];

const activeTab = ref('diagnose');

const loading = ref(false);
const saving = ref(false);
const error = ref('');
const success = ref('');

const mrCodeInput = ref('');
const registration = ref(null);
const diagnoseType = ref('out'); // 'in' = RAWAT INAP, 'out' = RAWAT JALAN
const notes = ref('');
const selectedIcds = ref([]); // [{icdId, icdCode, icdName}]
const existingDiagnoseId = ref(null);

// NO. MR bandbox (pencarian pasien berdasarkan nama/tgl lahir/alamat, seperti simrs legacy)
const mrSearchOpen = ref(false);
const mrSearchCode = ref('');
const mrSearchName = ref('');
const mrSearchBirthDate = ref('');
const mrSearchAddress = ref('');
const mrSearchResults = ref([]);
const mrSearching = ref(false);

const resepList = ref([]); // [{lineType, referenceId, description, quantity, unitPrice, unitName, instruction, instructionNote, components}]

// ICD search dialog
const showIcdDialog = ref(false);
const icdSearchCode = ref('');
const icdSearchName = ref('');
const icdResults = ref([]);
const icdSearching = ref(false);

// Obat (item) dialog
const showObatDialog = ref(false);
const itemSearchCode = ref('');
const itemSearchName = ref('');
const itemResults = ref([]);
const itemSearching = ref(false);
const itemQuantities = reactive({});

// Racikan dialog
const showRacikanDialog = ref(false);
const racikanSearchCode = ref('');
const racikanSearchName = ref('');
const racikanResults = ref([]);
const racikanSearching = ref(false);
const racikanQuantities = reactive({});
const racikanSelected = ref([]); // [{itemId, code, name, unitName, price, quantity}]
const racikanDescription = ref('');
const racikanUnitName = ref('BUNGKUS');

const grandTotal = computed(() => {
  return resepList.value.reduce((sum, line) => sum + (line.quantity * line.unitPrice), 0);
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

function clearForm() {
  registration.value = null;
  diagnoseType.value = 'out';
  notes.value = '';
  selectedIcds.value = [];
  existingDiagnoseId.value = null;
  resepList.value = [];
  mrCodeInput.value = '';
}

function hasText(value) {
  return value != null && value.trim().length > 0;
}

function isRajal() {
  return diagnoseType.value === 'out';
}

async function lookupMr() {
  error.value = '';
  success.value = '';
  if (!hasText(mrCodeInput.value)) {
    return;
  }

  loading.value = true;
  try {
    const reg = await request(`/mr/diagnose/registration?mrCode=${encodeURIComponent(mrCodeInput.value.trim())}`);
    registration.value = reg;
    mrCodeInput.value = reg.mrCode;
    diagnoseType.value = reg.ranap ? 'in' : 'out';
    notes.value = reg.notes || '';
    existingDiagnoseId.value = reg.existingDiagnoseId;
    selectedIcds.value = (reg.icdIds || []).map((id, index) => ({
      icdId: id,
      icdCode: '',
      icdName: reg.icdNames[index]
    }));
    resepList.value = [];
  } catch (requestError) {
    error.value = requestError.message;
  } finally {
    loading.value = false;
  }
}

// ===== NO. MR bandbox (cari pasien berdasarkan nama/tgl lahir/alamat) =====
function openMrBandbox() {
  mrSearchOpen.value = true;
  mrSearchResults.value = [];
}

async function searchMrPatients() {
  error.value = '';
  mrSearching.value = true;
  try {
    const params = new URLSearchParams();
    if (mrSearchCode.value) params.set('mrCode', mrSearchCode.value);
    if (mrSearchName.value) params.set('patientName', mrSearchName.value);
    if (mrSearchBirthDate.value) params.set('birthDate', mrSearchBirthDate.value);
    if (mrSearchAddress.value) params.set('address', mrSearchAddress.value);
    mrSearchResults.value = await request(`/mr/diagnose/patients/search?${params.toString()}`);
  } catch (requestError) {
    error.value = requestError.message;
  } finally {
    mrSearching.value = false;
  }
}

async function pickMrPatient(item) {
  mrCodeInput.value = item.mrCode;
  mrSearchOpen.value = false;
  await lookupMr();
}

// ===== ICD dialog =====
function openIcdDialog() {
  showIcdDialog.value = true;
  icdResults.value = [];
}

function closeIcdDialog() {
  showIcdDialog.value = false;
}

async function searchIcd() {
  error.value = '';
  icdSearching.value = true;
  try {
    const params = new URLSearchParams();
    if (icdSearchCode.value) params.set('code', icdSearchCode.value);
    if (icdSearchName.value) params.set('name', icdSearchName.value);
    icdResults.value = await request(`/mr/diagnose/icd/search?${params.toString()}`);
  } catch (requestError) {
    error.value = requestError.message;
  } finally {
    icdSearching.value = false;
  }
}

function addIcd(item) {
  if (selectedIcds.value.some((existing) => existing.icdId === item.icdId)) {
    return;
  }
  selectedIcds.value.push(item);
}

function removeIcd(icdId) {
  selectedIcds.value = selectedIcds.value.filter((item) => item.icdId !== icdId);
}

// ===== Obat dialog =====
function openObatDialog() {
  showObatDialog.value = true;
  itemResults.value = [];
  itemSearchCode.value = '';
  itemSearchName.value = '';
}

function closeObatDialog() {
  showObatDialog.value = false;
}

async function searchItems() {
  error.value = '';
  itemSearching.value = true;
  try {
    const params = new URLSearchParams();
    if (itemSearchCode.value) params.set('code', itemSearchCode.value);
    if (itemSearchName.value) params.set('name', itemSearchName.value);
    params.set('isRajal', String(isRajal()));
    itemResults.value = await request(`/mr/diagnose/items/search?${params.toString()}`);
  } catch (requestError) {
    error.value = requestError.message;
  } finally {
    itemSearching.value = false;
  }
}

function addObatLine(item) {
  const qty = Number(itemQuantities[item.itemId]);
  if (!qty || qty <= 0) {
    error.value = 'JUMLAH harus diisi dan lebih dari 0.';
    return;
  }
  if (qty > item.stockQuantity) {
    error.value = `Jumlah melebihi stok tersedia (${item.stockQuantity}).`;
    return;
  }
  resepList.value.push({
    lineType: 'ITEM',
    referenceId: item.itemId,
    description: `${item.itemCode} ${item.itemName}`,
    quantity: qty,
    unitPrice: item.price,
    unitName: item.unitName,
    instruction: ATURAN_PAKAI_OPTIONS[0],
    instructionNote: '',
    components: null
  });
  closeObatDialog();
}

// ===== Racikan dialog =====
function openRacikanDialog() {
  showRacikanDialog.value = true;
  racikanResults.value = [];
  racikanSelected.value = [];
  racikanDescription.value = '';
  racikanSearchCode.value = '';
  racikanSearchName.value = '';
}

function closeRacikanDialog() {
  showRacikanDialog.value = false;
}

async function searchRacikanItems() {
  error.value = '';
  racikanSearching.value = true;
  try {
    const params = new URLSearchParams();
    if (racikanSearchCode.value) params.set('code', racikanSearchCode.value);
    if (racikanSearchName.value) params.set('name', racikanSearchName.value);
    params.set('isRajal', 'false');
    racikanResults.value = await request(`/mr/diagnose/items/search?${params.toString()}`);
  } catch (requestError) {
    error.value = requestError.message;
  } finally {
    racikanSearching.value = false;
  }
}

function addRacikanComponent(item) {
  const qty = Number(racikanQuantities[item.itemId]);
  if (!qty || qty <= 0) {
    error.value = 'JUMLAH harus diisi dan lebih dari 0.';
    return;
  }
  if (qty > item.stockQuantity) {
    error.value = `Jumlah melebihi stok tersedia (${item.stockQuantity}).`;
    return;
  }
  if (racikanSelected.value.some((existing) => existing.itemId === item.itemId)) {
    return;
  }
  racikanSelected.value.push({
    itemId: item.itemId,
    code: item.itemCode,
    name: item.itemName,
    unitName: item.unitName,
    price: item.price,
    quantity: qty
  });
}

function removeRacikanComponent(itemId) {
  racikanSelected.value = racikanSelected.value.filter((c) => c.itemId !== itemId);
}

function saveRacikanLine() {
  if (!racikanSelected.value.length) {
    error.value = 'Pilih minimal satu komponen racikan.';
    return;
  }
  const totalPrice = racikanSelected.value.reduce((sum, c) => sum + (c.price * c.quantity), 0);
  const composition = racikanSelected.value.map((c) => `${c.name} ${c.quantity}${c.unitName}`).join(' + ');

  resepList.value.push({
    lineType: 'RACIKAN',
    referenceId: null,
    description: hasText(racikanDescription.value) ? racikanDescription.value : composition,
    quantity: 1,
    unitPrice: totalPrice,
    unitName: racikanUnitName.value,
    instruction: ATURAN_PAKAI_OPTIONS[0],
    instructionNote: '',
    components: racikanSelected.value.map((c) => ({ referenceId: c.itemId, quantity: c.quantity }))
  });
  closeRacikanDialog();
}

function removeRespLine(index) {
  resepList.value.splice(index, 1);
}

async function saveDiagnose() {
  error.value = '';
  success.value = '';

  if (!registration.value) {
    error.value = 'NO MR WAJIB DIISI..';
    return;
  }
  if (!hasText(notes.value)) {
    error.value = 'KELUHAN PASIEN WAJIB DIISI..';
    return;
  }
  if (!selectedIcds.value.length) {
    error.value = 'DIAGNOSA WAJIB DIISI..';
    return;
  }

  saving.value = true;
  try {
    const body = {
      mrCode: registration.value.mrCode,
      diagnoseType: diagnoseType.value,
      notes: notes.value,
      icdIds: selectedIcds.value.map((item) => item.icdId),
      existingDiagnoseId: existingDiagnoseId.value,
      prescriptionLines: resepList.value.map((line) => ({
        lineType: line.lineType,
        referenceId: line.referenceId,
        quantity: line.quantity,
        unitPrice: line.unitPrice,
        description: line.description,
        unitName: line.unitName,
        instruction: hasText(line.instructionNote) ? `${line.instructionNote}-${line.instruction}` : line.instruction,
        components: line.components
      }))
    };

    const result = await request('/mr/diagnose', {
      method: 'POST',
      body: JSON.stringify(body)
    });

    existingDiagnoseId.value = result.diagnoseId;
    success.value = result.noteNumber
      ? `Sukses Menyimpan Data! Nota: ${result.noteNumber}`
      : 'Sukses Menyimpan Data!';
  } catch (requestError) {
    error.value = requestError.message;
  } finally {
    saving.value = false;
  }
}
</script>

<template>
  <div class="screen-page">
    <div class="page-header">
      <div>
        <h2>📝 Form Rekam Medis Diagnosa</h2>
      </div>
    </div>

    <p v-if="error" class="status-banner status-banner--error">{{ error }}</p>
    <p v-if="success" class="status-banner status-banner--success">{{ success }}</p>

    <div class="tabs">
      <button class="tab-button" :class="{ active: activeTab === 'diagnose' }" type="button" @click="activeTab = 'diagnose'">DIAGNOSA PASIEN</button>
      <button class="tab-button" :class="{ active: activeTab === 'history' }" type="button" @click="activeTab = 'history'">HISTORY DIAGNOSA</button>
      <button class="tab-button" :class="{ active: activeTab === 'lab' }" type="button" @click="activeTab = 'lab'">HASIL LABORATORIUM</button>
    </div>

    <!-- TAB: DIAGNOSA PASIEN -->
    <div v-show="activeTab === 'diagnose'">
      <div class="card">
        <h3 class="card-title">DATA PASIEN</h3>
        <div class="form-grid">
          <div class="field">
            <label for="diagnose-mr">NO. MR</label>
            <div class="bandbox">
              <input
                id="diagnose-mr"
                v-model="mrCodeInput"
                type="text"
                placeholder="Pilih No. MR"
                readonly
                @focus="mrSearchOpen = true"
              />
              <button class="bandbox-btn" type="button" @click="mrSearchOpen = !mrSearchOpen">▾</button>
            </div>
            <div v-if="mrSearchOpen" class="bandbox-popup">
              <div class="bandbox-search-grid">
                <div class="bandbox-search-field">
                  <label>NO. MR</label>
                  <input v-model="mrSearchCode" type="text" placeholder="No. MR" />
                </div>
                <div class="bandbox-search-field">
                  <label>NAMA</label>
                  <input v-model="mrSearchName" type="text" placeholder="Nama pasien" />
                </div>
                <div class="bandbox-search-field">
                  <label>TGL. LAHIR</label>
                  <input v-model="mrSearchBirthDate" type="date" />
                </div>
                <div class="bandbox-search-field">
                  <label>ALAMAT</label>
                  <input v-model="mrSearchAddress" type="text" placeholder="Alamat" />
                </div>
              </div>
              <div class="bandbox-actions">
                <button class="small-button primary" type="button" :disabled="mrSearching" @click="searchMrPatients">CARI</button>
                <button class="small-button" type="button" @click="mrSearchOpen = false">TUTUP</button>
              </div>
              <table class="table bandbox-table">
                <thead>
                  <tr>
                    <th>NO. MR</th>
                    <th>NAMA</th>
                    <th>TGL. LAHIR</th>
                    <th>ALAMAT</th>
                  </tr>
                </thead>
                <tbody>
                  <tr
                    v-for="item in mrSearchResults"
                    :key="item.mrCode"
                    @click="pickMrPatient(item)"
                  >
                    <td class="strong">{{ item.mrCode }}</td>
                    <td>{{ item.patientName }}</td>
                    <td>{{ item.birthDate }}</td>
                    <td>{{ item.address }}</td>
                  </tr>
                  <tr v-if="!mrSearchResults.length">
                    <td colspan="4" class="empty-state">Isi minimal satu field lalu tekan CARI.</td>
                  </tr>
                </tbody>
              </table>
            </div>
          </div>
          <div class="field">
            <label for="diagnose-type">PASIEN</label>
            <div class="radio-row">
              <label><input type="radio" value="in" v-model="diagnoseType" /> RAWAT INAP</label>
              <label><input type="radio" value="out" v-model="diagnoseType" /> RAWAT JALAN</label>
            </div>
          </div>
          <div class="field">
            <label for="diagnose-name">NAMA</label>
            <input id="diagnose-name" class="field-input" :value="registration?.patientName" readonly />
          </div>
          <div class="field">
            <label for="diagnose-gender">JENIS KELAMIN</label>
            <input id="diagnose-gender" class="field-input" :value="registration?.gender === 'M' ? 'PRIA' : 'WANITA'" readonly />
          </div>
          <div class="field">
            <label for="diagnose-dob">TANGGAL LAHIR</label>
            <input id="diagnose-dob" class="field-input" :value="registration?.birthDate" readonly />
          </div>
          <div class="field">
            <label for="diagnose-doctor">DOKTER</label>
            <input id="diagnose-doctor" class="field-input" :value="registration?.doctorName" readonly />
          </div>
          <div class="field">
            <label for="diagnose-type-patient">TIPE PASIEN</label>
            <input id="diagnose-type-patient" class="field-input" :value="registration?.patientTypeDesc" readonly />
          </div>
          <div class="field">
            <label>DIAGNOSA</label>
            <div class="icd-chip-row">
              <span v-for="icd in selectedIcds" :key="icd.icdId" class="icd-chip">
                {{ icd.icdName }}
                <button type="button" class="chip-remove" @click="removeIcd(icd.icdId)">×</button>
              </span>
              <button type="button" class="small-button" @click="openIcdDialog">+ Cari Diagnosa</button>
            </div>
          </div>
        </div>
      </div>

      <div class="card">
        <h3 class="card-title">CATATAN DOKTER</h3>
        <textarea v-model="notes" class="notes-textarea" rows="4"></textarea>
      </div>

      <div class="card">
        <h3 class="card-title">RESEP UNTUK PASIEN</h3>
        <table class="file-table">
          <thead>
            <tr>
              <th>NAMA</th>
              <th>JUMLAH</th>
              <th>SATUAN</th>
              <th>HARGA</th>
              <th>ATURAN PAKAI</th>
              <th>SUBTOTAL</th>
              <th></th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="(line, index) in resepList" :key="index">
              <td>{{ line.description }}</td>
              <td>{{ line.quantity }}</td>
              <td>{{ line.unitName }}</td>
              <td>{{ line.unitPrice.toLocaleString() }}</td>
              <td>
                <input v-model="line.instructionNote" class="field-input small" placeholder="Catatan" />
                <select v-model="line.instruction" class="field-input small">
                  <option v-for="opt in ATURAN_PAKAI_OPTIONS" :key="opt" :value="opt">{{ opt }}</option>
                </select>
              </td>
              <td>{{ (line.quantity * line.unitPrice).toLocaleString() }}</td>
              <td><button type="button" class="small-button" @click="removeRespLine(index)">🗑️</button></td>
            </tr>
            <tr v-if="!resepList.length">
              <td colspan="7" class="empty-state">Belum ada resep.</td>
            </tr>
          </tbody>
        </table>

        <div class="resep-actions">
          <button class="small-button" type="button" @click="openObatDialog">💊 OBAT</button>
          <button class="small-button" type="button" @click="openRacikanDialog">🧪 RACIKAN</button>
          <span class="grand-total">GRAND TOTAL: {{ grandTotal.toLocaleString() }}</span>
        </div>
      </div>

      <div class="card">
        <div class="file-actions">
          <button class="small-button primary" type="button" :disabled="saving" @click="saveDiagnose">💾 SIMPAN</button>
          <button class="small-button" type="button" @click="clearForm">🆕 BARU</button>
        </div>
      </div>
    </div>

    <!-- TAB: HISTORY DIAGNOSA -->
    <div v-show="activeTab === 'history'">
      <DiagnoseHistoryTab :api-base-url="apiBaseUrl" :mr-code="registration?.mrCode" @session-expired="emit('session-expired', $event)" />
    </div>

    <!-- TAB: HASIL LABORATORIUM -->
    <div v-show="activeTab === 'lab'">
      <LaboratoryResultSection :api-base-url="apiBaseUrl" @session-expired="emit('session-expired', $event)" />
    </div>

    <!-- ICD SEARCH DIALOG -->
    <div v-if="showIcdDialog" class="modal-overlay" @click.self="closeIcdDialog">
      <div class="modal-card">
        <h3 class="card-title">PENCARIAN DATA DIAGNOSA</h3>
        <div class="search-form-grid">
          <label class="field">
            <span class="field-label">KODE</span>
            <input v-model="icdSearchCode" class="field-input" />
          </label>
          <label class="field">
            <span class="field-label">NAMA</span>
            <input v-model="icdSearchName" class="field-input" />
          </label>
        </div>
        <div class="modal-actions">
          <button class="small-button primary" type="button" :disabled="icdSearching" @click="searchIcd">CARI</button>
          <button class="small-button" type="button" @click="closeIcdDialog">TUTUP</button>
        </div>
        <table class="file-table">
          <thead><tr><th>KODE</th><th>NAMA</th><th></th></tr></thead>
          <tbody>
            <tr v-for="item in icdResults" :key="item.icdId">
              <td>{{ item.icdCode }}</td>
              <td>{{ item.icdName }}</td>
              <td><button class="small-button" type="button" @click="addIcd(item)">+ Pilih</button></td>
            </tr>
            <tr v-if="!icdResults.length"><td colspan="3" class="empty-state">Tidak ada data.</td></tr>
          </tbody>
        </table>
      </div>
    </div>

    <!-- OBAT DIALOG -->
    <div v-if="showObatDialog" class="modal-overlay" @click.self="closeObatDialog">
      <div class="modal-card">
        <h3 class="card-title">FORM INPUT OBAT-BAHAN MEDIS</h3>
        <div class="search-form-grid">
          <label class="field">
            <span class="field-label">KODE</span>
            <input v-model="itemSearchCode" class="field-input" />
          </label>
          <label class="field">
            <span class="field-label">NAMA</span>
            <input v-model="itemSearchName" class="field-input" />
          </label>
        </div>
        <div class="modal-actions">
          <button class="small-button primary" type="button" :disabled="itemSearching" @click="searchItems">CARI</button>
          <button class="small-button" type="button" @click="closeObatDialog">TUTUP</button>
        </div>
        <table class="file-table">
          <thead><tr><th>KODE</th><th>NAMA</th><th>STOK</th><th>HARGA</th><th>JUMLAH</th><th></th></tr></thead>
          <tbody>
            <tr v-for="item in itemResults" :key="item.itemId">
              <td>{{ item.itemCode }}</td>
              <td>{{ item.itemName }}</td>
              <td>{{ item.stockQuantity }}</td>
              <td>{{ item.price.toLocaleString() }}</td>
              <td><input type="number" min="0" class="field-input small" v-model="itemQuantities[item.itemId]" /></td>
              <td><button class="small-button" type="button" @click="addObatLine(item)">+ Tambah</button></td>
            </tr>
            <tr v-if="!itemResults.length"><td colspan="6" class="empty-state">Tidak ada data.</td></tr>
          </tbody>
        </table>
      </div>
    </div>

    <!-- RACIKAN DIALOG -->
    <div v-if="showRacikanDialog" class="modal-overlay" @click.self="closeRacikanDialog">
      <div class="modal-card">
        <h3 class="card-title">FORM TAMBAH RACIKAN</h3>
        <div class="search-form-grid">
          <label class="field">
            <span class="field-label">KODE</span>
            <input v-model="racikanSearchCode" class="field-input" />
          </label>
          <label class="field">
            <span class="field-label">NAMA</span>
            <input v-model="racikanSearchName" class="field-input" />
          </label>
        </div>
        <div class="modal-actions">
          <button class="small-button primary" type="button" :disabled="racikanSearching" @click="searchRacikanItems">CARI</button>
          <button class="small-button" type="button" @click="closeRacikanDialog">TUTUP</button>
        </div>
        <table class="file-table">
          <thead><tr><th>KODE</th><th>NAMA</th><th>STOK</th><th>HARGA</th><th>JUMLAH</th><th></th></tr></thead>
          <tbody>
            <tr v-for="item in racikanResults" :key="item.itemId">
              <td>{{ item.itemCode }}</td>
              <td>{{ item.itemName }}</td>
              <td>{{ item.stockQuantity }}</td>
              <td>{{ item.price.toLocaleString() }}</td>
              <td><input type="number" min="0" class="field-input small" v-model="racikanQuantities[item.itemId]" /></td>
              <td><button class="small-button" type="button" @click="addRacikanComponent(item)">+ Tambah</button></td>
            </tr>
            <tr v-if="!racikanResults.length"><td colspan="6" class="empty-state">Tidak ada data.</td></tr>
          </tbody>
        </table>

        <h3 class="card-title">KOMPONEN RACIKAN TERPILIH</h3>
        <table class="file-table">
          <thead><tr><th>NAMA</th><th>JUMLAH</th><th>SATUAN</th><th></th></tr></thead>
          <tbody>
            <tr v-for="c in racikanSelected" :key="c.itemId">
              <td>{{ c.name }}</td>
              <td>{{ c.quantity }}</td>
              <td>{{ c.unitName }}</td>
              <td><button class="small-button" type="button" @click="removeRacikanComponent(c.itemId)">🗑️</button></td>
            </tr>
            <tr v-if="!racikanSelected.length"><td colspan="4" class="empty-state">Belum ada komponen.</td></tr>
          </tbody>
        </table>

        <div class="form-grid">
          <label class="field">
            <span class="field-label">NAMA RACIKAN</span>
            <input v-model="racikanDescription" class="field-input" placeholder="Otomatis dari komponen jika kosong" />
          </label>
          <label class="field">
            <span class="field-label">SATUAN</span>
            <input v-model="racikanUnitName" class="field-input" />
          </label>
        </div>

        <div class="modal-actions">
          <button class="small-button primary" type="button" @click="saveRacikanLine">SIMPAN RACIKAN</button>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.screen-page { padding: 16px; }
.page-header { margin-bottom: 16px; display: flex; align-items: flex-start; justify-content: space-between; flex-wrap: wrap; gap: 12px; }
.page-header h2 { margin: 0; color: #304b73; font-size: 20px; }
.page-subtitle { margin: 4px 0 0; color: #6b7280; font-size: 14px; }

.status-banner { padding: 12px 16px; border-radius: 10px; font-weight: 600; margin-bottom: 12px; }
.status-banner--error { background: #fde8ea; color: #a32943; }
.status-banner--success { background: #e6f7ee; color: #1a7f4b; }

.tabs { display: flex; gap: 6px; margin-bottom: 16px; border-bottom: 2px solid #e5e7eb; }
.tab-button { padding: 10px 16px; border: none; background: none; cursor: pointer; font-weight: 600; color: #6b7280; border-bottom: 3px solid transparent; }
.tab-button.active { color: #304b73; border-bottom-color: #304b73; }

.card { background: #fff; border-radius: 12px; padding: 16px; box-shadow: 0 1px 3px rgba(0, 0, 0, 0.08); margin-bottom: 16px; }
.card-title { margin: 0 0 12px; color: #304b73; font-size: 15px; text-align: center; }

.form-grid { display: grid; grid-template-columns: repeat(2, 1fr); gap: 12px; }
.search-form-grid { display: grid; grid-template-columns: repeat(2, 1fr); gap: 12px; margin-bottom: 12px; }
.field { display: flex; flex-direction: column; gap: 4px; min-width: 180px; }
.field label { font-size: 12px; font-weight: 700; color: #304b73; }
.field-label { font-size: 12px; font-weight: 600; color: #6b7280; }
.field-input { padding: 8px 10px; border-radius: 8px; border: 1px solid #d1d5db; font-size: 13px; }
.field-input.small { padding: 4px 6px; font-size: 12px; width: 100%; margin-bottom: 4px; }
.field input[readonly] { background: #f6f8fb; color: #6b7280; }
.radio-row { display: flex; gap: 14px; align-items: center; font-size: 13px; }

.bandbox { display: flex; align-items: stretch; }
.bandbox input { flex: 1; border-top-right-radius: 0; border-bottom-right-radius: 0; }
.bandbox-btn { padding: 0 12px; border: 1px solid #d1d9e6; border-left: none; border-radius: 0 6px 6px 0; background: #f6f8fb; cursor: pointer; }

.bandbox-popup { border: 1px solid #d1d9e6; border-radius: 8px; padding: 10px; background: #fff; box-shadow: 0 8px 20px rgba(0,0,0,0.12); margin-top: 4px; }
.bandbox-search-grid { display: grid; grid-template-columns: repeat(2, 1fr); gap: 10px; margin-bottom: 8px; }
.bandbox-search-field { display: flex; flex-direction: column; gap: 4px; }
.bandbox-search-field label { font-size: 12px; font-weight: 700; color: #304b73; }
.bandbox-search-field input { padding: 8px 10px; border: 1px solid #d1d9e6; border-radius: 6px; font-size: 14px; text-transform: uppercase; }
.bandbox-search-field input:focus, .bandbox-search-field input:focus-visible { outline: none; border-color: #1d4ed8; box-shadow: 0 0 0 2px rgba(29, 78, 216, 0.15); }
.bandbox-actions { display: flex; gap: 10px; margin-bottom: 10px; }
.bandbox-table { font-size: 13px; width: 100%; border-collapse: collapse; }
.bandbox-table th, .bandbox-table td { padding: 8px 10px; border-bottom: 1px solid #eef2f7; text-align: left; }
.bandbox-table th { background: #f6f8fb; color: #304b73; white-space: nowrap; }
.bandbox-table tbody tr { cursor: pointer; }
.bandbox-table tbody tr:hover { background: #f6f8fb; }
.strong { font-weight: 700; }

.icd-chip-row { display: flex; flex-wrap: wrap; gap: 6px; align-items: center; }
.icd-chip { background: #eef2ff; border-radius: 999px; padding: 4px 10px; font-size: 12px; display: flex; align-items: center; gap: 6px; }
.chip-remove { border: none; background: none; cursor: pointer; font-weight: 700; color: #a32943; }

.notes-textarea { width: 100%; border-radius: 8px; border: 1px solid #d1d5db; padding: 8px; font-size: 13px; }

.file-table { width: 100%; border-collapse: collapse; font-size: 13px; }
.file-table th { text-align: left; padding: 8px; border-bottom: 2px solid #e5e7eb; color: #304b73; }
.file-table td { padding: 8px; border-bottom: 1px solid #f3f4f6; }
.empty-state { color: #9ca3af; text-align: center; padding: 12px; }

.resep-actions { display: flex; align-items: center; gap: 10px; margin-top: 12px; }
.grand-total { margin-left: auto; font-weight: 700; color: #304b73; }

.file-actions { display: flex; gap: 10px; }

.small-button { padding: 8px 14px; border-radius: 8px; border: 1px solid #d1d5db; background: #fff; cursor: pointer; font-weight: 600; }
.small-button.primary { background: #304b73; color: #fff; border-color: #304b73; }
.small-button:disabled { opacity: 0.6; cursor: default; }

.modal-overlay { position: fixed; inset: 0; background: rgba(15, 23, 42, 0.45); display: flex; align-items: center; justify-content: center; z-index: 50; }
.modal-card { background: #fff; border-radius: 12px; padding: 20px; width: 820px; max-width: 95vw; max-height: 85vh; overflow-y: auto; }
.modal-actions { display: flex; gap: 10px; margin-bottom: 12px; }
</style>