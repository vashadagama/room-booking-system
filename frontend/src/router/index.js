import { createRouter, createWebHistory } from 'vue-router'
import RoomListView from '@/views/RoomListView.vue'
import RoomDetailView from '@/views/RoomDetailView.vue'
import MyBookingsView from '@/views/MyBookingsView.vue'

const routes = [
  { path: '/', name: 'rooms', component: RoomListView },
  { path: '/rooms/:id', name: 'room-detail', component: RoomDetailView, props: true },
  { path: '/my-bookings', name: 'my-bookings', component: MyBookingsView }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

export default router