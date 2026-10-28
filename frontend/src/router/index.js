import { createRouter, createWebHistory } from 'vue-router'
import RoomListView from '@/views/RoomListView.vue'
import RoomDetailView from '@/views/RoomDetailView.vue'
import MyBookingsView from '@/views/MyBookingsView.vue'
import LoginView from '@/views/LoginView.vue'
import RegisterView from '@/views/RegisterView.vue'
import ProfileView from '@/views/ProfileView.vue'
import AdminView from '@/views/AdminView.vue'
import { useAuthStore } from '@/stores/auth'

const routes = [
  { path: '/', name: 'rooms', component: RoomListView },
  { path: '/rooms/:id', name: 'room-detail', component: RoomDetailView, props: true },
  { path: '/my-bookings', name: 'my-bookings', component: MyBookingsView, meta: { requiresAuth: true } },
  { path: '/login', name: 'login', component: LoginView },
  { path: '/register', name: 'register', component: RegisterView },
  { path: '/profile', name: 'profile', component: ProfileView, meta: { requiresAuth: true } },
  { path: '/admin', name: 'admin', component: AdminView, meta: { requiresAuth: true, requiresAdmin: true } }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

router.beforeEach((to) => {
  if (!to.meta.requiresAuth) return true

  const authStore = useAuthStore()
  if (!authStore.isAuthenticated) {
    return { name: 'register', query: { from: to.fullPath } }
  }

  if (to.meta.requiresAdmin && authStore.user?.role && !authStore.isAdmin) {
    return { name: 'profile' }
  }

  return true
})

export default router