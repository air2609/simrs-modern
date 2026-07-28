<script setup>
import { computed, ref } from 'vue';
import AdmissionRegistrationSection from './AdmissionRegistrationSection.vue';
import ApotikSection from './ApotikSection.vue';
import JournalSection from './JournalSection.vue';
import LocationMasterSection from './LocationMasterSection.vue';
import LabTreatmentMasterSection from './LabTreatmentMasterSection.vue';
import LaboratoryTransactionSection from './LaboratoryTransactionSection.vue';
import LaboratoryResultSection from './LaboratoryResultSection.vue';
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
const expandedModules = ref({});

// Map screen codes to Vue component view names
const screenViewMap = {
  'SC0011': 'apotik',
  'SC0091': 'polyclinic',
  'SC0041': 'laboratory-transaction',
  'SC0043': 'laboratory-result',
  'SC0201': 'journal',
  'SCM0051': 'lab-treatment-master',
  'SCM0013': 'location-master',
  'SCM0014': 'location-master',
  'SCM0015': 'location-master',
  'SCM0016': 'location-master'
};

// Extra menu items not mapped from user modules
const extraMenus = [
  { view: 'overview', label: '🏠 Overview' },
  { view: 'admission-registration', label: '📋 Pendaftaran Rawat Jalan' }
];

// Module expansion toggle (accordion style)
function toggleModule(moduleCode) {
  expandedModules.value[moduleCode] = !expandedModules.value[moduleCode];
}

function isModuleExpanded(moduleCode) {
  return expandedModules.value[moduleCode] === true;
}

// Navigate to a view
function navigate(view) {
  selectedView.value = view;
}

// Get a friendly icon/emoji for known screen codes
function screenIcon(screenCode) {
  const icons = {
    'SC0011': '💊',
    'SC0091': '🩺',
    'SC0041': '🧪',
    'SC0043': '📊',
    'SC0201': '📒',
    'SCM0051': '🔬',
    'SCM0013': '📍',
    'SCM0014': '📍',
    'SCM0015': '📍',
    'SCM0016': '📍'
  };
  return icons[screenCode] || '📄';
}

// Apotik units (extracted from user modules)
const apotikUnits = computed(() => {
  const seenUnitIds = new Set();
  const units = [];
  props.activeUser.modules.forEach((moduleItem) => {
    moduleItem.screens.forEach((screen) => {
      if (screen.screenCode !== 'SC0011') return;
      (screen.units || []).forEach((unit) => {
        const unitId = String(unit.unitId ?? '');
        if (!unitId || unit.warehouseId == null || seenUnitIds.has(unitId)) return;
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
      </div>

      <!-- Accordion Menu seperti legacy -->
      <nav class="accordion-menu">
        <!-- Extra static menus -->
        <div class="accordion-group">
          <div class="accordion-header static" @click="navigate('overview')">
            <span>🏠 Overview</span>
          </div>
        </div>

        <div class="accordion-group">
          <div class="accordion-header static" @click="navigate('admission-registration')">
            <span>📋 Pendaftaran Rawat Jalan</span>
          </div>
        </div>

        <!-- Dynamic modules from user access -->
        <div v-for="mod in activeUser.modules" :key="mod.moduleCode" class="accordion-group">
          <div class="accordion-header" :class="{ expanded: isModuleExpanded(mod.moduleCode) }" @click="toggleModule(mod.moduleCode)">
            <span class="accordion-title">{{ mod.moduleName }}</span>
            <span class="accordion-arrow">{{ isModuleExpanded(mod.moduleCode) ? '▾' : '▸' }}</span>
          </div>
          <div v-if="isModuleExpanded(mod.moduleCode)" class="accordion-body">
            <div v-for="screen in mod.screens" :key="screen.screenCode" class="accordion-item"
              :class="{ active: selectedView === screenViewMap[screen.screenCode] }"
              @click="screenViewMap[screen.screenCode] ? navigate(screenViewMap[screen.screenCode]) : null">
              <span class="item-icon">{{ screenIcon(screen.screenCode) }}</span>
              <span class="item-label">{{ screen.screenName }}</span>
              <span class="item-code">{{ screen.screenCode }}</span>
            </div>
          </div>
        </div>

      </nav>

      <!-- Logout -->
      <div class="sidebar-footer">
        <button class="logout-button" type="button" @click="emit('logout')">
          🚪 Logout
        </button>
      </div>
    </aside>

    <main class="content">
      <template v-if="selectedView === 'overview'">
        <div class="hero-card">
          <p class="hero-kicker">SIMRS Modern Scaffold</p>
          <h2>{{ activeUser.fullName || activeUser.username }}</h2>
          <p>Login tersambung ke user existing. Klik menu di samping untuk membuka modul.</p>
        </div>

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
            <span class="metric-label">Group ID</span>
            <strong>{{ activeUser.groupId ?? '-' }}</strong>
          </article>
          <article class="metric-card">
            <span class="metric-label">Branch ID</span>
            <strong>{{ activeUser.branchId ?? '-' }}</strong>
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

      <JournalSection
        v-else-if="selectedView === 'journal'"
        :api-base-url="apiBaseUrl"
        @session-expired="emit('session-expired', $event)"
      />

      <LabTreatmentMasterSection
        v-else-if="selectedView === 'lab-treatment-master'"
        :api-base-url="apiBaseUrl"
        @session-expired="emit('session-expired', $event)"
      />

      <LaboratoryTransactionSection
        v-else-if="selectedView === 'laboratory-transaction'"
        :api-base-url="apiBaseUrl"
        @session-expired="emit('session-expired', $event)"
      />

      <LaboratoryResultSection
        v-else-if="selectedView === 'laboratory-result'"
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
  grid-template-columns: 280px 1fr;
  background: linear-gradient(135deg, #f1efe9 0%, #edf2fa 100%);
}

.sidebar {
  display: flex;
  flex-direction: column;
  gap: 16px;
  padding: 20px 0;
  background: #304b73;
  color: #fff;
  overflow-y: auto;
}

.sidebar > div:first-child {
  padding: 0 16px 12px;
  border-bottom: 1px solid rgba(255,255,255,0.12);
}

.eyebrow {
  margin: 0 0 4px;
  text-transform: uppercase;
  letter-spacing: 0.08em;
  font-size: 11px;
  opacity: 0.7;
}

.sidebar h1 {
  margin: 0;
  font-size: 18px;
}

/* Accordion menu */
.accordion-menu {
  flex: 1;
  overflow-y: auto;
}

.accordion-group {
  border-bottom: 1px solid rgba(255,255,255,0.08);
}

.accordion-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 12px 16px;
  cursor: pointer;
  font-weight: 700;
  font-size: 13px;
  transition: background 0.15s;
}

.accordion-header:hover {
  background: rgba(255,255,255,0.1);
}

.accordion-header.static {
  padding: 10px 16px;
  font-size: 12px;
}

.accordion-header.expanded {
  background: rgba(255,255,255,0.08);
}

.accordion-title {
  font-size: 13px;
  text-transform: uppercase;
  letter-spacing: 0.05em;
}

.accordion-arrow {
  font-size: 11px;
  opacity: 0.7;
}

.accordion-body {
  background: rgba(0,0,0,0.15);
}

.accordion-item {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 10px 16px 10px 24px;
  cursor: pointer;
  font-size: 12px;
  transition: background 0.15s;
  border-left: 3px solid transparent;
}

.accordion-item:hover {
  background: rgba(255,255,255,0.08);
}

.accordion-item.active {
  background: rgba(255,255,255,0.12);
  border-left-color: #5f83c2;
}

.item-icon {
  font-size: 14px;
  flex-shrink: 0;
}

.item-label {
  flex: 1;
}

.item-code {
  font-size: 10px;
  opacity: 0.5;
  font-family: monospace;
}

.sidebar-footer {
  padding: 12px 16px;
  border-top: 1px solid rgba(255,255,255,0.12);
}

.logout-button {
  width: 100%;
  padding: 10px;
  border: 1px solid rgba(255,255,255,0.2);
  background: rgba(255,255,255,0.08);
  color: #fff;
  font-weight: 700;
  cursor: pointer;
  font-size: 13px;
  border-radius: 6px;
  transition: background 0.15s;
}

.logout-button:hover {
  background: rgba(255,255,255,0.18);
}

.content {
  padding: 32px;
  overflow-y: auto;
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

.hero-kicker {
  margin: 0 0 6px;
  text-transform: uppercase;
  letter-spacing: 0.08em;
  font-size: 12px;
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

.metric-label {
  text-transform: uppercase;
  letter-spacing: 0.08em;
  font-size: 12px;
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
