import { defineStore } from 'pinia'
import { ref } from 'vue'
import {
  getBookingsForRoom,
  getMyBookings,
  createBooking,
  cancelBooking
} from '@/api/bookings'

export const useBookingStore = defineStore('bookings', () => {
  const myBookings = ref([])
  const roomBookings = ref([])
  const loading = ref(false)
  const error = ref(null)

  async function fetchMyBookings(userId) {
    loading.value = true
    error.value = null
    try {
      const res = await getMyBookings(userId)
      myBookings.value = res.data
    } catch (e) {
      error.value = e.response?.data?.message || e.message
      throw e
    } finally {
      loading.value = false
    }
  }

  async function fetchRoomBookings(roomId, date) {
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

  async function removeBooking(id, userId) {
    await cancelBooking(id, userId)
  }

  return {
    myBookings,
    roomBookings,
    loading,
    error,
    fetchMyBookings,
    fetchRoomBookings,
    addBooking,
    removeBooking
  }
})