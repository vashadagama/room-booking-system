<script setup>
import { RouterLink, RouterView } from 'vue-router'
</script>

<template>
  <div class="app-shell">
    <div class="app-window retro-panel">
      <!-- Titlebar -->
      <div class="retro-titlebar">
        <span class="title">Room Booking System</span>
        <div class="window-controls">
          <button class="wc">_</button>
          <button class="wc">▢</button>
          <button class="wc close">✕</button>
        </div>
      </div>

      <!-- Toolbar -->
      <div class="toolbar">
        <RouterLink to="/" class="retro-btn" active-class="active">
          Rooms
        </RouterLink>
        <RouterLink to="/my-bookings" class="retro-btn" active-class="active">
          My Bookings
        </RouterLink>
        <div class="toolbar-spacer"></div>
        <span class="toolbar-hint">Lab 2 — Vue SPA</span>
      </div>

      <!-- Content -->
      <main class="app-content">
        <RouterView />
      </main>

      <!-- Status bar -->
      <div class="statusbar">
        <span>Ready.</span>
        <span>v0.2</span>
      </div>
    </div>
  </div>
</template>

<style scoped>
.app-shell {
  min-height: 100vh;
  display: flex;
  align-items: stretch;
  justify-content: center;
  padding: 24px;
}

.app-window {
  width: 100%;
  max-width: 1100px;
  display: flex;
  flex-direction: column;
  box-shadow: 4px 4px 0 rgba(0, 0, 0, 0.25);
}

.title { font-size: 13px; }

.window-controls { display: flex; gap: 2px; }
.wc {
  width: 20px;
  height: 18px;
  background: var(--surface);
  border: 1px solid var(--border-mid);
  box-shadow:
    inset 1px 1px 0 var(--border-light),
    inset -1px -1px 0 var(--border-dark);
  font-size: 11px;
  line-height: 1;
  cursor: pointer;
  padding: 0;
  color: #000;
}
.wc:active {
  box-shadow:
    inset -1px -1px 0 var(--border-light),
    inset 1px 1px 0 var(--border-dark);
}
.wc.close { font-weight: bold; }

.toolbar {
  display: flex;
  gap: 4px;
  align-items: center;
  padding: 6px 8px;
  background: var(--surface);
  border-bottom: 1px solid var(--border-mid);
}
.toolbar :deep(.router-link-active),
.toolbar :deep(.active) {
  box-shadow:
    inset -1px -1px 0 var(--border-light),
    inset 1px 1px 0 var(--border-dark);
  background: var(--surface-hover);
}
.toolbar-spacer { flex: 1; }
.toolbar-hint { color: var(--text-muted); font-size: 12px; }

.app-content {
  flex: 1;
  padding: 16px;
  background: var(--surface-alt);
  min-height: 480px;
}

.statusbar {
  display: flex;
  justify-content: space-between;
  padding: 3px 10px;
  background: var(--surface);
  border-top: 1px solid var(--border-mid);
  font-size: 12px;
  color: var(--text-muted);
}
</style>