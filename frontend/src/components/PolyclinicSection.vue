<script setup>
import { computed, onMounted, reactive, ref, watch } from 'vue';

const props = defineProps({
  apiBaseUrl: {
    type: String,
    required: true
  }
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
  resetMessages();

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
  <section class="poli-page">
    <header class="section-header">
      <div>
        <p class="section-kicker">Poliklinik</p>
        <h2>Transaksi Poliklinik</h2>
        <p class="section-copy">
          Migrasi form legacy SC0091 ke Vue dengan penyimpanan langsung ke tabel existing `tb_examination`, `tb_treatment_trx`,
          `tb_item_trx`, dan `tb_misc_trx`.
        </p>
      </div>
      <div class="header-actions">
        <button class="secondary-button" type="button" @click="startNewNote">Nota Baru</button>
      </div>
    </header>

    <p v-if="errorMessage" class="status-banner status-banner--error">{{ errorMessage }}</p>
    <p v-else-if="statusMessage" class="status-banner status-banner--success">{{ statusMessage }}</p>

    <div v-if="loading" class="panel-card">Memuat master poliklinik...</div>

    <template v-else>
      <div class="layout-grid">
        <section class="panel-card">
          <button
            class="section-toggle-button"
            :class="{ 'section-toggle-button--active': drawerState.header }"
            type="button"
            @click="toggleDrawer('header')"
          >
            Header Transaksi
          </button>

          <template v-if="drawerState.header">
            <div class="form-grid">
              <label>
                <span>Unit Poliklinik</span>
                <select v-model="form.unitId">
                  <option value="">Pilih unit</option>
                  <option v-for="unit in masters.units" :key="unit.unitId" :value="String(unit.unitId)">
                    {{ unit.unitCode }} - {{ unit.unitName }}
                  </option>
                </select>
              </label>

              <label class="checkbox-field">
                <input v-model="form.referencePatient" type="checkbox" />
                <span>{{ patientModeLabel }}</span>
              </label>

              <label>
                <span>No. Nota</span>
                <input :value="form.noteNumber" type="text" readonly />
              </label>

              <label>
                <span>Status Nota</span>
                <input :value="form.noteStatusLabel" type="text" readonly />
              </label>

              <label v-if="!form.referencePatient">
                <span>No. MR</span>
                <input :value="form.existingMrCode" type="text" readonly />
              </label>

              <label v-if="!form.referencePatient">
                <span>No. Registrasi</span>
                <input :value="form.registrationCode" type="text" readonly />
              </label>

              <label>
                <span>Nama Pasien</span>
                <input v-model="form.patientName" type="text" :readonly="!form.referencePatient && !isEditingExistingNote" />
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
                <input v-model="form.birthDate" type="date" :readonly="!form.referencePatient && !isEditingExistingNote" />
              </label>

              <label>
                <span>Tipe Pasien</span>
                <select v-model="form.patientTypeId">
                  <option value="">Pilih tipe</option>
                  <option v-for="type in masters.patientTypes" :key="type.patientTypeId" :value="String(type.patientTypeId)">
                    {{ type.patientTypeName }}
                  </option>
                </select>
              </label>

              <label>
                <span>Tipe Pembawa</span>
                <select v-model="form.escortId">
                  <option value="">-</option>
                  <option v-for="escort in masters.escorts" :key="escort.escortId" :value="String(escort.escortId)">
                    {{ escort.escortType }}
                  </option>
                </select>
              </label>

              <label>
                <span>Dokter Utama</span>
                <select v-model="form.doctorId">
                  <option value="">-</option>
                  <option v-for="doctor in doctorResults" :key="doctor.doctorId" :value="String(doctor.doctorId)">
                    {{ doctor.doctorCode }} - {{ doctor.doctorName }}
                  </option>
                </select>
              </label>

              <label class="form-grid__wide">
                <span>Alamat</span>
                <textarea v-model="form.address" rows="3" :readonly="!form.referencePatient && !isEditingExistingNote" />
              </label>
            </div>
          </template>
        </section>

        <section class="panel-card" v-if="!form.referencePatient">
          <button
            class="section-toggle-button"
            :class="{ 'section-toggle-button--active': drawerState.patientSearch }"
            type="button"
            @click="toggleDrawer('patientSearch')"
          >
            Pencarian Pasien Terdaftar
          </button>

          <template v-if="drawerState.patientSearch">
            <div class="search-grid">
              <label>
                <span>No. MR</span>
                <input v-model="patientSearch.mrCode" type="text" />
              </label>
              <label>
                <span>Nama</span>
                <input v-model="patientSearch.patientName" type="text" />
              </label>
              <label>
                <span>Alamat</span>
                <input v-model="patientSearch.address" type="text" />
              </label>
            </div>

            <div class="button-row">
              <button class="primary-button" type="button" :disabled="searchingPatients" @click="searchRegisteredPatients">
                {{ searchingPatients ? 'Mencari...' : 'Cari Pasien' }}
              </button>
            </div>

            <div class="table-wrap">
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
                  <tr v-for="patient in registeredPatients" :key="patient.medicalRecordId">
                    <td>{{ patient.medicalRecordCode }}</td>
                    <td>{{ patient.patientName }}</td>
                    <td>{{ patient.address }}</td>
                    <td><button class="link-button" type="button" @click="pickRegisteredPatient(patient.medicalRecordCode)">Pilih</button></td>
                  </tr>
                  <tr v-if="!registeredPatients.length">
                    <td colspan="4" class="empty-state">Belum ada hasil pencarian pasien.</td>
                  </tr>
                </tbody>
              </table>
            </div>
          </template>
        </section>
      </div>

      <section class="panel-card">
        <div class="entry-card-stack">
          <div class="button-row">
            <button
              class="section-toggle-button"
              :class="{ 'section-toggle-button--active': drawerState.treatment }"
              type="button"
              @click="toggleDrawer('treatment')"
            >
              Tambah Tindakan
            </button>
            <button
              class="section-toggle-button"
              :class="{ 'section-toggle-button--active': drawerState.item }"
              type="button"
              @click="toggleDrawer('item')"
            >
              Tambah O-BM
            </button>
            <button
              class="section-toggle-button"
              :class="{ 'section-toggle-button--active': drawerState.misc }"
              type="button"
              @click="toggleDrawer('misc')"
            >
              Tambah Biaya Lain-Lain
            </button>
          </div>

          <template v-if="drawerState.treatment">
            <div class="search-grid">
              <label>
                <span>Kode</span>
                <input v-model="treatmentSearch.code" type="text" />
              </label>
              <label>
                <span>Nama</span>
                <input v-model="treatmentSearch.name" type="text" />
              </label>
            </div>
            <div class="button-row">
              <button class="secondary-button" type="button" :disabled="treatmentSearching" @click="searchTreatments">
                {{ treatmentSearching ? 'Mencari...' : 'Cari Tindakan' }}
              </button>
            </div>
            <div class="table-wrap compact-table">
              <table class="result-table">
                <thead>
                  <tr>
                    <th>Kode</th>
                    <th>Nama</th>
                    <th>Harga</th>
                    <th />
                  </tr>
                </thead>
                <tbody>
                  <tr v-for="treatment in treatmentResults" :key="treatment.treatmentFeeId">
                    <td>{{ treatment.treatmentCode }}</td>
                    <td>{{ treatment.treatmentName }}</td>
                    <td>{{ formatCurrency(treatment.price) }}</td>
                    <td><button class="link-button" type="button" @click="addTreatment(treatment)">Tambah</button></td>
                  </tr>
                  <tr v-if="!treatmentResults.length">
                    <td colspan="4" class="empty-state">Hasil tindakan akan muncul di sini.</td>
                  </tr>
                </tbody>
              </table>
            </div>
          </template>

          <template v-if="drawerState.item">
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
            <div class="table-wrap compact-table">
              <table class="result-table">
                <thead>
                  <tr>
                    <th>Kode</th>
                    <th>Nama</th>
                    <th>Stok</th>
                    <th>Harga</th>
                    <th />
                  </tr>
                </thead>
                <tbody>
                  <tr v-for="item in itemResults" :key="item.itemId">
                    <td>{{ item.itemCode }}</td>
                    <td>{{ item.itemName }}</td>
                    <td>{{ formatCurrency(item.stockQuantity) }}</td>
                    <td>{{ formatCurrency(item.price) }}</td>
                    <td><button class="link-button" type="button" @click="addItem(item)">Tambah</button></td>
                  </tr>
                  <tr v-if="!itemResults.length">
                    <td colspan="5" class="empty-state">Hasil item akan muncul di sini.</td>
                  </tr>
                </tbody>
              </table>
            </div>
          </template>

          <template v-if="drawerState.misc">
            <div class="search-grid">
              <label>
                <span>Nama Biaya</span>
                <input v-model="miscDraft.description" type="text" />
              </label>
              <label>
                <span>Jumlah</span>
                <input v-model.number="miscDraft.quantity" type="number" min="1" />
              </label>
              <label>
                <span>Harga</span>
                <input v-model.number="miscDraft.unitPrice" type="number" min="0" />
              </label>
            </div>
            <div class="button-row">
              <button class="secondary-button" type="button" @click="addMisc">Tambah Biaya</button>
            </div>
          </template>
        </div>
      </section>

      <section class="panel-card">
        <div class="table-heading">
          <h3>Detail Transaksi Poliklinik</h3>
          <strong>Total: {{ formatCurrency(totalAmount) }}</strong>
        </div>

        <div class="table-wrap">
          <table class="result-table">
            <thead>
              <tr>
                <th>Jenis</th>
                <th>Kode</th>
                <th>Keterangan</th>
                <th>Jumlah</th>
                <th>Satuan</th>
                <th>Harga</th>
                <th>Diskon</th>
                <th>Subtotal</th>
                <th />
              </tr>
            </thead>
            <tbody>
              <tr v-for="(line, index) in lines" :key="line.clientId">
                <td>{{ line.lineType }}</td>
                <td>{{ line.code }}</td>
                <td>
                  <div>{{ line.description }}</div>
                  <input
                    v-if="line.lineType === 'ITEM' && !line.readOnly"
                    v-model="line.instruction"
                    type="text"
                    placeholder="Aturan pakai"
                  />
                </td>
                <td>
                  <input v-model.number="line.quantity" :disabled="line.readOnly" type="number" min="1" />
                </td>
                <td>{{ line.unitName }}</td>
                <td>{{ formatCurrency(line.unitPrice) }}</td>
                <td>
                  <div class="discount-stack">
                    <input v-model.number="line.discountValue" :disabled="line.readOnly" type="number" min="0" />
                    <select v-model="line.discountType" :disabled="line.readOnly">
                      <option value="RP">RP</option>
                      <option value="%">%</option>
                    </select>
                  </div>
                </td>
                <td>{{ formatCurrency(calculateSubtotal(line)) }}</td>
                <td>
                  <button v-if="!line.readOnly" class="link-button" type="button" @click="removeLine(index)">Hapus</button>
                  <span v-else class="muted-text">readonly</span>
                </td>
              </tr>
              <tr v-if="!lines.length">
                <td colspan="9" class="empty-state">Belum ada item transaksi poliklinik.</td>
              </tr>
            </tbody>
          </table>
        </div>

        <div class="button-row">
          <button
            class="primary-button"
            type="button"
            :disabled="saving || (isEditingExistingNote && !availableActions.canModify)"
            @click="saveNote"
          >
            {{ saving ? 'Menyimpan...' : isEditingExistingNote ? 'Simpan Perubahan' : 'Simpan Nota' }}
          </button>
          <button
            class="secondary-button"
            type="button"
            :disabled="saving || !availableActions.canValidate"
            @click="validateNote"
          >
            Validasi
          </button>
        </div>

        <label class="cancel-field">
          <span>Alasan Pembatalan</span>
          <textarea v-model="form.cancelationNote" rows="2" placeholder="Isi alasan pembatalan nota bila diperlukan." />
        </label>

        <div class="button-row">
          <button
            class="danger-button"
            type="button"
            :disabled="saving || !availableActions.canCancel"
            @click="cancelNote"
          >
            Batalkan Nota
          </button>
        </div>
      </section>

      <section class="panel-card">
        <h3>Riwayat Nota Poliklinik</h3>
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
          <button class="secondary-button" type="button" :disabled="searchingNotes" @click="searchNotes">
            {{ searchingNotes ? 'Mencari...' : 'Cari Nota' }}
          </button>
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
                <td>{{ note.createdAt ? note.createdAt.slice(0, 19).replace('T', ' ') : '-' }}</td>
                <td><button class="link-button" type="button" @click="loadNoteDetail(note.noteId)">Buka</button></td>
              </tr>
              <tr v-if="!noteResults.length">
                <td colspan="5" class="empty-state">Belum ada hasil pencarian nota.</td>
              </tr>
            </tbody>
          </table>
        </div>
      </section>
    </template>
  </section>
</template>

<style scoped>
.poli-page {
  display: grid;
  gap: 20px;
}

.section-header h2,
.panel-card h3 {
  margin: 0;
}

.section-kicker {
  margin: 0 0 8px;
  text-transform: uppercase;
  letter-spacing: 0.08em;
  font-size: 12px;
}

.section-copy {
  margin-bottom: 0;
}

.header-actions,
.button-row {
  display: flex;
  gap: 12px;
  flex-wrap: wrap;
}

.layout-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(320px, 1fr));
  gap: 20px;
}

.panel-card {
  background: rgba(255, 255, 255, 0.96);
  border: 1px solid rgba(150, 136, 117, 0.35);
  box-shadow: 0 12px 24px rgba(53, 64, 84, 0.08);
  padding: 24px;
}

.entry-card-stack {
  display: grid;
  gap: 16px;
}

.form-grid,
.search-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(180px, 1fr));
  gap: 16px;
}

.form-grid__wide {
  grid-column: 1 / -1;
}

label {
  display: grid;
  gap: 8px;
  font-weight: 600;
  color: #31415f;
}

input,
select,
textarea,
button {
  font: inherit;
}

input,
select,
textarea {
  min-height: 40px;
  border: 1px solid #c9d3e3;
  background: #fff;
  padding: 8px 10px;
}

textarea {
  min-height: 84px;
  resize: vertical;
}

.checkbox-field {
  display: flex;
  align-items: center;
  gap: 10px;
  padding-top: 28px;
}

.checkbox-field input {
  min-height: auto;
}

.table-wrap {
  overflow-x: auto;
}

.result-table {
  width: 100%;
  border-collapse: collapse;
}

.result-table th,
.result-table td {
  border-bottom: 1px solid #e2e8f0;
  padding: 10px;
  text-align: left;
  vertical-align: top;
}

.result-table th {
  color: #41526e;
  font-size: 13px;
}

.primary-button,
.secondary-button,
.danger-button,
.link-button {
  border: 0;
  cursor: pointer;
}

.primary-button,
.secondary-button,
.danger-button {
  min-height: 40px;
  padding: 0 18px;
  font-weight: 700;
}

.primary-button {
  background: #304b73;
  color: #fff;
}

.secondary-button {
  background: #dfe7f4;
  color: #304b73;
}

.section-toggle-button {
  min-height: 40px;
  padding: 0 18px;
  border: 0;
  background: #dfe7f4;
  color: #304b73;
  font-weight: 700;
  cursor: pointer;
}

.section-toggle-button--active {
  background: #304b73;
  color: #fff;
}

.danger-button {
  background: #b84747;
  color: #fff;
}

.link-button {
  background: transparent;
  color: #2a5cbf;
  font-weight: 700;
  padding: 0;
}

.status-banner {
  margin: 0;
  padding: 12px 16px;
  font-weight: 600;
}

.status-banner--success {
  background: #e3f7ea;
  color: #215b35;
}

.status-banner--error {
  background: #fee9e9;
  color: #8b2525;
}

.discount-stack {
  display: grid;
  grid-template-columns: 1fr 86px;
  gap: 8px;
}

.table-heading {
  display: flex;
  justify-content: space-between;
  gap: 16px;
  align-items: center;
  margin-bottom: 16px;
}

.empty-state,
.muted-text {
  color: #6b7280;
}

.cancel-field {
  margin-top: 18px;
}

@media (max-width: 1100px) {
  .layout-grid,
  .form-grid,
  .search-grid {
    grid-template-columns: 1fr;
  }
}
</style>
