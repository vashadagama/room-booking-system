import { defineStore } from 'pinia'
import { ref } from 'vue'
import { getRooms, getRoom } from '@/api/rooms'
import { onWsMessage } from '@/ws/socket'

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

  onWsMessage((message) => {
    if (message.type === 'ROOMS_CHANGED') {
      rooms.value = message.rooms
    }

    if (message.type === 'SOCKET_OPEN') {
      fetchRooms()
    }
  })

  return { rooms, currentRoom, loading, error, fetchRooms, fetchRoom }
})