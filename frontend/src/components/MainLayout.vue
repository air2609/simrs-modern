<script setup>
import { computed, ref } from 'vue';
import AdmissionRegistrationSection from './AdmissionRegistrationSection.vue';
import ApotikSection from './ApotikSection.vue';
import LocationMasterSection from './LocationMasterSection.vue';
import PolyclinicSection from './PolyclinicSection.vue';

const props = defineProps({
  activeUser: {
    type: Object,
    required: true
  },
  apiBaseUrl: {
    type: String,
    required: true
  },
  systemInfo: {
    type: Object,
    default: null
  }
});

const emit = defineEmits(['logout', 'session-expired']);

const selectedView = ref('overview');
const locationScreenCodes = ['SCM0013', 'SCM0014', 'SCM0015', 'SCM0016'];
const apotikScreenCode = 'SC0011';
const polyclinicScreenCode = 'SC0091';

const admissionModule = computed(() => {
  return props.activeUser.modules.find((moduleItem) => {
    return /adm|admission|admi/i.test(moduleItem.moduleCode || '') || /admisi|pendaftaran/i.test(moduleItem.moduleName || '');
  }) || null;
});

const hasLocationMasterAccess = computed(() => {
  return props.activeUser.modules.some((moduleItem) => {
    return moduleItem.screens.some((screen) => locationScreenCodes.includes(screen.screenCode));
  });
});

const hasPolyclinicAccess = computed(() => {
  return props.activeUser.modules.some((moduleItem) => {
    return moduleItem.screens.some((screen) => screen.screenCode === polyclinicScreenCode);
  });
});

const hasApotikAccess = computed(() => {
  return props.activeUser.modules.some((moduleItem) => {
    return moduleItem.screens.some((screen) => screen.screenCode === apotikScreenCode);
  });
});

const apotikUnits = computed(() => {
  const seenUnitIds = new Set();
  const units = [];

  props.activeUser.modules.forEach((moduleItem) => {
    moduleItem.screens.forEach((screen) => {
      if (screen.screenCode !== apotikScreenCode) {
        return;
      }

      (screen.units || []).forEach((unit) => {
        const unitId = String(unit.unitId ?? '');
        if (!unitId || unit.warehouseId == null || seenUnitIds.has(unitId)) {
          return;
        }

        seenUnitIds.add(unitId);
        units.push(unit);
      });
    });
  });

  return units;
});
</script>

<template>
  <div class="shell-page">
    <aside class="sidebar">
      <div>
        <p class="eyebrow">SIMRS</p>
        <h1>{{ activeUser.fullName || activeUser.username }}</h1>
        <p class="sidebar-copy">
          Login tersambung ke user existing dan session browser sekarang meniru pola aplikasi lama: login, buka shell, lalu timeout/logout kembali ke login.
        </p>
      </div>

      <div class="module-groups">
        <section class="group-card">
          <h2>Session</h2>
          <ul>
            <li>Username: {{ activeUser.username }}</li>
            <li>Group ID: {{ activeUser.groupId ?? '-' }}</li>
            <li>Branch ID: {{ activeUser.branchId ?? '-' }}</li>
          </ul>
        </section>

        <section class="group-card">
          <h2>Navigasi</h2>
          <div class="nav-stack">
            <button class="nav-button" type="button" @click="selectedView = 'overview'">Overview</button>
            <button class="nav-button" type="button" @click="selectedView = 'admission-registration'">
              Pendaftaran Rawat Jalan
            </button>
            <button v-if="hasApotikAccess" class="nav-button" type="button" @click="selectedView = 'apotik'">
              Transaksi Apotik
            </button>
            <button v-if="hasPolyclinicAccess" class="nav-button" type="button" @click="selectedView = 'polyclinic'">
              Transaksi Poliklinik
            </button>
            <button v-if="hasLocationMasterAccess" class="nav-button" type="button" @click="selectedView = 'location-master'">
              Master Wilayah
            </button>
          </div>
        </section>

        <section v-for="moduleItem in activeUser.modules" :key="moduleItem.moduleId" class="group-card">
          <h2>{{ moduleItem.moduleName }}</h2>
          <ul>
            <li v-for="screen in moduleItem.screens.slice(0, 6)" :key="screen.screenId">
              {{ screen.screenName }} <span class="access-pill">{{ screen.accessType }}</span>
            </li>
          </ul>
        </section>
      </div>

      <button class="back-button" type="button" @click="$emit('logout')">
        Logout
      </button>
    </aside>

    <main class="content">
      <template v-if="selectedView === 'overview'">
      <header class="hero-card">
        <p class="hero-kicker">Rewrite Strategy</p>
        <h2>Database-first, module-by-module migration</h2>
        <p>
          Session ini disimpan di server dan user diambil langsung dari `ms_user` existing dengan password MD5 legacy, username uppercase,
          serta privilege modul/screen yang diwarisi dari tabel privilege lama.
        </p>
      </header>

      <section class="metrics-grid">
        <article class="metric-card">
          <span class="metric-label">User login</span>
          <strong>{{ activeUser.username }}</strong>
        </article>
        <article class="metric-card">
          <span class="metric-label">Nama user</span>
          <strong>{{ activeUser.fullName || '-' }}</strong>
        </article>
        <article class="metric-card">
          <span class="metric-label">Legacy modules</span>
          <strong>{{ activeUser.modules.length }} modul</strong>
        </article>
        <article class="metric-card">
          <span class="metric-label">Backend target</span>
          <strong>{{ props.systemInfo?.backendStack || 'Spring Boot 2.7 + Java 8' }}</strong>
        </article>
      </section>

      <section class="roadmap-card">
        <h3>Module privilege preview</h3>
        <div class="screen-grid">
          <article v-for="moduleItem in activeUser.modules" :key="moduleItem.moduleCode" class="screen-card">
            <h4>{{ moduleItem.moduleName }}</h4>
            <p class="screen-code">{{ moduleItem.moduleCode }}</p>
            <ul>
              <li v-for="screen in moduleItem.screens" :key="screen.screenCode">
                <strong>{{ screen.screenCode }}</strong> - {{ screen.screenName }}
                <span v-if="screen.units.length">({{ screen.units.length }} unit)</span>
              </li>
            </ul>
          </article>
        </div>
      </section>
      </template>

      <AdmissionRegistrationSection
        v-else-if="selectedView === 'admission-registration'"
        :api-base-url="apiBaseUrl"
        @session-expired="emit('session-expired', $event)"
      />

      <ApotikSection
        v-else-if="selectedView === 'apotik'"
        :api-base-url="apiBaseUrl"
        :available-units="apotikUnits"
        @session-expired="emit('session-expired', $event)"
      />

      <PolyclinicSection
        v-else-if="selectedView === 'polyclinic'"
        :api-base-url="apiBaseUrl"
        @session-expired="emit('session-expired', $event)"
      />

      <LocationMasterSection
        v-else
        :api-base-url="apiBaseUrl"
        @session-expired="emit('session-expired', $event)"
      />
    </main>
  </div>
</template>

<style scoped>
.shell-page {
  min-height: 100vh;
  display: grid;
  grid-template-columns: 320px 1fr;
  background: linear-gradient(135deg, #f1efe9 0%, #edf2fa 100%);
}

.sidebar {
  display: flex;
  flex-direction: column;
  justify-content: space-between;
  gap: 24px;
  padding: 28px;
  background: #304b73;
  color: #fff;
}

.eyebrow,
.hero-kicker,
.metric-label {
  text-transform: uppercase;
  letter-spacing: 0.08em;
  font-size: 12px;
}

.sidebar h1,
.hero-card h2,
.roadmap-card h3 {
  margin: 0;
}

.sidebar-copy {
  margin: 12px 0 0;
  color: rgba(255, 255, 255, 0.88);
}

.module-groups {
  display: grid;
  gap: 14px;
}

.group-card {
  padding: 14px;
  border: 1px solid rgba(255, 255, 255, 0.16);
  background: rgba(255, 255, 255, 0.08);
}

.group-card h2 {
  margin: 0 0 10px;
  font-size: 16px;
}

.group-card ul {
  margin: 0;
  padding-left: 18px;
}

.group-card li + li {
  margin-top: 6px;
}

.nav-stack {
  display: grid;
  gap: 10px;
}

.nav-button {
  min-height: 36px;
  border: 1px solid rgba(255, 255, 255, 0.2);
  background: rgba(255, 255, 255, 0.12);
  color: #fff;
  font-weight: 700;
  cursor: pointer;
}

.access-pill {
  display: inline-block;
  margin-left: 6px;
  padding: 2px 6px;
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.18);
  font-size: 11px;
}

.back-button {
  height: 42px;
  border: 0;
  background: #5f83c2;
  color: #fff;
  font-weight: 700;
  cursor: pointer;
}

.content {
  padding: 32px;
}

.hero-card,
.metric-card,
.roadmap-card {
  background: rgba(255, 255, 255, 0.94);
  border: 1px solid rgba(150, 136, 117, 0.35);
  box-shadow: 0 12px 24px rgba(53, 64, 84, 0.08);
}

.hero-card {
  padding: 28px;
}

.hero-card p:last-child {
  margin-bottom: 0;
}

.metrics-grid {
  margin-top: 20px;
  display: grid;
  grid-template-columns: repeat(2, minmax(220px, 1fr));
  gap: 16px;
}

.metric-card {
  padding: 20px;
}

.metric-card strong {
  display: block;
  margin-top: 8px;
  font-size: 20px;
  color: #304b73;
}

.roadmap-card {
  margin-top: 20px;
  padding: 24px;
}

.roadmap-card ol {
  margin: 14px 0 0;
  padding-left: 20px;
}

.roadmap-card li + li {
  margin-top: 8px;
}

.screen-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(260px, 1fr));
  gap: 16px;
  margin-top: 16px;
}

.screen-card {
  padding: 16px;
  border: 1px solid rgba(150, 136, 117, 0.25);
  background: #fff;
}

.screen-card h4,
.screen-card p,
.screen-card ul {
  margin-top: 0;
}

.screen-card ul {
  margin-bottom: 0;
  padding-left: 18px;
}

.screen-card li + li {
  margin-top: 8px;
}

.screen-code {
  color: #5f83c2;
  font-weight: 700;
}

@media (max-width: 960px) {
  .shell-page {
    grid-template-columns: 1fr;
  }

  .metrics-grid {
    grid-template-columns: 1fr;
  }

  .screen-grid {
    grid-template-columns: 1fr;
  }
}
</style>
