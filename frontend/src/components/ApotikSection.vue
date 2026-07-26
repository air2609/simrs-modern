<script setup>
import { onMounted, reactive, ref } from 'vue';

const props = defineProps({
  apiBaseUrl: { type: String, required: true },
  availableUnits: { type: Array, default: () => [] }
});

const emit = defineEmits(['session-expired']);

const loading = ref(true);
const searchingPatients = ref(false);
const message = ref('');
const error = ref('');

const masters = reactive({ units: [], patientTypes: [], pajakObatRajal: 0.03 });
const selectedUnitId = ref('');

const patientSearch = reactive({ mrCode: '', patientName: '', address: '' });
const patientResults = ref([]);
const selectedPatient = ref(null);
const showSearch = ref(true);

const showObmPanel = ref(false);
const showRacikanPanel = ref(false);
const showMiscPanel = ref(false);

const itemSearch = reactive({ code: '', name: '' });
const itemResults = ref([]);
const searchingItems = ref(false);
const itemQuantities = reactive({});

const racikanForm = reactive({
  step: 1,
  quantity: 1,
  unitName: 'BUNGKUS',
  instruction: '',
  discountType: 'RP',
  discountValue: 0
});

const racikanComponents = ref([]);
const racikanComponentQty = reactive({});
const racikanSearchResults = ref([]);

const miscForm = reactive({
  name: '',
  quantity: 1,
  price: 0
});

const cartItems = ref([]);
const savedNote = ref(null);
const savingNote = ref(false);
const validatingNote = ref(false);

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

async function loadMasters() {
  try {
    const res = await request('/apotik/masters');
    masters.units = res.data.units || [];
    masters.patientTypes = res.data.patientTypes || [];
    masters.pajakObatRajal = res.data.pajakObatRajal ?? 0.03;
    if (masters.units.length > 0 && !selectedUnitId.value) {
      selectedUnitId.value = String(masters.units[0].unitId);
    }
  } catch (e) {
    error.value = e.message;
  }
}

async function searchPatients() {
  const params = new URLSearchParams();
  if (patientSearch.mrCode) params.set('mrCode', patientSearch.mrCode);
  if (patientSearch.patientName) params.set('patientName', patientSearch.patientName);
  if (patientSearch.address) params.set('address', patientSearch.address);

  if (!params.toString()) {
    error.value = 'Isi minimal satu kriteria pencarian (No. MR, Nama, atau Alamat).';
    return;
  }

  searchingPatients.value = true;
  message.value = '';
  error.value = '';
  selectedPatient.value = null;

  try {
    const res = await request(`/apotik/patients/registered?${params}`);
    patientResults.value = res.data || [];
    if (patientResults.value.length === 0) {
      message.value = 'Tidak ditemukan pasien dengan kriteria tersebut.';
    }
  } catch (e) {
    error.value = e.message;
  } finally {
    searchingPatients.value = false;
  }
}

function toggleObmPanel() {
  showObmPanel.value = !showObmPanel.value;
  if (showObmPanel.value) {
    itemSearch.code = '';
    itemSearch.name = '';
    itemResults.value = [];
    Object.keys(itemQuantities).forEach(k => delete itemQuantities[k]);
  }
}

async function searchItems() {
  if (!selectedUnitId.value) {
    error.value = 'Pilih lokasi transaksi terlebih dahulu.';
    return;
  }

  searchingItems.value = true;
  error.value = '';

  try {
    const params = new URLSearchParams();
    if (itemSearch.code) params.set('code', itemSearch.code);
    if (itemSearch.name) params.set('name', itemSearch.name);

    const res = await request(`/apotik/units/${selectedUnitId.value}/items?${params}`);
    // In racikan mode, save current selections before replacing results
    if (showRacikanPanel.value && racikanForm.step === 1) {
      collectRacikanComponents();
    }
    racikanSearchResults.value = res.data || [];
    itemResults.value = res.data || [];
    if (itemResults.value.length === 0) {
      error.value = 'Tidak ditemukan item dengan kriteria tersebut.';
    }
  } catch (e) {
    error.value = e.message;
  } finally {
    searchingItems.value = false;
  }
}

function formatCurrency(value) {
  const num = Number(value || 0);
  return Math.ceil(num).toLocaleString('id-ID');
}

function roundUp(value) {
  return Math.ceil(Number(value || 0));
}

function saveObmItems() {
  const itemsToAdd = [];
  let hasError = false;
  let errorMsg = '';

  itemResults.value.forEach((item) => {
    const qty = itemQuantities[item.itemId];
    if (qty !== undefined && qty !== null) {
      if (qty <= 0) {
        errorMsg = 'Jumlah harus lebih dari 0.';
        hasError = true;
        return;
      }
      if (qty > item.stockQuantity) {
        errorMsg = `Jumlah melebihi stok tersedia (${item.stockQuantity}).`;
        hasError = true;
        return;
      }
      const ppnRate = masters.pajakObatRajal || 0.03;
      const isRajal = selectedPatient.value && !selectedPatient.value.inpatient;
      const ppnAmount = isRajal ? item.price * ppnRate : 0;
      const priceWithPpn = item.price + ppnAmount;

      itemsToAdd.push({
        lineType: 'ITEM',
        referenceId: item.itemId,
        itemCode: item.itemCode,
        itemName: item.itemName,
        unitName: item.unitName,
        quantity: qty,
        unitPrice: item.price,
        unitPriceWithPpn: priceWithPpn,
        ppnAmount: ppnAmount * qty,
        description: item.itemName,
        discountType: 'RP',
        discountValue: 0
      });
    }
  });

  if (itemsToAdd.length === 0 && !hasError) {
    errorMsg = 'Tidak ada item dengan jumlah yang diisi.';
    hasError = true;
  }

  if (hasError) {
    error.value = errorMsg;
    return;
  }

  // Merge with existing cart (replace if same item already exists)
  const existingIds = new Set(cartItems.value.map(i => i.referenceId));
  itemsToAdd.forEach(item => {
    const idx = cartItems.value.findIndex(i => i.referenceId === item.referenceId);
    if (idx >= 0) {
      cartItems.value[idx] = item;
    } else {
      cartItems.value.push(item);
    }
  });

  error.value = '';
  message.value = `${itemsToAdd.length} item berhasil ditambahkan.`;
  showObmPanel.value = false;
  itemResults.value = [];
  Object.keys(itemQuantities).forEach(k => delete itemQuantities[k]);
}

function toggleRacikanPanel() {
  showRacikanPanel.value = !showRacikanPanel.value;
  if (showRacikanPanel.value) {
    showObmPanel.value = false;
    showMiscPanel.value = false;
    racikanForm.step = 1;
    racikanForm.quantity = 1;
    racikanForm.unitName = 'BUNGKUS';
    racikanForm.instruction = '';
    racikanForm.discountValue = 0;
    racikanComponents.value = [];
    racikanSearchResults.value = [];
    itemSearch.code = '';
    itemSearch.name = '';
    itemResults.value = [];
    Object.keys(racikanComponentQty).forEach(k => delete racikanComponentQty[k]);
  }
}

function collectRacikanComponents() {
  // Collect qty from current search results and add/update to accumulated components
  racikanSearchResults.value.forEach((item) => {
    const qty = racikanComponentQty[item.itemId];
    if (qty !== undefined && qty !== null && qty > 0) {
      const existing = racikanComponents.value.find(c => c.itemId === item.itemId);
      if (existing) {
        existing.quantity = qty;
        existing.price = item.price;
      } else {
        racikanComponents.value.push({
          itemId: item.itemId,
          itemCode: item.itemCode,
          itemName: item.itemName,
          unitName: item.unitName,
          quantity: qty,
          price: item.price
        });
      }
    }
  });
}

function proceedToRacikanStep2() {
  // Collect latest from current search results
  collectRacikanComponents();

  // Validate accumulated components
  let hasError = false;
  let errorMsg = '';

  for (const c of racikanComponents.value) {
    if (c.quantity <= 0) {
      errorMsg = `Jumlah ${c.itemName} harus lebih dari 0.`;
      hasError = true;
      break;
    }
  }

  if (racikanComponents.value.length === 0) {
    errorMsg = 'Pilih minimal 1 item dengan jumlah untuk racikan.';
    hasError = true;
  }

  if (hasError) {
    error.value = errorMsg;
    return;
  }

  racikanForm.step = 2;
  error.value = '';
}

function saveRacikanItem() {
  if (!racikanForm.quantity || racikanForm.quantity <= 0) {
    error.value = 'Jumlah racikan harus diisi dan lebih dari 0.';
    return;
  }

  const compNames = racikanComponents.value.map(c => c.itemName).join(', ');
  const compoundCode = 'RCK-' + new Date().getTime().toString().slice(-6);

  const ppnRate = masters.pajakObatRajal || 0.03;
  const isRajal = selectedPatient.value && !selectedPatient.value.inpatient;
  const totalComponentPrice = racikanComponents.value.reduce((t, c) => t + (c.quantity * c.price), 0);
  const ppnAmount = isRajal ? totalComponentPrice * ppnRate : 0;
  const priceWithPpn = totalComponentPrice + ppnAmount;

  cartItems.value.push({
    lineType: 'RACIKAN',
    referenceId: null,
    itemCode: compoundCode,
    itemName: `RACIKAN: ${racikanForm.unitName}`,
    unitName: racikanForm.unitName,
    quantity: racikanForm.quantity,
    unitPrice: totalComponentPrice,
    unitPriceWithPpn: priceWithPpn,
    ppnAmount: ppnAmount * racikanForm.quantity,
    description: `RACIKAN ${racikanForm.unitName} (${compNames})`,
    discountType: racikanForm.discountType,
    discountValue: racikanForm.discountValue,
    instruction: racikanForm.instruction,
    components: racikanComponents.value.map(c => ({
      referenceId: c.itemId,
      name: c.itemName,
      quantity: c.quantity
    }))
  });

  message.value = 'Racikan berhasil ditambahkan.';
  error.value = '';
  showRacikanPanel.value = false;
}

function toggleMiscPanel() {
  showMiscPanel.value = !showMiscPanel.value;
  if (showMiscPanel.value) {
    showObmPanel.value = false;
    showRacikanPanel.value = false;
    miscForm.name = '';
    miscForm.quantity = 1;
    miscForm.price = 0;
  }
}

function saveMiscItem() {
  if (!miscForm.name || miscForm.name.trim() === '') {
    error.value = 'Nama biaya harus diisi.';
    return;
  }
  if (!miscForm.quantity || miscForm.quantity <= 0) {
    error.value = 'Jumlah harus diisi dan lebih dari 0.';
    return;
  }
  if (!miscForm.price || miscForm.price <= 0) {
    error.value = 'Harga satuan harus diisi dan lebih dari 0.';
    return;
  }

  cartItems.value.push({
    lineType: 'MISC',
    referenceId: null,
    itemCode: 'MISC',
    itemName: miscForm.name,
    unitName: '-',
    quantity: miscForm.quantity,
    unitPrice: miscForm.price,
    unitPriceWithPpn: miscForm.price,
    ppnAmount: 0,
    description: miscForm.name,
    discountType: 'RP',
    discountValue: 0,
    instruction: ''
  });

  message.value = 'Biaya lain-lain berhasil ditambahkan.';
  error.value = '';
  showMiscPanel.value = false;
}

async function saveNote() {
  if (cartItems.value.length === 0) {
    error.value = 'Tidak ada item untuk disimpan.';
    return;
  }

  savingNote.value = true;
  error.value = '';
  message.value = '';

  try {
    const lines = cartItems.value.map(item => ({
      lineType: item.lineType,
      referenceId: item.referenceId,
      quantity: item.quantity,
      unitPrice: item.unitPrice,
      discountType: item.discountType || 'RP',
      discountValue: item.discountValue || 0,
      description: item.description,
      unitName: item.unitName,
      instruction: item.instruction || ''
    }));

    const totalWithPpn = cartItems.value.reduce((t, i) => t + roundUp(i.quantity * i.unitPriceWithPpn), 0);

    const reqBody = {
      unitId: parseInt(selectedUnitId.value),
      referencePatient: true,
      existingMrCode: selectedPatient.value.mrCode,
      patientTypeId: selectedPatient.value.patientTypeId,
      patientName: selectedPatient.value.patientName,
      gender: selectedPatient.value.gender,
      birthDate: selectedPatient.value.birthDate,
      address: selectedPatient.value.address,
      lines: lines
    };

    const res = await request('/apotik/notes', {
      method: 'POST',
      body: JSON.stringify(reqBody)
    });

    savedNote.value = res.data;
    message.value = `Transaksi berhasil disimpan. No. Nota: ${res.data.noteNumber}`;
    cartItems.value = [];
  } catch (e) {
    error.value = e.message;
  } finally {
    savingNote.value = false;
  }
}

async function validateNote() {
  if (!savedNote.value) return;

  validatingNote.value = true;
  error.value = '';

  try {
    const res = await request(`/apotik/notes/${savedNote.value.noteId}/validate`, {
      method: 'POST'
    });

    if (res.data.success) {
      message.value = `Nota ${savedNote.value.noteNumber} berhasil divalidasi.`;
    } else {
      message.value = res.data.message || 'Validasi berhasil.';
    }
  } catch (e) {
    error.value = e.message;
  } finally {
    validatingNote.value = false;
  }
}

function newTransaction() {
  savedNote.value = null;
  cartItems.value = [];
  message.value = '';
  error.value = '';
}

function resetAll() {
  cartItems.value = [];
  savedNote.value = null;
  patientResults.value = [];
  selectedPatient.value = null;
  message.value = '';
  error.value = '';
  showObmPanel.value = false;
  showRacikanPanel.value = false;
  showMiscPanel.value = false;
}

function removeRacikanComponent(itemId) {
  const idx = racikanComponents.value.findIndex(c => c.itemId === itemId);
  if (idx >= 0) {
    delete racikanComponentQty[itemId];
    racikanComponents.value.splice(idx, 1);
  }
}

function removeCartItem(index) {
  cartItems.value.splice(index, 1);
}

async function selectPatient(mrCode) {
  try {
    const res = await request(`/apotik/patients/${encodeURIComponent(mrCode)}`);
    selectedPatient.value = res.data;
    patientResults.value = [];
    message.value = '';
    error.value = '';
    showSearch.value = false;
  } catch (e) {
    error.value = e.message;
  }
}

function resetSearch() {
  patientSearch.mrCode = '';
  patientSearch.patientName = '';
  patientSearch.address = '';
  patientResults.value = [];
  selectedPatient.value = null;
  message.value = '';
  error.value = '';
  showSearch.value = true;
}

function toggleSearch() {
  showSearch.value = !showSearch.value;
}

function formatDate(dateStr) {
  if (!dateStr) return '-';
  const parts = dateStr.split('-');
  if (parts.length === 3) {
    return `${parts[2]}-${parts[1]}-${parts[0]}`;
  }
  return dateStr;
}

function calculateAge(dateStr) {
  if (!dateStr) return '-';
  const parts = dateStr.split('-');
  if (parts.length !== 3) return '-';
  const birth = new Date(parseInt(parts[0]), parseInt(parts[1]) - 1, parseInt(parts[2]));
  if (isNaN(birth.getTime())) return '-';

  const today = new Date();
  let years = today.getFullYear() - birth.getFullYear();
  let months = today.getMonth() - birth.getMonth();
  let days = today.getDate() - birth.getDate();

  if (days < 0) {
    months--;
    const prevMonth = new Date(today.getFullYear(), today.getMonth(), 0);
    days += prevMonth.getDate();
  }
  if (months < 0) {
    years--;
    months += 12;
  }

  return `${years} Thn, ${months} Bln, ${days} Hr`;
}

onMounted(async () => {
  await loadMasters();
  loading.value = false;
});
</script>

<template>
  <div class="apotik-section">
    <div v-if="loading" class="loading">Memuat...</div>
    <template v-else>
      <!-- Header -->
      <div class="page-header">
        <h2>Transaksi Apotik</h2>
        <p class="page-subtitle">Cari pasien terdaftar untuk memulai transaksi</p>
      </div>

      <!-- Unit Selector -->
      <div class="unit-bar">
        <label class="unit-label">LOKASI TRANSAKSI</label>
        <select v-model="selectedUnitId" class="unit-select">
          <option v-for="u in masters.units" :key="u.unitId" :value="String(u.unitId)">
            {{ u.unitCode }}. {{ u.unitName }}
          </option>
        </select>
      </div>

      <!-- Notifications -->
      <div v-if="message" class="status-banner status-banner--success">{{ message }}</div>
      <div v-if="error" class="status-banner status-banner--error">{{ error }}</div>

      <!-- Patient Search -->
      <div class="card search-card">
        <h3>
          <span>Pencarian Pasien</span>
          <span class="search-actions">
            <button v-if="selectedPatient && !showSearch" class="small-button" type="button" @click="toggleSearch">Cari Pasien Lain</button>
            <button v-if="patientResults.length || selectedPatient" class="small-button" type="button" @click="resetSearch">Reset</button>
          </span>
        </h3>

        <div v-if="showSearch" class="search-form">
          <div class="form-row">
            <label>
              No. MR
              <input v-model="patientSearch.mrCode" placeholder="Ketik No. Medical Record" @keyup.enter="searchPatients" />
            </label>
            <label>
              Nama Pasien
              <input v-model="patientSearch.patientName" placeholder="Nama lengkap pasien" @keyup.enter="searchPatients" />
            </label>
          </div>
          <div class="form-row">
            <label class="wide">
              Alamat
              <input v-model="patientSearch.address" placeholder="Alamat pasien" @keyup.enter="searchPatients" />
            </label>
          </div>
          <button class="primary-button" :disabled="searchingPatients" @click="searchPatients">
            {{ searchingPatients ? 'Mencari...' : 'Cari Pasien' }}
          </button>
        </div>

        <!-- Search Results -->
        <div v-if="showSearch && patientResults.length" class="table-wrap">
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
              <tr v-for="p in patientResults" :key="p.medicalRecordId || p.mrCode">
                <td><strong>{{ p.mrCode }}</strong></td>
                <td>{{ p.patientName }}</td>
                <td>{{ p.address || '-' }}</td>
                <td>
                  <button class="link-button" @click="selectPatient(p.mrCode)">Pilih</button>
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
              <span class="detail-value">{{ selectedPatient.mrCode }}</span>
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
              <span class="detail-label">Alamat</span>
              <span class="detail-value">{{ selectedPatient.address || '-' }}</span>
            </div>
            <div class="detail-item">
              <span class="detail-label">Tipe Pasien</span>
              <span class="detail-value">{{ selectedPatient.patientTypeName || '-' }}</span>
            </div>
            <div class="detail-item">
              <span class="detail-label">Kelas Tarif</span>
              <span class="detail-value">{{ selectedPatient.tariffClass || 'KELAS II' }}</span>
            </div>
            <div class="detail-item">
              <span class="detail-label">No. Registrasi</span>
              <span class="detail-value">{{ selectedPatient.registrationCode || '-' }}</span>
            </div>
            <div class="detail-item">
              <span class="detail-label">Rawat Inap</span>
              <span class="detail-value">{{ selectedPatient.inpatient ? 'Ya' : 'Tidak' }}</span>
            </div>
          </div>
        </div>

        <!-- Action Buttons -->
        <div v-if="selectedPatient" class="action-bar">
          <button class="action-button action-button--obm" type="button" :class="{ 'is-active': showObmPanel }" @click="toggleObmPanel">
            <span class="action-icon">💊</span>
            <span class="action-label">Tambah O-BM</span>
          </button>
          <button class="action-button action-button--racikan" type="button" :class="{ 'is-active': showRacikanPanel }" @click="toggleRacikanPanel">
            <span class="action-icon">⚗️</span>
            <span class="action-label">Racikan</span>
          </button>
          <button class="action-button action-button--misc" type="button" :class="{ 'is-active': showMiscPanel }" @click="toggleMiscPanel">
            <span class="action-icon">📋</span>
            <span class="action-label">Biaya Lain-Lain</span>
          </button>
        </div>

        <!-- O-BM Search Panel -->
        <div v-if="showObmPanel && selectedPatient" class="obm-panel">
          <div class="obm-header">
            <h4>FORM TAMBAH OBAT - BAHAN MEDIS</h4>
            <button class="small-button" type="button" @click="showObmPanel = false">Tutup</button>
          </div>

          <div class="obm-search-form">
            <div class="form-row">
              <label>
                KODE
                <input v-model="itemSearch.code" placeholder="Kode item/obat" @keyup.enter="searchItems" />
              </label>
              <label>
                NAMA
                <input v-model="itemSearch.name" placeholder="Nama item/obat" @keyup.enter="searchItems" />
              </label>
              <label class="search-btn-wrap">
                &nbsp;
                <button class="primary-button" :disabled="searchingItems" @click="searchItems">
                  {{ searchingItems ? 'Mencari...' : 'Cari' }}
                </button>
              </label>
            </div>
          </div>

          <div v-if="searchingItems" class="loading">Mencari data...</div>

          <div v-if="itemResults.length" class="table-wrap">
            <table class="table obm-table">
              <thead>
                <tr>
                  <th>KODE</th>
                  <th>JLH STOK</th>
                  <th>NAMA</th>
                  <th>HARGA</th>
                  <th>SATUAN</th>
                  <th>JUMLAH</th>
                </tr>
              </thead>
              <tbody>
                <tr v-for="item in itemResults" :key="item.itemId">
                  <td><strong>{{ item.itemCode }}</strong></td>
                  <td>{{ item.stockQuantity }}</td>
                  <td>{{ item.itemName }}</td>
                  <td>Rp {{ formatCurrency(item.price) }}</td>
                  <td>{{ item.unitName || '-' }}</td>
                  <td>
                    <input class="qty-input" type="number" v-model.number="itemQuantities[item.itemId]" min="1" step="1" />
                  </td>
                </tr>
              </tbody>
            </table>
          </div>

          <div v-if="itemResults.length" class="obm-actions">
            <button class="primary-button" @click="saveObmItems">Simpan</button>
            <button class="small-button" type="button" @click="showObmPanel = false">Selesai</button>
          </div>
        </div>

        <!-- RACIKAN Panel - Step 1: Pilih Obat -->
        <div v-if="showRacikanPanel && selectedPatient && racikanForm.step === 1" class="obm-panel">
          <div class="obm-header">
            <h4>PILIH OBAT UNTUK RACIKAN</h4>
            <button class="small-button" type="button" @click="showRacikanPanel = false">Tutup</button>
          </div>

          <div class="obm-search-form">
            <div class="form-row">
              <label>
                KODE
                <input v-model="itemSearch.code" placeholder="Kode item/obat" @keyup.enter="searchItems" />
              </label>
              <label>
                NAMA
                <input v-model="itemSearch.name" placeholder="Nama item/obat" @keyup.enter="searchItems" />
              </label>
              <label class="search-btn-wrap">
                &nbsp;
                <button class="primary-button" :disabled="searchingItems" @click="searchItems">
                  {{ searchingItems ? 'Mencari...' : 'Cari' }}
                </button>
              </label>
            </div>
          </div>

          <!-- Accumulated Components -->
          <div v-if="racikanComponents.length" class="component-list">
            <strong>Komponen Terpilih ({{ racikanComponents.length }}):</strong>
            <div class="component-chips">
              <span v-for="c in racikanComponents" :key="c.itemId" class="chip">
                {{ c.itemName }} × {{ c.quantity }} {{ c.unitName || '' }}
                <button class="chip-remove" type="button" @click="removeRacikanComponent(c.itemId)">×</button>
              </span>
            </div>
          </div>

          <div v-if="searchingItems" class="loading">Mencari data...</div>

          <div v-if="itemResults.length" class="table-wrap">
            <table class="table obm-table">
              <thead>
                <tr>
                  <th>KODE</th>
                  <th>JLH STOK</th>
                  <th>NAMA</th>
                  <th>HARGA</th>
                  <th>SATUAN</th>
                  <th>JUMLAH</th>
                </tr>
              </thead>
              <tbody>
                <tr v-for="item in itemResults" :key="item.itemId">
                  <td><strong>{{ item.itemCode }}</strong></td>
                  <td>{{ item.stockQuantity }}</td>
                  <td>{{ item.itemName }}</td>
                  <td>Rp {{ formatCurrency(item.price) }}</td>
                  <td>{{ item.unitName || '-' }}</td>
                  <td>
                    <input class="qty-input" type="number" v-model.number="racikanComponentQty[item.itemId]" min="1" step="1" />
                  </td>
                </tr>
              </tbody>
            </table>
          </div>

          <div v-if="itemResults.length" class="obm-actions">
            <button class="primary-button" @click="proceedToRacikanStep2">Lanjutkan ke Racikan</button>
            <button class="small-button" type="button" @click="showRacikanPanel = false">Batal</button>
          </div>
        </div>

        <!-- RACIKAN Panel - Step 2: Input Racikan -->
        <div v-if="showRacikanPanel && selectedPatient && racikanForm.step === 2" class="obm-panel">
          <div class="obm-header">
            <h4>INPUT OBAT RACIKAN</h4>
            <button class="small-button" type="button" @click="showRacikanPanel = false">Tutup</button>
          </div>

          <div v-if="racikanComponents.length" class="component-summary">
            <strong>Komponen Racikan:</strong>
            <ul>
              <li v-for="c in racikanComponents" :key="c.itemId">
                {{ c.itemName }} x {{ c.quantity }} {{ c.unitName || '' }}
              </li>
            </ul>
          </div>

          <div class="racikan-form">
            <div class="form-row">
              <label>
                JUMLAH
                <input type="number" v-model.number="racikanForm.quantity" min="1" step="1" />
              </label>
              <label>
                SATUAN
                <select v-model="racikanForm.unitName">
                  <option value="BUNGKUS">1. BUNGKUS</option>
                  <option value="KAPSUL">2. KAPSUL</option>
                  <option value="BOTOL">3. BOTOL</option>
                  <option value="SALEP">4. SALEP</option>
                </select>
              </label>
            </div>
            <div class="form-row">
              <label class="wide">
                ATURAN PAKAI
                <input type="text" v-model="racikanForm.instruction" placeholder="(Setiap 8 Jam) sesudah makan" />
              </label>
            </div>
          </div>

          <div class="obm-actions">
            <button class="primary-button" @click="saveRacikanItem">Simpan</button>
            <button class="small-button" type="button" @click="racikanForm.step = 1">Kembali</button>
            <button class="small-button" type="button" @click="showRacikanPanel = false">Batal</button>
          </div>
        </div>

        <!-- MISC Panel -->
        <div v-if="showMiscPanel && selectedPatient" class="obm-panel">
          <div class="obm-header">
            <h4>FORM TRANSAKSI LAIN-LAIN</h4>
            <button class="small-button" type="button" @click="showMiscPanel = false">Tutup</button>
          </div>

          <div class="misc-form">
            <div class="form-row">
              <label class="wide">
                NAMA
                <input type="text" v-model="miscForm.name" placeholder="Nama biaya" />
              </label>
            </div>
            <div class="form-row">
              <label>
                JUMLAH
                <input type="number" v-model.number="miscForm.quantity" min="1" step="1" />
              </label>
              <label>
                HARGA SATUAN
                <input type="number" v-model.number="miscForm.price" min="0" step="100" />
              </label>
            </div>
          </div>

          <div class="obm-actions">
            <button class="primary-button" @click="saveMiscItem">Simpan</button>
            <button class="small-button" type="button" @click="showMiscPanel = false">Selesai</button>
          </div>
        </div>

        <!-- Cart Items Summary -->
        <div v-if="cartItems.length" class="card">
          <h3>
            <span>Item Transaksi ({{ cartItems.length }})</span>
            <span class="cart-total">Total: Rp {{ formatCurrency(roundUp(cartItems.reduce((t, i) => t + (i.quantity * i.unitPriceWithPpn), 0))) }}</span>
          </h3>
          <div class="table-wrap">
            <table class="table">
              <thead>
                <tr>
                  <th>KODE</th>
                  <th>NAMA</th>
                  <th>HARGA</th>
                  <th>QTY</th>
                  <th>SUBTOTAL</th>
                  <th>ATURAN PAKAI</th>
                  <th></th>
                </tr>
              </thead>
              <tbody>
                <tr v-for="(item, idx) in cartItems" :key="item.referenceId">
                  <td><strong>{{ item.itemCode }}</strong></td>
                  <td>{{ item.itemName }}</td>
                  <td>Rp {{ formatCurrency(item.unitPriceWithPpn) }}</td>
                  <td>{{ item.quantity }} {{ item.unitName || '' }}</td>
                  <td>Rp {{ formatCurrency(roundUp(item.quantity * item.unitPriceWithPpn)) }}</td>
                  <td>
                    <input class="rule-input" type="text" v-model="item.instruction" placeholder="Aturan pakai" />
                  </td>
                  <td>
                    <button class="link-button link-danger" @click="removeCartItem(idx)">Hapus</button>
                  </td>
                </tr>
              </tbody>
            </table>
          </div>

          <!-- Save / Validate Actions -->
          <div v-if="cartItems.length && !savedNote" class="card">
            <div class="save-actions">
              <button class="primary-button primary-button--lg" :disabled="savingNote" @click="saveNote">
                {{ savingNote ? 'Menyimpan...' : '💾 Simpan Transaksi' }}
              </button>
              <button class="secondary-button" type="button" @click="resetAll">Batal</button>
            </div>
          </div>

          <!-- Saved Note Info -->
          <div v-if="savedNote" class="card saved-note-card">
            <h3>✅ Transaksi Tersimpan</h3>
            <div class="saved-note-info">
              <div class="note-info-item">
                <span class="note-info-label">No. Nota</span>
                <span class="note-info-value">{{ savedNote.noteNumber }}</span>
              </div>
              <div class="note-info-item">
                <span class="note-info-label">Status</span>
                <span class="note-info-value note-status">Belum Validasi</span>
              </div>
            </div>
            <div class="save-actions">
              <button class="primary-button primary-button--lg" :disabled="validatingNote" @click="validateNote">
                {{ validatingNote ? 'Memvalidasi...' : '✅ Validasi Nota' }}
              </button>
              <button class="secondary-button" type="button" @click="newTransaction">Baru</button>
            </div>
          </div>
        </div>
      </div>
    </template>
  </div>
</template>

<script>
function statusBadgeClass(label) {
  if (!label) return '';
  const lc = label.toLowerCase();
  if (lc.includes('validasi')) return 'badge--success';
  if (lc.includes('batal')) return 'badge--danger';
  if (lc.includes('aktif')) return 'badge--info';
  return '';
}
</script>

<style scoped>
.apotik-section { padding: 16px; }

.page-header { margin-bottom: 16px; }

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

.unit-label {
  font-size: 12px;
  font-weight: 700;
  color: #304b73;
  white-space: nowrap;
}

.unit-select {
  min-width: 280px;
  padding: 6px 10px;
  border: 1px solid #d1d9e6;
  border-radius: 8px;
  font: inherit;
  font-size: 13px;
  background: #fff;
}
.page-header h2 { margin: 0; color: #304b73; font-size: 20px; }
.page-subtitle { margin: 4px 0 0; color: #6b7280; font-size: 14px; }

.loading { padding: 24px; text-align: center; color: #9ca3af; }

.status-banner { padding: 12px 16px; border-radius: 10px; font-weight: 600; margin-bottom: 12px; }
.status-banner--success { background: #e6f5ea; color: #1d6b3a; }
.status-banner--error { background: #fde8ea; color: #a32943; }

.card { background: #fff; border: 1px solid #e2e8f0; border-radius: 12px; padding: 20px; margin-bottom: 16px; }
.card h3 { margin: 0 0 12px; font-size: 16px; color: #304b73; display: flex; align-items: center; justify-content: space-between; flex-wrap: wrap; gap: 8px; }
.search-actions { display: flex; gap: 6px; }
.card h4 { margin: 12px 0 8px; font-size: 14px; color: #304b73; }

.form-row { display: grid; grid-template-columns: 1fr 1fr; gap: 10px; margin-bottom: 10px; }
.form-row label { display: grid; gap: 4px; font-size: 13px; color: #3d4b63; }
.form-row .wide { grid-column: 1 / -1; }

input, select { padding: 8px 10px; border: 1px solid #d1d9e6; border-radius: 8px; font: inherit; background: #fff; }
input:disabled, select:disabled { background: #f7f7f9; color: #6b7280; }

.primary-button { padding: 8px 20px; background: #304b73; color: #fff; border: 0; border-radius: 8px; font-weight: 700; cursor: pointer; }
.primary-button:disabled { opacity: 0.5; cursor: not-allowed; }
.small-button { padding: 6px 12px; font-size: 12px; background: #eef3fb; border: 1px solid #d1d9e6; border-radius: 6px; cursor: pointer; }

.action-bar {
  display: flex;
  gap: 12px;
  margin-top: 16px;
  flex-wrap: wrap;
}

.action-button {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 12px 20px;
  border: 1px solid #d1d9e6;
  border-radius: 10px;
  background: #fff;
  font-weight: 700;
  font-size: 14px;
  cursor: pointer;
  transition: all 0.15s ease;
  min-width: 160px;
}

.action-button:hover {
  transform: translateY(-1px);
  box-shadow: 0 4px 12px rgba(48, 75, 115, 0.12);
}

.action-button--obm { border-left: 4px solid #2d7d46; color: #2d7d46; }
.action-button--racikan { border-left: 4px solid #b8860b; color: #b8860b; }
.action-button--misc { border-left: 4px solid #5f83c2; color: #304b73; }

.action-icon { font-size: 18px; }
.action-label { white-space: nowrap; }
.action-button.is-active { background: #eef3fb; border-color: #304b73; }

.obm-panel {
  margin-top: 16px;
  padding: 16px;
  background: #fafbfc;
  border: 1px solid #d1d9e6;
  border-radius: 10px;
}

.obm-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
}

.obm-header h4 {
  margin: 0;
  font-size: 14px;
  color: #304b73;
}

.obm-search-form {
  margin-bottom: 12px;
}

.search-btn-wrap {
  display: flex !important;
  flex-direction: column;
  justify-content: flex-end;
}

.obm-table th, .obm-table td {
  white-space: nowrap;
}

.qty-input {
  width: 60px;
  padding: 4px 6px;
  border: 1px solid #d1d9e6;
  border-radius: 6px;
  font: inherit;
  font-size: 12px;
  text-align: center;
}

.obm-actions {
  display: flex;
  gap: 10px;
  margin-top: 12px;
}

.component-list { margin-bottom: 10px; }

.component-chips {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  margin-top: 6px;
}

.chip {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  padding: 4px 10px;
  background: #e8f0fe;
  border: 1px solid #c5d8f0;
  border-radius: 20px;
  font-size: 12px;
  font-weight: 600;
  color: #1a4972;
}

.chip-remove {
  background: transparent;
  border: 0;
  color: #7a9bcb;
  font-weight: 700;
  font-size: 14px;
  cursor: pointer;
  padding: 0 2px;
  line-height: 1;
}
.chip-remove:hover { color: #a32943; }

.component-summary {
  margin-bottom: 12px;
  padding: 10px 14px;
  background: #f0f7ff;
  border: 1px solid #d1d9e6;
  border-radius: 8px;
  font-size: 13px;
}
.component-summary ul {
  margin: 6px 0 0;
  padding-left: 18px;
}
.component-summary li + li {
  margin-top: 4px;
}

.rule-input {
  width: 140px;
  padding: 4px 8px;
  border: 1px solid #d1d9e6;
  border-radius: 6px;
  font: inherit;
  font-size: 12px;
}

.cart-total {
  font-size: 14px;
  color: #2d7d46;
  font-weight: 700;
}

.link-danger { color: #a32943; }

.save-actions {
  display: flex;
  gap: 12px;
  align-items: center;
  flex-wrap: wrap;
}

.primary-button--lg {
  padding: 12px 28px;
  font-size: 15px;
}

.secondary-button {
  padding: 8px 20px;
  background: #fff;
  border: 1px solid #d1d9e6;
  border-radius: 8px;
  font-weight: 700;
  cursor: pointer;
  color: #3d4b63;
}
.secondary-button:hover {
  background: #f6f8fb;
}

.saved-note-card {
  border-left: 4px solid #2d7d46;
}

.saved-note-card h3 {
  color: #2d7d46;
}

.saved-note-info {
  display: flex;
  gap: 32px;
  margin: 12px 0;
}

.note-info-item {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.note-info-label {
  font-size: 11px;
  color: #6b7280;
  text-transform: uppercase;
  letter-spacing: 0.05em;
}

.note-info-value {
  font-size: 18px;
  font-weight: 700;
  color: #2b2b2b;
}

.note-status {
  color: #b8860b;
}
.link-button { background: transparent; border: 0; color: #2d5aa3; font-weight: 700; padding: 0; cursor: pointer; }

.table-wrap { overflow: auto; margin: 10px 0; }
.table { width: 100%; border-collapse: collapse; font-size: 13px; }
.table th, .table td { padding: 8px 10px; border-bottom: 1px solid #eef2f7; text-align: left; }
.table th { background: #f6f8fb; color: #304b73; white-space: nowrap; }

.selected-patient {
  margin-top: 16px;
  padding: 16px;
  background: #f8faff;
  border: 1px solid #d1d9e6;
  border-radius: 10px;
}

.patient-detail-grid {
  display: grid;
  grid-template-columns: 1fr 1fr 1fr;
  gap: 10px;
}

.detail-item {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.detail-label {
  font-size: 11px;
  color: #6b7280;
  text-transform: uppercase;
  letter-spacing: 0.05em;
}

.detail-value {
  font-size: 14px;
  font-weight: 600;
  color: #2b2b2b;
}

@media (max-width: 768px) {
  .form-row { grid-template-columns: 1fr; }
  .patient-detail-grid { grid-template-columns: 1fr 1fr; }
}
</style>

