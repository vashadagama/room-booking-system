<script setup>
import { ref, onMounted, computed } from 'vue'
import {
  getAdminRooms,
  getAdminSummary,
  createRoom,
  updateRoom,
  deleteRoom
} from '@/api/admin'
import { errorText } from '@/api/client'

const rooms = ref([])
const summary = ref(null)
const loading = ref(false)
const error = ref(null)

const form = ref(null)
const saving = ref(false)
const formError = ref(null)

const activeCount = computed(() => rooms.value.filter(r => r.isActive).length)

const emptyForm = () => ({ id: null, name: '', capacity: 1, location: '', description: '', equipment: '' })

async function load() {
  loading.value = true
  error.value = null
  try {
    // Два независимых запроса: сводка и список комнат. Ошибка в одном
    // не должна прятать второй, поэтому ловим их вместе.
    const [summaryRes, roomsRes] = await Promise.all([getAdminSummary(), getAdminRooms()])
    summary.value = summaryRes.data
    rooms.value = roomsRes.data
  } catch (e) {
    error.value = errorText(e, 'Не удалось загрузить данные админки')
  } finally {
    loading.value = false
  }
}

function startCreate() {
  form.value = emptyForm()
  formError.value = null
}

function startEdit(room) {
  form.value = {
    id: room.id,
    name: room.name,
    capacity: room.capacity,
    location: room.location,
    description: room.description || '',
    equipment: room.equipment || ''
  }
  formError.value = null
}

function cancelEdit() {
  form.value = null
  formError.value = null
}

async function save() {
  const value = form.value
  if (!value) return
  saving.value = true
  formError.value = null
  const payload = {
    name: value.name,
    capacity: Number(value.capacity),
    location: value.location,
    description: value.description,
    equipment: value.equipment
  }
  try {
    if (value.id) {
      await updateRoom(value.id, payload)
    } else {
      await createRoom(payload)
    }
    form.value = null
    await load()
  } catch (e) {
    formError.value = errorText(e, 'Не удалось сохранить комнату')
  } finally {
    saving.value = false
  }
}

async function deactivate(room) {
  try {
    await deleteRoom(room.id)
    await load()
  } catch (e) {
    error.value = errorText(e, 'Не удалось отключить комнату')
  }
}

onMounted(load)
</script>

<template>
  <section>
    <div class="panel-title">Admin</div>

    <div v-if="loading" class="state">Loading…</div>
    <div v-else-if="error" class="state error">{{ error }}</div>

    <template v-else-if="summary">
      <!-- Сводка -->
      <div class="retro-panel summary">
        <div class="summary-grid">
          <div class="tile">
            <span class="tile-value">{{ summary.users }}</span>
            <span class="tile-label">Users ({{ summary.admins }} admins)</span>
          </div>
          <div class="tile">
            <span class="tile-value">{{ activeCount }} / {{ summary.rooms }}</span>
            <span class="tile-label">Rooms active</span>
          </div>
          <div class="tile">
            <span class="tile-value">{{ summary.activeBookings }}</span>
            <span class="tile-label">Active bookings of {{ summary.bookings }}</span>
          </div>
          <div class="tile">
            <span class="tile-value">{{ summary.bookingsLast30Days }}</span>
            <span class="tile-label">Bookings, 30 days</span>
          </div>
          <div class="tile">
            <span class="tile-value">{{ summary.gameScoreSum }}</span>
            <span class="tile-label">Game points</span>
          </div>
        </div>

        <div class="summary-lists">
          <div class="retro-panel-inset list">
            <div class="list-title">Top bookers</div>
            <p v-if="summary.topBookers.length === 0" class="hint">No bookings yet</p>
            <div v-for="u in summary.topBookers" :key="u.userId" class="list-row">
              <span>{{ u.userName }}</span>
              <span class="list-value">{{ u.bookings }}</span>
            </div>
          </div>

          <div class="retro-panel-inset list">
            <div class="list-title">Games</div>
            <p v-if="summary.games.length === 0" class="hint">No games played yet</p>
            <div v-for="g in summary.games" :key="g.game" class="list-row">
              <span>{{ g.game }}</span>
              <span class="list-value">{{ g.wins }} wins · {{ g.score }} pts</span>
            </div>
          </div>
        </div>
      </div>

      <!-- Комнаты -->
      <div class="rooms-head">
        <div class="panel-title small">Rooms</div>
        <button class="retro-btn primary" @click="form ? cancelEdit() : startCreate()">
          {{ form ? 'Close' : 'Add Room' }}
        </button>
      </div>

      <form v-if="form" class="retro-panel room-form" @submit.prevent="save">
        <div class="form-grid">
          <label>
            Name
            <input v-model="form.name" class="retro-input" required maxlength="100" />
          </label>
          <label>
            Capacity
            <input v-model.number="form.capacity" class="retro-input" type="number" min="1" required />
          </label>
          <label>
            Location
            <input v-model="form.location" class="retro-input" required maxlength="200" />
          </label>
          <label>
            Equipment
            <input v-model="form.equipment" class="retro-input" maxlength="200" />
          </label>
          <label class="wide">
            Description
            <input v-model="form.description" class="retro-input" maxlength="500" />
          </label>
        </div>
        <p v-if="formError" class="error-text">{{ formError }}</p>
        <div class="form-actions">
          <button class="retro-btn primary" type="submit" :disabled="saving">
            {{ saving ? 'Saving…' : form.id ? 'Save' : 'Create' }}
          </button>
          <button class="retro-btn" type="button" @click="cancelEdit">Cancel</button>
        </div>
      </form>

      <table class="rooms-table">
        <thead>
          <tr>
            <th>Name</th>
            <th>Capacity</th>
            <th>Location</th>
            <th>Status</th>
            <th></th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="room in rooms" :key="room.id">
            <td>{{ room.name }}</td>
            <td>{{ room.capacity }}</td>
            <td>{{ room.location }}</td>
            <td>
              <span class="status" :class="room.isActive ? 'active' : 'cancelled'">
                {{ room.isActive ? 'ACTIVE' : 'DISABLED' }}
              </span>
            </td>
            <td class="row-actions">
              <button class="retro-btn" @click="startEdit(room)">Edit</button>
              <button
                class="retro-btn danger"
                :disabled="!room.isActive"
                @click="deactivate(room)"
              >
                Disable
              </button>
            </td>
          </tr>
        </tbody>
      </table>
    </template>
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

.panel-title.small {
  font-size: 13px;
  margin: 18px 0 8px;
}

.state {
  padding: 24px;
  text-align: center;
  color: var(--text-muted);
  background: var(--surface);
  border: 1px solid var(--border-mid);
}
.state.error { color: var(--danger); }

.summary { padding: 12px; }

.summary-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(140px, 1fr));
  gap: 10px;
}

.tile {
  display: flex;
  flex-direction: column;
  gap: 2px;
  padding: 8px 10px;
  background: var(--surface-alt);
  border: 1px solid var(--border-mid);
}

.tile-value {
  font-family: var(--font-mono);
  font-size: 20px;
  color: var(--accent-dark);
}

.tile-label { font-size: 11px; color: var(--text-muted); }

.summary-lists {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(220px, 1fr));
  gap: 10px;
  margin-top: 10px;
}

.list { padding: 8px 10px; }
.list-title { font-size: 11px; color: var(--text-muted); margin-bottom: 6px; }

.list-row {
  display: flex;
  justify-content: space-between;
  gap: 10px;
  padding: 2px 0;
  font-size: 12px;
}

.list-value { font-family: var(--font-mono); color: var(--accent-dark); }
.hint { margin: 0; font-size: 11px; color: var(--text-muted); }

.rooms-head {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  gap: 10px;
}

.room-form { padding: 12px; margin-bottom: 10px; }

.form-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(180px, 1fr));
  gap: 10px;
}

.form-grid label {
  display: flex;
  flex-direction: column;
  gap: 3px;
  font-size: 11px;
  color: var(--text-muted);
}

.form-grid label.wide { grid-column: 1 / -1; }

.form-actions { display: flex; gap: 8px; margin-top: 10px; }

.error-text { color: var(--danger); font-size: 12px; margin: 8px 0 0; }

.rooms-table { width: 100%; border-collapse: collapse; margin-top: 10px; }

.rooms-table th {
  text-align: left;
  font-size: 11px;
  color: var(--text-muted);
  padding: 6px 8px;
  border-bottom: 1px solid var(--border-mid);
}

.rooms-table td {
  padding: 6px 8px;
  font-size: 12px;
  border-bottom: 1px solid var(--border-mid);
}

.rooms-table tr:hover td { background: var(--surface-hover); }

.row-actions { display: flex; gap: 6px; justify-content: flex-end; }

.status {
  font-family: var(--font-mono);
  font-size: 11px;
  padding: 1px 6px;
  border: 1px solid;
}

.status.active { background: var(--success-bg); color: var(--success); border-color: var(--success); }
.status.cancelled { background: var(--danger-bg); color: var(--danger); border-color: var(--danger); }
</style>