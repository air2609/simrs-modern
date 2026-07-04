<script setup>
import { computed, onMounted, reactive, ref } from 'vue';

const props = defineProps({
  apiBaseUrl: {
    type: String,
    required: true
  },
  availableUnits: {
    type: Array,
    default: () => []
  }
});

const emit = defineEmits(['session-expired']);

const activeTab = ref('transaction');
const loading = ref(true);
const patientSearching = ref(false);
const itemSearching = ref(false);
const compoundSearching = ref(false);
const noteSearching = ref(false);
const returnSearching = ref(false);
const saving = ref(false);
const message = ref('');
const error = ref('');

const masters = reactive({
  units: [],
  patientTypes: []
});

const patientResults = ref([]);
const itemResults = ref([]);
const compoundResults = ref([]);
const noteResults = ref([]);
const returnResults = ref([]);
const returnDetail = ref(null);

const patientSearch = reactive({
  mrCode: '',
  patientName: '',
  address: ''
});

const itemSearch = reactive({
  code: '',
  name: ''
});

const compoundSearch = reactive({
  code: '',
  name: ''
});

const noteSearch = reactive({
  noteNumber: '',
  patientName: ''
});

const returnSearch = reactive({
  returnNumber: '',
  patientName: '',
  startDate: '',
  endDate: ''
});

const miscDraft = reactive({
  description: '',
  quantity: 1,
  unitPrice: 0
});

const compoundDraft = reactive({
  description: '',
  quantity: 1,
  unitName: 'BUNGKUS',
  discountType: 'RP',
  discountValue: 0,
  instruction: ''
});

const form = reactive({
  unitId: '',
  referencePatient: false,
  existingMrCode: '',
  registrationCode: '',
  patientTypeId: '',
  patientName: '',
  gender: 'M',
  birthDate: '',
  address: '',
  tariffClass: 'KELAS II',
  inpatient: false,
  receiptNumber: '',
  noteId: null,
  noteNumber: '',
  noteStatusLabel: '',
  cancelationNote: ''
});

const lines = ref([]);
const compoundItems = ref([]);
const availableActions = reactive({
  canModify: false,
  canValidate: false,
  canCancel: false
});

const compoundUnitOptions = ['BUNGKUS', 'KAPSUL', 'BOTOL', 'POT', 'TUBE'];

const selectedUnit = computed(() => {
  return masters.units.find((item) => String(item.unitId) === String(form.unitId)) || null;
});

const totalAmount = computed(() => {
  return lines.value.reduce((total, line) => total + calculateSubtotal(line), 0);
});

const isEditingExistingNote = computed(() => Boolean(form.noteId));

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

function resetMessages() {
  message.value = '';
  error.value = '';
}

function normalizeDiscountType(discountType) {
  return discountType === '%' ? '%' : 'RP';
}

function calculateSubtotal(line) {
  const amount = Number(line.unitPrice || 0) * Number(line.quantity || 0);
  const discountValue = Number(line.discountValue || 0);

  if (normalizeDiscountType(line.discountType) === '%') {
    return amount - (amount * discountValue / 100);
  }

  return amount - discountValue;
}

function formatCurrency(value) {
  return new Intl.NumberFormat('id-ID', {
    maximumFractionDigits: 2,
    minimumFractionDigits: 0
  }).format(Number(value || 0));
}

function toDateInputValue(date) {
  const year = date.getFullYear();
  const month = String(date.getMonth() + 1).padStart(2, '0');
  const day = String(date.getDate()).padStart(2, '0');
  return `${year}-${month}-${day}`;
}

function buildDefaultReturnDates() {
  const endDate = new Date();
  const startDate = new Date(endDate);
  startDate.setDate(endDate.getDate() - 30);

  return {
    startDate: toDateInputValue(startDate),
    endDate: toDateInputValue(endDate)
  };
}

function formatDateTime(value) {
  if (!value) {
    return '-';
  }

  const date = new Date(value);
  if (Number.isNaN(date.getTime())) {
    return String(value);
  }

  return new Intl.DateTimeFormat('id-ID', {
    dateStyle: 'medium',
    timeStyle: 'short'
  }).format(date);
}

function normalizeUnitOption(unit) {
  return {
    unitId: unit?.unitId,
    unitCode: unit?.unitCode || '',
    unitName: unit?.unitName || '',
    warehouseId: unit?.warehouseId ?? null
  };
}

function mergeUnitOptions(primaryUnits = [], fallbackUnits = []) {
  const merged = [];
  const seenUnitIds = new Set();

  [...primaryUnits, ...fallbackUnits].forEach((unit) => {
    const normalized = normalizeUnitOption(unit);
    const unitId = String(normalized.unitId ?? '');

    if (!unitId || seenUnitIds.has(unitId)) {
      return;
    }

    seenUnitIds.add(unitId);
    merged.push(normalized);
  });

  return merged;
}

function formatLineType(lineType) {
  if (lineType === 'ITEM') {
    return 'O-BM';
  }
  if (lineType === 'RACIKAN') {
    return 'Racikan';
  }
  if (lineType === 'MISC') {
    return 'Biaya Lain-lain';
  }
  return lineType;
}

function formatCompoundSummary(components = []) {
  return components
    .map((component) => `${component.description} ${component.quantity} ${component.unitName}`)
    .join(', ');
}

function resetCompoundDraft() {
  compoundDraft.description = '';
  compoundDraft.quantity = 1;
  compoundDraft.unitName = 'BUNGKUS';
  compoundDraft.discountType = 'RP';
  compoundDraft.discountValue = 0;
  compoundDraft.instruction = '';
  compoundItems.value = [];
}

function setSelectedDoctor() {}

function emptyForm(keepUnit = true) {
  const unitId = keepUnit ? form.unitId : '';
  form.unitId = unitId;
  form.referencePatient = false;
  form.existingMrCode = '';
  form.registrationCode = '';
  form.patientTypeId = '';
  form.patientName = '';
  form.gender = 'M';
  form.birthDate = '';
  form.address = '';
  form.noteId = null;
  form.noteNumber = '';
  form.noteStatusLabel = '';
  form.cancelationNote = '';
  form.tariffClass = 'KELAS II';
  form.inpatient = false;
  form.receiptNumber = '';
  setSelectedDoctor(null);
  lines.value = [];
  itemResults.value = [];
  compoundResults.value = [];
  resetCompoundDraft();
  returnDetail.value = null;
  availableActions.canModify = false;
  availableActions.canValidate = false;
  availableActions.canCancel = false;
}

async function loadMasters() {
  loading.value = true;
  resetMessages();
  masters.units = mergeUnitOptions([], props.availableUnits);

  try {
    const data = await request('/apotik/masters');
    masters.units = mergeUnitOptions(data.units || [], props.availableUnits);
    masters.patientTypes = data.patientTypes || [];
  } catch (requestError) {
    console.error('Failed to load apotik masters', requestError);
    error.value = 'Data master apotik gagal dimuat. Silakan muat ulang halaman atau hubungi tim IT.';
  } finally {
    if (!form.unitId && masters.units.length) {
      form.unitId = String(masters.units[0].unitId);
    }
    loading.value = false;
  }
}

async function searchPatients() {
  patientSearching.value = true;
  resetMessages();

  try {
    const params = new URLSearchParams();
    Object.entries(patientSearch).forEach(([key, value]) => {
      if (value) {
        params.set(key, value);
      }
    });
    patientResults.value = await request(`/apotik/patients/registered?${params.toString()}`);
  } catch (requestError) {
    error.value = requestError.message;
  } finally {
    patientSearching.value = false;
  }
}

async function pickPatient(mrCode) {
  resetMessages();

  try {
    const detail = await request(`/apotik/patients/${encodeURIComponent(mrCode)}`);
    form.referencePatient = false;
    form.existingMrCode = detail.medicalRecordCode || '';
    form.registrationCode = detail.registrationCode || '';
    form.patientTypeId = detail.patientTypeId ? String(detail.patientTypeId) : '';
    form.patientName = detail.patientName || '';
    form.gender = detail.gender || 'M';
    form.birthDate = detail.birthDate || '';
    form.address = detail.address || '';
    form.tariffClass = detail.tariffClass || 'KELAS II';
    form.inpatient = Boolean(detail.inpatient);
  } catch (requestError) {
    error.value = requestError.message;
  }
}

async function searchItems() {
  if (!form.unitId) {
    error.value = 'Pilih unit apotik terlebih dahulu.';
    return;
  }

  itemSearching.value = true;
  resetMessages();

  try {
    const params = new URLSearchParams();
    if (itemSearch.code) {
      params.set('code', itemSearch.code);
    }
    if (itemSearch.name) {
      params.set('name', itemSearch.name);
    }
    params.set('tariffClass', form.tariffClass || 'KELAS II');
    itemResults.value = await request(`/apotik/units/${encodeURIComponent(form.unitId)}/items?${params.toString()}`);
  } catch (requestError) {
    error.value = requestError.message;
  } finally {
    itemSearching.value = false;
  }
}

async function searchCompoundItems() {
  if (!form.unitId) {
    error.value = 'Pilih unit apotik terlebih dahulu.';
    return;
  }

  compoundSearching.value = true;
  resetMessages();

  try {
    const params = new URLSearchParams();
    if (compoundSearch.code) {
      params.set('code', compoundSearch.code);
    }
    if (compoundSearch.name) {
      params.set('name', compoundSearch.name);
    }
    params.set('tariffClass', form.tariffClass || 'KELAS II');
    compoundResults.value = await request(`/apotik/units/${encodeURIComponent(form.unitId)}/items?${params.toString()}`);
  } catch (requestError) {
    error.value = requestError.message;
  } finally {
    compoundSearching.value = false;
  }
}

function addItem(option) {
  lines.value.push({
    clientId: `item-${Date.now()}-${Math.random()}`,
    lineType: 'ITEM',
    referenceId: option.itemId,
    code: option.itemCode,
    description: option.itemName,
    quantity: 1,
    unitName: option.unitName,
    unitPrice: option.price,
    discountType: 'RP',
    discountValue: 0,
    instruction: ''
  });
}

function addMisc() {
  if (!miscDraft.description || !miscDraft.quantity || !miscDraft.unitPrice) {
    error.value = 'Lengkapi nama, jumlah, dan harga biaya lain-lain.';
    return;
  }

  lines.value.push({
    clientId: `misc-${Date.now()}-${Math.random()}`,
    lineType: 'MISC',
    referenceId: null,
    code: 'MISC-001',
    description: miscDraft.description,
    quantity: Number(miscDraft.quantity),
    unitName: '-',
    unitPrice: Number(miscDraft.unitPrice),
    discountType: 'RP',
    discountValue: 0,
    instruction: null
  });

  miscDraft.description = '';
  miscDraft.quantity = 1;
  miscDraft.unitPrice = 0;
}

function addCompoundComponent(option) {
  const existingComponent = compoundItems.value.find((item) => item.referenceId === option.itemId);
  if (existingComponent) {
    existingComponent.quantity = Number(existingComponent.quantity) + 1;
    return;
  }

  compoundItems.value.push({
    referenceId: option.itemId,
    code: option.itemCode,
    description: option.itemName,
    unitName: option.unitName,
    quantity: 1,
    unitPrice: Number(option.price || 0)
  });
}

function removeCompoundComponent(index) {
  compoundItems.value.splice(index, 1);
}

function addCompound() {
  if (!compoundDraft.description.trim()) {
    error.value = 'Nama racikan wajib diisi.';
    return;
  }
  if (!compoundDraft.quantity || Number(compoundDraft.quantity) <= 0) {
    error.value = 'Jumlah racikan wajib lebih besar dari nol.';
    return;
  }
  if (!compoundDraft.instruction.trim()) {
    error.value = 'Aturan pakai racikan wajib diisi.';
    return;
  }
  if (!compoundItems.value.length) {
    error.value = 'Minimal satu komposisi racikan harus ditambahkan.';
    return;
  }

  const unitPrice = compoundItems.value.reduce((total, component) => {
    return total + (Number(component.quantity || 0) * Number(component.unitPrice || 0));
  }, 0);

  lines.value.push({
    clientId: `compound-${Date.now()}-${Math.random()}`,
    lineType: 'RACIKAN',
    referenceId: null,
    code: 'AUTO',
    description: compoundDraft.description,
    quantity: Number(compoundDraft.quantity),
    unitName: compoundDraft.unitName,
    unitPrice,
    discountType: normalizeDiscountType(compoundDraft.discountType),
    discountValue: Number(compoundDraft.discountValue || 0),
    instruction: compoundDraft.instruction,
    components: compoundItems.value.map((component) => ({
      referenceId: component.referenceId,
      code: component.code,
      description: component.description,
      unitName: component.unitName,
      quantity: Number(component.quantity || 0),
      unitPrice: Number(component.unitPrice || 0)
    }))
  });

  resetCompoundDraft();
}

function removeLine(index) {
  lines.value.splice(index, 1);
}

function buildPayload() {
  return {
    unitId: Number(form.unitId),
    referencePatient: form.referencePatient,
    existingMrCode: form.referencePatient ? null : form.existingMrCode || null,
    patientTypeId: form.patientTypeId ? Number(form.patientTypeId) : null,
    patientName: form.referencePatient ? form.patientName || null : null,
    gender: form.referencePatient ? form.gender || 'M' : null,
    birthDate: form.referencePatient ? form.birthDate || null : null,
    address: form.referencePatient ? form.address || null : null,
    receiptNumber: form.receiptNumber || null,
    lines: lines.value.map((line) => ({
      lineType: line.lineType,
      referenceId: line.referenceId,
      quantity: Number(line.quantity),
      unitPrice: line.lineType === 'MISC' ? Number(line.unitPrice) : null,
      discountType: normalizeDiscountType(line.discountType),
      discountValue: Number(line.discountValue || 0),
      description: line.lineType === 'ITEM' ? null : line.description,
      unitName: line.lineType === 'RACIKAN' ? line.unitName : null,
      instruction: line.lineType === 'ITEM' || line.lineType === 'RACIKAN' ? line.instruction || null : null,
      components: line.lineType === 'RACIKAN'
        ? (line.components || []).map((component) => ({
            referenceId: component.referenceId,
            quantity: Number(component.quantity)
          }))
        : null
    }))
  };
}

async function saveNote() {
  if (!form.unitId) {
    error.value = 'Unit apotik wajib dipilih.';
    return;
  }
  if (!lines.value.length) {
    error.value = 'Minimal satu transaksi harus ditambahkan.';
    return;
  }

  saving.value = true;
  resetMessages();

  try {
    const payload = buildPayload();
    const result = isEditingExistingNote.value
      ? await request(`/apotik/notes/${encodeURIComponent(form.noteId)}`, {
          method: 'PUT',
          body: JSON.stringify(payload)
        })
      : await request('/apotik/notes', {
          method: 'POST',
          body: JSON.stringify(payload)
        });

    message.value = `Nota ${result.noteNumber} berhasil disimpan.`;
    await loadNoteDetail(result.noteId);
  } catch (requestError) {
    error.value = requestError.message;
  } finally {
    saving.value = false;
  }
}

async function searchNotes() {
  if (!form.unitId) {
    error.value = 'Pilih unit apotik terlebih dahulu.';
    return;
  }

  noteSearching.value = true;
  resetMessages();

  try {
    const params = new URLSearchParams();
    if (noteSearch.noteNumber) {
      params.set('noteNumber', noteSearch.noteNumber);
    }
    if (noteSearch.patientName) {
      params.set('patientName', noteSearch.patientName);
    }
    noteResults.value = await request(`/apotik/units/${encodeURIComponent(form.unitId)}/notes?${params.toString()}`);
  } catch (requestError) {
    error.value = requestError.message;
  } finally {
    noteSearching.value = false;
  }
}

async function loadNoteDetail(noteId) {
  resetMessages();

  try {
    const detail = await request(`/apotik/notes/${encodeURIComponent(noteId)}`);
    form.noteId = detail.noteId;
    form.noteNumber = detail.noteNumber || '';
    form.noteStatusLabel = detail.statusLabel || '';
    form.unitId = detail.unitId ? String(detail.unitId) : form.unitId;
    form.referencePatient = !detail.medicalRecordCode;
    form.existingMrCode = detail.medicalRecordCode || '';
    form.registrationCode = detail.registrationCode || '';
    form.patientTypeId = detail.patientTypeId ? String(detail.patientTypeId) : '';
    form.patientName = detail.patientName || '';
    form.gender = detail.gender || 'M';
    form.birthDate = detail.birthDate || '';
    form.address = detail.address || '';
    form.receiptNumber = detail.receiptNumber || '';
    form.inpatient = Boolean(detail.inpatient);
    form.tariffClass = detail.tariffClass || 'KELAS II';
    form.cancelationNote = detail.cancelationNote || '';

    availableActions.canModify = detail.canModify;
    availableActions.canValidate = detail.canValidate;
    availableActions.canCancel = detail.canCancel;

    lines.value = (detail.lines || []).map((line) => ({
      clientId: `loaded-${line.lineType}-${line.lineId}`,
      lineType: line.lineType,
      referenceId: line.referenceId,
      code: line.code,
      description: line.description,
      quantity: Number(line.quantity || 0),
      unitName: line.unitName,
      unitPrice: Number(line.unitPrice || 0),
      discountType: normalizeDiscountType(line.discountType),
      discountValue: Number(line.discountValue || 0),
      instruction: line.instruction || '',
      components: (line.components || []).map((component) => ({
        referenceId: component.referenceId,
        code: component.code,
        description: component.description,
        unitName: component.unitName,
        quantity: Number(component.quantity || 0)
      }))
    }));
  } catch (requestError) {
    error.value = requestError.message;
  }
}

async function validateNote() {
  if (!form.noteId) {
    return;
  }

  resetMessages();

  try {
    const result = await request(`/apotik/notes/${encodeURIComponent(form.noteId)}/validate`, {
      method: 'POST'
    });
    message.value = `Nota ${result.noteNumber} berhasil divalidasi.`;
    await loadNoteDetail(form.noteId);
  } catch (requestError) {
    error.value = requestError.message;
  }
}

async function cancelNote() {
  if (!form.noteId) {
    return;
  }
  if (!form.cancelationNote.trim()) {
    error.value = 'Alasan pembatalan wajib diisi.';
    return;
  }

  resetMessages();

  try {
    const result = await request(`/apotik/notes/${encodeURIComponent(form.noteId)}/cancel`, {
      method: 'POST',
      body: JSON.stringify({
        reason: form.cancelationNote
      })
    });
    message.value = `Nota ${result.noteNumber} berhasil dibatalkan.`;
    await loadNoteDetail(form.noteId);
  } catch (requestError) {
    error.value = requestError.message;
  }
}

async function searchReturns() {
  returnSearching.value = true;
  resetMessages();

  try {
    const params = new URLSearchParams();
    Object.entries(returnSearch).forEach(([key, value]) => {
      if (value) {
        params.set(key, value);
      }
    });
    returnResults.value = await request(`/apotik/returns?${params.toString()}`);
  } catch (requestError) {
    error.value = requestError.message;
  } finally {
    returnSearching.value = false;
  }
}

async function loadReturnDetail(returnId) {
  resetMessages();

  try {
    returnDetail.value = await request(`/apotik/returns/${encodeURIComponent(returnId)}`);
  } catch (requestError) {
    error.value = requestError.message;
  }
}

function switchReferencePatient() {
  resetMessages();
  lines.value = [];
  form.noteId = null;
  form.noteNumber = '';
  form.noteStatusLabel = '';
  form.cancelationNote = '';
  availableActions.canModify = false;
  availableActions.canValidate = false;
  availableActions.canCancel = false;
  form.existingMrCode = '';
  form.registrationCode = '';
  form.patientTypeId = '';
  form.patientName = '';
  form.gender = 'M';
  form.birthDate = '';
  form.address = '';
  form.tariffClass = 'KELAS II';
  form.inpatient = false;
  itemResults.value = [];
  compoundResults.value = [];
  resetCompoundDraft();
}

onMounted(async () => {
  const defaultDates = buildDefaultReturnDates();
  returnSearch.startDate = defaultDates.startDate;
  returnSearch.endDate = defaultDates.endDate;
  await loadMasters();
});
</script>

<template>
  <section class="apotik-wrapper">
    <header class="section-header">
      <div>
        <p class="section-kicker">Apotik</p>
        <h2>Transaksi dan monitoring retur obat</h2>
        <p class="section-copy">
          Modul modern ini mengikuti tiga transaksi utama apotik: tambah O-BM, tambah racikan, dan tambah biaya lain-lain;
          lalu menutup alur simpan nota, validasi, batal, dan monitoring retur obat dari database legacy.
        </p>
      </div>
      <div class="tab-row">
        <button class="tab-button" :class="{ 'tab-button--active': activeTab === 'transaction' }" type="button" @click="activeTab = 'transaction'">
          Transaksi
        </button>
        <button class="tab-button" :class="{ 'tab-button--active': activeTab === 'return' }" type="button" @click="activeTab = 'return'">
          Retur Obat
        </button>
      </div>
    </header>

    <p v-if="error" class="status-banner status-banner--error">{{ error }}</p>
    <p v-else-if="message" class="status-banner status-banner--success">{{ message }}</p>

    <template v-if="activeTab === 'transaction'">
      <section class="panel-card">
        <div class="meta-grid">
          <label>
            <span>Lokasi Transaksi</span>
            <select v-model="form.unitId">
              <option value="">-</option>
              <option v-for="unit in masters.units" :key="unit.unitId" :value="String(unit.unitId)">
                {{ unit.unitCode }} - {{ unit.unitName }}
              </option>
            </select>
          </label>
          <label>
            <span>No. Nota</span>
            <input :value="form.noteNumber || '-'" type="text" disabled />
          </label>
          <label>
            <span>Status Nota</span>
            <input :value="form.noteStatusLabel || 'BARU'" type="text" disabled />
          </label>
          <label>
            <span>Kelas Tarif</span>
            <input :value="form.tariffClass || 'KELAS II'" type="text" disabled />
          </label>
        </div>

        <div class="toggle-row">
          <label class="checkbox-label">
            <input v-model="form.referencePatient" type="checkbox" @change="switchReferencePatient" />
            Pasien Bebas / Rujukan
          </label>
          <span class="info-badge">Registrasi: {{ form.registrationCode || '-' }}</span>
          <span class="info-badge">MR: {{ form.existingMrCode || '-' }}</span>
          <span class="info-badge">Mode: {{ form.referencePatient ? 'Pasien Bebas' : 'Pasien Terdaftar' }}</span>
        </div>
      </section>

      <div class="layout-grid">
        <section class="panel-card">
          <h3>Pencarian Pasien Aktif</h3>
          <div v-if="!form.referencePatient" class="search-grid">
            <label>
              <span>No. MR</span>
              <input v-model="patientSearch.mrCode" type="text" />
            </label>
            <label>
              <span>Nama</span>
              <input v-model="patientSearch.patientName" type="text" />
            </label>
            <label class="search-grid__wide">
              <span>Alamat</span>
              <input v-model="patientSearch.address" type="text" />
            </label>
          </div>

          <div v-if="!form.referencePatient" class="button-row">
            <button class="primary-button" type="button" :disabled="patientSearching" @click="searchPatients">
              {{ patientSearching ? 'Mencari...' : 'Cari Pasien' }}
            </button>
          </div>

          <div v-if="!patientResults.length" class="table-wrap">
            <table class="result-table">
              <thead>
                <tr>
                  <th>No. MR</th>
                  <th>Nama</th>
                  <th>Alamat</th>
                  <th />
                </tr>
              </thead>
              <tbody>
                <tr v-if="!patientResults.length">
                  <td colspan="4" class="empty-cell">Belum ada hasil pencarian.</td>
                </tr>
              </tbody>
            </table>
          </div>

          <div v-else class="table-wrap">
            <table class="result-table">
              <thead>
                <tr>
                  <th>No. MR</th>
                  <th>Nama</th>
                  <th>Alamat</th>
                  <th />
                </tr>
              </thead>
              <tbody>
                <tr v-for="patient in patientResults" :key="patient.medicalRecordCode">
                  <td>{{ patient.medicalRecordCode }}</td>
                  <td>{{ patient.patientName }}</td>
                  <td>{{ patient.address }}</td>
                  <td>
                    <button class="link-button" type="button" @click="pickPatient(patient.medicalRecordCode)">Pilih</button>
                  </td>
                </tr>
              </tbody>
            </table>
          </div>

          <div class="form-grid">
            <label>
              <span>Nama Pasien</span>
              <input v-model="form.patientName" type="text" :disabled="!form.referencePatient" />
            </label>
            <label>
              <span>Jenis Kelamin</span>
              <select v-model="form.gender" :disabled="!form.referencePatient">
                <option value="M">PRIA</option>
                <option value="F">WANITA</option>
              </select>
            </label>
            <label>
              <span>Tanggal Lahir</span>
              <input v-model="form.birthDate" type="date" :disabled="!form.referencePatient" />
            </label>
            <label>
              <span>Tipe Pasien</span>
              <select v-model="form.patientTypeId" :disabled="!form.referencePatient">
                <option value="">-</option>
                <option v-for="item in masters.patientTypes" :key="item.patientTypeId" :value="String(item.patientTypeId)">
                  {{ item.patientTypeCode }} - {{ item.patientTypeName }}
                </option>
              </select>
            </label>
            <label class="form-grid__wide">
              <span>Alamat</span>
              <textarea v-model="form.address" rows="2" :disabled="!form.referencePatient" />
            </label>
            <label>
              <span>No. Resep</span>
              <input v-model="form.receiptNumber" type="text" />
            </label>
            <label>
              <span>Jenis Registrasi</span>
              <input :value="form.inpatient ? 'RAWAT INAP' : 'RAWAT JALAN / UMUM'" type="text" disabled />
            </label>
          </div>
        </section>

        <section class="panel-card">
          <h3>Pencarian Nota Apotik</h3>
          <div class="search-grid">
            <label>
              <span>No. Nota</span>
              <input v-model="noteSearch.noteNumber" type="text" />
            </label>
            <label>
              <span>Nama Pasien</span>
              <input v-model="noteSearch.patientName" type="text" />
            </label>
          </div>

          <div class="button-row">
            <button class="secondary-button" type="button" :disabled="noteSearching" @click="searchNotes">
              {{ noteSearching ? 'Mencari...' : 'Cari Nota' }}
            </button>
            <button class="secondary-button" type="button" @click="emptyForm(true)">Baru</button>
          </div>

          <div class="table-wrap">
            <table class="result-table">
              <thead>
                <tr>
                  <th>No. Nota</th>
                  <th>Pasien</th>
                  <th>Status</th>
                  <th>Dibuat</th>
                  <th />
                </tr>
              </thead>
              <tbody>
                <tr v-for="note in noteResults" :key="note.noteId">
                  <td>{{ note.noteNumber }}</td>
                  <td>{{ note.patientName }}</td>
                  <td>{{ note.statusLabel }}</td>
                  <td>{{ formatDateTime(note.createdAt) }}</td>
                  <td>
                    <button class="link-button" type="button" @click="loadNoteDetail(note.noteId)">Buka</button>
                  </td>
                </tr>
                <tr v-if="!noteResults.length">
                  <td colspan="5" class="empty-cell">Belum ada hasil pencarian nota.</td>
                </tr>
              </tbody>
            </table>
          </div>

          <label class="form-grid__wide">
            <span>Alasan Pembatalan</span>
            <textarea v-model="form.cancelationNote" rows="2" />
          </label>

          <div class="button-row">
            <button class="primary-button" type="button" :disabled="saving" @click="saveNote">
              {{ saving ? 'Menyimpan...' : (isEditingExistingNote ? 'Simpan Perubahan' : 'Simpan Nota') }}
            </button>
            <button class="secondary-button" type="button" :disabled="!availableActions.canValidate" @click="validateNote">
              Validasi
            </button>
            <button class="danger-button" type="button" :disabled="!availableActions.canCancel" @click="cancelNote">
              Batalkan Nota
            </button>
          </div>
        </section>
      </div>

      <div class="layout-grid layout-grid--triple">
        <section class="panel-card">
          <h3>Tambah O-BM (Obat / Bahan Medis)</h3>
          <div class="search-grid">
            <label>
              <span>Kode</span>
              <input v-model="itemSearch.code" type="text" />
            </label>
            <label>
              <span>Nama</span>
              <input v-model="itemSearch.name" type="text" />
            </label>
          </div>

          <div class="button-row">
            <button class="secondary-button" type="button" :disabled="itemSearching" @click="searchItems">
              {{ itemSearching ? 'Mencari...' : 'Cari Item' }}
            </button>
          </div>

          <div class="table-wrap">
            <table class="result-table">
              <thead>
                <tr>
                  <th>Kode</th>
                  <th>Nama</th>
                  <th>Satuan</th>
                  <th>Stok</th>
                  <th>Harga</th>
                  <th />
                </tr>
              </thead>
              <tbody>
                <tr v-for="item in itemResults" :key="item.itemId">
                  <td>{{ item.itemCode }}</td>
                  <td>{{ item.itemName }}</td>
                  <td>{{ item.unitName }}</td>
                  <td>{{ item.stockQuantity }}</td>
                  <td>{{ formatCurrency(item.price) }}</td>
                  <td><button class="link-button" type="button" @click="addItem(item)">Tambah</button></td>
                </tr>
                <tr v-if="!itemResults.length">
                  <td colspan="6" class="empty-cell">Belum ada hasil pencarian item.</td>
                </tr>
              </tbody>
            </table>
          </div>
        </section>

        <section class="panel-card">
          <h3>Tambah Racikan</h3>
          <div class="search-grid">
            <label>
              <span>Kode Item</span>
              <input v-model="compoundSearch.code" type="text" />
            </label>
            <label>
              <span>Nama Item</span>
              <input v-model="compoundSearch.name" type="text" />
            </label>
          </div>

          <div class="button-row">
            <button class="secondary-button" type="button" :disabled="compoundSearching" @click="searchCompoundItems">
              {{ compoundSearching ? 'Mencari...' : 'Cari Komposisi' }}
            </button>
          </div>

          <div class="table-wrap">
            <table class="result-table">
              <thead>
                <tr>
                  <th>Kode</th>
                  <th>Nama</th>
                  <th>Satuan</th>
                  <th>Harga</th>
                  <th />
                </tr>
              </thead>
              <tbody>
                <tr v-for="item in compoundResults" :key="`compound-${item.itemId}`">
                  <td>{{ item.itemCode }}</td>
                  <td>{{ item.itemName }}</td>
                  <td>{{ item.unitName }}</td>
                  <td>{{ formatCurrency(item.price) }}</td>
                  <td><button class="link-button" type="button" @click="addCompoundComponent(item)">Pilih</button></td>
                </tr>
                <tr v-if="!compoundResults.length">
                  <td colspan="5" class="empty-cell">Belum ada hasil pencarian komposisi racikan.</td>
                </tr>
              </tbody>
            </table>
          </div>

          <div class="form-grid">
            <label>
              <span>Nama Racikan</span>
              <input v-model="compoundDraft.description" type="text" />
            </label>
            <label>
              <span>Satuan Racikan</span>
              <select v-model="compoundDraft.unitName">
                <option v-for="option in compoundUnitOptions" :key="option" :value="option">
                  {{ option }}
                </option>
              </select>
            </label>
            <label>
              <span>Jumlah Racikan</span>
              <input v-model.number="compoundDraft.quantity" type="number" min="1" step="1" />
            </label>
            <label>
              <span>Diskon</span>
              <div class="discount-inline">
                <select v-model="compoundDraft.discountType">
                  <option value="RP">RP</option>
                  <option value="%">%</option>
                </select>
                <input v-model.number="compoundDraft.discountValue" type="number" min="0" step="1" />
              </div>
            </label>
            <label class="form-grid__wide">
              <span>Aturan Pakai</span>
              <input v-model="compoundDraft.instruction" type="text" placeholder="cth: 3x1 sesudah makan" />
            </label>
          </div>

          <div class="table-wrap">
            <table class="result-table">
              <thead>
                <tr>
                  <th>Kode</th>
                  <th>Komposisi</th>
                  <th>Qty per Racikan</th>
                  <th>Satuan</th>
                  <th>Harga</th>
                  <th />
                </tr>
              </thead>
              <tbody>
                <tr v-for="(component, index) in compoundItems" :key="`component-${component.referenceId}`">
                  <td>{{ component.code }}</td>
                  <td>{{ component.description }}</td>
                  <td>
                    <input v-model.number="component.quantity" class="table-input table-input--sm" type="number" min="0.1" step="0.1" />
                  </td>
                  <td>{{ component.unitName }}</td>
                  <td>Rp {{ formatCurrency(component.unitPrice) }}</td>
                  <td>
                    <button class="link-button link-button--danger" type="button" @click="removeCompoundComponent(index)">Hapus</button>
                  </td>
                </tr>
                <tr v-if="!compoundItems.length">
                  <td colspan="6" class="empty-cell">Belum ada komposisi racikan yang dipilih.</td>
                </tr>
              </tbody>
            </table>
          </div>

          <div class="section-row">
            <strong class="total-pill">Harga per racikan: Rp {{ formatCurrency(compoundItems.reduce((total, component) => total + (Number(component.quantity || 0) * Number(component.unitPrice || 0)), 0)) }}</strong>
            <button class="secondary-button" type="button" @click="addCompound">Tambah Racikan</button>
          </div>
        </section>

        <section class="panel-card">
          <h3>Tambah Biaya Lain-lain</h3>
          <div class="search-grid">
            <label class="search-grid__wide">
              <span>Nama Biaya</span>
              <input v-model="miscDraft.description" type="text" />
            </label>
            <label>
              <span>Jumlah</span>
              <input v-model.number="miscDraft.quantity" type="number" min="1" step="1" />
            </label>
            <label>
              <span>Harga Satuan</span>
              <input v-model.number="miscDraft.unitPrice" type="number" min="0" step="100" />
            </label>
          </div>

          <div class="button-row">
            <button class="secondary-button" type="button" @click="addMisc">Tambah Biaya</button>
          </div>
        </section>
      </div>

      <section class="panel-card">
        <div class="section-row">
          <h3>Detail Transaksi</h3>
          <strong class="total-pill">Total: Rp {{ formatCurrency(totalAmount) }}</strong>
        </div>

        <div class="table-wrap">
          <table class="result-table">
            <thead>
              <tr>
                <th>Tipe</th>
                <th>Kode</th>
                <th>Deskripsi</th>
                <th>Jumlah</th>
                <th>Satuan</th>
                <th>Harga</th>
                <th>Diskon</th>
                <th>Subtotal</th>
                <th>Aturan Pakai</th>
                <th />
              </tr>
            </thead>
            <tbody>
              <tr v-for="(line, index) in lines" :key="line.clientId">
                <td>{{ formatLineType(line.lineType) }}</td>
                <td>{{ line.code }}</td>
                <td>
                  <div>{{ line.description }}</div>
                  <small v-if="line.lineType === 'RACIKAN' && line.components?.length" class="line-helper">
                    {{ formatCompoundSummary(line.components) }}
                  </small>
                </td>
                <td>
                  <input
                    v-model.number="line.quantity"
                    class="table-input table-input--sm"
                    type="number"
                    :min="line.lineType === 'ITEM' ? '0.1' : '1'"
                    :step="line.lineType === 'ITEM' ? '0.1' : '1'"
                  />
                </td>
                <td>{{ line.unitName }}</td>
                <td><input v-model.number="line.unitPrice" class="table-input" type="number" :disabled="line.lineType !== 'MISC'" min="0" step="100" /></td>
                <td class="discount-cell">
                  <select v-model="line.discountType" class="table-input table-input--sm">
                    <option value="RP">RP</option>
                    <option value="%">%</option>
                  </select>
                  <input v-model.number="line.discountValue" class="table-input table-input--sm" type="number" min="0" step="1" />
                </td>
                <td>Rp {{ formatCurrency(calculateSubtotal(line)) }}</td>
                <td>
                  <input
                    v-if="line.lineType === 'ITEM' || line.lineType === 'RACIKAN'"
                    v-model="line.instruction"
                    class="table-input"
                    placeholder="cth: 3x1 sesudah makan"
                    type="text"
                  />
                  <span v-else>-</span>
                </td>
                <td><button class="link-button link-button--danger" type="button" @click="removeLine(index)">Hapus</button></td>
              </tr>
              <tr v-if="!lines.length">
                <td colspan="10" class="empty-cell">Belum ada item transaksi.</td>
              </tr>
            </tbody>
          </table>
        </div>
      </section>
    </template>

    <template v-else>
      <div class="layout-grid">
        <section class="panel-card">
          <h3>Pencarian Retur Obat</h3>
          <div class="search-grid">
            <label>
              <span>No. Retur</span>
              <input v-model="returnSearch.returnNumber" type="text" />
            </label>
            <label>
              <span>Nama Pasien</span>
              <input v-model="returnSearch.patientName" type="text" />
            </label>
            <label>
              <span>Dari Tanggal</span>
              <input v-model="returnSearch.startDate" type="date" />
            </label>
            <label>
              <span>Sampai Tanggal</span>
              <input v-model="returnSearch.endDate" type="date" />
            </label>
          </div>

          <div class="button-row">
            <button class="primary-button" type="button" :disabled="returnSearching" @click="searchReturns">
              {{ returnSearching ? 'Mencari...' : 'Cari Retur' }}
            </button>
          </div>

          <div class="table-wrap">
            <table class="result-table">
              <thead>
                <tr>
                  <th>No. Retur</th>
                  <th>No. Nota</th>
                  <th>Pasien</th>
                  <th>Status</th>
                  <th>Total</th>
                  <th>Tanggal</th>
                  <th />
                </tr>
              </thead>
              <tbody>
                <tr v-for="item in returnResults" :key="item.returnId">
                  <td>{{ item.returnNumber }}</td>
                  <td>{{ item.originalNoteNumber || '-' }}</td>
                  <td>{{ item.patientName }}</td>
                  <td>{{ item.statusLabel }}</td>
                  <td>Rp {{ formatCurrency(item.totalAmount) }}</td>
                  <td>{{ formatDateTime(item.createdAt) }}</td>
                  <td><button class="link-button" type="button" @click="loadReturnDetail(item.returnId)">Buka</button></td>
                </tr>
                <tr v-if="!returnResults.length">
                  <td colspan="7" class="empty-cell">Belum ada hasil pencarian retur.</td>
                </tr>
              </tbody>
            </table>
          </div>
        </section>

        <section class="panel-card">
          <h3>Detail Retur</h3>

          <template v-if="returnDetail">
            <div class="badge-row">
              <span class="info-badge">Retur: {{ returnDetail.returnNumber }}</span>
              <span class="info-badge">Nota: {{ returnDetail.originalNoteNumber || '-' }}</span>
              <span class="info-badge">MR: {{ returnDetail.medicalRecordCode || '-' }}</span>
              <span class="info-badge">Registrasi: {{ returnDetail.registrationCode || '-' }}</span>
            </div>

            <div class="meta-grid">
              <label>
                <span>Pasien</span>
                <input :value="returnDetail.patientName" type="text" disabled />
              </label>
              <label>
                <span>Status</span>
                <input :value="returnDetail.statusLabel" type="text" disabled />
              </label>
              <label>
                <span>Total Retur</span>
                <input :value="`Rp ${formatCurrency(returnDetail.totalAmount)}`" type="text" disabled />
              </label>
              <label>
                <span>Dibuat</span>
                <input :value="formatDateTime(returnDetail.createdAt)" type="text" disabled />
              </label>
            </div>

            <div class="table-wrap">
              <table class="result-table">
                <thead>
                  <tr>
                    <th>Kode</th>
                    <th>Item</th>
                    <th>Satuan</th>
                    <th>Qty Awal</th>
                    <th>Qty Retur</th>
                    <th>Nilai</th>
                  </tr>
                </thead>
                <tbody>
                  <tr v-for="line in returnDetail.lines" :key="line.lineId">
                    <td>{{ line.itemCode }}</td>
                    <td>{{ line.itemName }}</td>
                    <td>{{ line.unitName }}</td>
                    <td>{{ line.originalQuantity ?? '-' }}</td>
                    <td>{{ line.returnedQuantity ?? '-' }}</td>
                    <td>Rp {{ formatCurrency(line.value) }}</td>
                  </tr>
                  <tr v-if="!returnDetail.lines.length">
                    <td colspan="6" class="empty-cell">Detail retur belum tersedia.</td>
                  </tr>
                </tbody>
              </table>
            </div>
          </template>

          <p v-else class="empty-state">Pilih satu data retur untuk melihat detail item yang dikembalikan.</p>
        </section>
      </div>
    </template>
  </section>
</template>

<style scoped>
.apotik-wrapper {
  display: grid;
  gap: 20px;
}

.section-header,
.panel-card {
  background: rgba(255, 255, 255, 0.94);
  border: 1px solid rgba(150, 136, 117, 0.35);
  box-shadow: 0 12px 24px rgba(53, 64, 84, 0.08);
}

.section-header {
  display: flex;
  justify-content: space-between;
  gap: 20px;
  padding: 24px 28px;
}

.section-kicker {
  margin: 0 0 6px;
  text-transform: uppercase;
  letter-spacing: 0.08em;
  font-size: 12px;
}

.section-header h2,
.panel-card h3 {
  margin: 0;
}

.section-copy {
  margin: 12px 0 0;
  max-width: 820px;
}

.tab-row,
.button-row,
.toggle-row,
.badge-row,
.section-row {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  align-items: center;
}

.tab-button,
.primary-button,
.secondary-button,
.danger-button,
.link-button {
  cursor: pointer;
}

.tab-button,
.primary-button,
.secondary-button,
.danger-button {
  min-height: 38px;
  border-radius: 10px;
  padding: 0 14px;
  border: 1px solid transparent;
  font-weight: 700;
}

.tab-button {
  background: #eef3fb;
  color: #304b73;
}

.tab-button--active,
.primary-button {
  background: #304b73;
  color: #fff;
}

.secondary-button {
  background: #f3efe7;
  border-color: rgba(150, 136, 117, 0.35);
  color: #5a4a34;
}

.danger-button {
  background: #7e2331;
  color: #fff;
}

.status-banner {
  margin: 0;
  padding: 12px 16px;
  border-radius: 10px;
  font-weight: 600;
}

.status-banner--success {
  background: #e6f5ea;
  color: #1d6b3a;
}

.status-banner--error {
  background: #fde8ea;
  color: #a32943;
}

.panel-card {
  padding: 22px 24px;
}

.layout-grid {
  display: grid;
  grid-template-columns: minmax(0, 1.25fr) minmax(0, 1fr);
  gap: 20px;
}

.layout-grid--wide {
  grid-template-columns: minmax(0, 1.5fr) minmax(320px, 0.7fr);
}

.layout-grid--triple {
  grid-template-columns: repeat(3, minmax(0, 1fr));
}

.search-grid,
.form-grid,
.meta-grid {
  display: grid;
  gap: 14px;
}

.search-grid,
.meta-grid {
  grid-template-columns: repeat(2, minmax(0, 1fr));
}

.form-grid {
  grid-template-columns: repeat(2, minmax(0, 1fr));
  margin-top: 16px;
}

.search-grid__wide,
.form-grid__wide {
  grid-column: 1 / -1;
}

label {
  display: grid;
  gap: 6px;
  font-size: 14px;
  color: #3d4b63;
}

input,
select,
textarea {
  width: 100%;
  border: 1px solid rgba(136, 145, 163, 0.45);
  border-radius: 10px;
  padding: 10px 12px;
  font: inherit;
  background: #fff;
  box-sizing: border-box;
}

input:disabled,
textarea:disabled {
  background: #f7f7f9;
  color: #586175;
}

.checkbox-label {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  font-weight: 600;
}

.table-wrap {
  overflow: auto;
  margin-top: 14px;
}

.result-table {
  width: 100%;
  border-collapse: collapse;
}

.result-table th,
.result-table td {
  padding: 10px 12px;
  border-bottom: 1px solid rgba(136, 145, 163, 0.18);
  text-align: left;
  vertical-align: top;
}

.result-table th {
  background: #f6f8fb;
  color: #304b73;
  white-space: nowrap;
}

.empty-cell,
.empty-state {
  color: #6b7280;
}

.info-badge,
.total-pill {
  display: inline-flex;
  align-items: center;
  min-height: 32px;
  padding: 0 12px;
  border-radius: 999px;
  background: #eef3fb;
  color: #304b73;
  font-weight: 700;
}

.link-button {
  background: transparent;
  border: 0;
  color: #2d5aa3;
  font-weight: 700;
  padding: 0;
}

.link-button--danger {
  color: #a32943;
}

.table-input {
  min-width: 120px;
  padding: 8px 10px;
}

.table-input--sm {
  min-width: 72px;
}

.discount-cell {
  display: grid;
  gap: 8px;
}

.discount-inline {
  display: grid;
  grid-template-columns: 92px 1fr;
  gap: 8px;
}

.line-helper {
  display: block;
  margin-top: 4px;
  color: #6b7280;
}

@media (max-width: 1100px) {
  .layout-grid,
  .layout-grid--wide,
  .layout-grid--triple,
  .search-grid,
  .form-grid,
  .meta-grid {
    grid-template-columns: 1fr;
  }

  .section-header {
    flex-direction: column;
  }
}
</style>
