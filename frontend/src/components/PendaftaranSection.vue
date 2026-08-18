<script setup>
import { ref } from 'vue';
import AdmissionRegistrationSection from './AdmissionRegistrationSection.vue';
import RanapRegistrationSection from './RanapRegistrationSection.vue';

const props = defineProps({
  apiBaseUrl: { type: String, required: true }
});

const emit = defineEmits(['session-expired', 'close']);

const activeTab = ref('rajal');
</script>

<template>
  <div class="screen-page">
    <div class="page-header">
      <h2>📋 PENDAFTARAN PASIEN</h2>
      <p class="page-subtitle">Migrasi screen legacy SC0001 — Pendaftaran.zul (Pendaftaran Rawat Jalan &amp; Rawat Inap)</p>
    </div>

    <div class="card">
      <div class="tabs">
        <button class="tab" :class="{ active: activeTab === 'rajal' }" type="button" @click="activeTab = 'rajal'">🩺 PENDAFTARAN RAWAT JALAN</button>
        <button class="tab" :class="{ active: activeTab === 'ranap' }" type="button" @click="activeTab = 'ranap'">🛏️ PENDAFTARAN RAWAT INAP</button>
      </div>

      <AdmissionRegistrationSection
        v-if="activeTab === 'rajal'"
        :api-base-url="apiBaseUrl"
        @session-expired="emit('session-expired', $event)"
      />

      <RanapRegistrationSection
        v-else
        :api-base-url="apiBaseUrl"
        @session-expired="emit('session-expired', $event)"
      />

      <div class="action-bar">
        <button class="small-button" type="button" @click="emit('close')">✅ SELESAI</button>
      </div>
    </div>
  </div>
</template>

<style scoped>
.screen-page { padding: 16px; }
.page-header { margin-bottom: 16px; display: flex; flex-direction: column; gap: 4px; }
.page-header h2 { margin: 0; color: #304b73; font-size: 20px; }
.page-subtitle { margin: 0; color: #6b7280; font-size: 14px; }

.card { background: #fff; border-radius: 12px; padding: 16px; box-shadow: 0 1px 3px rgba(0,0,0,0.08); margin-bottom: 16px; }

.tabs { display: flex; gap: 8px; margin-bottom: 16px; border-bottom: 2px solid #eef2f7; padding-bottom: 8px; flex-wrap: wrap; }
.tab { padding: 8px 16px; border: 1px solid #d1d9e6; border-radius: 8px; background: #fff; cursor: pointer; font-weight: 700; font-size: 13px; color: #304b73; }
.tab.active { background: #304b73; color: #fff; border-color: #304b73; }

.action-bar { display: flex; flex-wrap: wrap; gap: 10px; justify-content: center; margin-top: 12px; }
.small-button { padding: 8px 14px; border-radius: 8px; border: 1px solid #d1d5db; background: #fff; cursor: pointer; font-weight: 600; font-size: 13px; }
</style>
