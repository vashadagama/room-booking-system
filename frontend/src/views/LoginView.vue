<script setup>
import { ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import { errorText } from '@/api/client'

const route = useRoute()
const router = useRouter()
const authStore = useAuthStore()

const form = ref({ username: '', password: '' })
const error = ref(null)
const loading = ref(false)

async function submit() {
  loading.value = true
  error.value = null
  try {
    await authStore.loginUser(form.value)
    router.push(route.query.from || '/')
  } catch (e) {
    error.value = errorText(e, 'Login failed')
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <section>
    <div class="panel-title">Login</div>

    <div class="form-container retro-panel">
      <div v-if="error" class="error-text">{{ error }}</div>

      <div class="form-group">
        <label>Username</label>
        <input v-model="form.username" type="text" class="retro-input" />
      </div>

      <div class="form-group">
        <label>Password</label>
        <input v-model="form.password" type="password" class="retro-input" />
      </div>

      <button class="retro-btn primary" :disabled="loading" @click="submit">
        {{ loading ? 'Logging in...' : 'Login' }}
      </button>

      <div class="form-footer">
        Don't have an account?
        <router-link to="/register">Register</router-link>
      </div>
    </div>
  </section>
</template>

<style scoped>
.form-container {
  max-width: 400px;
  padding: 20px;
}

.form-group {
  margin-bottom: 12px;
}

.form-group label {
  display: block;
  margin-bottom: 4px;
  font-size: 12px;
  font-weight: bold;
}

.form-group input {
  width: 100%;
}

.error-text {
  color: var(--danger);
  margin-bottom: 12px;
  font-size: 12px;
}

.form-footer {
  margin-top: 12px;
  font-size: 12px;
  text-align: center;
}
</style>
