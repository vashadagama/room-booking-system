<script setup>
import { computed, onMounted, onUnmounted, ref, watch } from 'vue'
import { useAuthStore } from '@/stores/auth'
import { submitSolitaireResult } from '@/api/games'
import {
  RANKS,
  SUITS,
  allCardsRevealed,
  canDropOnFoundation,
  canDropOnTableau,
  cardLabel,
  deal,
  drawFromStock,
  hasAnyMove,
  hasBoardMove,
  isRed,
  moveTableauToFoundation,
  moveTableauToTableau,
  moveWasteToFoundation,
  moveWasteToTableau,
  planAutoFinish,
  rankOf,
  suitOf,
  undo
} from '@/games/solitaire'

/**
 * Карточка в развёрнутом ряду сдвигается по вертикали, а карты в ряду
 * наезжают друг на друга. Шаг один и для открытых, и для закрытых:
 * иначе стопка «ёлает» на стыке двух разных шагов. Держи его в паре
 * с `min-height` у `.sol-tableau` — двенадцать таких промежутков.
 */
const FAN = 10

const authStore = useAuthStore()

const state = ref(deal())
const seconds = ref(0)
const award = ref(null)
const notice = ref('')
/** Кликом выбранная карта — второй способ ходить, без перетаскивания. */
const picked = ref(null)
/** Перетаскивание: карта следует за курсором, место сброса подсвечивается. */
const drag = ref(null)

const PICK_WASTE = 'waste'
/** Сдвиг мыши короче этого считаем кликом, а не перетаскиванием. */
const DRAG_THRESHOLD = 5

let timerId = null

const stockText = computed(() => String(state.value.stock.length).padStart(2, '0'))
const timeText = computed(() => String(seconds.value).padStart(3, '0'))
const wasteTop = computed(() => {
  const waste = state.value.waste
  return waste.length ? waste[waste.length - 1] : null
})
const foundationTop = (suit) => {
  const pile = state.value.foundations[suit]
  return pile.length ? pile[pile.length - 1] : null
}

/** Тупик: партия не выиграна и ни одного хода не осталось. */
const stuck = computed(() => !state.value.won && !hasAnyMove(state.value))

/** Ходов по столу нет — остаётся только перебирать колоду. */
const onlyDeck = computed(() => !state.value.won && !hasBoardMove(state.value))
/** Сколько карт уедет вместе с перетаскиваемой — для подсказки на призраке. */
const dragCount = computed(() => {
  if (!drag.value || drag.value.source === PICK_WASTE) return 1
  return state.value.tableau[drag.value.source].length - drag.value.index
})

function stopTimer() {
  if (timerId !== null) {
    clearInterval(timerId)
    timerId = null
  }
}

function newGame() {
  stopTimer()
  state.value = deal()
  seconds.value = 0
  picked.value = null
  award.value = null
  notice.value = ''
  autoStarted = false
  autoMoves.value = 0
  autoplay.value = false
  clearDrag()
  maybeAutoFinish()
}

const autoplay = ref(false)
const autoMoves = ref(0)
let autoStarted = false

/** Ходы, сделанные игроком: автодобор в очки за аккуратность не идёт. */
const playerMoves = computed(() => Math.max(0, state.value.moves - autoMoves.value))

const sleep = (ms) => new Promise((resolve) => setTimeout(resolve, ms))

/** Где сейчас лежит карта: в отбое или в каком ряду. */
function locateCard(card) {
  if (wasteTop.value === card) return { source: PICK_WASTE, index: 0 }
  for (let pile = 0; pile < state.value.tableau.length; pile++) {
    const index = state.value.tableau[pile].indexOf(card)
    if (index !== -1) return { source: pile, index }
  }
  return null
}

/** Карта-призрак: летит из ряда в своё основание и гаснет над ним. */
function launchFly(card, from, to, delay, duration) {
  const fly = document.createElement('div')
  fly.className = 'sol-fly' + (isRed(card) ? ' red' : '')
  fly.style.left = `${from.left}px`
  fly.style.top = `${from.top}px`
  fly.style.width = `${from.width}px`
  fly.style.height = `${from.height}px`

  const corner = document.createElement('span')
  corner.className = 'corner'
  corner.textContent = RANKS[rankOf(card)]
  const suitMark = document.createElement('i')
  suitMark.textContent = SUITS[suitOf(card)]
  corner.appendChild(suitMark)

  const pip = document.createElement('span')
  pip.className = 'pip'
  pip.textContent = SUITS[suitOf(card)]

  fly.append(corner, pip)
  document.body.appendChild(fly)

  const anim = fly.animate(
    [
      { transform: 'translate(0, 0) scale(1)', opacity: 1, offset: 0 },
      {
        transform: `translate(${to.left - from.left}px, ${to.top - from.top}px) scale(0.68)`,
        opacity: 0.25,
        offset: 0.75
      },
      {
        transform: `translate(${to.left - from.left}px, ${to.top - from.top}px) scale(0.6)`,
        opacity: 0,
        offset: 1
      }
    ],
    { duration, delay, easing: 'cubic-bezier(0.3, 0, 0.2, 1)', fill: 'both' }
  )
  const remove = () => fly.remove()
  anim.addEventListener('finish', remove)
  // Подстраховка на случай, если анимации не проиграются: призрак
  // не должен остаться висеть поверх стола.
  setTimeout(remove, delay + duration + 150)
}

/**
 * Проигрывает план до победы: карты улетают в основания каскадом.
 * Игрок в это время не вмешивается, таймер остановлен — партию он
 * фактически доиграл сам, до последнего хода.
 */
async function runAutoFinish(plan) {
  autoplay.value = true
  stopTimer()

  const STAGGER = 70
  const DURATION = 340

  for (let i = 0; i < plan.length; i++) {
    // Игрок мог начать новую партию — тогда доигрывать нечего.
    if (!autoplay.value) return
    const step = plan[i]
    const spot = locateCard(step.card)
    if (!spot) break

    const cardEl = document.querySelector(`.sol-card[data-card="${step.card}"]`)
    const foundationEl = document.querySelector(`.sol-foundation[data-foundation="${suitOf(step.card)}"]`)
    const from = cardEl ? cardEl.getBoundingClientRect() : null
    const to = foundationEl ? foundationEl.getBoundingClientRect() : null

    let moved = false
    if (step.to === 'tableau') {
      moved = moveTableauToTableau(state.value, spot.source, step.pile, spot.index)
    } else if (spot.source === PICK_WASTE) {
      moved = moveWasteToFoundation(state.value)
    } else {
      moved = moveTableauToFoundation(state.value, spot.source, spot.index)
    }
    if (!moved) break

    autoMoves.value++
    if (step.to === 'foundation' && from && to) {
      launchFly(step.card, from, to, i * STAGGER, DURATION)
    }
    await sleep(16)
  }

  await sleep(plan.length * STAGGER + DURATION)
  autoplay.value = false
}

/** Когда закрывать больше нечего, партия доигрывается сама. */
function maybeAutoFinish() {
  if (state.value.won || autoplay.value || autoStarted) return
  if (!allCardsRevealed(state.value)) return
  const plan = planAutoFinish(state.value)
  if (!plan || !plan.length) return
  autoStarted = true
  runAutoFinish(plan)
}

watch(() => [state.value.moves, state.value.stock.length, state.value.waste.length], maybeAutoFinish)

function offset(pileIndex, cardIndex) {
  return { top: `${cardIndex * FAN}px` }
}

function isPicked(source, index) {
  return picked.value?.source === source && picked.value?.index === index
}

function isDragging(source, index) {
  return drag.value?.source === source && drag.value?.index === index
}

function startDrag(event, source, index, card) {
  if (state.value.won || autoplay.value) return
  // Тянуть можно только открытые карты.
  if (source !== PICK_WASTE && !state.value.faceUp.has(card)) return
  if (event.button !== 0 && event.pointerType === 'mouse') return

  const rect = event.currentTarget.getBoundingClientRect()
  drag.value = {
    source,
    index,
    card,
    grabX: event.clientX - rect.left,
    grabY: event.clientY - rect.top,
    x: event.clientX,
    y: event.clientY,
    startX: event.clientX,
    startY: event.clientY,
    moved: false,
    over: null,
    valid: false
  }
  picked.value = null
  notice.value = ''

  window.addEventListener('pointermove', onDragMove)
  window.addEventListener('pointerup', onDragEnd)
  window.addEventListener('pointercancel', onDragCancel)
}

function onDragMove(event) {
  const current = drag.value
  if (!current) return
  current.x = event.clientX
  current.y = event.clientY
  if (Math.abs(event.clientX - current.startX) > DRAG_THRESHOLD
    || Math.abs(event.clientY - current.startY) > DRAG_THRESHOLD) {
    current.moved = true
  }
  if (!current.moved) return

  const target = hitTest(event.clientX, event.clientY)
  current.over = target
  current.valid = !!target && checkTarget(current, target)
}

/** Что под курсором: ряд или основание. Призрак не перехватывает события. */
function hitTest(x, y) {
  const element = document.elementFromPoint(x, y)
  if (!element) return null

  const pile = element.closest('.sol-pile')
  if (pile) {
    return { kind: 'tableau', index: Number(pile.dataset.pile) }
  }
  const foundation = element.closest('.sol-foundation')
  if (foundation) {
    return { kind: 'foundation', index: Number(foundation.dataset.foundation) }
  }
  return null
}

function checkTarget(source, target) {
  if (target.kind === 'foundation') {
    return canDropOnFoundation(state.value, source.source, source.index)
  }
  return canDropOnTableau(state.value, source.source, source.index, target.index)
}

function onDragEnd(event) {
  const current = drag.value
  if (!current) return

  if (!current.moved) {
    // Короткое нажатие без перетаскивания — обычный выбор карты.
    pick(current.source, current.index)
    clearDrag()
    return
  }

  if (current.over && current.valid) {
    applyDrop(current.source, current.index, current.over)
  } else if (current.over && !current.valid) {
    notice.value = 'Так ходить нельзя'
  }
  clearDrag()
}

function onDragCancel() {
  clearDrag()
}

function clearDrag() {
  drag.value = null
  window.removeEventListener('pointermove', onDragMove)
  window.removeEventListener('pointerup', onDragEnd)
  window.removeEventListener('pointercancel', onDragCancel)
}

function applyDrop(source, index, target) {
  if (autoplay.value) return false
  let moved = false
  if (target.kind === 'foundation') {
    moved = source === PICK_WASTE
      ? moveWasteToFoundation(state.value)
      : moveTableauToFoundation(state.value, source, index)
  } else if (source === PICK_WASTE) {
    moved = moveWasteToTableau(state.value, target.index)
  } else if (source !== target.index) {
    moved = moveTableauToTableau(state.value, source, target.index, index)
  }

  if (moved) {
    picked.value = null
    if (state.value.won) finish()
  } else {
    notice.value = 'Так ходить нельзя'
  }
  return moved
}

function pick(source, index) {
  if (state.value.won || autoplay.value) return
  notice.value = ''
  picked.value = isPicked(source, index) ? null : { source, index }
}

/** Клик по ряду или основанию: сначала снимаем выбор, затем кладём. */
function dropOn(target) {
  if (state.value.won || autoplay.value) return
  const choice = picked.value
  if (!choice) return
  applyDrop(choice.source, choice.index, target)
}

function undoMove() {
  if (state.value.won || autoplay.value) return
  if (undo(state.value)) {
    picked.value = null
    notice.value = ''
    autoStarted = false
  }
}

/** Кнопки отмены в карточке нет, поэтому ход отменяется с клавиатуры. */
function onKeyDown(event) {
  if ((event.ctrlKey || event.metaKey) && event.key.toLowerCase() === 'z') {
    event.preventDefault()
    undoMove()
  }
}

function draw() {
  if (state.value.won || autoplay.value) return
  notice.value = ''
  picked.value = null
  drawFromStock(state.value)
}

async function finish() {
  stopTimer()
  if (!authStore.isAuthenticated) return
  try {
    const res = await submitSolitaireResult(seconds.value, playerMoves.value)
    award.value = res.data
  } catch (e) {
    award.value = null
  }
}

// Победа приходит из движка после последнего хода — ловим её здесь.
watch(
  () => state.value.won,
  (isWin) => {
    if (isWin) finish()
  }
)

onMounted(() => {
  window.addEventListener('keydown', onKeyDown)
})

onUnmounted(() => {
  window.removeEventListener('keydown', onKeyDown)
  stopTimer()
  clearDrag()
})
newGame()
</script>

<template>
  <div class="sol" :class="{ dragging: !!drag }">
    <div class="sol-hud">
      <span class="sol-counter">{{ stockText }}</span>
      <button
        type="button"
        class="retro-btn sol-face"
        title="Новая партия"
        aria-label="Новая партия"
        @click="newGame"
      >
        {{ state.won ? '🏆' : '♠' }}
      </button>
      <span class="sol-counter">{{ timeText }}</span>
    </div>

    <div class="sol-field">
      <!-- Колода, отбой и четыре основания -->
      <div class="sol-top">
        <button
          type="button"
          class="sol-card stock"
          :class="{ empty: state.stock.length === 0 }"
          :aria-label="state.stock.length ? 'Взять карту из колоды' : 'Перевернуть колоду'"
          @click="draw"
        >
          <span v-if="state.stock.length === 0" class="recycle">↻</span>
        </button>

        <div class="sol-slot">
          <button
            v-if="wasteTop !== null"
            type="button"
            class="sol-card face"
            :data-card="wasteTop"
            :class="{ red: isRed(wasteTop), picked: picked?.source === PICK_WASTE }"
            :aria-label="cardLabel(wasteTop)"
            @click="pick(PICK_WASTE, 0)"
            @dblclick="pick(PICK_WASTE, 0); dropOn({ kind: 'foundation', index: suitOf(wasteTop) })"
            @pointerdown="startDrag($event, PICK_WASTE, 0, wasteTop)"
          >
            <span class="corner">{{ RANKS[rankOf(wasteTop)] }}<i>{{ SUITS[suitOf(wasteTop)] }}</i></span>
            <span class="pip">{{ SUITS[suitOf(wasteTop)] }}</span>
          </button>
          <span v-else class="sol-card face empty" aria-hidden="true"></span>
        </div>

        <span class="sol-gap"></span>

        <button
          v-for="suit in 4"
          :key="'f' + suit"
          type="button"
          class="sol-foundation"
          :data-foundation="suit - 1"
          :class="{
            'drop-ok': drag?.over?.kind === 'foundation' && drag.over.index === suit - 1 && drag.valid,
            'drop-no': drag?.over?.kind === 'foundation' && drag.over.index === suit - 1 && !drag.valid
          }"
          :aria-label="`Основание ${SUITS[suit - 1]}`"
          @click="dropOn({ kind: 'foundation', index: suit - 1 })"
        >
          <span
            v-if="foundationTop(suit - 1) !== null"
            class="sol-card face"
            :class="{ red: isRed(foundationTop(suit - 1)) }"
          >
            <span class="corner">
              {{ RANKS[rankOf(foundationTop(suit - 1))] }}<i>{{ SUITS[suitOf(foundationTop(suit - 1))] }}</i>
            </span>
            <span class="pip">{{ SUITS[suitOf(foundationTop(suit - 1))] }}</span>
          </span>
          <span v-else class="sol-card face empty">
            <span class="ghost">{{ SUITS[suit - 1] }}</span>
          </span>
        </button>
      </div>

      <!-- Семь рядов -->
      <div class="sol-tableau">
        <div
          v-for="pileIndex in 7"
          :key="'p' + pileIndex"
          class="sol-pile"
          :data-pile="pileIndex - 1"
          :class="{
            'drop-ok': drag?.over?.kind === 'tableau' && drag.over.index === pileIndex - 1 && drag.valid,
            'drop-no': drag?.over?.kind === 'tableau' && drag.over.index === pileIndex - 1 && !drag.valid
          }"
          @click="dropOn({ kind: 'tableau', index: pileIndex - 1 })"
        >
          <span v-if="state.tableau[pileIndex - 1].length === 0" class="sol-card face empty"></span>
          <button
            v-for="(card, cardIndex) in state.tableau[pileIndex - 1]"
            :key="card"
            type="button"
            class="sol-card face"
            :data-card="card"
            :class="{
              up: state.faceUp.has(card),
              down: !state.faceUp.has(card),
              red: isRed(card),
              picked: isPicked(pileIndex - 1, cardIndex),
              lifted: isDragging(pileIndex - 1, cardIndex)
            }"
            :style="offset(pileIndex - 1, cardIndex)"
            :aria-label="state.faceUp.has(card) ? cardLabel(card) : 'Закрытая карта'"
            :aria-pressed="isPicked(pileIndex - 1, cardIndex)"
            @click.stop="pick(pileIndex - 1, cardIndex)"
            @dblclick.stop="pick(pileIndex - 1, cardIndex); dropOn({ kind: 'foundation', index: suitOf(card) })"
            @pointerdown.stop="startDrag($event, pileIndex - 1, cardIndex, card)"
          >
            <template v-if="state.faceUp.has(card)">
              <span class="corner">{{ RANKS[rankOf(card)] }}<i>{{ SUITS[suitOf(card)] }}</i></span>
              <span class="pip">{{ SUITS[suitOf(card)] }}</span>
            </template>
          </button>
        </div>
      </div>
    </div>

    <p v-if="state.won" class="sol-result">
      Собрано! Ходов: {{ playerMoves }} · {{ timeText }} с
      <template v-if="award">&nbsp;· всего {{ award.score }} очков</template>
      <template v-else-if="!authStore.isAuthenticated">
        &nbsp;· войдите в аккаунт, чтобы очки считались в профиле
      </template>
    </p>
    <p v-else-if="stuck" class="sol-notice sol-stuck">
      Ходов больше нет — партия зашла в тупик. Начните новую.
    </p>
    <p v-else-if="onlyDeck" class="sol-notice">
      Ходов по столу нет — осталось только перебирать колоду.
    </p>
    <p v-else-if="notice" class="sol-notice">{{ notice }}</p>
    <p v-else class="sol-hint">Тащите карту мышью на ряд или основание.</p>

    <!-- Призрак перетаскиваемой карты -->
    <div
      v-if="drag && drag.moved"
      class="sol-ghost"
      :class="{ red: isRed(drag.card), ok: drag.valid }"
      :style="{
        left: `${drag.x - drag.grabX}px`,
        top: `${drag.y - drag.grabY}px`
      }"
    >
      <span class="corner">{{ RANKS[rankOf(drag.card)] }}<i>{{ SUITS[suitOf(drag.card)] }}</i></span>
      <span class="pip">{{ SUITS[suitOf(drag.card)] }}</span>
      <span v-if="dragCount > 1" class="ghost-count">{{ dragCount }}</span>
    </div>
  </div>
</template>

<style scoped>
/*
  Раскрытый пасьянс занимает три ряда сетки и две колонки: семи
  колонок по 38px карты в одну колонку сетки не хватает, а в две
  помещаются настоящие карты. На совсем узком экране остаётся
  одна колонка, и карты уменьшаются.
*/
.sol {
  --card-w: 58px;
  --card-h: 81px;
  --gap: 4px;

  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
  width: 100%;
  min-width: 0;
}

.sol.dragging {
  user-select: none;
  cursor: grabbing;
}

.sol-hud {
  display: flex;
  align-items: center;
  justify-content: space-between;
  width: 100%;
  max-width: calc(var(--card-w) * 7 + var(--gap) * 6);
}

.sol-counter {
  min-width: 44px;
  padding: 2px 6px;
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

.sol-face {
  padding: 1px 10px;
  font-size: 15px;
  line-height: 1.4;
}

/* ---------- карта ---------- */

.sol-card {
  position: relative;
  display: block;
  width: var(--card-w);
  height: var(--card-h);
  padding: 3px;
  flex: none;
  text-align: left;
  overflow: hidden;
  font-family: var(--font);
  background: #fff;
  color: #1a1a1a;
  border: 1px solid #4a4640;
  border-radius: 3px;
  box-shadow:
    inset 1px 1px 0 rgba(255, 255, 255, 0.9),
    inset -1px -1px 0 rgba(0, 0, 0, 0.15),
    1px 1px 0 rgba(0, 0, 0, 0.2);
  cursor: pointer;
}

.sol-card.red { color: #c01818; }

.sol-card.up:hover { background: #fffdf0; }

/* Рубашка: ретро-узор в тон акцента, как у кнопок проекта. */
.sol-card.down,
.sol-card.stock:not(.empty) {
  background:
    repeating-linear-gradient(45deg,
      var(--accent) 0 3px,
      var(--accent-dark) 3px 7px);
  border-color: #2c4458;
}

.sol-card.stock:hover { filter: brightness(1.12); }

.sol-card.empty {
  cursor: default;
  background: var(--surface);
  border: 1px dashed var(--border-mid);
  box-shadow: none;
  display: flex;
  align-items: center;
  justify-content: center;
}

.sol-card.empty .ghost { color: var(--border-mid); font-size: 15px; }

.sol-card.picked {
  outline: 2px solid var(--warning);
  outline-offset: -2px;
}

/* Карта, которую сейчас тянут, на месте остаётся полупрозрачной. */
.sol-card.lifted {
  opacity: 0.35;
}

.sol-card .recycle {
  position: absolute;
  inset: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  color: var(--text-muted);
  font-size: 18px;
}

/* Достоинство и масть одной строкой в левом верхнем углу: в стопке
   каждая карта наезжает на следующую, и видна только эта полоска —
   значит подпись должна влезать в неё целиком.
   Позиционируем абсолютно: карты в рядах — это <button>, а браузер
   центрирует их содержимое по вертикали, и без этого подпись уезжала
   в середину карты вместо верхнего угла. */
.sol-card .corner {
  position: absolute;
  top: 3px;
  left: 4px;
  display: flex;
  flex-direction: row;
  align-items: baseline;
  gap: 2px;
  font-size: 12px;
  font-weight: bold;
  line-height: 1;
  white-space: nowrap;
}

.sol-card .corner i {
  font-style: normal;
  font-size: 13px;
}

.sol-card .pip {
  position: absolute;
  inset: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: calc(var(--card-h) * 0.34);
  line-height: 1;
  pointer-events: none;
}

/* ---------- место сброса ---------- */

.sol-pile,
.sol-foundation {
  position: relative;
  display: block;
  width: var(--card-w);
  height: var(--card-h);
  padding: 0;
  flex: none;
  border: 0;
  background: none;
}

.sol-pile .sol-card,
.sol-foundation .sol-card {
  position: absolute;
  left: 0;
  top: 0;
}

.sol-pile.drop-ok,
.sol-foundation.drop-ok {
  outline: 2px solid var(--success);
  outline-offset: 2px;
  border-radius: 3px;
}

.sol-pile.drop-no,
.sol-foundation.drop-no {
  outline: 2px solid var(--danger);
  outline-offset: 2px;
  border-radius: 3px;
}

/* ---------- верхний ряд ---------- */

.sol-top {
  display: flex;
  align-items: flex-start;
  gap: var(--gap);
  justify-content: center;
}

.sol-slot {
  position: relative;
  width: var(--card-w);
  height: var(--card-h);
  flex: none;
}

.sol-slot .sol-card {
  position: absolute;
  left: 0;
  top: 0;
}

/* Распорка между отбоем и тузами должна быть ровно в одну колонку:
   вместе с двумя промежутками по --gap она даёт верхнему ряду ту же
   ширину, что и раскладке из семи колонок, и карты встают друг над
   другом. Любая другая ширина сдвигает тузы относительно рядов. */
.sol-gap { width: var(--card-w); }

/* ---------- семь рядов ---------- */

.sol-tableau {
  display: flex;
  align-items: flex-start;
  gap: var(--gap);
  justify-content: center;
  /* Колода с основаниями и ряды — разные зоны, отодвигаем их друг
     от друга, иначе стопки визуальчно липнут к верхнему ряду. */
  margin-top: 10px;
  /* Место под длинный ряд зарезервировано заранее — иначе карточка
     менялась бы в размере посреди партии. Шаг у всех карт одинаковый,
     поэтому берём худший случай: 13 карт в одном ряду — это
     двенадцать промежутков по 10px. */
  min-height: calc(var(--card-h) + 12 * 10px);
}

/* ---------- низ ---------- */

.sol-result {
  margin: 0;
  font-size: 12px;
  font-weight: bold;
  color: var(--success);
  text-align: center;
}

.sol-notice {
  margin: 0;
  font-size: 12px;
  color: var(--danger);
  text-align: center;
}

/* Тупик — не ошибка хода, а конец партии, поэтому заметнее подсказки. */
.sol-stuck {
  padding: 3px 8px;
  font-weight: bold;
  border: 1px solid var(--danger);
  border-radius: 3px;
}

.sol-hint {
  margin: 0;
  font-size: 11px;
  color: var(--text-muted);
  text-align: center;
}

/* ---------- призрак под курсором ---------- */

.sol-ghost {
  position: fixed;
  z-index: 50;
  width: var(--card-w);
  height: var(--card-h);
  padding: 3px;
  pointer-events: none;
  text-align: left;
  background: #fff;
  color: #1a1a1a;
  border: 1px solid #4a4640;
  border-radius: 3px;
  box-shadow: 3px 3px 6px rgba(0, 0, 0, 0.35);
  transform: rotate(1.5deg);
}

.sol-ghost.red { color: #c01818; }

.sol-ghost.ok { border-color: var(--success); }

.sol-ghost .corner {
  display: flex;
  flex-direction: row;
  align-items: baseline;
  gap: 2px;
  font-size: 12px;
  font-weight: bold;
  line-height: 1;
  white-space: nowrap;
}

.sol-ghost .corner i { font-style: normal; font-size: 13px; }

.sol-ghost .pip {
  position: absolute;
  inset: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: calc(var(--card-h) * 0.34);
  line-height: 1;
}

.ghost-count {
  position: absolute;
  right: -6px;
  bottom: -6px;
  min-width: 16px;
  height: 16px;
  padding: 0 3px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 10px;
  font-weight: bold;
  color: #fff;
  background: var(--accent-dark);
  border: 1px solid var(--border-dark);
  border-radius: 50%;
}

/* ---------- узкий экран ---------- */

@media (max-width: 885px) {
  .sol {
    --card-w: 42px;
    --card-h: 59px;
  }

  .sol-gap { width: var(--card-w); }

  .sol-tableau {
    min-height: calc(var(--card-h) + 12 * 10px);
  }

  .sol-card .corner { font-size: 9px; }
  .sol-card .corner i { font-size: 10px; }
}
</style>
<!-- Призрак автодобора живёт вне компонента, поэтому стили без scope:
     иначе scoped-атрибута на нём не будет и карта останется без облика. -->
<style>
.sol-fly {
  position: fixed;
  z-index: 60;
  box-sizing: border-box;
  padding: 3px;
  pointer-events: none;
  background: #fff;
  color: #1a1a1a;
  border: 1px solid #4a4640;
  border-radius: 3px;
  box-shadow:
    inset 1px 1px 0 rgba(255, 255, 255, 0.9),
    2px 2px 4px rgba(0, 0, 0, 0.3);
  font-family: var(--font);
}

.sol-fly.red { color: #c01818; }

.sol-fly .corner {
  position: absolute;
  top: 3px;
  left: 4px;
  display: flex;
  align-items: baseline;
  gap: 2px;
  font-size: 12px;
  font-weight: bold;
  line-height: 1;
}

.sol-fly .corner i { font-style: normal; font-size: 13px; }

.sol-fly .pip {
  position: absolute;
  inset: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 26px;
  line-height: 1;
}
</style>
