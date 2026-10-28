<script setup>
import { computed, onMounted, ref, watch } from 'vue'
import { useAuthStore } from '@/stores/auth'
import { getGameStats } from '@/api/games'
import { errorText } from '@/api/client'

const authStore = useAuthStore()

const stats = ref(null)
const loading = ref(false)
const error = ref(null)

const LABELS = { MINESWEEPER: 'Minesweeper', SOLITAIRE: 'Solitaire' }

const games = computed(() =>
  (stats.value?.games || []).map((game) => ({
    ...game,
    label: LABELS[game.game] || game.game,
    winRate: game.games > 0 ? Math.round((game.wins / game.games) * 100) : 0
  }))
)

const hasPlayed = computed(() => (stats.value?.totalScore ?? 0) > 0 || games.value.some(g => g.games > 0))

async function loadStats() {
  loading.value = true
  error.value = null
  try {
    const res = await getGameStats()
    stats.value = res.data
  } catch (e) {
    error.value = errorText(e, 'Не удалось загрузить статистику игр')
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  loadStats()
})

watch(() => authStore.user?.id, () => {
  if (authStore.isAuthenticated) {
    loadStats()
  }
})
</script>

<template>
  <section class="games-profile">
    <div class="panel-title">Games</div>

    <div class="retro-panel games-box">
      <div v-if="loading" class="state">Loading…</div>
      <div v-else-if="error" class="state error">{{ error }}</div>

      <template v-else-if="stats">
        <div class="total-row">
          <span>Total score:</span>
          <b class="total-value">{{ stats.totalScore }}</b>
        </div>

        <p v-if="!hasPlayed" class="hint">
          Здесь появятся очки из сапёра и морского боя.
        </p>

        <div v-for="game in games" :key="game.game" class="game-row">
          <div class="game-head">
            <span class="game-name">{{ game.label }}</span>
            <span class="game-score">{{ game.score }}</span>
          </div>
          <div class="game-meta">
            <span>W/L: <b>{{ game.wins }}</b>/<b>{{ game.losses }}</b></span>
            <span v-if="game.games">Win rate: {{ game.winRate }}%</span>
            <span v-if="game.bestGame">Best: {{ game.bestGame }}</span>
          </div>
        </div>
      </template>

      <div v-else class="state">Войдите, чтобы видеть очки</div>
    </div>
  </section>
</template>

<style scoped>
.games-box {
  max-width: 400px;
  padding: 12px 16px;
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.total-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding-bottom: 8px;
  border-bottom: 1px solid var(--border-mid);
  font-weight: bold;
  color: var(--accent-dark);
}

.total-value {
  font-family: var(--font-mono);
  font-size: 18px;
  color: var(--success);
}

.game-row {
  display: flex;
  flex-direction: column;
  gap: 2px;
  padding: 6px 8px;
  background: var(--surface-alt);
  border: 1px solid var(--border-mid);
  box-shadow:
    inset 1px 1px 0 var(--border-light),
    inset -1px -1px 0 var(--border-dark);
}

.game-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.game-name {
  font-weight: bold;
}

.game-score {
  font-family: var(--font-mono);
  font-weight: bold;
  color: var(--accent);
}

.game-meta {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  font-size: 11px;
  color: var(--text-muted);
}

.hint {
  font-size: 11px;
  color: var(--text-muted);
  font-style: italic;
}
</style>