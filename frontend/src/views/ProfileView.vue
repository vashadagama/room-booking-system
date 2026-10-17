<script setup>
import { onMounted } from 'vue'
import { useAuthStore } from '@/stores/auth'
import GameStatsPanel from '@/components/features/GameStatsPanel.vue'

const authStore = useAuthStore()

onMounted(() => {
  authStore.ensureUserLoaded()
})
</script>

<template>
  <section>
    <div class="panel-title">Profile</div>

    <div class="profile-container retro-panel">
      <div v-if="authStore.user" class="profile-info">
        <div class="profile-row">
          <span class="label">Username:</span>
          <span class="value">{{ authStore.user.username }}</span>
        </div>
        <div class="profile-row">
          <span class="label">First Name:</span>
          <span class="value">{{ authStore.user.firstName }}</span>
        </div>
        <div class="profile-row">
          <span class="label">Last Name:</span>
          <span class="value">{{ authStore.user.lastName }}</span>
        </div>
      </div>
      <div v-else class="state">Loading...</div>
    </div>

    <GameStatsPanel v-if="authStore.isAuthenticated" />
  </section>
</template>

<style scoped>
section {
  display: flex;
  flex-direction: column;
  gap: 18px;
}

.profile-container {
  max-width: 400px;
  padding: 20px;
}

.profile-info {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.profile-row {
  display: flex;
  gap: 8px;
}

.label {
  font-weight: bold;
  min-width: 100px;
}

.value {
  color: var(--text-muted);
}
</style>
