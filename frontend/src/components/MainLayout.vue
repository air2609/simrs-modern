<script setup>
import { computed, ref } from 'vue';
import AdmissionRegistrationSection from './AdmissionRegistrationSection.vue';
import AntrianApotikSection from './AntrianApotikSection.vue';
import AntrianApotikDisplaySection from './AntrianApotikDisplaySection.vue';
import ApotikSection from './ApotikSection.vue';
import BankSection from './BankSection.vue';
import BatchItemSection from './BatchItemSection.vue';
import BatchTreatmentSection from './BatchTreatmentSection.vue';

import BedDisplaySection from './BedDisplaySection.vue';
import BedInfoSection from './BedInfoSection.vue';
import BedSection from './BedSection.vue';
import CardTypeSection from './CardTypeSection.vue';
import CoaSection from './CoaSection.vue';
import DivisionSection from './DivisionSection.vue';
import DoctorSection from './DoctorSection.vue';

import ExpiredItemSection from './ExpiredItemSection.vue';

import GimSection from './GimSection.vue';

import GroupMasterSection from './GroupMasterSection.vue';
import Icd9cmSection from './Icd9cmSection.vue';
import IcdSection from './IcdSection.vue';

import InsuranceSection from './InsuranceSection.vue';
import ItemInventorySection from './ItemInventorySection.vue';
import ItemMeasurementSection from './ItemMeasurementSection.vue';
import ItemSection from './ItemSection.vue';
import ItemSellingPriceSection from './ItemSellingPriceSection.vue';


import JournalSection from './JournalSection.vue';
import LocationMasterSection from './LocationMasterSection.vue';
import LabTreatmentMasterSection from './LabTreatmentMasterSection.vue';
import LaboratoryTransactionSection from './LaboratoryTransactionSection.vue';
import LaboratoryResultSection from './LaboratoryResultSection.vue';
import MrPreparationSection from './MrPreparationSection.vue';
import MrBorrowRequestSection from './MrBorrowRequestSection.vue';
import MrFileStatusSection from './MrFileStatusSection.vue';
import MrLoanListSection from './MrLoanListSection.vue';
import DiagnoseSection from './DiagnoseSection.vue';
import DeliveryOrderSection from './DeliveryOrderSection.vue';
import MrDiagnosaSection from './MrDiagnosaSection.vue';
import PolyclinicSection from './PolyclinicSection.vue';
import PolyDoctorSection from './PolyDoctorSection.vue';
import PurchaseOrderApprovalSection from './PurchaseOrderApprovalSection.vue';
import PurchaseOrderSection from './PurchaseOrderSection.vue';
import PurchaseRequestApprovalSection from './PurchaseRequestApprovalSection.vue';
import PurchaseRequestSection from './PurchaseRequestSection.vue';
import RoomSection from './RoomSection.vue';
import ScreenMasterSection from './ScreenMasterSection.vue';
import StaffSection from './StaffSection.vue';
import TreatmentClassSection from './TreatmentClassSection.vue';
import TreatmentGroupSection from './TreatmentGroupSection.vue';
import TreatmentSection from './TreatmentSection.vue';
import UnitSection from './UnitSection.vue';
import UserMaintenanceSection from './UserMaintenanceSection.vue';
import VendorSection from './VendorSection.vue';
import WarehouseSection from './WarehouseSection.vue';



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
  'SCM0054': 'antrian-apotik',
  'RPT0020': 'antrian-apotik-display',
  'SC0091': 'polyclinic',
  'SC0041': 'laboratory-transaction',
  'SC0043': 'laboratory-result',
  'SC0006': 'mr-preparation',
  'SC0175': 'mr-borrow-request',
  'SC0081': 'mr-file-status',
  'SC0082': 'mr-loan-list',
  'SC0083': 'mr-diagnosa',
  'SC0206': 'mr-diagnose',
  'SC0201': 'journal',
  'SC0190': 'expired-item',
  'SC0191': 'purchase-request',
  'SC0192': 'purchase-request-approval',
  'SC0193': 'purchase-order',
  'SC0194': 'purchase-order-approval',
  'SC0195': 'delivery-order',
  'SCM0051': 'lab-treatment-master',
  'SCM0013': 'location-master',
  'SCM0014': 'location-master',
  'SCM0015': 'location-master',
  'SCM0016': 'location-master',
  'SCM0003': 'screen-master',
  'SCM0001': 'user-maintenance',
  'SCM0002': 'group-master',
  'SCM0058': 'bed-display',
  'SC0072': 'bed-info',
  'SCM0059': 'poly-doctor',
  'SCM0021': 'treatment-class',
  'SCM0022': 'division',
  'SCM0023': 'treatment-group',
  'SCM0024': 'unit',
  'SCM0030': 'doctor',
  'SCM0031': 'staff',
  'SCM0043': 'vendor',

  'SCM0026': 'treatment',

  'SCM0056': 'batch-treatment',
  'SCM0055': 'batch-item',
  'SCM0019': 'room',

  'SCM0020': 'bed',
  'SCM0033': 'bank',
  'SCM0048': 'card-type',
  'SCM0034': 'insurance',

  'SCM0032': 'item-inventory',
  'SCM0035': 'warehouse',
  'SCM0038': 'item',
  'SCM0040': 'item-measurement',
  'SCM0041': 'item-selling-price',
  'SCM0046': 'coa',
  'SCM0047': 'gim',
  'SCM0027': 'icd',
  'SCM0028': 'icd9cm'
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
    'SCM0054': '💊',
    'RPT0020': '📺',
    'SC0091': '🩺',
    'SC0041': '🧪',
    'SC0043': '📊',
    'SC0006': '🗂️',
    'SC0175': '📤',
    'SC0081': '🗃️',
    'SC0082': '🔁',
    'SC0083': '📝',
    'SC0206': '📝',
    'SC0201': '📒',
    'SC0190': '📦',
    'SC0191': '🛒',
    'SC0192': '✅',
    'SC0193': '🛍️',
    'SC0194': '✅',
    'SC0195': '🚚',
    'SCM0051': '🔬',
    'SCM0013': '📍',
    'SCM0014': '📍',
    'SCM0015': '📍',
    'SCM0016': '📍',
    'SCM0003': '🖥️',
    'SCM0001': '👤',
    'SCM0002': '👥',
    'SCM0058': '🛏️',
    'SC0072': '🛏️',
    'SCM0059': '🩺',
    'SCM0021': '🏷️',
    'SCM0022': '🗂️',
    'SCM0023': '🗂️',
    'SCM0024': '🏢',
    'SCM0030': '🩺',
    'SCM0031': '👥',
    'SCM0043': '🏭',

    'SCM0026': '🩺',

    'SCM0056': '🩺',
    'SCM0055': '📦',
    'SCM0019': '🛏️',

    'SCM0020': '🛏️',
    'SCM0032': '📦',
    'SCM0033': '🏦',
    'SCM0048': '💳',
    'SCM0034': '🛡️',

    'SCM0035': '🏬',
    'SCM0038': '📦',
    'SCM0040': '📏',
    'SCM0041': '💰',
    'SCM0046': '📒',
    'SCM0047': '⚙️',
    'SCM0027': '🩺',
    'SCM0028': '🩺'
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

      <MrPreparationSection
        v-else-if="selectedView === 'mr-preparation'"
        :api-base-url="apiBaseUrl"
        @session-expired="emit('session-expired', $event)"
      />

      <MrBorrowRequestSection
        v-else-if="selectedView === 'mr-borrow-request'"
        :api-base-url="apiBaseUrl"
        @session-expired="emit('session-expired', $event)"
      />

      <MrFileStatusSection
        v-else-if="selectedView === 'mr-file-status'"
        :api-base-url="apiBaseUrl"
        @session-expired="emit('session-expired', $event)"
      />

      <MrLoanListSection
        v-else-if="selectedView === 'mr-loan-list'"
        :api-base-url="apiBaseUrl"
        @session-expired="emit('session-expired', $event)"
      />

      <DiagnoseSection
        v-else-if="selectedView === 'mr-diagnose'"
        :api-base-url="apiBaseUrl"
        @session-expired="emit('session-expired', $event)"
      />

      <MrDiagnosaSection
        v-else-if="selectedView === 'mr-diagnosa'"
        :api-base-url="apiBaseUrl"
        @session-expired="emit('session-expired', $event)"
      />

      <AntrianApotikSection
        v-else-if="selectedView === 'antrian-apotik'"
        :api-base-url="apiBaseUrl"
        @session-expired="emit('session-expired', $event)"
      />

      <AntrianApotikDisplaySection
        v-else-if="selectedView === 'antrian-apotik-display'"
        :api-base-url="apiBaseUrl"
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

      <ExpiredItemSection
        v-else-if="selectedView === 'expired-item'"
        :api-base-url="apiBaseUrl"
        @session-expired="emit('session-expired', $event)"
      />

      <PurchaseRequestSection
        v-else-if="selectedView === 'purchase-request'"
        :api-base-url="apiBaseUrl"
        @session-expired="emit('session-expired', $event)"
      />

      <PurchaseRequestApprovalSection
        v-else-if="selectedView === 'purchase-request-approval'"
        :api-base-url="apiBaseUrl"
        @session-expired="emit('session-expired', $event)"
      />

      <PurchaseOrderSection
        v-else-if="selectedView === 'purchase-order'"
        :api-base-url="apiBaseUrl"
        @session-expired="emit('session-expired', $event)"
        @close="navigate('overview')"
      />

      <PurchaseOrderApprovalSection
        v-else-if="selectedView === 'purchase-order-approval'"
        :api-base-url="apiBaseUrl"
        @session-expired="emit('session-expired', $event)"
        @close="navigate('overview')"
      />

      <DeliveryOrderSection
        v-else-if="selectedView === 'delivery-order'"
        :api-base-url="apiBaseUrl"
        @session-expired="emit('session-expired', $event)"
        @close="navigate('overview')"
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

      <ScreenMasterSection
        v-else-if="selectedView === 'screen-master'"
        :api-base-url="apiBaseUrl"
        @session-expired="emit('session-expired', $event)"
      />

      <UserMaintenanceSection
        v-else-if="selectedView === 'user-maintenance'"
        :api-base-url="apiBaseUrl"
        @session-expired="emit('session-expired', $event)"
      />

      <GroupMasterSection
        v-else-if="selectedView === 'group-master'"
        :api-base-url="apiBaseUrl"
        @session-expired="emit('session-expired', $event)"
      />

      <BedDisplaySection
        v-else-if="selectedView === 'bed-display'"
        :api-base-url="apiBaseUrl"
        @session-expired="emit('session-expired', $event)"
      />

      <BedInfoSection
        v-else-if="selectedView === 'bed-info'"
        :api-base-url="apiBaseUrl"
        @session-expired="emit('session-expired', $event)"
      />

      <PolyDoctorSection
        v-else-if="selectedView === 'poly-doctor'"
        :api-base-url="apiBaseUrl"
        @session-expired="emit('session-expired', $event)"
      />

      <TreatmentClassSection
        v-else-if="selectedView === 'treatment-class'"
        :api-base-url="apiBaseUrl"
        @session-expired="emit('session-expired', $event)"
      />

      <DivisionSection
        v-else-if="selectedView === 'division'"
        :api-base-url="apiBaseUrl"
        @session-expired="emit('session-expired', $event)"
      />

      <TreatmentGroupSection
        v-else-if="selectedView === 'treatment-group'"
        :api-base-url="apiBaseUrl"
        @session-expired="emit('session-expired', $event)"
      />

      <UnitSection
        v-else-if="selectedView === 'unit'"
        :api-base-url="apiBaseUrl"
        @session-expired="emit('session-expired', $event)"
      />

      <DoctorSection
        v-else-if="selectedView === 'doctor'"
        :api-base-url="apiBaseUrl"
        @session-expired="emit('session-expired', $event)"
      />

      <StaffSection
        v-else-if="selectedView === 'staff'"
        :api-base-url="apiBaseUrl"
        @session-expired="emit('session-expired', $event)"
      />

      <VendorSection
        v-else-if="selectedView === 'vendor'"
        :api-base-url="apiBaseUrl"
        @session-expired="emit('session-expired', $event)"
      />

      <TreatmentSection

        v-else-if="selectedView === 'treatment'"
        :api-base-url="apiBaseUrl"
        @session-expired="emit('session-expired', $event)"
      />

      <BatchTreatmentSection
        v-else-if="selectedView === 'batch-treatment'"
        :api-base-url="apiBaseUrl"
        @session-expired="emit('session-expired', $event)"
      />

      <BatchItemSection
        v-else-if="selectedView === 'batch-item'"
        :api-base-url="apiBaseUrl"
        @session-expired="emit('session-expired', $event)"
      />

      <RoomSection

        v-else-if="selectedView === 'room'"
        :api-base-url="apiBaseUrl"
        @session-expired="emit('session-expired', $event)"
      />

      <BedSection
        v-else-if="selectedView === 'bed'"
        :api-base-url="apiBaseUrl"
        @session-expired="emit('session-expired', $event)"
      />

      <BankSection
        v-else-if="selectedView === 'bank'"
        :api-base-url="apiBaseUrl"
        @session-expired="emit('session-expired', $event)"
      />

      <CardTypeSection
        v-else-if="selectedView === 'card-type'"
        :api-base-url="apiBaseUrl"
        @session-expired="emit('session-expired', $event)"
      />

      <InsuranceSection

        v-else-if="selectedView === 'insurance'"
        :api-base-url="apiBaseUrl"
        @session-expired="emit('session-expired', $event)"
      />

      <WarehouseSection
        v-else-if="selectedView === 'warehouse'"
        :api-base-url="apiBaseUrl"
        @session-expired="emit('session-expired', $event)"
      />

      <ItemSection
        v-else-if="selectedView === 'item'"
        :api-base-url="apiBaseUrl"
        @session-expired="emit('session-expired', $event)"
      />

      <ItemMeasurementSection
        v-else-if="selectedView === 'item-measurement'"
        :api-base-url="apiBaseUrl"
        @session-expired="emit('session-expired', $event)"
      />

      <ItemSellingPriceSection
        v-else-if="selectedView === 'item-selling-price'"
        :api-base-url="apiBaseUrl"
        @session-expired="emit('session-expired', $event)"
      />

      <CoaSection
        v-else-if="selectedView === 'coa'"
        :api-base-url="apiBaseUrl"
        @session-expired="emit('session-expired', $event)"
      />

      <GimSection
        v-else-if="selectedView === 'gim'"
        :api-base-url="apiBaseUrl"
        @session-expired="emit('session-expired', $event)"
      />

      <IcdSection
        v-else-if="selectedView === 'icd'"
        :api-base-url="apiBaseUrl"
        @session-expired="emit('session-expired', $event)"
      />

      <Icd9cmSection
        v-else-if="selectedView === 'icd9cm'"
        :api-base-url="apiBaseUrl"
        @session-expired="emit('session-expired', $event)"
      />


      <ItemInventorySection
        v-else-if="selectedView === 'item-inventory'"
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
