<script setup>
import { onMounted } from 'vue'
import { storeToRefs } from 'pinia'
import { useBookingStore } from '@/stores/bookings'
import { MOCK_USER_ID } from '@/api/client'

const bookingStore = useBookingStore()
const { myBookings, loading, error } = storeToRefs(bookingStore)

async function load() {
  try {
    await bookingStore.fetchMyBookings(MOCK_USER_ID)
  } catch {
    /* error отображается через store */
  }
}

onMounted(load)

async function cancel(id) {
  if (!confirm('Cancel this booking?')) return
  try {
    await bookingStore.removeBooking(id, MOCK_USER_ID)
    await load()
  } catch (e) {
    alert(e.response?.data?.message || e.message)
  }
}

function fmt(dt) {
  return new Date(dt).toLocaleString()
}
</script>

<template>
  <section>
    <div class="panel-title">My Bookings</div>

    <div v-if="loading" class="state">Loading…</div>
    <div v-else-if="error" class="state error">{{ error }}</div>
    <div v-else-if="myBookings.length === 0" class="state">
      You have no bookings yet.
    </div>

    <table v-else class="bookings-table">
      <thead>
        <tr>
          <th>Room</th>
          <th>Title</th>
          <th>Start</th>
          <th>End</th>
          <th>Status</th>
          <th></th>
        </tr>
      </thead>
      <tbody>
        <tr v-for="b in myBookings" :key="b.id">
          <td>{{ b.roomName }}</td>
          <td>{{ b.title }}</td>
          <td>{{ fmt(b.startTime) }}</td>
          <td>{{ fmt(b.endTime) }}</td>
          <td>
            <span class="status" :class="b.status.toLowerCase()">{{ b.status }}</span>
          </td>
          <td>
            <button
              class="retro-btn danger"
              :disabled="b.status !== 'ACTIVE'"
              @click="cancel(b.id)"
            >
              Cancel
            </button>
          </td>
        </tr>
      </tbody>
    </table>
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

.bookings-table {
  width: 100%;
  border-collapse: collapse;
  background: var(--surface);
  border: 1px solid var(--border-mid);
  box-shadow:
    inset 1px 1px 0 var(--border-light),
    inset -1px -1px 0 var(--border-dark);
  font-size: 12px;
}
.bookings-table th {
  text-align: left;
  padding: 6px 10px;
  background: linear-gradient(180deg, var(--surface-alt), var(--surface));
  border-bottom: 1px solid var(--border-mid);
  border-right: 1px solid var(--border-mid);
  color: var(--accent-dark);
}
.bookings-table td {
  padding: 6px 10px;
  border-bottom: 1px solid var(--border-mid);
  border-right: 1px solid var(--border-mid);
}
.bookings-table tr:hover td { background: var(--surface-hover); }

.status {
  display: inline-block;
  padding: 1px 6px;
  font-size: 11px;
  border: 1px solid var(--border-dark);
}
.status.active    { background: var(--success-bg); color: var(--success); border-color: var(--success); }
.status.cancelled { background: var(--danger-bg);  color: var(--danger);  border-color: var(--danger); }
.status.completed { background: var(--surface-alt); color: var(--text-muted); }
</style>