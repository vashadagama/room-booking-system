<script setup>
import { computed, onMounted, ref, watch } from 'vue'
import { storeToRefs } from 'pinia'
import { RouterLink } from 'vue-router'
import { useRoomStore } from '@/stores/rooms'
import GamesCard from '@/components/features/GamesCard.vue'

const roomStore = useRoomStore()
const { rooms, loading, error } = storeToRefs(roomStore)

const MS_SLOTS = 4
const gamesSlot = ref(0)

watch(
  rooms,
  list => {
    const maxSlot = Math.min(list.length, MS_SLOTS)
    gamesSlot.value = Math.floor(Math.random() * (maxSlot + 1))
  },
  { immediate: true }
)

const gridItems = computed(() => {
  const items = rooms.value.map(room => ({ kind: 'room', room }))
  items.splice(gamesSlot.value, 0, { kind: 'games' })
  return items
})

onMounted(() => {
  roomStore.fetchRooms()
})
</script>

<template>
  <section>
    <div class="panel-title">Available Rooms</div>

    <div v-if="loading" class="state">Loading…</div>
    <div v-else-if="error" class="state error">{{ error }}</div>

    <div v-else class="room-grid">
      <div v-if="rooms.length === 0" class="state">No rooms available.</div>

      <template v-for="item in gridItems" :key="item.kind === 'room' ? item.room.id : 'minesweeper'">
        <RouterLink
          v-if="item.kind === 'room'"
          :to="`/rooms/${item.room.id}`"
          class="room-card retro-panel"
        >
          <div class="room-card-header">
            <span class="room-icon">🏢</span>
            <span class="room-name">{{ item.room.name }}</span>
          </div>
          <div class="room-card-body retro-panel-inset">
            <div><b>Capacity:</b> {{ item.room.capacity }}</div>
            <div><b>Location:</b> {{ item.room.location }}</div>
            <div v-if="item.room.equipment"><b>Equipment:</b> {{ item.room.equipment }}</div>
            <div v-if="item.room.description" class="desc">{{ item.room.description }}</div>
          </div>
          <div class="room-card-footer">
            <span class="hint">Click to book →</span>
          </div>
        </RouterLink>

        <GamesCard v-else />
      </template>
    </div>
  </section>
</template>

<style scoped>
.panel-title {
  font-weight: bold;
  font-size: 14px;
  margin-bottom: 10px;
  padding-bottom: 6px;
  border-bottom: 1px solid var(--border-mid);
  color: var(--accent-dark);
}

.state {
  padding: 24px;
  text-align: center;
  color: var(--text-muted);
  background: var(--surface);
  border: 1px solid var(--border-mid);
}
.state.error { color: var(--danger); }

.room-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(260px, 1fr));
  gap: 12px;
}

.room-card {
  display: flex;
  flex-direction: column;
  padding: 0;
  text-decoration: none;
  color: var(--text);
  transition: transform 0.05s;
}
.room-card:hover {
  background: var(--surface-hover);
  transform: translate(-1px, -1px);
  box-shadow:
    inset 1px 1px 0 var(--border-light),
    inset -1px -1px 0 var(--border-dark),
    2px 2px 0 rgba(0, 0, 0, 0.2);
}
.room-card:active {
  transform: translate(1px, 1px);
  box-shadow:
    inset -1px -1px 0 var(--border-light),
    inset 1px 1px 0 var(--border-dark);
}

.room-card-header {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 6px 8px;
  background: linear-gradient(90deg, var(--accent-dark), var(--accent-light));
  color: #fff;
  font-weight: bold;
}
.room-icon { font-size: 14px; }
.room-name { font-size: 13px; }

.room-card-body {
  flex: 1;
  padding: 8px 10px;
  font-size: 12px;
  line-height: 1.5;
  margin: 4px;
}
.room-card-body .desc {
  margin-top: 6px;
  color: var(--text-muted);
  font-style: italic;
}

.room-card-footer {
  padding: 4px 10px 8px;
  text-align: right;
}
.hint { color: var(--accent); font-size: 11px; }
</style>