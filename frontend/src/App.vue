<script setup>
import { computed, onMounted, ref } from 'vue';
import LoginScreen from './components/LoginScreen.vue';
import MainLayout from './components/MainLayout.vue';

const API_BASE_URL = (import.meta.env.VITE_API_BASE_URL || '/api').replace(/\/$/, '');

const loading = ref(true);
const authenticating = ref(false);
const systemInfo = ref(null);
const loadError = ref('');
const authError = ref('');
const sessionMessage = ref('');
const activeUser = ref(null);

const migrationHighlights = computed(() => {
  if (!systemInfo.value) {
    return [];
  }

  return [
    `${systemInfo.value.legacyUiPages} halaman legacy ZK terpetakan`,
    `${systemInfo.value.legacyHibernateMappings} mapping Hibernate existing dipertahankan`,
    `${systemInfo.value.legacyUiControllers} controller UI legacy terinventarisasi`,
    systemInfo.value.databaseStrategy
  ];
});

const shellVisible = computed(() => Boolean(activeUser.value));

async function loadSystemInfo() {
  loading.value = true;

  try {
    const response = await fetch(`${API_BASE_URL}/system/info`);
    if (!response.ok) {
      throw new Error(`HTTP ${response.status}`);
    }

    const payload = await response.json();
    systemInfo.value = payload.data;
  } catch (error) {
    loadError.value = 'Backend scaffold belum merespons. Jalankan backend Spring Boot di port 9090.';
  } finally {
    loading.value = false;
  }
}

async function parseResponse(response) {
  const payload = await response.json().catch(() => null);
  return payload;
}

async function loadSession() {
  try {
    const response = await fetch(`${API_BASE_URL}/auth/me`, {
      credentials: 'include'
    });

    if (response.status === 401) {
      const payload = await parseResponse(response);
      activeUser.value = null;
      sessionMessage.value = payload?.message || '';
      return;
    }

    if (!response.ok) {
      throw new Error(`HTTP ${response.status}`);
    }

    const payload = await parseResponse(response);
    activeUser.value = payload.data;
    sessionMessage.value = '';
  } catch (error) {
    if (!loadError.value) {
      loadError.value = 'Backend auth belum merespons. Pastikan backend Spring Boot aktif.';
    }
  }
}

async function login(credentials) {
  authenticating.value = true;
  authError.value = '';
  sessionMessage.value = '';

  try {
    const response = await fetch(`${API_BASE_URL}/auth/login`, {
      method: 'POST',
      credentials: 'include',
      headers: {
        'Content-Type': 'application/json'
      },
      body: JSON.stringify(credentials)
    });

    const payload = await parseResponse(response);

    if (response.status === 401) {
      authError.value = payload?.message || 'login.invalid';
      return;
    }

    if (!response.ok) {
      throw new Error(`HTTP ${response.status}`);
    }

    activeUser.value = payload.data;
  } catch (error) {
    authError.value = 'Login belum bisa diproses karena backend tidak merespons.';
  } finally {
    authenticating.value = false;
  }
}

async function logout() {
  try {
    await fetch(`${API_BASE_URL}/auth/logout`, {
      method: 'POST',
      credentials: 'include'
    });
  } finally {
    activeUser.value = null;
    authError.value = '';
    sessionMessage.value = 'Sesi login telah ditutup.';
  }
}

function handleSessionExpired(message) {
  activeUser.value = null;
  authError.value = '';
  sessionMessage.value = message || 'Your session has been expired. You need to login again.';
}

onMounted(() => {
  loadSystemInfo();
  loadSession();
});
</script>

<template>
  <MainLayout
    v-if="shellVisible"
    :api-base-url="API_BASE_URL"
    :active-user="activeUser"
    :system-info="systemInfo"
    @logout="logout"
    @session-expired="handleSessionExpired"
  />
  <LoginScreen
    v-else
    :auth-error="authError"
    :authenticating="authenticating"
    :loading="loading"
    :load-error="loadError"
    :session-message="sessionMessage"
    :system-info="systemInfo"
    :migration-highlights="migrationHighlights"
    @login="login"
    @retry="loadSystemInfo"
  />
</template>
