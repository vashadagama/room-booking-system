<script setup>
import { ref, onMounted, computed } from 'vue'
import { useRoute, RouterLink } from 'vue-router'
import { useBookingStore } from '@/stores/bookings'
import { useRoomStore } from '@/stores/rooms'
import { MOCK_USER_ID, MOCK_USER_NAME } from '@/api/client'
import BookingGrid from '@/components/features/BookingGrid.vue'

const route = useRoute()
const roomId = route.params.id
const bookingStore = useBookingStore()
const roomStore = useRoomStore()

const selectedDate = ref(new Date().toISOString().split('T')[0])
const feedback = ref(null)

const room = computed(() => roomStore.currentRoom)

async function loadBookings() {
  await bookingStore.fetchRoomBookings(roomId, selectedDate.value)
}

onMounted(async () => {
  await roomStore.fetchRoom(roomId)
  await loadBookings()
})

async function handleBook(slot) {
  feedback.value = null
  try {
    await bookingStore.addBooking({
      roomId: Number(roomId),
      title: `${MOCK_USER_NAME}'s meeting`,
      startTime: slot.start,
      endTime: slot.end,
      userId: MOCK_USER_ID,
      userName: MOCK_USER_NAME
    })
    feedback.value = { type: 'success', text: 'Booking created.' }
    await loadBookings()
  } catch (e) {
    feedback.value = {
      type: 'error',
      text: e.response?.data?.message || e.message || 'Booking failed'
    }
  }
}
</script>

<template>
  <section>
    <div class="crumb">
      <RouterLink to="/" class="retro-btn">← Back to rooms</RouterLink>
    </div>

    <div v-if="!room" class="state">Loading…</div>

    <div v-else>
      <div class="panel-title">
        🏢 {{ room.name }}
        <span class="subtitle">— {{ room.location }}</span>
      </div>

      <div class="room-meta retro-panel-inset">
        <div><b>Capacity:</b> {{ room.capacity }}</div>
        <div><b>Equipment:</b> {{ room.equipment || '—' }}</div>
        <div v-if="room.description"><b>Description:</b> {{ room.description }}</div>
      </div>

      <div class="date-row">
        <label>
          <b>Date:</b>
          <input
            class="retro-input"
            type="date"
            v-model="selectedDate"
            @change="loadBookings"
          />
        </label>

        <div v-if="feedback" class="feedback" :class="feedback.type">
          {{ feedback.text }}
        </div>
      </div>

      <BookingGrid
        :bookings="bookingStore.roomBookings"
        :date="selectedDate"
        @book="handleBook"
      />
    </div>
  </section>
</template>

<style scoped>
.crumb { margin-bottom: 12px; }

.panel-title {
  font-weight: bold;
  font-size: 15px;
  margin-bottom: 10px;
  padding-bottom: 6px;
  border-bottom: 1px solid var(--border-mid);
  color: var(--accent-dark);
}
.subtitle { color: var(--text-muted); font-weight: normal; font-size: 12px; }

.room-meta {
  padding: 8px 10px;
  font-size: 12px;
  line-height: 1.6;
  margin-bottom: 12px;
}

.date-row {
  display: flex;
  align-items: center;
  gap: 16px;
  margin-bottom: 12px;
  padding: 8px;
  background: var(--surface);
  border: 1px solid var(--border-mid);
  box-shadow:
    inset 1px 1px 0 var(--border-light),
    inset -1px -1px 0 var(--border-dark);
}
.date-row label { display: flex; align-items: center; gap: 6px; }

.feedback {
  padding: 3px 10px;
  border: 1px solid var(--border-mid);
  font-size: 12px;
}
.feedback.success { background: var(--success-bg); color: var(--success); border-color: var(--success); }
.feedback.error   { background: var(--danger-bg);  color: var(--danger);  border-color: var(--danger); }

.state { padding: 24px; text-align: center; color: var(--text-muted); }
</style>