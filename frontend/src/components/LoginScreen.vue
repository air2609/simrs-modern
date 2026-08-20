<script setup>
import { computed, reactive } from 'vue';

const props = defineProps({
  authenticating: {
    type: Boolean,
    required: true
  },
  loading: {
    type: Boolean,
    required: true
  },
  loadError: {
    type: String,
    required: true
  },
  sessionMessage: {
    type: String,
    required: true
  },
  authError: {
    type: String,
    required: true
  },
  systemInfo: {
    type: Object,
    default: null
  },
  migrationHighlights: {
    type: Array,
    required: true
  }
});

const emit = defineEmits(['login', 'retry']);

const form = reactive({
  username: '',
  password: ''
});

const canPreview = computed(() => Boolean(props.systemInfo) && !props.loading && !props.authenticating);

function submit() {
  if (!canPreview.value) {
    return;
  }

  emit('login', {
    username: form.username,
    password: form.password
  });
}

function reset() {
  form.username = '';
  form.password = '';
}

function normalizeUsername(event) {
  form.username = event.target.value.toUpperCase();
}
</script>

<template>
  <main class="login-page">
    <div class="bg-image" />
    <div class="bg-overlay" />

    <section class="login-wrap">
      <div class="brand-side">
        <div class="brand-logo">
          <span class="logo-mark">🏥</span>
        </div>
        <h1 class="brand-name">MEDISAFE</h1>
        <p class="brand-sub">Sistem Informasi Manajemen Rumah Sakit</p>

        <div class="motto-card">
          <p class="motto-text">
            “Satu Data, Satu Sistem —
            <span class="motto-highlight">Pelayanan Sehat, Aman &amp; Terpercaya</span>”
          </p>
        </div>

        <ul class="brand-points">
          <li>🩺 Manajemen Pelayanan Pasien Terpadu</li>
          <li>💊 Rekam Medis &amp; Apotek Terintegrasi</li>
          <li>📊 Laporan Lengkap &amp; Real-time</li>
          <li>🔒 Keamanan Data Terjamin</li>
        </ul>
      </div>

      <div class="login-card">
        <div class="card-header">
          <span class="card-icon">🔐</span>
          <h2>Masuk ke Sistem</h2>
          <p>Silakan masukkan username dan password Anda</p>
        </div>

        <div class="field-group">
          <label class="field-label" for="username">Username</label>
          <div class="input-wrap">
            <span class="input-icon">👤</span>
            <input
              id="username"
              v-model="form.username"
              class="modern-input"
              type="text"
              autocomplete="username"
              placeholder="Masukkan username"
              @input="normalizeUsername"
              @keyup.enter="submit"
            />
          </div>
        </div>

        <div class="field-group">
          <label class="field-label" for="password">Password</label>
          <div class="input-wrap">
            <span class="input-icon">🔑</span>
            <input
              id="password"
              v-model="form.password"
              class="modern-input"
              type="password"
              autocomplete="current-password"
              placeholder="Masukkan password"
              @keyup.enter="submit"
            />
          </div>
        </div>

        <div class="button-row">
          <button class="login-button" type="button" :disabled="!canPreview" @click="submit">
            {{ authenticating ? 'Memproses...' : 'Masuk' }}
          </button>
          <button class="reset-button" type="button" @click="reset">
            Reset
          </button>
        </div>

        <div class="status-area">
          <p v-if="loading" class="status-pill">
            ⏳ Memeriksa backend scaffold...
          </p>
          <p v-else-if="sessionMessage" class="status-pill">
            {{ sessionMessage }}
          </p>
          <p v-else-if="authError" class="status-pill status-pill--error">
            {{ authError }}
          </p>
          <p v-else-if="loadError" class="status-pill status-pill--error">
            {{ loadError }}
          </p>
          <p v-else class="status-pill status-pill--success">
            ✅ {{ systemInfo.applicationName }} siap digunakan.
          </p>

          <button
            v-if="loadError"
            class="retry-button"
            type="button"
            @click="emit('retry')"
          >
            🔄 Coba Lagi
          </button>
        </div>
      </div>
    </section>

    <footer class="login-footer">
      <span>MEDISAFE — Aman, Cepat, Akurat</span>
    </footer>
  </main>
</template>

<style scoped>
.login-page {
  min-height: 100vh;
  position: relative;
  overflow: hidden;
  display: flex;
  flex-direction: column;
  justify-content: center;
  font-family: 'Segoe UI', 'Helvetica Neue', Arial, sans-serif;
}

/* ===== Background (custom SIMRS illustration) + overlay ===== */
.bg-image {
  position: absolute;
  inset: 0;
  background-image: url('/images/simrs-bg.svg');
  background-position: center center;
  background-repeat: no-repeat;
  background-size: cover;
}

.bg-overlay {
  position: absolute;
  inset: 0;
  background: linear-gradient(100deg, rgba(10, 28, 62, 0.55) 0%, rgba(13, 42, 92, 0.28) 45%, rgba(13, 95, 122, 0.12) 100%);
}

/* ===== Layout ===== */
.login-wrap {
  position: relative;
  z-index: 2;
  width: min(1080px, 94vw);
  margin: 0 auto;
  display: grid;
  grid-template-columns: 1.05fr 0.95fr;
  gap: 44px;
  align-items: center;
  padding: 40px 0;
}

/* ===== Brand side ===== */
.brand-side {
  color: #fff;
  animation: fade-up 0.7s ease both;
}

.brand-logo {
  display: flex;
  align-items: center;
  gap: 14px;
  margin-bottom: 10px;
}

.logo-mark {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 62px;
  height: 62px;
  border-radius: 16px;
  background: linear-gradient(135deg, #2f86eb, #1f5fae);
  box-shadow: 0 10px 26px rgba(15, 35, 80, 0.55);
  font-size: 34px;
}

.brand-name {
  margin: 0;
  font-size: 46px;
  letter-spacing: 0.06em;
  font-weight: 900;
  text-shadow: 0 4px 18px rgba(0, 0, 0, 0.35);
}

.brand-sub {
  margin: 6px 0 0;
  font-size: 16px;
  font-weight: 600;
  opacity: 0.9;
  letter-spacing: 0.02em;
}

.motto-card {
  margin-top: 26px;
  padding: 18px 22px;
  border-left: 5px solid #6cc5ff;
  background: rgba(255, 255, 255, 0.12);
  border-radius: 0 14px 14px 0;
  backdrop-filter: blur(6px);
  max-width: 460px;
}

.motto-text {
  margin: 0;
  font-size: 18px;
  line-height: 1.6;
  font-weight: 600;
}

.motto-highlight {
  color: #ffd76a;
}

.brand-points {
  list-style: none;
  margin: 22px 0 0;
  padding: 0;
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 10px 18px;
  max-width: 480px;
}

.brand-points li {
  font-size: 14px;
  font-weight: 600;
  opacity: 0.92;
}

/* ===== Login card ===== */
.login-card {
  background: rgba(255, 255, 255, 0.97);
  border-radius: 22px;
  padding: 34px 36px;
  box-shadow: 0 30px 70px rgba(8, 22, 52, 0.45);
  animation: fade-up 0.7s 0.12s ease both;
}

.card-header {
  text-align: center;
  margin-bottom: 22px;
}

.card-icon {
  font-size: 34px;
  display: inline-block;
}

.card-header h2 {
  margin: 8px 0 4px;
  color: #1d3a6b;
  font-size: 22px;
  font-weight: 800;
}

.card-header p {
  margin: 0;
  color: #7c8799;
  font-size: 13px;
}

.field-group {
  margin-bottom: 16px;
}

.field-label {
  display: block;
  margin-bottom: 6px;
  font-size: 13px;
  font-weight: 700;
  color: #2b436e;
}

.input-wrap {
  position: relative;
  display: flex;
  align-items: center;
}

.input-icon {
  position: absolute;
  left: 12px;
  font-size: 15px;
  opacity: 0.75;
  pointer-events: none;
}

.modern-input {
  width: 100%;
  box-sizing: border-box;
  padding: 12px 14px 12px 38px;
  border: 1.5px solid #d6dfea;
  border-radius: 10px;
  font-size: 14px;
  color: #22324d;
  background: #f7f9fc;
  transition: border-color 0.15s, box-shadow 0.15s;
}

.modern-input:focus {
  outline: none;
  border-color: #2f6fd0;
  background: #fff;
  box-shadow: 0 0 0 3px rgba(47, 111, 208, 0.16);
}

.button-row {
  display: flex;
  gap: 12px;
  margin-top: 22px;
}

.login-button,
.reset-button {
  flex: 1;
  padding: 13px 0;
  border: none;
  border-radius: 10px;
  font-size: 15px;
  font-weight: 800;
  cursor: pointer;
  transition: transform 0.12s, box-shadow 0.15s, background 0.15s;
}

.login-button {
  background: linear-gradient(135deg, #2f86eb, #1d5fae);
  color: #fff;
  box-shadow: 0 10px 22px rgba(31, 95, 174, 0.35);
}

.login-button:hover:not(:disabled) {
  transform: translateY(-1px);
  box-shadow: 0 14px 28px rgba(31, 95, 174, 0.42);
}

.login-button:disabled {
  opacity: 0.55;
  cursor: not-allowed;
}

.reset-button {
  background: #eef2f7;
  color: #3c4d6b;
  border: 1px solid #d8e0ea;
}

.reset-button:hover {
  background: #e4eaf2;
}

.status-area {
  margin-top: 18px;
  min-height: 44px;
}

.status-pill {
  margin: 0;
  padding: 10px 12px;
  border-radius: 10px;
  background: #eef3fb;
  border-left: 4px solid #5f83c2;
  font-size: 13px;
  font-weight: 600;
  color: #2b436e;
}

.status-pill--error {
  background: #fff2f2;
  border-left-color: #d64567;
  color: #a32943;
}

.status-pill--success {
  background: #edf8ef;
  border-left-color: #2f9e63;
  color: #1f6e46;
}

.retry-button {
  margin-top: 10px;
  padding: 9px 18px;
  border: none;
  border-radius: 8px;
  background: #d64567;
  color: #fff;
  font-weight: 700;
  cursor: pointer;
}

/* ===== Footer ===== */
.login-footer {
  position: relative;
  z-index: 2;
  text-align: center;
  color: rgba(255, 255, 255, 0.82);
  font-size: 13px;
  font-weight: 600;
  padding-bottom: 26px;
  letter-spacing: 0.02em;
}

@keyframes fade-up {
  from {
    opacity: 0;
    transform: translateY(22px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

@media (max-width: 900px) {
  .login-wrap {
    grid-template-columns: 1fr;
    gap: 26px;
    padding: 26px 0;
  }

  .brand-name {
    font-size: 34px;
  }

  .brand-points {
    grid-template-columns: 1fr;
  }
}
</style>
