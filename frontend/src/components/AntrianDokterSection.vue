<script setup>
import { computed, onBeforeUnmount, onMounted, ref } from 'vue';

const props = defineProps({
  apiBaseUrl: { type: String, required: true }
});

const emit = defineEmits(['session-expired', 'close']);

const loading = ref(false);
const error = ref('');
const hospitalName = ref('RS. TIARA SELLA');
const textAntrian = ref('');
const delaySeconds = ref(10);
const doctors = ref([]);
const index = ref(0);

const currentDoctor = computed(() => {
  if (!doctors.value.length) return null;
  return doctors.value[index.value % doctors.value.length];
});

// paging antrian (pageSize 12, sesuai legacy mold=paging pageSize=12)
const pageSize = 12;
const currentPage = ref(1);
const pagedQueue = computed(() => {
  const q = currentDoctor.value?.queue || [];
  const start = (currentPage.value - 1) * pageSize;
  return q.slice(start, start + pageSize);
});
const totalPages = computed(() => Math.max(1, Math.ceil((currentDoctor.value?.queue || []).length / pageSize)));

let timerId = null;

async function request(path) {
  const response = await fetch(`${props.apiBaseUrl}${path}`, { credentials: 'include' });
  const payload = await response.json().catch(() => null);
  if (response.status === 401) {
    emit('session-expired', payload?.message || 'Your session has been expired. You need to login again.');
    throw new Error(payload?.message || 'Unauthorized');
  }
  if (!response.ok) throw new Error(payload?.message || `HTTP ${response.status}`);
  return payload.data;
}

async function refreshDisplay() {
  try {
    const data = await request('/antrian/display');
    hospitalName.value = data.hospitalName || 'RS. TIARA SELLA';
    textAntrian.value = data.textAntrian || '';
    if (data.delaySeconds) {
      delaySeconds.value = data.delaySeconds;
      restartTimer();
    }
    // pertahankan indeks dokter, muat ulang data (mengikuti perubahan antrian)
    if (data.doctors && data.doctors.length) {
      if (doctors.value.length && data.doctors.length !== doctors.value.length) {
        index.value = 0;
      }
      doctors.value = data.doctors;
    } else {
      doctors.value = [];
    }
    currentPage.value = 1;
  } catch (requestError) {
    error.value = requestError.message;
  }
}

function advanceDoctor() {
  if (doctors.value.length) {
    index.value = (index.value + 1) % doctors.value.length;
    currentPage.value = 1;
  }
}

function restartTimer() {
  if (timerId) clearInterval(timerId);
  const intervalMs = (delaySeconds.value || 10) * 1000;
  timerId = setInterval(() => {
    refreshDisplay();
    advanceDoctor();
  }, intervalMs);
}

function numberLabel(number) {
  if (number === null || number === undefined) return '';
  return number < 10 ? `0${number}` : `${number}`;
}

onMounted(async () => {
  await refreshDisplay();
  restartTimer();
});

onBeforeUnmount(() => {
  if (timerId) clearInterval(timerId);
});
</script>

<template>
  <div class="screen-page">
    <div class="page-header">
      <h2>📺 ANTRIAN DOKTER</h2>
    </div>

    <p v-if="error" class="status-banner status-banner--error">{{ error }}</p>

    <div class="display-board">
      <div class="display-header">
        <span class="display-kicker">ANTRIAN DOKTER</span>
        <span class="display-hospital">{{ hospitalName }}</span>
      </div>

      <div class="display-doctor">
        {{ currentDoctor ? currentDoctor.name : 'TIDAK ADA ANTRIAN' }}
      </div>

      <div v-if="currentDoctor" class="display-queue">
        <div class="queue-row queue-row--head">
          <span class="col-no">NO. ANTRIAN</span>
          <span class="col-name">NAMA PASIEN</span>
          <span class="col-mr">NO. MR</span>
        </div>
        <div v-for="row in pagedQueue" :key="row.registrationId" class="queue-row">
          <span class="col-no">{{ numberLabel(row.number) }}</span>
          <span class="col-name">{{ row.patientName }}</span>
          <span class="col-mr">{{ row.mrCode }}</span>
        </div>
        <div v-if="!pagedQueue.length" class="queue-row queue-row--empty">
          <span class="col-name">Belum ada pasien dalam antrian.</span>
        </div>
        <div class="queue-pager" v-if="(currentDoctor.queue || []).length > pageSize">
          <button class="page-btn" type="button" :disabled="currentPage <= 1" @click="currentPage--">◀</button>
          <span>{{ currentPage }} / {{ totalPages }}</span>
          <button class="page-btn" type="button" :disabled="currentPage >= totalPages" @click="currentPage++">▶</button>
        </div>
      </div>

      <div v-else class="display-empty">
        <span class="display-empty-text">SILAHKAN MENUNGGU...</span>
      </div>

      <div class="display-marquee">
        <div class="marquee-track">{{ textAntrian }}</div>
      </div>
    </div>

    <div class="action-bar">
      <button class="small-button" type="button" @click="emit('close')">✅ SELESAI</button>
    </div>
  </div>
</template>

<style scoped>
.screen-page { padding: 16px; }
.page-header { margin-bottom: 16px; display: flex; flex-direction: column; gap: 4px; }
.page-header h2 { margin: 0; color: #304b73; font-size: 20px; }
.page-subtitle { margin: 0; color: #6b7280; font-size: 14px; }

.status-banner { padding: 12px 16px; border-radius: 10px; font-weight: 600; margin-bottom: 12px; }
.status-banner--error { background: #fde8ea; color: #a32943; }

/* ---- papan display TV ---- */
.display-board {
  background: linear-gradient(135deg, #0f1b33 0%, #14264a 100%);
  border-radius: 16px;
  padding: 24px 28px;
  box-shadow: 0 16px 40px rgba(15, 23, 42, 0.35);
  border: 3px solid #5f83c2;
  min-height: 480px;
  display: flex;
  flex-direction: column;
  gap: 18px;
}

.display-header { display: flex; align-items: baseline; justify-content: space-between; gap: 12px; flex-wrap: wrap; }
.display-kicker { color: #9fc2ff; font-size: 22px; font-weight: 800; letter-spacing: 0.12em; }
.display-hospital { color: #ffffff; font-size: 30px; font-weight: 900; text-transform: uppercase; }

.display-doctor {
  color: #ffe082;
  font-size: 38px;
  font-weight: 900;
  text-align: center;
  text-transform: uppercase;
  padding: 8px 0;
  border-top: 2px solid rgba(95, 131, 194, 0.6);
  border-bottom: 2px solid rgba(95, 131, 194, 0.6);
}

.display-queue { flex: 1; }
.queue-row { display: grid; grid-template-columns: 130px 1fr 160px; gap: 12px; padding: 10px 8px; color: #fff; font-size: 26px; font-weight: 700; border-bottom: 1px solid rgba(255,255,255,0.12); align-items: center; }
.queue-row--head { color: #9fc2ff; font-size: 16px; font-weight: 800; letter-spacing: 0.08em; border-bottom: 2px solid #5f83c2; }
.queue-row--empty { color: #93a4c4; font-size: 22px; justify-content: center; display: flex; padding: 30px 0; }
.col-no { font-variant-numeric: tabular-nums; }
.col-name { text-transform: uppercase; }
.col-mr { color: #9fc2ff; font-size: 20px; }

.queue-pager { display: flex; align-items: center; justify-content: center; gap: 14px; margin-top: 14px; color: #fff; font-size: 18px; font-weight: 700; }
.page-btn { padding: 6px 16px; border-radius: 8px; border: 1px solid #5f83c2; background: rgba(95,131,194,0.25); color: #fff; cursor: pointer; font-weight: 800; font-size: 16px; }
.page-btn:disabled { opacity: 0.4; cursor: default; }

.display-empty { flex: 1; display: flex; align-items: center; justify-content: center; }
.display-empty-text { color: #93a4c4; font-size: 40px; font-weight: 900; letter-spacing: 0.15em; animation: blink 1.6s ease-in-out infinite; }
@keyframes blink { 0%, 100% { opacity: 1; } 50% { opacity: 0.25; } }

.display-marquee { overflow: hidden; background: rgba(0,0,0,0.35); border-radius: 10px; padding: 10px 0; }
.marquee-track {
  display: inline-block;
  white-space: nowrap;
  color: #ffe082;
  font-size: 24px;
  font-weight: 800;
  padding-left: 100%;
  animation: marquee 20s linear infinite;
}
@keyframes marquee { 0% { transform: translateX(0); } 100% { transform: translateX(-100%); } }

.action-bar { display: flex; flex-wrap: wrap; gap: 10px; justify-content: center; margin-top: 14px; }
.small-button { padding: 8px 14px; border-radius: 8px; border: 1px solid #d1d5db; background: #fff; cursor: pointer; font-weight: 600; font-size: 13px; }

@media (max-width: 960px) {
  .display-hospital { font-size: 20px; }
  .display-doctor { font-size: 26px; }
  .queue-row { font-size: 18px; grid-template-columns: 90px 1fr 110px; }
}
</style>
