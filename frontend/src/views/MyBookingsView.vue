<script setup>
import { ref, onMounted } from 'vue'
import { storeToRefs } from 'pinia'
import { useBookingStore } from '@/stores/bookings'
import { useAuthStore } from '@/stores/auth'

const bookingStore = useBookingStore()
const authStore = useAuthStore()
const { myBookings, loading, error } = storeToRefs(bookingStore)

const showCancelModal = ref(false)
const cancelTargetId = ref(null)
const cancelError = ref(null)

// Токен читается из localStorage синхронно, поэтому isAuthenticated
// уже готов на момент F5. user здесь не нужен — он приходит асинхронно.
onMounted(() => {
  if (authStore.isAuthenticated) {
    bookingStore.fetchMyBookings().catch(() => {})
  }
})

function askCancel(id) {
  cancelTargetId.value = id
  cancelError.value = null
  showCancelModal.value = true
}

async function confirmCancel() {
  const id = cancelTargetId.value
  if (!id) return
  try {
    await bookingStore.removeBooking(id)
    showCancelModal.value = false
    cancelTargetId.value = null
    const booking = myBookings.value.find(b => b.id === id)
    if (booking) booking.status = 'CANCELLED'
  } catch (e) {
    cancelError.value = e.response?.data?.message || e.message
  }
}

function closeCancelModal() {
  showCancelModal.value = false
  cancelTargetId.value = null
  cancelError.value = null
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
                @click="askCancel(b.id)"
              >
                Cancel
              </button>
            </td>
        </tr>
      </tbody>
    </table>

    <div v-if="showCancelModal" class="modal-overlay" @click.self="closeCancelModal">
      <div class="modal retro-panel">
        <div class="modal-title">Cancel Booking</div>
        <div class="modal-body">
          <p>Are you sure you want to cancel this booking?</p>
          <p v-if="cancelError" class="error-text">{{ cancelError }}</p>
        </div>
        <div class="modal-actions">
          <button class="retro-btn" @click.prevent="closeCancelModal">No</button>
          <button class="retro-btn danger" @click.prevent="confirmCancel">Yes, Cancel</button>
        </div>
      </div>
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

.modal-overlay {
  position: fixed;
  inset: 0;
  background: rgba(0, 0, 0, 0.4);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1000;
}

.modal {
  min-width: 320px;
  max-width: 480px;
  padding: 0;
}

.modal-title {
  padding: 8px 12px;
  background: linear-gradient(90deg, var(--accent-dark), var(--accent-light));
  color: #fff;
  font-weight: bold;
  font-size: 13px;
}

.modal-body {
  padding: 16px 12px;
  font-size: 13px;
}

.modal-actions {
  display: flex;
  gap: 8px;
  justify-content: flex-end;
  padding: 8px 12px;
  border-top: 1px solid var(--border-mid);
}

.error-text {
  color: var(--danger);
  margin-top: 8px;
}
</style>