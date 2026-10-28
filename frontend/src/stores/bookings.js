import { defineStore } from 'pinia'
import { ref } from 'vue'
import {
  getBookingsForRoom,
  getMyBookings,
  createBooking,
  cancelBooking
} from '@/api/bookings'
import { onWsMessage } from '@/ws/socket'

export const useBookingStore = defineStore('bookings', () => {
  const myBookings = ref([])
  const roomBookings = ref([])
  const loading = ref(false)
  const error = ref(null)

  // Какую комнату на какую дату сейчас смотрим.
  const currentRoomId = ref(null)
  const currentDate = ref(null)

  async function fetchMyBookings() {
    loading.value = true
    error.value = null
    try {
      const res = await getMyBookings()
      myBookings.value = res.data
    } catch (e) {
      error.value = e.response?.data?.message || e.message
      throw e
    } finally {
      loading.value = false
    }
  }

  async function fetchRoomBookings(roomId, date) {
    currentRoomId.value = Number(roomId)
    currentDate.value = date

    loading.value = true
    error.value = null
    try {
      const res = await getBookingsForRoom(roomId, date)
      roomBookings.value = res.data
    } catch (e) {
      error.value = e.response?.data?.message || e.message
      throw e
    } finally {
      loading.value = false
    }
  }

  async function addBooking(data) {
    const res = await createBooking(data)
    return res.data
  }

  async function removeBooking(id) {
    await cancelBooking(id)
  }

  onWsMessage((message) => {
    if (message.type === 'BOOKINGS_CHANGED') {
      if (
        currentRoomId.value === message.roomId &&
        currentDate.value === message.date
      ) {
        roomBookings.value = message.bookings
      }
      return
    }

    if (message.type === 'SOCKET_OPEN' && currentRoomId.value && currentDate.value) {
      fetchRoomBookings(currentRoomId.value, currentDate.value).catch(() => {})
    }
  })

  return {
    myBookings,
    roomBookings,
    loading,
    error,
    currentRoomId,
    currentDate,
    fetchMyBookings,
    fetchRoomBookings,
    addBooking,
    removeBooking
  }
})