<script setup>
import { computed, onMounted, ref } from 'vue';

const props = defineProps({
  apiBaseUrl: { type: String, required: true }
});

const emit = defineEmits(['session-expired', 'close']);

const loading = ref(false);
const error = ref('');
const activeTab = ref('payment');

const masters = ref(null);
const patient = ref(null);
const notes = ref([]);          // nota terpilih
const lines = ref([]);          // baris nota (cashierList)
const kwitansiCode = ref('');

const form = ref({
  unitId: null,
  mrCode: '', regNo: '', patientName: '', address: '', patientTypeName: '', bed: '',
  nameOnBill: '', addrOnBill: '',
  transactionType: 'pelunasan',
  ppn: 0,
  discount: 0,
  discountType: 'RP',
  cash: null,
  deposit: null
});
const depositBalance = ref(0);
const payLocked = ref(false);

// modal
const showPatientModal = ref(false);
const patientSearch = ref({ mrCode: '', name: '', address: '' });
const patientResults = ref([]);
const showNoteModal = ref(false);
const noteSearch = ref({ noteNo: '', name: '' });
const noteResults = ref([]);

// cari kwitansi (re-print)
const showBillModal = ref(false);
const billSearch = ref({ code: '', nameOnBill: '' });
const billResults = ref([]);
const loadedNoteNos = ref('');
const billTotalPaid = ref(null);

// tab CARA PEMBAYARAN
const bankPay = ref({ type: 'creditcard', bankId: null, cardType: 'visa', accountNo: '', amount: null });
const insurancePay = ref({ insuranceId: null, amount: null });
const paymentTerms = ref([]); // {kind: 'bank'|'insurance', desc, amount, bankId, insuranceId, accountNo}

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

const fmtMoney = (v) => Number(v || 0).toLocaleString('en-US', { minimumFractionDigits: 0, maximumFractionDigits: 0 });

// ================= FORMAT RIBUAN (input tunai / deposit) =================
// Tampilkan angka dengan pemisah ribuan titik (format Indonesia): 12000 -> 12.000
function formatThousands(value) {
  if (value === null || value === undefined || value === '') return '';
  const num = Number(value);
  if (!isFinite(num)) return '';
  let [intStr, decStr] = String(num).split('.');
  const formatted = (intStr || '0').replace(/\B(?=(\d{3})+(?!\d))/g, '.');
  if (decStr !== undefined) {
    decStr = decStr.slice(0, 2);
    return `${formatted},${decStr}`;
  }
  return formatted;
}

// Ubah teks input menjadi angka. Pemisah terakhir dengan <= 2 digit dianggap
// pemisah desimal, selain itu dianggap pemisah ribuan (dibuang semua).
function parseThousands(text) {
  if (text === null || text === undefined) return null;
  let s = String(text).trim();
  if (!s) return null;
  const lastIdx = Math.max(s.lastIndexOf('.'), s.lastIndexOf(','));
  if (lastIdx >= 0) {
    const after = s.slice(lastIdx + 1).replace(/\D/g, '');
    if (after.length <= 2) {
      const intPart = s.slice(0, lastIdx).replace(/\D/g, '');
      const num = Number(`${intPart || '0'}.${after || '0'}`);
      return isFinite(num) ? num : null;
    }
  }
  const digits = s.replace(/\D/g, '');
  if (!digits) return null;
  const num = parseInt(digits, 10);
  return isFinite(num) ? num : null;
}

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
    masters.value = await request('/cashier/masters');
    if (masters.value.units.length) form.value.unitId = masters.value.units[0].unitId;
  } catch (requestError) {
    error.value = requestError.message;
  }
});

// ================= PASIEN =================

async function searchPatient() {
  const s = patientSearch.value;
  if (!s.mrCode && !s.name && !s.address) {
    await showAlert('Salah satu field pencarian pasien harus diisi!');
    return;
  }
  loading.value = true;
  error.value = '';
  try {
    patientResults.value = await request(`/cashier/patients/registered${qs(s)}`);
  } catch (requestError) {
    error.value = requestError.message;
  } finally {
    loading.value = false;
  }
}

async function selectPatient(result) {
  showPatientModal.value = false;
  loading.value = true;
  error.value = '';
  try {
    patient.value = await request(`/cashier/patients/${encodeURIComponent(result.mrCode)}`);
    form.value.mrCode = patient.value.mrCode;
    form.value.regNo = patient.value.registrationNumber || '';
    form.value.patientName = patient.value.patientName;
    form.value.address = patient.value.address || '';
    form.value.patientTypeName = patient.value.patientTypeName || '';
    form.value.bed = patient.value.bed || '';
    form.value.nameOnBill = patient.value.patientName;
    form.value.addrOnBill = patient.value.address || '';
    depositBalance.value = patient.value.depositBalance || 0;
    // ambil semua nota yang sudah divalidasi & belum lunas milik pasien ini,
    // lalu tampilkan langsung di DATA TRANSAKSI PASIEN (migrasi legacy getRegistration + getNoteDetil)
    await autoLoadNotes();
  } catch (requestError) {
    error.value = requestError.message;
  } finally {
    loading.value = false;
  }
}

async function autoLoadNotes() {
  if (!patient.value || !patient.value.registrationId) {
    notes.value = [];
    lines.value = [];
    return;
  }
  notes.value = await request(`/cashier/notes${qs({ registrationId: patient.value.registrationId })}`);
  noteResults.value = notes.value.map((n) => ({ ...n }));
  lines.value = [];
  for (const note of notes.value) {
    const noteLines = await request(`/cashier/notes/${note.noteId}/lines`);
    noteLines.forEach((line) => {
      lines.value.push({ ...line, noteNo: note.noteNo });
    });
  }
}

// ================= NOTA =================

async function searchNote() {
  const s = noteSearch.value;
  if (!s.noteNo && !s.name) {
    await showAlert('Salah satu field pencarian nota harus diisi!');
    return;
  }
  loading.value = true;
  error.value = '';
  try {
    noteResults.value = await request(`/cashier/notes${qs({ unitId: form.value.unitId, noteNo: s.noteNo, patientName: s.name })}`);
  } catch (requestError) {
    error.value = requestError.message;
  } finally {
    loading.value = false;
  }
}

function toggleNote(note) {
  const idx = notes.value.findIndex((n) => n.noteId === note.noteId);
  if (idx >= 0) notes.value.splice(idx, 1);
  else notes.value.push(note);
}

function isNoteSelected(note) {
  return notes.value.some((n) => n.noteId === note.noteId);
}

async function loadSelectedNotes() {
  lines.value = [];
  for (const note of notes.value) {
    const noteLines = await request(`/cashier/notes/${note.noteId}/lines`);
    noteLines.forEach((line) => {
      lines.value.push({ ...line, noteNo: note.noteNo });
    });
  }
  showNoteModal.value = false;
}

// ================= CARI KWITANSI (RE-PRINT) =================

async function searchBill() {
  const s = billSearch.value;
  if (!s.code && !s.nameOnBill) {
    await showAlert('Salah satu field pencarian kwitansi harus diisi!');
    return;
  }
  loading.value = true;
  error.value = '';
  try {
    billResults.value = await request(`/cashier/bills${qs({ code: s.code, nameOnBill: s.nameOnBill })}`);
  } catch (requestError) {
    error.value = requestError.message;
  } finally {
    loading.value = false;
  }
}

async function selectBill(result) {
  showBillModal.value = false;
  loading.value = true;
  error.value = '';
  try {
    const bill = await request(`/cashier/bills/${result.billId}`);
    // tampilkan seluruh nota + baris transaksi di dalam kwitansi (untuk re-print)
    form.value.mrCode = bill.mrCode || '';
    form.value.patientName = bill.patientName || '';
    form.value.address = bill.address || '';
    form.value.patientTypeName = bill.patientTypeName || '';
    form.value.bed = bill.bed || '';
    form.value.nameOnBill = bill.nameOnBill || '';
    form.value.addrOnBill = bill.addrOnBill || '';
    form.value.cash = bill.cashAmount || 0;
    form.value.deposit = bill.depositAmount || 0;
    form.value.ppn = 0;
    form.value.discount = bill.discount || 0;
    form.value.discountType = 'RP';
    depositBalance.value = bill.depositBalance || 0;
    kwitansiCode.value = bill.billCode;
    loadedNoteNos.value = bill.noteNos || '';
    billTotalPaid.value = bill.totalPaid || null;
    patient.value = null;
    notes.value = [];
    lines.value = bill.lines || [];
    payLocked.value = true; // mode re-print: BAYAR dinonaktifkan
    showToast(`Kwitansi ${bill.billCode} dimuat (${bill.lines ? bill.lines.length : 0} baris).`, 'info');
  } catch (requestError) {
    error.value = requestError.message;
  } finally {
    loading.value = false;
  }
}

// ================= TOTAL =================

const biaya = computed(() => lines.value.reduce((s, l) => s + (Number(l.subtotal) || 0), 0));

const discountValue = computed(() => {
  const d = Number(form.value.discount) || 0;
  return form.value.discountType === '%' ? biaya.value * d / 100 : d;
});

const totalAmount = computed(() => {
  const base = biaya.value - discountValue.value;
  const ppn = Number(form.value.ppn) || 0;
  return base + (base * ppn / 100);
});

const paidTotal = computed(() => (Number(form.value.cash) || 0) + (Number(form.value.deposit) || 0)
  + paymentTerms.value.reduce((s, t) => s + (Number(t.amount) || 0), 0));

// BAYAR NON TUNAI = nilai dari tab CARA PEMBAYARAN (kartu + asuransi)
const nonCash = computed(() => paymentTerms.value.reduce((s, t) => s + (Number(t.amount) || 0), 0));

const difference = computed(() => paidTotal.value - totalAmount.value);

// input BAYAR TUNAI / BAYAR DEPOSIT dengan pemisah ribuan otomatis
const cashDisplay = computed({
  get: () => formatThousands(form.value.cash),
  set: (value) => { form.value.cash = parseThousands(value); }
});

const depositDisplay = computed({
  get: () => formatThousands(form.value.deposit),
  set: (value) => { form.value.deposit = parseThousands(value); }
});

function bankName(id) {
  return masters.value?.banks?.find((b) => b.bankId === id)?.name || '';
}

function insuranceName(id) {
  return masters.value?.insurances?.find((i) => i.insuranceId === id)?.name || '';
}

// ================= CARA PEMBAYARAN =================

function addBankTerm() {
  if (!bankPay.value.bankId) {
    showAlert('PILIH NAMA BANK TERLEBIH DAHULU!');
    return;
  }
  if (!bankPay.value.amount || bankPay.value.amount <= 0) {
    showAlert('SEBESAR HARUS DI ISI!');
    return;
  }
  paymentTerms.value.push({
    kind: 'bank',
    cardType: bankPay.value.cardType,
    bankId: bankPay.value.bankId,
    accountNo: bankPay.value.accountNo,
    amount: Number(bankPay.value.amount),
    desc: `${bankPay.value.cardType.toUpperCase()}; NO. ACC ${bankPay.value.accountNo}; ${bankName(bankPay.value.bankId)}`
  });
  bankPay.value = { type: 'creditcard', bankId: null, cardType: 'visa', accountNo: '', amount: null };
}

function addInsuranceTerm() {
  if (!insurancePay.value.insuranceId) {
    showAlert('PILIH NAMA PERUSAHAAN TERLEBIH DAHULU!');
    return;
  }
  if (!insurancePay.value.amount || insurancePay.value.amount <= 0) {
    showAlert('SEBESAR HARUS DI ISI!');
    return;
  }
  paymentTerms.value.push({
    kind: 'insurance',
    insuranceId: insurancePay.value.insuranceId,
    amount: Number(insurancePay.value.amount),
    desc: insuranceName(insurancePay.value.insuranceId)
  });
  insurancePay.value = { insuranceId: null, amount: null };
}

function deleteTerm(index) {
  paymentTerms.value.splice(index, 1);
}

// ================= BAYAR =================

function clearAll() {
  patient.value = null;
  notes.value = [];
  lines.value = [];
  kwitansiCode.value = '';
  loadedNoteNos.value = '';
  billTotalPaid.value = null;
  depositBalance.value = 0;
  paymentTerms.value = [];
  payLocked.value = false;
  Object.assign(form.value, {
    mrCode: '', regNo: '', patientName: '', address: '', patientTypeName: '', bed: '',
    nameOnBill: '', addrOnBill: '', transactionType: 'pelunasan', ppn: 0,
    discount: 0, discountType: 'RP', cash: null, deposit: null
  });
}

async function pay() {
  if (!patient.value) {
    await showAlert('PILIH PASIEN TERLEBIH DAHULU!');
    return;
  }
  if (form.value.transactionType !== 'pelunasan') {
    // DEPOSIT / RETUR-DEPOSIT (khusus ranap)
    const amount = form.value.transactionType === 'retur' ? (form.value.cash || 0) : (form.value.cash || 0);
    if (!amount) {
      await showAlert('JUMLAH DEPOSIT HARUS DI ISI!');
      return;
    }
    const ok = await showConfirm(`${form.value.transactionType === 'retur' ? 'Retur' : 'Setor'} deposit ${fmtMoney(amount)}?`, { title: 'DEPOSIT' });
    if (!ok) return;
    loading.value = true;
    error.value = '';
    try {
      const result = await request(`/cashier/deposit${qs({ retur: form.value.transactionType === 'retur' })}`, {
        method: 'POST',
        body: JSON.stringify({
          registrationId: patient.value.registrationId,
          unitId: form.value.unitId,
          nameOnBill: form.value.nameOnBill,
          addrOnBill: form.value.addrOnBill,
          amount
        })
      });
      kwitansiCode.value = result.kwitansiCode;
      depositBalance.value = result.depositBalance;
      payLocked.value = true;
      showToast(result.message, 'success');
    } catch (requestError) {
      error.value = requestError.message;
    } finally {
      loading.value = false;
    }
    return;
  }
  if (!notes.value.length) {
    await showAlert('PILIH NOTA TERLEBIH DAHULU!');
    return;
  }
  if (difference.value > 0) {
    await showAlert('PEMBAYARAN BELUM CUKUP!');
    return;
  }
  const ok = await showConfirm(`Bayar ${fmtMoney(totalAmount.value)} untuk ${notes.value.length} nota?`, { title: 'KONFIRMASI BAYAR' });
  if (!ok) return;
  loading.value = true;
  error.value = '';
  try {
    const result = await request('/cashier/pay', {
      method: 'POST',
      body: JSON.stringify({
        registrationId: patient.value.registrationId,
        unitId: form.value.unitId,
        nameOnBill: form.value.nameOnBill,
        addrOnBill: form.value.addrOnBill,
        ppn: form.value.ppn,
        discount: form.value.discount,
        discountType: form.value.discountType,
        cash: form.value.cash,
        deposit: form.value.deposit,
        noteIds: notes.value.map((n) => n.noteId),
        settlements: paymentTerms.value.map((t) => t.kind === 'bank'
          ? { type: 1, amount: t.amount, bankId: t.bankId, accountNo: t.accountNo }
          : { type: 2, amount: t.amount, insuranceId: t.insuranceId })
      })
    });
    kwitansiCode.value = result.kwitansiCode;
    depositBalance.value = result.depositBalance;
    payLocked.value = true;
    showToast(result.message, 'success');
  } catch (requestError) {
    error.value = requestError.message;
  } finally {
    loading.value = false;
  }
}

function printKwitansi() {
  if (!kwitansiCode.value) {
    showAlert('BELUM ADA KWITANSI!');
    return;
  }
  window.open(`${props.apiBaseUrl}/cashier/bill/${encodeURIComponent(kwitansiCode.value)}/print`, '_blank');
}
</script>

<template>
  <div class="screen-page">
    <div class="page-header">
      <h2>💵 TRANSAKSI KASIR</h2>
    </div>

    <p v-if="error" class="status-banner status-banner--error">{{ error }}</p>

    <div class="card">
      <div class="tabs">
        <button class="tab" :class="{ active: activeTab === 'payment' }" type="button" @click="activeTab = 'payment'">💰 TRANSAKSI PEMBAYARAN</button>
        <button class="tab" :class="{ active: activeTab === 'paidBy' }" type="button" @click="activeTab = 'paidBy'">💳 CARA PEMBAYARAN</button>
      </div>

      <!-- ==================== TAB 1: TRANSAKSI PEMBAYARAN ==================== -->
      <div v-if="activeTab === 'payment'">
        <div class="section-title">DATA PASIEN</div>
        <div class="patient-grid">
          <div class="field">
            <label>LOKASI TRANSAKSI</label>
            <select v-model="form.unitId">
              <option v-for="u in (masters?.units || [])" :key="u.unitId" :value="u.unitId">{{ u.code }}-{{ u.name }}</option>
            </select>
          </div>
          <div class="field">
            <label>NO. KWITANSI</label>
            <div class="input-row">
              <input :value="kwitansiCode" readonly placeholder="-" />
              <button class="mini primary" type="button" @click="showBillModal = true">CARI KWITANSI</button>
            </div>
          </div>
          <div class="field">
            <label>JENIS TRANSAKSI</label>
            <select v-model="form.transactionType" :disabled="payLocked">
              <option value="pelunasan">PELUNASAN</option>
              <option value="deposit">DEPOSIT</option>
              <option value="retur">RETUR-DEPOSIT</option>
            </select>
          </div>
          <div class="field">
            <label>NO. NOTA</label>
            <div class="input-row">
              <input :value="notes.length ? notes.map(n => n.noteNo).join(', ') : loadedNoteNos" readonly placeholder="-" />
              <button class="mini primary" type="button" :disabled="payLocked" @click="showNoteModal = true">CARI NOTA</button>
            </div>
          </div>
          <div class="field">
            <label>NO. MR</label>
            <div class="input-row">
              <input v-model="form.mrCode" readonly placeholder="-" />
              <button class="mini primary" type="button" :disabled="payLocked" @click="showPatientModal = true">CARI PASIEN</button>
            </div>
          </div>
          <div class="field">
            <label>NO. REGISTRASI</label>
            <input v-model="form.regNo" readonly />
          </div>
          <div class="field">
            <label>NAMA</label>
            <input v-model="form.patientName" readonly />
          </div>
          <div class="field">
            <label>ALAMAT</label>
            <input v-model="form.address" readonly />
          </div>
          <div class="field">
            <label>TIPE PASIEN</label>
            <input v-model="form.patientTypeName" readonly />
          </div>
          <div class="field">
            <label>BED</label>
            <input v-model="form.bed" readonly />
          </div>
          <div class="field">
            <label>NAMA PENANGGUNG</label>
            <input v-model="form.nameOnBill" :readonly="payLocked" />
          </div>
          <div class="field">
            <label>ALAMAT PENANGGUNG</label>
            <input v-model="form.addrOnBill" :readonly="payLocked" />
          </div>
        </div>

        <div class="section-title">DATA TRANSAKSI PASIEN</div>
        <div class="table-wrap">
          <table class="table">
            <thead>
              <tr><th>NO. NOTA</th><th>KODE</th><th>KETERANGAN</th><th class="num">JUMLAH</th><th>SATUAN</th><th class="num">HARGA</th><th class="num">DISKON</th><th class="num">SUBTOTAL</th></tr>
            </thead>
            <tbody>
              <tr v-for="(line, index) in lines" :key="index">
                <td>{{ line.noteNo }}</td>
                <td>{{ line.code }}</td>
                <td>{{ line.name }}</td>
                <td class="num">{{ line.qty }}</td>
                <td>{{ line.unit }}</td>
                <td class="num">{{ fmtMoney(line.price) }}</td>
                <td class="num">{{ fmtMoney(line.discount) }}</td>
                <td class="num strong">{{ fmtMoney(line.subtotal) }}</td>
              </tr>
              <tr v-if="!lines.length">
                <td colspan="8" class="empty-state">Pilih nota yang akan dibayar.</td>
              </tr>
            </tbody>
          </table>
        </div>

        <div class="totals-grid">
          <div class="field"><label>BIAYA</label><input :value="fmtMoney(biaya)" readonly /></div>
          <div class="field"><label>TTL. RETUR</label><input value="0" readonly /></div>
          <div class="field"><label>PPN (%)</label><input v-model.number="form.ppn" type="number" min="0" :disabled="payLocked" /></div>
          <div class="field"><label>DISKON</label>
            <div class="rt-rw">
              <input v-model.number="form.discount" type="number" min="0" :disabled="payLocked" />
              <select v-model="form.discountType" :disabled="payLocked">
                <option value="RP">RP</option>
                <option value="%">%</option>
              </select>
            </div>
          </div>
          <div class="field"><label>TTL. BIAYA</label><input :value="fmtMoney(billTotalPaid ?? totalAmount)" readonly class="highlight" /></div>
          <div class="field"><label>JUMLAH DEPOSIT</label><input :value="fmtMoney(depositBalance)" readonly /></div>
          <div class="field"><label>BAYAR TUNAI</label><input v-model="cashDisplay" type="text" inputmode="numeric" placeholder="0" :disabled="payLocked" /></div>
          <div class="field"><label>BAYAR NON TUNAI</label><input :value="fmtMoney(nonCash)" readonly /></div>
          <div class="field"><label>BAYAR DEPOSIT</label><input v-model="depositDisplay" type="text" inputmode="numeric" placeholder="0" :disabled="payLocked" /></div>
          <div class="field"><label>KELEBIHAN / KEKURANGAN</label>
            <input :value="fmtMoney(difference)" readonly :class="difference < 0 ? 'highlight' : ''" />
          </div>
        </div>

        <div class="action-bar">
          <button class="small-button primary" type="button" :disabled="loading || payLocked" @click="pay">💾 BAYAR</button>
          <button class="small-button" type="button" @click="clearAll">🆕 BARU</button>
          <button class="small-button" type="button" :disabled="!kwitansiCode" @click="printKwitansi">🖨️ CETAK</button>
          <button class="small-button" type="button" @click="emit('close')">✅ SELESAI</button>
        </div>
      </div>

      <!-- ==================== TAB 2: CARA PEMBAYARAN ==================== -->
      <div v-else>
        <div class="section-title">PEMBAYARAN VIA KARTU KREDIT / KARTU DEBIT</div>
        <div class="patient-grid">
          <div class="field"><label>CARA PEMBAYARAN</label>
            <select v-model="bankPay.type"><option value="creditcard">KARTU KREDIT</option><option value="debitcard">KARTU DEBIT</option></select>
          </div>
          <div class="field"><label>NAMA BANK</label>
            <select v-model="bankPay.bankId">
              <option :value="null" />
              <option v-for="b in (masters?.banks || [])" :key="b.bankId" :value="b.bankId">{{ b.name }}</option>
            </select>
          </div>
          <div class="field"><label>TIPE KARTU</label>
            <select v-model="bankPay.cardType"><option value="visa">VISA</option><option value="master">MASTER</option></select>
          </div>
          <div class="field"><label>NO. ACCOUNT</label><input v-model="bankPay.accountNo" /></div>
          <div class="field"><label>SEBESAR</label><input v-model.number="bankPay.amount" type="number" min="0" /></div>
        </div>
        <div class="action-bar">
          <button class="small-button primary" type="button" @click="addBankTerm">➕ TAMBAH</button>
        </div>

        <div class="section-title">DI TANGGUNG PERUSAHAAN / ASURANSI</div>
        <div class="patient-grid">
          <div class="field"><label>NAMA PERUSAHAAN</label>
            <select v-model="insurancePay.insuranceId">
              <option :value="null" />
              <option v-for="i in (masters?.insurances || [])" :key="i.insuranceId" :value="i.insuranceId">{{ i.name }}</option>
            </select>
          </div>
          <div class="field"><label>SEBESAR</label><input v-model.number="insurancePay.amount" type="number" min="0" /></div>
        </div>
        <div class="action-bar">
          <button class="small-button primary" type="button" @click="addInsuranceTerm">➕ TAMBAH</button>
        </div>

        <div class="section-title">KETERANGAN BAYAR</div>
        <div class="table-wrap">
          <table class="table">
            <thead><tr><th>CARA BAYAR</th><th>KETERANGAN</th><th class="num">SEBESAR</th><th></th></tr></thead>
            <tbody>
              <tr v-for="(t, index) in paymentTerms" :key="index">
                <td class="strong">{{ t.kind === 'bank' ? 'KARTU KREDIT' : 'DITANGGUNG PERUSAHAAN' }}</td>
                <td>{{ t.desc }}</td>
                <td class="num">{{ fmtMoney(t.amount) }}</td>
                <td><button class="mini danger" type="button" @click="deleteTerm(index)">✖</button></td>
              </tr>
              <tr v-if="!paymentTerms.length"><td colspan="4" class="empty-state">Belum ada cara pembayaran tambahan.</td></tr>
            </tbody>
          </table>
        </div>
      </div>
    </div>

    <!-- ==================== MODAL: CARI PASIEN ==================== -->
    <div v-if="showPatientModal" class="modal-overlay" @click.self="showPatientModal = false">
      <div class="modal">
        <div class="modal-header">CARI DATA PASIEN</div>
        <div class="modal-body">
          <div class="field"><label>NO. MR</label><input v-model="patientSearch.mrCode" /></div>
          <div class="field"><label>NAMA</label><input v-model="patientSearch.name" /></div>
          <div class="field"><label>ALAMAT</label><input v-model="patientSearch.address" /></div>
          <button class="small-button primary" type="button" :disabled="loading" @click="searchPatient">🔍 CARI</button>
          <div class="table-wrap modal-list">
            <table class="table">
              <thead><tr><th>NO. MR</th><th>NAMA</th><th>TIPE</th><th>ALAMAT</th></tr></thead>
              <tbody>
                <tr v-for="r in patientResults" :key="r.mrId" @click="selectPatient(r)">
                  <td class="strong">{{ r.mrCode }}</td><td>{{ r.patientName }}</td><td>{{ r.patientType }}</td><td>{{ r.address }}</td>
                </tr>
              </tbody>
            </table>
          </div>
        </div>
        <div class="modal-footer"><button class="small-button" type="button" @click="showPatientModal = false">TUTUP</button></div>
      </div>
    </div>

    <!-- ==================== MODAL: CARI NOTA ==================== -->
    <div v-if="showNoteModal" class="modal-overlay" @click.self="showNoteModal = false">
      <div class="modal">
        <div class="modal-header">CARI NOTA</div>
        <div class="modal-body">
          <div class="field"><label>NO. NOTA</label><input v-model="noteSearch.noteNo" /></div>
          <div class="field"><label>NAMA</label><input v-model="noteSearch.name" /></div>
          <button class="small-button primary" type="button" :disabled="loading" @click="searchNote">🔍 CARI</button>
          <div class="table-wrap modal-list">
            <table class="table">
              <thead><tr><th></th><th>NO. NOTA</th><th>NAMA</th><th>TANGGAL</th><th class="num">NILAI</th></tr></thead>
              <tbody>
                <tr v-for="n in noteResults" :key="n.noteId" :class="{ selected: isNoteSelected(n) }" @click="toggleNote(n)">
                  <td><input type="checkbox" :checked="isNoteSelected(n)" @change="toggleNote(n)" /></td>
                  <td class="strong">{{ n.noteNo }}</td><td>{{ n.patientName }}</td><td>{{ n.date }}</td><td class="num">{{ fmtMoney(n.total) }}</td>
                </tr>
                <tr v-if="!noteResults.length"><td colspan="5" class="empty-state">Tidak ada nota belum lunas.</td></tr>
              </tbody>
            </table>
          </div>
          <div class="action-bar">
            <button class="small-button primary" type="button" :disabled="!notes.length || loading" @click="loadSelectedNotes">✔ PILIH</button>
          </div>
        </div>
        <div class="modal-footer"><button class="small-button" type="button" @click="showNoteModal = false">TUTUP</button></div>
      </div>
    </div>

    <!-- ==================== MODAL: CARI KWITANSI (RE-PRINT) ==================== -->
    <div v-if="showBillModal" class="modal-overlay" @click.self="showBillModal = false">
      <div class="modal">
        <div class="modal-header">CARI KWITANSI</div>
        <div class="modal-body">
          <div class="field"><label>NO. KWITANSI</label><input v-model="billSearch.code" @keyup.enter="searchBill" /></div>
          <div class="field"><label>NAMA</label><input v-model="billSearch.nameOnBill" @keyup.enter="searchBill" /></div>
          <button class="small-button primary" type="button" :disabled="loading" @click="searchBill">🔍 CARI</button>
          <div class="table-wrap modal-list">
            <table class="table">
              <thead><tr><th>NO. KWITANSI</th><th>NAMA</th><th>TANGGAL</th></tr></thead>
              <tbody>
                <tr v-for="r in billResults" :key="r.billId" @click="selectBill(r)">
                  <td class="strong">{{ r.billCode }}</td><td>{{ r.nameOnBill }}</td><td>{{ r.date }}</td>
                </tr>
                <tr v-if="!billResults.length"><td colspan="3" class="empty-state">Tidak ada kwitansi ditemukan.</td></tr>
              </tbody>
            </table>
          </div>
        </div>
        <div class="modal-footer"><button class="small-button" type="button" @click="showBillModal = false">TUTUP</button></div>
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
.screen-page { padding: 16px; }
.page-header { margin-bottom: 16px; display: flex; flex-direction: column; gap: 4px; }
.page-header h2 { margin: 0; color: #304b73; font-size: 20px; }
.page-subtitle { margin: 0; color: #6b7280; font-size: 14px; }

.status-banner { padding: 12px 16px; border-radius: 10px; font-weight: 600; margin-bottom: 12px; }
.status-banner--error { background: #fde8ea; color: #a32943; }

.card { background: #fff; border-radius: 12px; padding: 16px; box-shadow: 0 1px 3px rgba(0,0,0,0.08); margin-bottom: 16px; }
.tabs { display: flex; gap: 8px; margin-bottom: 16px; border-bottom: 2px solid #eef2f7; padding-bottom: 8px; flex-wrap: wrap; }
.tab { padding: 8px 16px; border: 1px solid #d1d9e6; border-radius: 8px; background: #fff; cursor: pointer; font-weight: 700; font-size: 13px; color: #304b73; }
.tab.active { background: #304b73; color: #fff; border-color: #304b73; }

.section-title { font-weight: 800; color: #304b73; font-size: 14px; margin: 14px 0 10px; padding-bottom: 6px; border-bottom: 1px solid #eef2f7; }

.patient-grid { display: grid; grid-template-columns: repeat(3, 1fr); gap: 10px 18px; }
.totals-grid { display: grid; grid-template-columns: repeat(3, 1fr); gap: 10px 18px; margin-top: 12px; }
.field { display: flex; flex-direction: column; gap: 4px; }
.field label { font-size: 11px; font-weight: 700; color: #6b7280; text-transform: uppercase; letter-spacing: 0.03em; }
.field input, .field select { padding: 7px 9px; border: 1px solid #d1d9e6; border-radius: 6px; font-size: 13px; width: 100%; box-sizing: border-box; }
.field input[readonly] { background: #f3f5f8; color: #4b5563; }
.highlight { font-weight: 800; color: #177245; }
.rt-rw { display: flex; gap: 6px; align-items: center; }
.rt-rw input { flex: 1; }
.rt-rw select { width: 90px; }
.input-row { display: flex; gap: 6px; align-items: center; }
.input-row input { flex: 1; }
.mini { padding: 6px 10px; border-radius: 6px; border: 1px solid #d1d5db; background: #fff; cursor: pointer; font-weight: 600; font-size: 12px; white-space: nowrap; }
.mini.primary { background: #304b73; color: #fff; border-color: #304b73; }
.mini.danger { color: #a32943; border-color: #a32943; }

.table-wrap { overflow: auto; margin: 10px 0; }
.table { width: 100%; border-collapse: collapse; font-size: 13px; }
.table th, .table td { padding: 7px 9px; border-bottom: 1px solid #eef2f7; text-align: left; white-space: nowrap; }
.table th { background: #f6f8fb; color: #304b73; }
.table tbody tr:hover { background: #f6f8fb; }
.table tbody tr.selected { background: #e8eef8; }
.num { text-align: right; }
.strong { font-weight: 700; }
.empty-state { color: #9ca3af; text-align: center; padding: 16px; }

.action-bar { display: flex; flex-wrap: wrap; gap: 10px; justify-content: center; margin-top: 12px; }
.small-button { padding: 8px 14px; border-radius: 8px; border: 1px solid #d1d5db; background: #fff; cursor: pointer; font-weight: 600; font-size: 13px; }
.small-button.primary { background: #304b73; color: #fff; border-color: #304b73; }
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
  .patient-grid, .totals-grid { grid-template-columns: 1fr; }
}
</style>
