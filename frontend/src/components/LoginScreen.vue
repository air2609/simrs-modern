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
    <section class="login-overlay">
      <div class="login-panel">
        <div class="login-form-card">
          <div class="legacy-spacer" />
          <label class="field-label" for="username">Username</label>
          <input
            id="username"
            v-model="form.username"
            class="legacy-input"
            type="text"
            autocomplete="username"
            @input="normalizeUsername"
            @keyup.enter="submit"
          />

          <label class="field-label" for="password">Password</label>
          <input
            id="password"
            v-model="form.password"
            class="legacy-input"
            type="password"
            autocomplete="current-password"
            @keyup.enter="submit"
          />

          <div class="button-row">
            <button class="legacy-button" type="button" :disabled="!canPreview" @click="submit">
              {{ authenticating ? 'Loading...' : 'Login' }}
            </button>
            <button class="legacy-button" type="button" @click="reset">
              Reset
            </button>
          </div>
        </div>

        <aside class="status-card">
          <h1>SIMRS Modern Scaffold</h1>
          <p class="status-text">
            Tampilan login ini sengaja dibuat mengikuti pola visual layar lama sebagai pijakan parity UI.
          </p>

          <p v-if="loading" class="status-pill">
            Memeriksa backend scaffold...
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
            {{ systemInfo.applicationName }} siap dipakai sebagai fondasi rewrite.
          </p>

          <ul v-if="migrationHighlights.length" class="highlight-list">
            <li v-for="item in migrationHighlights" :key="item">{{ item }}</li>
          </ul>

          <button
            v-if="loadError"
            class="secondary-button"
            type="button"
            @click="emit('retry')"
          >
            Coba Lagi
          </button>
        </aside>
      </div>
    </section>
  </main>
</template>

<style scoped>
.login-page {
  min-height: 100vh;
  background-image: url('/legacy/images/TS1.jpg');
  background-position: center center;
  background-repeat: no-repeat;
  background-size: cover;
}

.login-overlay {
  min-height: 100vh;
  background: rgba(242, 241, 238, 0.15);
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 24px;
}

.login-panel {
  width: min(1120px, 100%);
  display: grid;
  grid-template-columns: minmax(320px, 420px) minmax(320px, 480px);
  gap: 28px;
  align-items: center;
}

.login-form-card {
  display: flex;
  flex-direction: column;
  align-items: center;
  min-height: 440px;
  padding-top: 300px;
}

.legacy-spacer {
  height: 12px;
}

.field-label {
  margin-bottom: 8px;
  font-size: 15px;
  font-weight: 700;
  color: #222;
}

.legacy-input {
  width: 180px;
  height: 20px;
  margin-bottom: 18px;
  padding: 0 8px;
  border: 1px solid #968875;
  background: #f2f1ee;
  font-size: 8pt;
  font-weight: 700;
  color: #222;
}

.button-row {
  display: flex;
  gap: 12px;
  margin-top: 6px;
}

.legacy-button,
.secondary-button {
  min-width: 80px;
  height: 32px;
  border: 1px solid #4d6ba0;
  background: #5f83c2;
  color: #fff;
  font-size: 10pt;
  font-weight: 700;
  cursor: pointer;
}

.legacy-button:disabled {
  opacity: 0.7;
  cursor: not-allowed;
}

.secondary-button {
  background: #6f6b64;
  border-color: #6f6b64;
}

.status-card {
  padding: 28px;
  background: rgba(255, 255, 255, 0.9);
  border: 1px solid rgba(150, 136, 117, 0.75);
  box-shadow: 0 18px 38px rgba(0, 0, 0, 0.16);
}

.status-card h1 {
  margin: 0 0 10px;
  color: #968875;
  font-size: 28px;
}

.status-text {
  margin: 0 0 16px;
}

.status-pill {
  margin: 0 0 18px;
  padding: 10px 12px;
  background: #eef3fb;
  border-left: 4px solid #5f83c2;
}

.status-pill--error {
  background: #fff2f2;
  border-left-color: #b53e3e;
}

.status-pill--success {
  background: #edf8ef;
  border-left-color: #4f8c5c;
}

.highlight-list {
  margin: 0 0 18px;
  padding-left: 18px;
}

.highlight-list li + li {
  margin-top: 8px;
}

@media (max-width: 900px) {
  .login-panel {
    grid-template-columns: 1fr;
  }

  .login-form-card {
    min-height: 380px;
    padding-top: 220px;
  }
}
</style>
