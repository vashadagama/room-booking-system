<script setup>
import { computed, onUnmounted, ref, watch } from 'vue'
import { useAuthStore } from '@/stores/auth'
import { submitMinesweeperResult } from '@/api/games'
import SolitairePanel from '@/components/features/SolitairePanel.vue'

const authStore = useAuthStore()

const COLS = 9
const ROWS = 9
const MINES = 10
const TOTAL = COLS * ROWS

const cells = ref([])
const started = ref(false)
const over = ref(false)
const won = ref(false)
const flags = ref(0)
const seconds = ref(0)
const exploded = ref(-1)
const award = ref(null)

let timerId = null

const minesLeft = computed(() => String(MINES - flags.value).padStart(3, '0'))
const timeText = computed(() => String(seconds.value).padStart(3, '0'))
const face = computed(() => {
  if (!over.value) return '🙂'
  return won.value ? '😀' : '💀'
})

/** Сколько флажков стояло на минах — из этого складываются очки. */
const correctFlags = computed(() => cells.value.filter(c => c.flagged && c.mine).length)

async function reportWin() {
  if (!authStore.isAuthenticated) return
  try {
    const res = await submitMinesweeperResult(seconds.value, correctFlags.value)
    award.value = res.data
  } catch (e) {
    award.value = null
  }
}

function rowOf(idx) {
  return Math.floor(idx / COLS) + 1
}
function colOf(idx) {
  return (idx % COLS) + 1
}

function neighbors(idx) {
  const r = Math.floor(idx / COLS)
  const c = idx % COLS
  const result = []
  for (let dr = -1; dr <= 1; dr++) {
    for (let dc = -1; dc <= 1; dc++) {
      if (dr === 0 && dc === 0) continue
      const nr = r + dr
      const nc = c + dc
      if (nr < 0 || nr >= ROWS || nc < 0 || nc >= COLS) continue
      result.push(nr * COLS + nc)
    }
  }
  return result
}

function reset() {
  stopTimer()
  award.value = null
  cells.value = Array.from({ length: TOTAL }, () => ({
    mine: false,
    revealed: false,
    flagged: false,
    adjacent: 0
  }))
  started.value = false
  over.value = false
  won.value = false
  flags.value = 0
  seconds.value = 0
  exploded.value = -1
}

function placeMines(safeIdx) {
  const prevFlags = cells.value.map(c => c.flagged)
  const banned = new Set([safeIdx, ...neighbors(safeIdx)])
  const pool = []
  for (let i = 0; i < TOTAL; i++) {
    if (!banned.has(i)) pool.push(i)
  }
  for (let i = pool.length - 1; i > 0; i--) {
    const j = Math.floor(Math.random() * (i + 1))
    const tmp = pool[i]
    pool[i] = pool[j]
    pool[j] = tmp
  }
  const mineSet = new Set(pool.slice(0, MINES))

  cells.value = Array.from({ length: TOTAL }, (_, i) => ({
    mine: mineSet.has(i),
    revealed: false,
    flagged: prevFlags[i],
    adjacent: 0
  }))
  for (let i = 0; i < TOTAL; i++) {
    cells.value[i].adjacent = neighbors(i).reduce(
      (sum, n) => sum + (mineSet.has(n) ? 1 : 0),
      0
    )
  }
}

function startTimer() {
  timerId = setInterval(() => {
    seconds.value++
  }, 1000)
}

function stopTimer() {
  if (timerId !== null) {
    clearInterval(timerId)
    timerId = null
  }
}

function flood(idx) {
  const stack = [idx]
  while (stack.length) {
    const current = stack.pop()
    const cell = cells.value[current]
    if (cell.revealed || cell.flagged) continue
    cell.revealed = true
    if (cell.adjacent === 0) {
      for (const n of neighbors(current)) stack.push(n)
    }
  }
}

function checkWin() {
  const cleared = cells.value.filter(c => !c.mine && c.revealed).length
  if (cleared === TOTAL - MINES) {
    won.value = true
    endGame()
  }
}

function endGame() {
  over.value = true
  stopTimer()
  if (!won.value) {
    for (const cell of cells.value) {
      if (cell.mine) cell.revealed = true
    }
  }
}

function onClick(idx) {
  if (over.value) return
  const cell = cells.value[idx]
  if (cell.revealed || cell.flagged) return

  if (!started.value) {
    started.value = true
    placeMines(idx)
    startTimer()
  }

  const target = cells.value[idx]
  if (target.mine) {
    target.revealed = true
    exploded.value = idx
    won.value = false
    endGame()
    return
  }
  flood(idx)
  checkWin()
}

// Правый клик — флажок.
function onContext(idx) {
  if (over.value) return
  const cell = cells.value[idx]
  if (!cell.revealed || cell.mine || cell.adjacent === 0) return

  const around = neighbors(idx)
  if (around.filter(n => cells.value[n].flagged).length !== cell.adjacent) return

  for (const n of around) {
    if (cells.value[n].flagged || cells.value[n].revealed) continue
    if (cells.value[n].mine) {
      cells.value[n].revealed = true
      exploded.value = n
      won.value = false
      endGame()
      return
    }
    flood(n)
  }
  checkWin()
}

function onDblClick(idx) {
  if (e.button !== 1) return
  e.preventDefault()
  chord(idx)
}

function cellClass(cell) {
  return {
    open: cell.revealed,
    boom: cell.revealed && cell.mine,
    flagged: cell.flagged
  }
}

function cellLabel(cell, idx) {
  const at = `строка ${rowOf(idx)}, колонка ${colOf(idx)}`
  if (cell.flagged) return `${at}: флажок`
  if (cell.revealed) {
    if (cell.mine) return `${at}: мина`
    return `${at}: ${cell.adjacent}`
  }
  return `${at}: закрыто`
}

const selected = ref(null)

const TITLES = { minesweeper: 'Minesweeper', solitaire: 'Solitaire' }
const ICONS = { minesweeper: '💣', solitaire: '♠' }
const title = computed(() => (selected.value ? TITLES[selected.value] : 'Games'))
const icon = computed(() => (selected.value ? ICONS[selected.value] : '🎮'))

function openGame(id) {
  selected.value = id
}

function backToList() {
  selected.value = null
}

// Победа засчитывается один раз: за watches следим, а не пишем в onClick.
watch(won, (isWin) => {
  if (isWin) reportWin()
})

reset()
onUnmounted(stopTimer)
</script>

<template>
  <div class="games-card retro-panel" :class="{ expanded: selected, [selected]: selected }">
    <div class="card-header">
      <span class="card-icon">{{ icon }}</span>
      <span class="card-name">{{ title }}</span>
      <button
        v-if="selected"
        type="button"
        class="retro-btn back-btn"
        @click="backToList"
      >
        ◀ Games
      </button>
    </div>

    <!-- Список игр: карточка занимает один ряд, как обычная комната. -->
    <div v-show="!selected" class="card-body retro-panel-inset">
      <ul class="game-list">
        <li>
          <button type="button" class="retro-btn game-btn" @click="openGame('minesweeper')">
            💣 Minesweeper
          </button>
        </li>
        <li>
          <button type="button" class="retro-btn game-btn" @click="openGame('solitaire')">
            ♠ Solitaire
          </button>
        </li>
        <li>
          <button type="button" class="retro-btn game-btn" disabled>Coming soon…</button>
        </li>
      </ul>
    </div>

    <div v-if="selected === 'minesweeper'" class="card-body retro-panel-inset">
      <div class="hud">
        <span class="led">{{ minesLeft }}</span>
        <button
          type="button"
          class="retro-btn face"
          title="Новая игра"
          aria-label="Новая игра"
          @click="reset"
        >
          {{ face }}
        </button>
        <span class="led">{{ timeText }}</span>
      </div>

      <p v-if="award" class="award">
        Победа записана · всего {{ award.score }} очков
      </p>
      <p v-else-if="won && !authStore.isAuthenticated" class="award muted">
        Войдите в аккаунт, чтобы очки считались в профиле
      </p>

      <div class="board">
        <button
          v-for="(cell, i) in cells"
          :key="i"
          type="button"
          class="cell"
          :class="cellClass(cell)"
          :aria-label="cellLabel(cell, i)"
          @click="onClick(i)"
          @dblclick.prevent="onDblClick(i)"
          @mousedown="onMouseDown(i, $event)"
          @contextmenu.prevent="onContext(i)"
        >
          <span v-if="cell.flagged" class="mark" :class="{ wrong: !cell.mine && over && !won }">
            {{ !cell.mine && over && !won ? '✖' : '🚩' }}
          </span>
          <span v-else-if="cell.revealed && cell.mine" class="mark">
            {{ i === exploded ? '💥' : '💣' }}
          </span>
          <span v-else-if="cell.revealed && cell.adjacent" :class="['n', 'n' + cell.adjacent]">
            {{ cell.adjacent }}
          </span>
        </button>
      </div>
    </div>

    <div v-if="selected === 'solitaire'" class="card-body retro-panel-inset">
      <SolitairePanel />
    </div>

    <div v-if="!selected" class="card-footer">
      <span class="hint">3 games</span>
    </div>
  </div>
</template>

<style scoped>
/* Карточка повторяет обвязку .room-card, но без hover-эффекта:
   это не ссылка, и подсветка мешала бы прицеливаться в поле.
   Свёрнутая занимает один ряд, как комната; развёрнутая — span 2,
   то есть высоту двух обычных карточек. */
.games-card {
  display: flex;
  flex-direction: column;
  padding: 0;
}
.games-card.expanded {
  grid-row: span 2;
}

/*
  Пасьянс требует места с двух сторон: семь рядов с разъезжающимися
  картами в высоту двух карточек комнаты не помещаются, а семи колонок
  по 38px карты в одну колонку сетки тесно. Поэтому он занимает три
  ряда и две колонки. На узком экране, где в сетке всего одна колонка,
  остаётся одна колонка, а карты уменьшаются.
*/
.games-card.expanded.solitaire {
  grid-row: span 3;
  grid-column: span 2;
}

@media (max-width: 613px) {
  .games-card.expanded.solitaire {
    grid-column: span 1;
  }
}

.card-header {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 6px 8px;
  background: linear-gradient(90deg, var(--accent-dark), var(--accent-light));
  color: #fff;
  font-weight: bold;
}
.card-icon { font-size: 14px; }
.card-name { font-size: 13px; }

/* Кнопка «назад» живёт в заголовке, чтобы не тратить высоту карточки.
   line-height и padding подобраны так, чтобы она не раздувала
   заголовок выше, чем у карточки комнаты. */
.back-btn {
  margin-left: auto;
  padding: 0 5px;
  font-size: 10px;
  line-height: 1.3;
}

.game-list {
  list-style: none;
  margin: 0;
  padding: 0;
  width: 100%;
  display: flex;
  flex-direction: column;
  gap: 4px;
}
.game-btn {
  width: 100%;
  justify-content: flex-start;
}

.card-footer {
  padding: 2px 10px 6px;
  text-align: right;
}
.hint {
  color: var(--accent);
  font-size: 11px;
}

.card-body {
  /* Поле квадратное, поэтому ячейки всегда квадратные: ширина равна
     высоте, а 9 колонок делят её поровну. 250px подобрано так, чтобы
     развёрнутая карточка укладывалась ровно в две строки сетки и не
     раздувала ряды, в которые попадает. На узких экранах поле
     сжимается пропорционально, ячейки остаются квадратными. */
  --board-size: 250px;

  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  /* Лишняя высота уходит в равные отступы, а не прижимает поле к верху. */
  justify-content: center;
  gap: 6px;
  padding: 6px;
  margin: 4px;
}

.hud,
.board {
  width: 100%;
  max-width: var(--board-size);
}

.hud {
  display: flex;
  align-items: center;
  justify-content: space-between;
}
.led {
  min-width: 46px;
  padding: 1px 5px;
  text-align: center;
  font-family: var(--font-mono);
  font-size: 14px;
  color: var(--danger-light);
  background: var(--accent-dark);
  border: 1px solid var(--border-mid);
  box-shadow:
    inset 1px 1px 0 #16222d,
    inset -1px -1px 0 var(--accent-light);
}
.face {
  padding: 1px 7px;
  font-size: 14px;
  line-height: 1.2;
}

.board {
  display: grid;
  grid-template-columns: repeat(9, 1fr);
  grid-auto-rows: 1fr;
  /* Квадратное поле: высота равна ширине, поэтому ячейки квадратные. */
  aspect-ratio: 1;
  gap: 1px;
  padding: 3px;
  background: var(--surface);
  border: 1px solid var(--border-mid);
  box-shadow:
    inset 1px 1px 0 var(--border-dark),
    inset -1px -1px 0 var(--border-light);
}

.cell {
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 0;
  font-family: var(--font);
  font-size: clamp(11px, 1.2vw, 16px);
  font-weight: bold;
  color: var(--text);
  background: var(--surface);
  border: 1px solid var(--border-mid);
  box-shadow:
    inset 1px 1px 0 var(--border-light),
    inset -1px -1px 0 var(--border-dark);
  cursor: pointer;
  user-select: none;
}
.cell:not(.open):hover { background: var(--surface-hover); }
.cell:not(.open):active {
  box-shadow:
    inset -1px -1px 0 var(--border-light),
    inset 1px 1px 0 var(--border-dark);
}
.cell:focus-visible {
  outline: 2px solid var(--accent);
  outline-offset: -2px;
}

/* Открытая клетка «утоплена», как в настоящем сапёре. */
.cell.open {
  background: var(--surface-alt);
  box-shadow:
    inset 1px 1px 0 var(--border-dark),
    inset -1px -1px 0 var(--border-light);
  cursor: default;
}
.cell.boom {
  background: var(--danger-bg);
  border-color: var(--danger);
}

.award {
  font-size: 11px;
  font-weight: bold;
  color: var(--success);
  text-align: center;
}

.award.muted {
  font-weight: normal;
  color: var(--text-muted);
}

.mark { font-size: 0.85em; }
.mark.wrong { color: var(--danger); }

.n1 { color: var(--accent); }
.n2 { color: var(--success); }
.n3 { color: var(--danger); }
.n4 { color: var(--accent-dark); }
.n5 { color: var(--warning); }
.n6 { color: #4a7c79; }
.n7 { color: var(--text); }
.n8 { color: var(--text-muted); }
</style>