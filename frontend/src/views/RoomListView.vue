<script setup>
import { onMounted } from 'vue'
import { storeToRefs } from 'pinia'
import { RouterLink } from 'vue-router'
import { useRoomStore } from '@/stores/rooms'

const roomStore = useRoomStore()
const { rooms, loading, error } = storeToRefs(roomStore)

onMounted(() => {
  roomStore.fetchRooms()
})
</script>

<template>
  <section>
    <div class="panel-title">Available Rooms</div>

    <div v-if="loading" class="state">Loading…</div>
    <div v-else-if="error" class="state error">{{ error }}</div>
    <div v-else-if="rooms.length === 0" class="state">No rooms available.</div>

    <div v-else class="room-grid">
      <RouterLink
        v-for="room in rooms"
        :key="room.id"
        :to="`/rooms/${room.id}`"
        class="room-card retro-panel"
      >
        <div class="room-card-header">
          <span class="room-icon">🏢</span>
          <span class="room-name">{{ room.name }}</span>
        </div>
        <div class="room-card-body retro-panel-inset">
          <div><b>Capacity:</b> {{ room.capacity }}</div>
          <div><b>Location:</b> {{ room.location }}</div>
          <div v-if="room.equipment"><b>Equipment:</b> {{ room.equipment }}</div>
          <div v-if="room.description" class="desc">{{ room.description }}</div>
        </div>
        <div class="room-card-footer">
          <span class="hint">Click to book →</span>
        </div>
      </RouterLink>
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