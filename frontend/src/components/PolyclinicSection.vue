<script setup>
import { computed, onMounted, reactive, ref, watch } from 'vue';

const props = defineProps({
  apiBaseUrl: {
    type: String,
    required: true
  },
  availableUnits: { type: Array, default: () => [] }
});

const emit = defineEmits(['session-expired']);

const loading = ref(true);
const saving = ref(false);
const searchingPatients = ref(false);
const searchingNotes = ref(false);
const treatmentSearching = ref(false);
const itemSearching = ref(false);
const statusMessage = ref('');
const errorMessage = ref('');

const searchMode = ref('patient'); // 'patient' or 'note'
const selectedPatient = ref(null);
const showSearch = ref(true);

const masters = reactive({
  units: [],
  patientTypes: [],
  escorts: []
});

const registeredPatients = ref([]);
const noteResults = ref([]);
const noteDetail = ref(null);
const treatmentResults = ref([]);
const itemResults = ref([]);
const doctorResults = ref([]);

const patientSearch = reactive({
  mrCode: '',
  patientName: '',
  address: ''
});

const noteSearch = reactive({
  noteNumber: '',
  patientName: ''
});

const treatmentSearch = reactive({
  code: '',
  name: ''
});

const itemSearch = reactive({
  code: '',
  name: ''
});

const miscDraft = reactive({
  description: '',
  quantity: 1,
  unitPrice: 0
});

const drawerState = reactive({
  header: true,
  patientSearch: false,
  treatment: false,
  item: false,
  misc: false
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
  escortId: '',
  doctorId: '',
  doctorLabel: '',
  noteId: null,
  noteNumber: '',
  noteStatusLabel: '',
  cancelationNote: ''
});

const lines = ref([]);
const availableActions = reactive({
  canModify: false,
  canValidate: false,
  canCancel: false
});

const selectedUnit = computed(() => {
  return masters.units.find((item) => String(item.unitId) === String(form.unitId)) || null;
});

const totalAmount = computed(() => {
  return lines.value.reduce((total, line) => total + calculateSubtotal(line), 0);
});

const isEditingExistingNote = computed(() => Boolean(form.noteId));

const patientModeLabel = computed(() => (form.referencePatient ? 'Pasien Bebas / Rujukan' : 'Pasien Terdaftar'));

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
  statusMessage.value = '';
  errorMessage.value = '';
}

function normalizeDiscountType(discountType) {
  return discountType === '%' ? '%' : 'RP';
}

function calculateSubtotal(line) {
  const amount = Number(line.unitPrice || 0) * Number(line.quantity || 0);
  const discountValue = Number(line.discountValue || 0);
  let result;

  if (normalizeDiscountType(line.discountType) === '%') {
    result = amount - (amount * discountValue / 100);
  } else {
    result = amount - discountValue;
  }

  return Math.ceil(result);
}

function formatCurrency(value) {
  return new Intl.NumberFormat('id-ID', {
    maximumFractionDigits: 0,
    minimumFractionDigits: 0
  }).format(Math.ceil(Number(value || 0)));
}

function setSelectedDoctor(doctor) {
  form.doctorId = doctor ? String(doctor.doctorId) : '';
  form.doctorLabel = doctor ? `${doctor.doctorCode} - ${doctor.doctorName}` : '';
}

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
  form.escortId = '';
  form.noteId = null;
  form.noteNumber = '';
  form.noteStatusLabel = '';
  form.cancelationNote = '';
  setSelectedDoctor(null);
  lines.value = [];
  noteDetail.value = null;
  availableActions.canModify = false;
  availableActions.canValidate = false;
  availableActions.canCancel = false;
}

async function loadMasters() {
  loading.value = true;
  resetMessages();

  try {
    const data = await request('/polyclinic/masters');
    masters.units = data.units || [];
    masters.patientTypes = data.patientTypes || [];
    masters.escorts = data.escorts || [];
    if (!form.unitId && masters.units.length) {
      form.unitId = String(masters.units[0].unitId);
    }
  } catch (error) {
    errorMessage.value = error.message;
  } finally {
    loading.value = false;
  }
}

async function searchRegisteredPatients() {
  if (!form.unitId) {
    errorMessage.value = 'Pilih unit poliklinik terlebih dahulu.';
    return;
  }

  searchingPatients.value = true;
  resetMessages();

  try {
    const params = new URLSearchParams({
      unitId: form.unitId
    });

    Object.entries(patientSearch).forEach(([key, value]) => {
      if (value) {
        params.set(key, value);
      }
    });

    registeredPatients.value = await request(`/polyclinic/patients/registered?${params.toString()}`);
  } catch (error) {
    errorMessage.value = error.message;
  } finally {
    searchingPatients.value = false;
  }
}

async function pickRegisteredPatient(mrCode) {
  resetMessages();

  try {
    const detail = await request(`/polyclinic/patients/${encodeURIComponent(mrCode)}?unitId=${encodeURIComponent(form.unitId)}`);
    form.referencePatient = false;
    form.existingMrCode = detail.medicalRecordCode || '';
    form.registrationCode = detail.registrationCode || '';
    form.patientTypeId = detail.patientTypeId ? String(detail.patientTypeId) : '';
    form.patientName = detail.patientName || '';
    form.gender = detail.gender || 'M';
    form.birthDate = detail.birthDate || '';
    form.address = detail.address || '';
    setSelectedDoctor(detail.doctorId ? {
      doctorId: detail.doctorId,
      doctorCode: detail.doctorCode,
      doctorName: detail.doctorName
    } : null);
  } catch (error) {
    errorMessage.value = error.message;
  }
}

async function searchDoctors() {
  if (!form.unitId) {
    errorMessage.value = 'Pilih unit poliklinik terlebih dahulu.';
    return;
  }

  try {
    doctorResults.value = await request(
      `/polyclinic/units/${encodeURIComponent(form.unitId)}/doctors?code=&name=`
    );
  } catch (error) {
    errorMessage.value = error.message;
  }
}

async function searchTreatments() {
  if (!form.unitId) {
    errorMessage.value = 'Pilih unit poliklinik terlebih dahulu.';
    return;
  }

  treatmentSearching.value = true;
  resetMessages();

  try {
    const params = new URLSearchParams();
    if (treatmentSearch.code) {
      params.set('code', treatmentSearch.code);
    }
    if (treatmentSearch.name) {
      params.set('name', treatmentSearch.name);
    }
    params.set('tariffClass', 'KELAS II');
    treatmentResults.value = await request(`/polyclinic/units/${encodeURIComponent(form.unitId)}/treatments?${params.toString()}`);
  } catch (error) {
    errorMessage.value = error.message;
  } finally {
    treatmentSearching.value = false;
  }
}

async function searchItems() {
  if (!form.unitId) {
    errorMessage.value = 'Pilih unit poliklinik terlebih dahulu.';
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
    params.set('tariffClass', 'KELAS II');
    itemResults.value = await request(`/polyclinic/units/${encodeURIComponent(form.unitId)}/items?${params.toString()}`);
  } catch (error) {
    errorMessage.value = error.message;
  } finally {
    itemSearching.value = false;
  }
}

function addTreatment(option) {
  lines.value.push({
    clientId: `treatment-${Date.now()}-${Math.random()}`,
    lineType: 'TREATMENT',
    referenceId: option.treatmentFeeId,
    code: option.treatmentCode,
    description: option.doctorFee > 0 && form.doctorLabel
      ? `${option.treatmentName} - ${form.doctorLabel.split(' - ')[1]}`
      : option.treatmentName,
    quantity: 1,
    unitName: '-',
    unitPrice: option.price,
    discountType: 'RP',
    discountValue: 0,
    doctorStaffId: form.doctorId ? Number(form.doctorId) : null,
    readOnly: false
  });
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
    instruction: '',
    stockQuantity: option.stockQuantity,
    readOnly: false
  });
}

function addMisc() {
  if (!miscDraft.description || !miscDraft.quantity || !miscDraft.unitPrice) {
    errorMessage.value = 'Lengkapi nama, jumlah, dan harga biaya lain-lain.';
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
    readOnly: false
  });

  miscDraft.description = '';
  miscDraft.quantity = 1;
  miscDraft.unitPrice = 0;
}

function removeLine(index) {
  lines.value.splice(index, 1);
}

function toggleDrawer(name) {
  if (name === 'treatment' || name === 'item' || name === 'misc') {
    const nextValue = !drawerState[name];
    drawerState.treatment = false;
    drawerState.item = false;
    drawerState.misc = false;
    drawerState[name] = nextValue;
    return;
  }

  drawerState[name] = !drawerState[name];
}

function buildPayload() {
  return {
    unitId: Number(form.unitId),
    referencePatient: form.referencePatient,
    existingMrCode: form.referencePatient ? null : form.existingMrCode || null,
    patientTypeId: form.patientTypeId ? Number(form.patientTypeId) : null,
    patientName: form.patientName || null,
    gender: form.gender || 'M',
    birthDate: form.birthDate || null,
    address: form.address || null,
    escortId: form.escortId ? Number(form.escortId) : null,
    lines: lines.value
      .filter((line) => line.lineType !== 'BUNDLE')
      .map((line) => ({
        lineType: line.lineType,
        referenceId: line.referenceId,
        quantity: Number(line.quantity),
        unitPrice: line.lineType === 'MISC' ? Number(line.unitPrice) : null,
        discountType: normalizeDiscountType(line.discountType),
        discountValue: Number(line.discountValue || 0),
        description: line.lineType === 'MISC' ? line.description : null,
        doctorStaffId: line.lineType === 'TREATMENT' && line.doctorStaffId ? Number(line.doctorStaffId) : null,
        instruction: line.lineType === 'ITEM' ? line.instruction || null : null,
        contextUnitId: Number(form.unitId)
      }))
  };
}

async function saveNote() {
  if (!form.unitId) {
    errorMessage.value = 'Unit poliklinik wajib dipilih.';
    return;
  }
  if (!lines.value.length) {
    errorMessage.value = 'Minimal satu transaksi harus ditambahkan.';
    return;
  }

  saving.value = true;
  resetMessages();

  try {
    const payload = buildPayload();
    const result = isEditingExistingNote.value
      ? await request(`/polyclinic/notes/${encodeURIComponent(form.noteId)}`, {
          method: 'PUT',
          body: JSON.stringify(payload)
        })
      : await request('/polyclinic/notes', {
          method: 'POST',
          body: JSON.stringify(payload)
        });

    statusMessage.value = `Nota ${result.noteNumber} berhasil disimpan.`;
    await loadNoteDetail(result.noteId);
  } catch (error) {
    errorMessage.value = error.message;
  } finally {
    saving.value = false;
  }
}

async function searchNotes() {
  if (!form.unitId) {
    errorMessage.value = 'Pilih unit poliklinik terlebih dahulu.';
    return;
  }

  searchingNotes.value = true;
  resetMessages();

  try {
    const params = new URLSearchParams();
    if (noteSearch.noteNumber) {
      params.set('noteNumber', noteSearch.noteNumber);
    }
    if (noteSearch.patientName) {
      params.set('patientName', noteSearch.patientName);
    }
    noteResults.value = await request(`/polyclinic/units/${encodeURIComponent(form.unitId)}/notes?${params.toString()}`);
  } catch (error) {
    errorMessage.value = error.message;
  } finally {
    searchingNotes.value = false;
  }
}

async function loadNoteDetail(noteId) {
  try {
    const detail = await request(`/polyclinic/notes/${encodeURIComponent(noteId)}`);
    noteDetail.value = detail;
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
    form.escortId = detail.escortId ? String(detail.escortId) : '';
    form.cancelationNote = detail.cancelationNote || '';
    setSelectedDoctor(detail.doctorId ? {
      doctorId: detail.doctorId,
      doctorCode: detail.doctorCode,
      doctorName: detail.doctorName
    } : null);

    availableActions.canModify = detail.canModify;
    availableActions.canValidate = detail.canValidate;
    availableActions.canCancel = detail.canCancel;

    lines.value = (detail.lines || []).map((line) => ({
      clientId: `loaded-${line.lineType}-${line.lineId}`,
      lineType: line.lineType,
      referenceId: line.referenceId,
      code: line.code,
      description: line.description,
      quantity: line.quantity,
      unitName: line.unitName,
      unitPrice: line.unitPrice,
      discountType: line.discountType || 'RP',
      discountValue: line.discountValue || 0,
      doctorStaffId: line.doctorId || null,
      doctorName: line.doctorName || '',
      instruction: line.instruction || '',
      readOnly: line.lineType === 'BUNDLE'
    }));
  } catch (error) {
    errorMessage.value = error.message;
  }
}

function resetNoteSearch() {
  noteSearch.noteNumber = '';
  noteSearch.patientName = '';
  noteResults.value = [];
  noteDetail.value = null;
  errorMessage.value = '';
}

async function selectExistingNote(noteId) {
  searchMode.value = 'patient';
  await loadNoteDetail(noteId);
}

async function validateNote() {
  if (!form.noteId) {
    return;
  }

  saving.value = true;
  resetMessages();

  try {
    const result = await request(`/polyclinic/notes/${encodeURIComponent(form.noteId)}/validate`, {
      method: 'POST'
    });
    statusMessage.value = `Nota ${result.noteNumber} berhasil divalidasi.`;
    await loadNoteDetail(form.noteId);
  } catch (error) {
    errorMessage.value = error.message;
  } finally {
    saving.value = false;
  }
}

async function cancelNote() {
  if (!form.noteId) {
    return;
  }

  if (!form.cancelationNote) {
    errorMessage.value = 'Alasan pembatalan wajib diisi.';
    return;
  }

  saving.value = true;
  resetMessages();

  try {
    const result = await request(`/polyclinic/notes/${encodeURIComponent(form.noteId)}/cancel`, {
      method: 'POST',
      body: JSON.stringify({
        reason: form.cancelationNote
      })
    });
    statusMessage.value = `Nota ${result.noteNumber} berhasil dibatalkan.`;
    await loadNoteDetail(form.noteId);
  } catch (error) {
    errorMessage.value = error.message;
  } finally {
    saving.value = false;
  }
}

function startNewNote() {
  resetMessages();
  emptyForm(true);
  registeredPatients.value = [];
  noteResults.value = [];
}

function switchSearchMode(mode) {
  searchMode.value = mode;
  errorMessage.value = '';
}

function toggleSearch() {
  showSearch.value = !showSearch.value;
}

function resetSearch() {
  patientSearch.mrCode = '';
  patientSearch.patientName = '';
  patientSearch.address = '';
  registeredPatients.value = [];
  selectedPatient.value = null;
  showSearch.value = true;
  errorMessage.value = '';
}

async function selectPatient(mrCode) {
  try {
    const unitId = form.unitId;
    if (!unitId) {
      errorMessage.value = 'Pilih LOKASI TRANSAKSI terlebih dahulu.';
      return;
    }
    const res = await request(`/polyclinic/patients/${encodeURIComponent(mrCode)}?unitId=${unitId}`);
    selectedPatient.value = res;
    form.existingMrCode = mrCode;
    form.registrationCode = res.registrationCode || '';
    form.patientName = res.patientName;
    form.gender = res.gender || 'M';
    form.birthDate = res.birthDate || '';
    form.address = res.address || '';
    form.patientTypeId = res.patientTypeId ? String(res.patientTypeId) : '';
    form.doctorId = res.doctorId ? String(res.doctorId) : '';
    form.doctorLabel = res.doctorName || '';
    showSearch.value = false;
  } catch (e) {
    errorMessage.value = e.message;
  }
}

function formatDate(dateStr) {
  if (!dateStr) return '-';
  const d = new Date(dateStr);
  const day = String(d.getDate()).padStart(2, '0');
  const month = String(d.getMonth() + 1).padStart(2, '0');
  const year = d.getFullYear();
  return `${day}/${month}/${year}`;
}

function calculateAge(birthDateStr) {
  if (!birthDateStr) return '';
  const birthDate = new Date(birthDateStr);
  const today = new Date();
  let age = today.getFullYear() - birthDate.getFullYear();
  const monthDiff = today.getMonth() - birthDate.getMonth();
  if (monthDiff < 0 || (monthDiff === 0 && today.getDate() < birthDate.getDate())) {
    age--;
  }
  return age + ' Thn';
}

function patientTypeName(typeId) {
  if (!typeId) return '-';
  const found = masters.patientTypes.find(pt => String(pt.patientTypeId) === String(typeId));
  return found ? found.patientTypeName : '-';
}

function escortTypeName(escortId) {
  if (!escortId) return '-';
  const found = masters.escorts.find(e => String(e.escortId) === String(escortId));
  return found ? found.escortType : '-';
}

onMounted(async () => {
  await loadMasters();
  await searchDoctors();
});

watch(
  () => form.unitId,
  async (newValue, oldValue) => {
    if (!newValue || newValue === oldValue) {
      return;
    }
    setSelectedDoctor(null);
    await searchDoctors();
  }
);

watch(
  () => form.doctorId,
  (newValue) => {
    const selectedDoctor = doctorResults.value.find((doctor) => String(doctor.doctorId) === String(newValue));
    if (selectedDoctor) {
      form.doctorLabel = `${selectedDoctor.doctorCode} - ${selectedDoctor.doctorName}`;
    }
  }
);
</script>

<template>
  <div class="poli-page">
    <!-- Header -->
    <div class="page-header">
      <div>
        <h2>🏥 Transaksi Poliklinik</h2>
      </div>
    </div>

    <!-- Unit Selector -->
    <div class="unit-bar">
      <label class="unit-label">LOKASI TRANSAKSI</label>
      <select v-model="form.unitId" class="unit-select">
        <option value="">Pilih unit</option>
        <option v-for="u in masters.units" :key="u.unitId" :value="String(u.unitId)">
          {{ u.unitCode }}. {{ u.unitName }}
        </option>
      </select>
    </div>

    <!-- Notifications -->
    <p v-if="errorMessage" class="status-banner status-banner--error">{{ errorMessage }}</p>
    <p v-else-if="statusMessage" class="status-banner status-banner--success">{{ statusMessage }}</p>

    <div v-if="loading" class="loading">Memuat master poliklinik...</div>

    <template v-else>
      <!-- Mode Tabs -->
      <div class="mode-tabs">
        <button class="mode-tab" :class="{ 'mode-tab--active': searchMode === 'patient' }" @click="switchSearchMode('patient')">
          🏥 Cari / Input Pasien Baru
        </button>
        <button class="mode-tab" :class="{ 'mode-tab--active': searchMode === 'note' }" @click="switchSearchMode('note')">
          📋 Cari Nota Aktif
        </button>
      </div>

      <!-- ======================== PATIENT SEARCH MODE ======================== -->
      <div v-if="searchMode === 'patient'" class="card search-card">
        <h3>
          <span>Pencarian Pasien</span>
          <span class="search-actions">
            <button v-if="selectedPatient && !showSearch" class="small-button" type="button" @click="toggleSearch">Cari Pasien Lain</button>
            <button v-if="registeredPatients.length || selectedPatient" class="small-button" type="button" @click="resetSearch">Reset</button>
          </span>
        </h3>

        <!-- Search Form -->
        <div v-if="showSearch" class="search-form">
          <div class="form-row">
            <label>
              No. MR
              <input v-model="patientSearch.mrCode" placeholder="Ketik No. Medical Record" @keyup.enter="searchRegisteredPatients" />
            </label>
            <label>
              Nama Pasien
              <input v-model="patientSearch.patientName" placeholder="Nama lengkap pasien" @keyup.enter="searchRegisteredPatients" />
            </label>
          </div>
          <div class="form-row">
            <label class="wide">
              Alamat
              <input v-model="patientSearch.address" placeholder="Alamat pasien" @keyup.enter="searchRegisteredPatients" />
            </label>
          </div>
          <button class="primary-button" :disabled="searchingPatients" @click="searchRegisteredPatients">
            {{ searchingPatients ? 'Mencari...' : 'Cari Pasien' }}
          </button>
        </div>

        <!-- Search Results -->
        <div v-if="showSearch && registeredPatients.length" class="table-wrap">
          <table class="table">
            <thead>
              <tr>
                <th>No. MR</th>
                <th>Nama Pasien</th>
                <th>Alamat</th>
                <th>Aksi</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="patient in registeredPatients" :key="patient.medicalRecordId">
                <td><strong>{{ patient.medicalRecordCode }}</strong></td>
                <td>{{ patient.patientName }}</td>
                <td>{{ patient.address || '-' }}</td>
                <td>
                  <button class="link-button" @click="selectPatient(patient.medicalRecordCode)">Pilih</button>
                </td>
              </tr>
            </tbody>
          </table>
        </div>

        <!-- Selected Patient Detail -->
        <div v-if="selectedPatient" class="selected-patient">
          <h4>Detail Pasien Terpilih</h4>
          <div class="patient-detail-grid">
            <div class="detail-item">
              <span class="detail-label">No. MR</span>
              <span class="detail-value">{{ selectedPatient.medicalRecordCode }}</span>
            </div>
            <div class="detail-item">
              <span class="detail-label">No. Registrasi</span>
              <span class="detail-value">{{ selectedPatient.registrationCode || '-' }}</span>
            </div>
            <div class="detail-item">
              <span class="detail-label">Nama</span>
              <span class="detail-value">{{ selectedPatient.patientName }}</span>
            </div>
            <div class="detail-item">
              <span class="detail-label">Jenis Kelamin</span>
              <span class="detail-value">{{ selectedPatient.gender === 'M' ? 'Laki-Laki' : 'Perempuan' }}</span>
            </div>
            <div class="detail-item">
              <span class="detail-label">Tgl. Lahir / Umur</span>
              <span class="detail-value">{{ formatDate(selectedPatient.birthDate) }} / {{ calculateAge(selectedPatient.birthDate) }}</span>
            </div>
            <div class="detail-item">
              <span class="detail-label">Tipe Pasien</span>
              <span class="detail-value">{{ patientTypeName(selectedPatient.patientTypeId) }}</span>
            </div>
            <div class="detail-item">
              <span class="detail-label">Dokter Utama</span>
              <span class="detail-value">{{ selectedPatient.doctorName || '-' }}</span>
            </div>
            <div class="detail-item">
              <span class="detail-label">Alamat</span>
              <span class="detail-value">{{ selectedPatient.address || '-' }}</span>
            </div>
            <div class="detail-item">
              <span class="detail-label">Tipe Pembawa</span>
              <span class="detail-value">{{ escortTypeName(selectedPatient.escortId) }}</span>
            </div>
          </div>
        </div>

        <!-- Action Buttons -->
        <div v-if="selectedPatient" class="action-bar">
          <button class="action-button action-button--treatment" type="button" :class="{ 'is-active': drawerState.treatment }" @click="toggleDrawer('treatment')">
            <span class="action-icon">🪚</span>
            <span class="action-label">Tambah Tindakan</span>
          </button>
          <button class="action-button action-button--item" type="button" :class="{ 'is-active': drawerState.item }" @click="toggleDrawer('item')">
            <span class="action-icon">💊</span>
            <span class="action-label">Tambah O-BM</span>
          </button>
          <button class="action-button action-button--misc" type="button" :class="{ 'is-active': drawerState.misc }" @click="toggleDrawer('misc')">
            <span class="action-icon">📋</span>
            <span class="action-label">Biaya Lain-Lain</span>
          </button>
        </div>

        <!-- Treatment Panel -->
        <div v-if="drawerState.treatment && selectedPatient" class="obm-panel">
          <div class="obm-header">
            <h4>FORM TAMBAH TINDAKAN</h4>
            <button class="small-button" type="button" @click="toggleDrawer('treatment')">Tutup</button>
          </div>
          <div class="form-row">
            <label>KODE <input v-model="treatmentSearch.code" placeholder="Kode tindakan" @keyup.enter="searchTreatments" /></label>
            <label>NAMA <input v-model="treatmentSearch.name" placeholder="Nama tindakan" @keyup.enter="searchTreatments" /></label>
          </div>
          <button class="primary-button" :disabled="treatmentSearching" @click="searchTreatments" style="margin-bottom:8px">
            {{ treatmentSearching ? 'Mencari...' : 'Cari Tindakan' }}
          </button>
          <div v-if="treatmentResults.length" class="table-wrap">
            <table class="table">
              <thead><tr><th>KODE</th><th>NAMA</th><th>HARGA</th><th>JUMLAH</th><th></th></tr></thead>
              <tbody>
                <tr v-for="t in treatmentResults" :key="t.treatmentFeeId">
                  <td><strong>{{ t.treatmentCode }}</strong></td>
                  <td>{{ t.treatmentName }}</td>
                  <td>Rp {{ formatCurrency(t.price) }}</td>
                  <td><input class="qty-input" type="number" v-model.number="t.qty" min="1" /></td>
                  <td><button class="link-button" @click="addTreatment(t)">Tambah</button></td>
                </tr>
              </tbody>
            </table>
          </div>
        </div>

        <!-- O-BM Panel -->
        <div v-if="drawerState.item && selectedPatient" class="obm-panel">
          <div class="obm-header">
            <h4>FORM TAMBAH OBAT - BAHAN MEDIS</h4>
            <button class="small-button" type="button" @click="toggleDrawer('item')">Tutup</button>
          </div>
          <div class="form-row">
            <label>KODE <input v-model="itemSearch.code" placeholder="Kode item/obat" @keyup.enter="searchItems" /></label>
            <label>NAMA <input v-model="itemSearch.name" placeholder="Nama item/obat" @keyup.enter="searchItems" /></label>
          </div>
          <button class="primary-button" :disabled="itemSearching" @click="searchItems" style="margin-bottom:8px">
            {{ itemSearching ? 'Mencari...' : 'Cari O-BM' }}
          </button>
          <div v-if="itemResults.length" class="table-wrap">
            <table class="table">
              <thead><tr><th>KODE</th><th>NAMA</th><th>HARGA</th><th>STOK</th><th>JUMLAH</th><th></th></tr></thead>
              <tbody>
                <tr v-for="item in itemResults" :key="item.itemId">
                  <td><strong>{{ item.itemCode }}</strong></td>
                  <td>{{ item.itemName }}</td>
                  <td>Rp {{ formatCurrency(item.price) }}</td>
                  <td>{{ formatCurrency(item.stockQuantity) }}</td>
                  <td><input class="qty-input" type="number" v-model.number="item.qty" min="1" /></td>
                  <td><button class="link-button" @click="addItem(item)">Tambah</button></td>
                </tr>
              </tbody>
            </table>
          </div>
        </div>

        <!-- Misc Panel -->
        <div v-if="drawerState.misc && selectedPatient" class="obm-panel">
          <div class="obm-header">
            <h4>FORM TRANSAKSI LAIN-LAIN</h4>
            <button class="small-button" type="button" @click="toggleDrawer('misc')">Tutup</button>
          </div>
          <div class="form-row">
            <label class="wide">NAMA <input type="text" v-model="miscDraft.description" placeholder="Nama biaya" /></label>
          </div>
          <div class="form-row">
            <label>JUMLAH <input type="number" v-model.number="miscDraft.quantity" min="1" step="1" /></label>
            <label>HARGA SATUAN <input type="number" v-model.number="miscDraft.unitPrice" min="0" step="100" /></label>
          </div>
          <button class="primary-button" @click="addMisc" style="margin-top:4px">Simpan</button>
        </div>

        <!-- Cart Items -->
        <div v-if="lines.length" class="card" style="margin-top:12px">
          <h3>
            <span>Item Transaksi ({{ lines.length }})</span>
            <span class="cart-total">Total: Rp {{ formatCurrency(totalAmount) }}</span>
          </h3>
          <div class="table-wrap">
            <table class="table">
              <thead><tr><th>JENIS</th><th>KODE</th><th>KETERANGAN</th><th>QTY</th><th>SATUAN</th><th>HARGA</th><th>DISKON</th><th>SUBTOTAL</th><th></th></tr></thead>
              <tbody>
                <tr v-for="(line, idx) in lines" :key="line.clientId">
                  <td>{{ line.lineType }}</td>
                  <td><strong>{{ line.code }}</strong></td>
                  <td>
                    <div>{{ line.description }}</div>
                    <input v-if="line.lineType==='ITEM' && !line.readOnly" v-model="line.instruction" type="text" placeholder="Aturan pakai" />
                  </td>
                  <td><input class="qty-input" type="number" v-model.number="line.quantity" :disabled="line.readOnly" min="1" /></td>
                  <td>{{ line.unitName }}</td>
                  <td>Rp {{ formatCurrency(line.unitPrice) }}</td>
                  <td>
                    <div class="discount-stack">
                      <input type="number" v-model.number="line.discountValue" :disabled="line.readOnly" min="0" />
                      <select v-model="line.discountType" :disabled="line.readOnly">
                        <option value="RP">RP</option><option value="%">%</option>
                      </select>
                    </div>
                  </td>
                  <td>Rp {{ formatCurrency(calculateSubtotal(line)) }}</td>
                  <td>
                    <button v-if="!line.readOnly" class="link-button link-danger" @click="removeLine(idx)">Hapus</button>
                    <span v-else class="muted-text">readonly</span>
                  </td>
                </tr>
              </tbody>
            </table>
          </div>
        </div>

        <!-- Save / Validate / Cancel -->
        <div v-if="lines.length" class="save-actions" style="margin-top:12px">
          <button class="primary-button primary-button--lg" :disabled="saving || (isEditingExistingNote && !availableActions.canModify)" @click="saveNote">
            {{ saving ? 'Menyimpan...' : '💾 Simpan Transaksi' }}
          </button>
          <button class="secondary-button" :disabled="saving || !availableActions.canValidate" @click="validateNote">Validasi</button>
        </div>
      </div>

      <!-- ======================== NOTE SEARCH MODE ======================== -->
      <div v-if="searchMode === 'note'" class="card search-card">
        <h3>
          <span>🔍 Pencarian Nota Aktif</span>
          <span class="search-actions">
            <button v-if="noteDetail" class="small-button" type="button" @click="resetNoteSearch">Reset</button>
          </span>
        </h3>

        <div v-if="!noteDetail" class="search-form">
          <div class="form-row">
            <label>
              No. Nota
              <input v-model="noteSearch.noteNumber" placeholder="Ketik No. Nota" @keyup.enter="searchNotes" />
            </label>
            <label>
              Nama Pasien
              <input v-model="noteSearch.patientName" placeholder="Nama pasien" @keyup.enter="searchNotes" />
            </label>
          </div>
          <button class="primary-button" :disabled="searchingNotes" @click="searchNotes">
            {{ searchingNotes ? 'Mencari...' : 'Cari Nota' }}
          </button>
        </div>

        <!-- Note Search Results -->
        <div v-if="noteResults.length" class="table-wrap">
          <table class="table">
            <thead>
              <tr>
                <th>No. Nota</th>
                <th>Pasien</th>
                <th>Status</th>
                <th>Tgl. Buat</th>
                <th>Aksi</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="n in noteResults" :key="n.noteId">
                <td><strong>{{ n.noteNumber }}</strong></td>
                <td>{{ n.patientName }}</td>
                <td><span class="badge">{{ n.statusLabel }}</span></td>
                <td>{{ n.createdAt ? n.createdAt.slice(0, 19).replace('T', ' ') : '-' }}</td>
                <td>
                  <button class="link-button" :disabled="n.statusCode !== 1" @click="selectExistingNote(n.noteId)">
                    {{ n.statusCode === 1 ? 'Pilih & Validasi' : 'Tervalidasi' }}
                  </button>
                </td>
              </tr>
            </tbody>
          </table>
        </div>

        <!-- Selected Note Detail -->
        <div v-if="noteDetail" class="selected-patient">
          <h4>Detail Nota</h4>
          <div class="patient-detail-grid">
            <div class="detail-item">
              <span class="detail-label">No. Nota</span>
              <span class="detail-value">{{ noteDetail.noteNumber }}</span>
            </div>
            <div class="detail-item">
              <span class="detail-label">Status</span>
              <span class="detail-value">{{ noteDetail.statusLabel }}</span>
            </div>
            <div class="detail-item">
              <span class="detail-label">Pasien</span>
              <span class="detail-value">{{ noteDetail.patientName }}</span>
            </div>
            <div class="detail-item">
              <span class="detail-label">Total</span>
              <span class="detail-value">Rp {{ formatCurrency(noteDetail.totalAmount) }}</span>
            </div>
          </div>

          <!-- Note Lines -->
          <div v-if="noteDetail.lines && noteDetail.lines.length" class="table-wrap" style="margin-top: 12px;">
            <table class="table">
              <thead>
                <tr>
                  <th>KODE</th>
                  <th>NAMA</th>
                  <th>HARGA</th>
                  <th>QTY</th>
                  <th>SUBTOTAL</th>
                </tr>
              </thead>
              <tbody>
                <tr v-for="(line, idx) in noteDetail.lines" :key="idx">
                  <td><strong>{{ line.code }}</strong></td>
                  <td>{{ line.description }}</td>
                  <td>Rp {{ formatCurrency(line.unitPrice) }}</td>
                  <td>{{ line.quantity }}</td>
                  <td>Rp {{ formatCurrency(line.subtotal) }}</td>
                </tr>
              </tbody>
            </table>
          </div>
        </div>
      </div>
    </template>
  </div>
</template>

<style scoped>
.poli-page { padding: 16px; }
.page-header { margin-bottom: 16px; }
.page-header h2 { margin: 0; color: #304b73; font-size: 20px; }
.page-subtitle { margin: 4px 0 0; color: #6b7280; font-size: 14px; }

.unit-bar {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 16px;
  padding: 10px 16px;
  background: #fff;
  border: 1px solid #e2e8f0;
  border-radius: 10px;
}
.unit-label { font-size: 12px; font-weight: 700; color: #304b73; white-space: nowrap; }
.unit-select { min-width: 280px; padding: 6px 10px; border: 1px solid #d1d9e6; border-radius: 8px; font: inherit; font-size: 13px; background: #fff; }

.loading { padding: 24px; text-align: center; color: #9ca3af; }

.status-banner { padding: 12px 16px; border-radius: 10px; font-weight: 600; margin-bottom: 12px; }
.status-banner--success { background: #e6f5ea; color: #1d6b3a; }
.status-banner--error { background: #fde8ea; color: #a32943; }

.card { background: #fff; border: 1px solid #e2e8f0; border-radius: 12px; padding: 20px; margin-bottom: 16px; }
.card h3 { margin: 0 0 12px; font-size: 16px; color: #304b73; display: flex; align-items: center; justify-content: space-between; flex-wrap: wrap; gap: 8px; }
.card h4 { margin: 12px 0 8px; font-size: 14px; color: #304b73; }
.search-actions { display: flex; gap: 6px; }

.mode-tabs { display: flex; gap: 8px; margin-bottom: 16px; }
.mode-tab { flex: 1; padding: 12px 20px; border: 2px solid #d1d9e6; border-radius: 10px; background: #fff; font-weight: 700; font-size: 14px; color: #3d4b63; cursor: pointer; transition: all .2s; }
.mode-tab:hover { border-color: #5f83c2; color: #304b73; }
.mode-tab--active { background: #304b73; border-color: #304b73; color: #fff; }
.mode-tab--active:hover { background: #1f3352; }

.form-row { display: grid; grid-template-columns: 1fr 1fr; gap: 10px; margin-bottom: 10px; }
.form-row label { display: grid; gap: 4px; font-size: 13px; color: #3d4b63; }
.form-row .wide { grid-column: 1 / -1; }

input, select { padding: 8px 10px; border: 1px solid #d1d9e6; border-radius: 8px; font: inherit; background: #fff; }
input:disabled, select:disabled { background: #f7f7f9; color: #6b7280; }

.primary-button, .secondary-button, .danger-button { border: 0; cursor: pointer; padding: 8px 20px; font-weight: 700; border-radius: 8px; }
.primary-button { background: #304b73; color: #fff; }
.primary-button:disabled { opacity: 0.5; cursor: not-allowed; }
.secondary-button { background: #fff; border: 1px solid #d1d9e6; color: #3d4b63; }
.secondary-button:hover { background: #f6f8fb; }
.danger-button { background: #b84747; color: #fff; }
.small-button { padding: 6px 12px; font-size: 12px; background: #eef3fb; border: 1px solid #d1d9e6; border-radius: 6px; cursor: pointer; }

.action-bar { display: flex; gap: 12px; margin-top: 16px; flex-wrap: wrap; }
.action-button {
  display: flex; align-items: center; gap: 8px;
  padding: 12px 20px; border: 1px solid #d1d9e6; border-radius: 10px;
  background: #fff; font-weight: 700; font-size: 14px; cursor: pointer;
  transition: all 0.15s ease; min-width: 160px;
}
.action-button:hover { transform: translateY(-1px); box-shadow: 0 4px 12px rgba(48, 75, 115, 0.12); }
.action-button.is-active { background: #eef3fb; border-color: #304b73; }
.action-button--treatment { border-left: 4px solid #5f83c2; color: #304b73; }
.action-button--item { border-left: 4px solid #2d7d46; color: #2d7d46; }
.action-button--misc { border-left: 4px solid #b8860b; color: #b8860b; }
.action-icon { font-size: 18px; }
.action-label { white-space: nowrap; }

.obm-panel { margin-top: 16px; padding: 16px; background: #fafbfc; border: 1px solid #d1d9e6; border-radius: 10px; }
.obm-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 12px; }
.obm-header h4 { margin: 0; font-size: 14px; color: #304b73; }
.obm-panel .form-row { display: grid; grid-template-columns: 1fr 1fr; gap: 10px; margin-bottom: 10px; }
.obm-panel .form-row label { display: grid; gap: 4px; font-size: 13px; color: #3d4b63; }
.obm-panel .form-row label.wide { grid-column: 1 / -1; }

.qty-input { width: 60px; padding: 4px 6px; border: 1px solid #d1d9e6; border-radius: 6px; font: inherit; font-size: 12px; text-align: center; }

.table-wrap { overflow: auto; margin: 10px 0; }
.table { width: 100%; border-collapse: collapse; font-size: 13px; }
.table th, .table td { padding: 8px 10px; border-bottom: 1px solid #eef2f7; text-align: left; }
.table th { background: #f6f8fb; color: #304b73; white-space: nowrap; }

.badge { display: inline-block; padding: 2px 8px; border-radius: 12px; font-size: 11px; font-weight: 700; }
.link-button { background: transparent; border: 0; color: #2d5aa3; font-weight: 700; padding: 0; cursor: pointer; }
.link-danger { color: #a32943; }

.selected-patient { margin-top: 16px; padding: 16px; background: #f8faff; border: 1px solid #d1d9e6; border-radius: 10px; }
.selected-patient h4 { margin: 0 0 10px; font-size: 14px; color: #304b73; }
.patient-detail-grid { display: grid; grid-template-columns: 1fr 1fr 1fr; gap: 10px; }
.detail-item { display: flex; flex-direction: column; gap: 2px; }
.detail-label { font-size: 11px; color: #6b7280; text-transform: uppercase; letter-spacing: 0.05em; }
.detail-value { font-size: 14px; font-weight: 600; color: #2b2b2b; }

.form-grid { display: grid; grid-template-columns: repeat(2, 1fr); gap: 10px; }
.discount-stack { display: grid; grid-template-columns: 1fr 80px; gap: 6px; }
.empty-state, .muted-text { color: #9ca3af; }
.cart-total { font-size: 14px; color: #2d7d46; font-weight: 700; }
.save-actions { display: flex; gap: 12px; align-items: center; flex-wrap: wrap; }
.primary-button--lg { padding: 12px 28px; font-size: 15px; }
</style>
