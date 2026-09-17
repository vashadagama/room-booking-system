import { defineStore } from 'pinia'
import { ref } from 'vue'
import { getRooms, getRoom } from '@/api/rooms'

export const useRoomStore = defineStore('rooms', () => {
  const rooms = ref([])
  const currentRoom = ref(null)
  const loading = ref(false)
  const error = ref(null)

  async function fetchRooms() {
    loading.value = true
    error.value = null
    try {
      const res = await getRooms()
      rooms.value = res.data
    } catch (e) {
      error.value = e.response?.data?.message || e.message
    } finally {
      loading.value = false
    }
  }

  async function fetchRoom(id) {
    loading.value = true
    error.value = null
    try {
      const res = await getRoom(id)
      currentRoom.value = res.data
    } catch (e) {
      error.value = e.response?.data?.message || e.message
    } finally {
      loading.value = false
    }
  }

  return { rooms, currentRoom, loading, error, fetchRooms, fetchRoom }
})