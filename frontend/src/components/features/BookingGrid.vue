<script setup>
import { computed } from 'vue'

const props = defineProps({
  bookings: { type: Array, default: () => [] },
  date: { type: String, required: true }
})

const emit = defineEmits(['book'])

const START_HOUR = 8
const END_HOUR = 20

const slots = computed(() => {
  const result = []
  for (let h = START_HOUR; h < END_HOUR; h++) {
    result.push({
      hour: h,
      start: `${props.date}T${String(h).padStart(2, '0')}:00:00`,
      end: `${props.date}T${String(h + 1).padStart(2, '0')}:00:00`,
      booking: null
    })
  }
  for (const b of props.bookings) {
    const bStart = new Date(b.startTime).getHours()
    const bEnd = new Date(b.endTime).getHours()
    for (let h = bStart; h < bEnd; h++) {
      const slot = result.find(s => s.hour === h)
      if (slot) slot.booking = b
    }
  }
  return result
})

function onClick(slot) {
  if (!slot.booking) emit('book', slot)
}

function fmtHour(h) {
  return `${String(h).padStart(2, '0')}:00`
}
</script>

<template>
  <div class="grid-wrap retro-panel">
    <div class="grid-title">
      Booking Grid — {{ date }}
      <span class="legend">
        <i class="dot free"></i> Free
        <i class="dot busy"></i> Booked
      </span>
    </div>

    <div class="grid">
      <button
        v-for="slot in slots"
        :key="slot.hour"
        type="button"
        class="slot"
        :class="slot.booking ? 'booked' : 'free'"
        :disabled="!!slot.booking"
        @click="onClick(slot)"
      >
        <span class="slot-time">{{ fmtHour(slot.hour) }} — {{ fmtHour(slot.hour + 1) }}</span>
        <span v-if="slot.booking" class="slot-info">
          <b>{{ slot.booking.title }}</b>
          <span class="who">— {{ slot.booking.userName }}</span>
        </span>
        <span v-else class="slot-info free-label">Free · click to book</span>
      </button>
    </div>
  </div>
</template>

<style scoped>
.grid-wrap { padding: 0; }

.grid-title {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 6px 10px;
  background: linear-gradient(90deg, var(--accent-dark), var(--accent-light));
  color: #fff;
  font-weight: bold;
  font-size: 12px;
}
.legend { font-weight: normal; font-size: 11px; display: flex; align-items: center; gap: 6px; }
.dot {
  display: inline-block;
  width: 10px; height: 10px;
  margin-left: 8px;
  border: 1px solid var(--border-dark);
}
.dot.free { background: var(--success-bg); }
.dot.busy { background: var(--danger-bg); }

.grid {
  display: flex;
  flex-direction: column;
  gap: 2px;
  padding: 6px;
  background: var(--surface-alt);
}

.slot {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 6px 10px;
  background: var(--surface);
  border: 1px solid var(--border-mid);
  box-shadow:
    inset 1px 1px 0 var(--border-light),
    inset -1px -1px 0 var(--border-dark);
  cursor: pointer;
  text-align: left;
  font-family: var(--font);
  font-size: 12px;
  color: var(--text);
}
.slot:not(:disabled):hover { background: var(--surface-hover); }
.slot:not(:disabled):active {
  box-shadow:
    inset -1px -1px 0 var(--border-light),
    inset 1px 1px 0 var(--border-dark);
}
.slot:disabled { cursor: not-allowed; }

.slot.free .slot-time { color: var(--accent-dark); }
.slot.free .free-label { color: var(--text-muted); font-style: italic; }

.slot.booked {
  background: var(--danger-bg);
  border-color: var(--danger);
}
.slot.booked .slot-time { color: var(--danger); }
.slot.booked .slot-info { color: var(--danger); }

.slot-time { font-weight: bold; min-width: 110px; }
.slot-info { flex: 1; }
.slot-info .who { color: var(--text-muted); font-weight: normal; margin-left: 6px; }
</style>