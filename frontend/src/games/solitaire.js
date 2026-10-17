/**
 * Клондойк: колода, раздача и правила ходов.
 *
 * Модуль намеренно не зависит от Vue и от DOM — так его можно
 * покрыть обычными тестами, а компонент только рисует состояние.
 *
 * Карты описываются числами, а не объектами: масть — старшие два бита,
 * достоинство — младшие. Сравнения и раскладка по мастям от этого
 * дешевле, а колода из 52 карт копируется целиком на каждый откат.
 */

/** Масти: ♠ ♥ ♦ ♣ */
export const SUITS = ['♠', '♥', '♦', '♣']
/** Значения карт: 1 — туз, 11 — валет, 12 — дама, 13 — король. */
export const RANKS = ['', 'A', '2', '3', '4', '5', '6', '7', '8', '9', '10', 'J', 'Q', 'K']

/** Красные масти — по ним чередуются карты в рядах. */
const RED_SUITS = new Set([1, 2])

export function suitOf(card) {
  return (card >> 4) & 0x3
}

export function rankOf(card) {
  return card & 0xf
}

export function isRed(card) {
  return RED_SUITS.has(suitOf(card))
}

export function cardLabel(card) {
  return RANKS[rankOf(card)] + SUITS[suitOf(card)]
}

export function newDeck() {
  const deck = []
  for (let suit = 0; suit < 4; suit++) {
    for (let rank = 1; rank <= 13; rank++) {
      deck.push(suit * 16 + rank)
    }
  }
  return deck
}

function shuffle(cards) {
  for (let i = cards.length - 1; i > 0; i--) {
    const j = Math.floor(Math.random() * (i + 1));
    const tmp = cards[i]
    cards[i] = cards[j]
    cards[j] = tmp
  }
  return cards
}

export const TABLEAU_PILES = 7
export const FOUNDATIONS = 4

/**
 * Раздача клондойка: 24 карты в колоду и по 1–7 карт в ряды,
 * в каждом ряду открыта только последняя карта.
 */
export function deal() {
  const deck = shuffle(newDeck())
  const tableau = []
  for (let pile = 0; pile < TABLEAU_PILES; pile++) {
    tableau.push(deck.splice(0, pile + 1))
  }
  const faceUp = new Set()
  for (const pile of tableau) {
    faceUp.add(pile[pile.length - 1])
  }
  return {
    stock: deck,
    waste: [],
    foundations: [[], [], [], []],
    tableau,
    faceUp,
    // Откаты ходов: полные снимки состояния.
    history: [],
    moves: 0,
    won: false
  }
}

function snapshot(state) {
  return {
    stock: state.stock.slice(),
    waste: state.waste.slice(),
    foundations: state.foundations.map((pile) => pile.slice()),
    tableau: state.tableau.map((pile) => pile.slice()),
    faceUp: new Set(state.faceUp),
    moves: state.moves
  }
}

function restore(state, snap) {
  state.stock = snap.stock.slice()
  state.waste = snap.waste.slice()
  state.foundations = snap.foundations.map((pile) => pile.slice())
  state.tableau = snap.tableau.map((pile) => pile.slice())
  state.faceUp = new Set(snap.faceUp)
  state.moves = snap.moves
  state.won = state.foundations.every((pile) => pile.length === 13)
}

/**
 * Завершает ход: снимок ДО него уходит в историю, счётчик растёт.
 *
 * Состояние здесь не трогаем — вызывающий код уже применил ход к
 * snap-у, который снял перед началом.
 */
function commit(state, snap) {
  state.history.push(snap)
  // Больше сотни откатов держать смысла нет: партия столько не длится,
  // а память на снимки растёт линейно.
  if (state.history.length > 100) {
    state.history.shift()
  }
  state.moves += 1
}

/** Откат последнего хода. Возвращает false, если ходов ещё не было. */
export function undo(state) {
  const snap = state.history.pop()
  if (!snap) return false
  restore(state, snap)
  return true
}

/** Открывает карту, если она лежит рубашкой вверху стопки. */
function flipTop(state, pile) {
  if (pile.length === 0) return
  const top = pile[pile.length - 1]
  state.faceUp.add(top)
}

/**
 * Можно ли положить карту (или стопку карт) на основание.
 * Основание собирает масть по возрастанию от туза.
 */
export function canToFoundation(state, card) {
  const foundation = state.foundations[suitOf(card)]
  if (foundation.length === 0) {
    return rankOf(card) === 1
  }
  const top = foundation[foundation.length - 1]
  return suitOf(top) === suitOf(card) && rankOf(card) === rankOf(top) + 1
}

/**
 * Можно ли положить стопку на ряд: достоинство ниже на единицу
 * и цвет противоположный.
 */
export function canToTableau(state, moving, targetPile) {
  if (moving.length === 0) return false
  const head = moving[0]
  if (targetPile.length === 0) {
    // На пустой ряд кладут только короля.
    return rankOf(head) === 13
  }
  const target = targetPile[targetPile.length - 1]
  return isRed(head) !== isRed(target) && rankOf(head) === rankOf(target) - 1
}

/**
 * Можно ли сейчас взять карту (или стопку) и положить её в ряд.
 * Нужна подсветке при перетаскивании, чтобы показать заранее,
 * что ход пройдёт, а что нет.
 */
export function canDropOnTableau(state, source, index, targetPile) {
  if (source === 'waste') {
    if (state.waste.length === 0) return false
    return canToTableau(state, [state.waste[state.waste.length - 1]], state.tableau[targetPile])
  }
  const moving = runFrom(state, source, index)
  if (!moving) return false
  return source !== targetPile && canToTableau(state, moving, state.tableau[targetPile])
}

/** То же самое для основания: подходит ли верхняя карта перетаскиваемой стопки. */
export function canDropOnFoundation(state, source, index) {
  if (source === 'waste') {
    return state.waste.length > 0 && canToFoundation(state, state.waste[state.waste.length - 1])
  }
  const moving = runFrom(state, source, index)
  return !!moving && moving.length === 1 && canToFoundation(state, moving[0])
}

/** Стопка в ряду, начиная с заданной позиции, если она вся открыта. */
export function runFrom(state, pile, index) {
  const cards = state.tableau[pile].slice(index)
  if (cards.length === 0) return null
  for (const card of cards) {
    if (!state.faceUp.has(card)) return null
  }
  return cards
}

/** Ход из отбоя в ряд. */
export function moveWasteToTableau(state, pile) {
  if (state.waste.length === 0) return false
  const card = state.waste[state.waste.length - 1]
  if (!canToTableau(state, [card], state.tableau[pile])) return false

  const snap = snapshot(state)
  state.waste.pop()
  state.tableau[pile].push(card)
  commit(state, snap)
  checkWin(state)
  return true
}

/** Ход из отбоя в основание. */
export function moveWasteToFoundation(state) {
  if (state.waste.length === 0) return false
  const card = state.waste[state.waste.length - 1]
  if (!canToFoundation(state, card)) return false

  const snap = snapshot(state)
  state.waste.pop()
  state.foundations[suitOf(card)].push(card)
  commit(state, snap)
  checkWin(state)
  return true
}

/** Ход между рядами: переносится стопка из index и ниже. */
export function moveTableauToTableau(state, from, to, index) {
  if (from === to) return false
  const moving = runFrom(state, from, index)
  if (!moving) return false
  if (!canToTableau(state, moving, state.tableau[to])) return false

  const snap = snapshot(state)
  state.tableau[from].length = index
  state.tableau[to].push(...moving)
  flipTop(state, state.tableau[from])
  commit(state, snap)
  checkWin(state)
  return true
}

/** Ход из ряда в основание. */
export function moveTableauToFoundation(state, from, index) {
  const moving = runFrom(state, from, index)
  if (!moving || moving.length !== 1) return false
  const card = moving[0]
  if (!canToFoundation(state, card)) return false

  const snap = snapshot(state)
  state.tableau[from].length = index
  state.foundations[suitOf(card)].push(card)
  flipTop(state, state.tableau[from])
  commit(state, snap)
  checkWin(state)
  return true
}

/** Взять карту из колоды в отбой. Когда колода пуста, переворачиваем отбой. */
export function drawFromStock(state) {
  if (state.stock.length === 0) {
    if (state.waste.length === 0) return false
    const snap = snapshot(state)
    // Порядок сохраняем: следующим из колоды выйдет та же карта, что
    // была сверху отбоя.
    state.stock = state.waste.slice()
    state.waste = []
    for (const card of state.stock) {
      state.faceUp.delete(card)
    }
    commit(state, snap)
    return true
  }

  const snap = snapshot(state)
  const card = state.stock.pop()
  state.waste.push(card)
  state.faceUp.add(card)
  commit(state, snap)
  return true
}

/** Двойной клик: попытаться убрать карту в основание. */
export function autoToFoundation(state, source, index) {
  if (source === 'waste') {
    return moveWasteToFoundation(state)
  }
  return moveTableauToFoundation(state, source, index)
}

/**
 * Есть ли ход по столу: карта из отбоя или ряда ложится в ряд или в
 * основание. Колодой здесь не считаем — перебор колоды не продвигает
 * партию и не должен считаться настоящим ходом.
 */
export function hasBoardMove(state) {
  if (state.waste.length > 0) {
    const top = state.waste[state.waste.length - 1]
    if (canToFoundation(state, top)) return true
    for (let to = 0; to < state.tableau.length; to++) {
      if (canToTableau(state, [top], state.tableau[to])) return true
    }
  }

  for (let from = 0; from < state.tableau.length; from++) {
    for (let index = 0; index < state.tableau[from].length; index++) {
      const moving = runFrom(state, from, index)
      if (!moving) continue
      if (moving.length === 1 && canToFoundation(state, moving[0])) return true
      for (let to = 0; to < state.tableau.length; to++) {
        if (to === from) continue
        if (canToTableau(state, moving, state.tableau[to])) return true
      }
    }
  }
  return false
}

/**
 * Есть ли хоть какой-то ход. Колода и отбой — всегда ход: либо взять
 * карту, либо перевернуть отбой обратно в колоду. Поэтому тупик
 * возможен только когда обе стопки пусты.
 */
export function hasAnyMove(state) {
  if (state.stock.length > 0 || state.waste.length > 0) return true
  return hasBoardMove(state)
}

/** Есть ли в рядах закрытые карты. */
export function hasHiddenCards(state) {
  for (const pile of state.tableau) {
    for (const card of pile) {
      if (!state.faceUp.has(card)) return true
    }
  }
  return false
}

/**
 * Все карты на виду: колода и отбой пусты, в рядах ничего не закрыто.
 * Дальше прятать нечего, поэтому партию можно доиграть автоматически.
 */
export function allCardsRevealed(state) {
  return state.stock.length === 0 && state.waste.length === 0 && !hasHiddenCards(state)
}

/** Копия состояния для пробных ходов. Историю не копируем. */
function copyState(state) {
  return {
    stock: state.stock.slice(),
    waste: state.waste.slice(),
    foundations: state.foundations.map((pile) => pile.slice()),
    tableau: state.tableau.map((pile) => pile.slice()),
    faceUp: new Set(state.faceUp),
    history: [],
    moves: state.moves,
    won: state.won
  }
}

/**
 * Пробует убрать в основание любую подходящую карту.
 * Возвращает ход или null.
 */
function takeToFoundation(sim) {
  if (sim.waste.length > 0 && canToFoundation(sim, sim.waste[sim.waste.length - 1])) {
    const card = sim.waste[sim.waste.length - 1]
    moveWasteToFoundation(sim)
    return { card, to: 'foundation' }
  }
  for (let from = 0; from < sim.tableau.length; from++) {
    for (let index = 0; index < sim.tableau[from].length; index++) {
      const before = sim.foundations.map((pile) => pile.length)
      if (moveTableauToFoundation(sim, from, index)) {
        const suit = sim.foundations.findIndex((pile, s) => pile.length > before[s])
        return { card: sim.foundations[suit][sim.foundations[suit].length - 1], to: 'foundation' }
      }
    }
  }
  return null
}

/**
 * Пробует переложить стопку между рядами, чтобы открыть карту.
 * Возвращает ход или null.
 */
function takeTableau(sim) {
  for (let from = 0; from < sim.tableau.length; from++) {
    for (let index = 0; index < sim.tableau[from].length; index++) {
      const moving = runFrom(sim, from, index)
      for (let to = 0; to < sim.tableau.length; to++) {
        if (to === from || !moving) continue
        if (moveTableauToTableau(sim, from, to, index)) {
          return { card: moving[0], to: 'tableau', pile: to }
        }
      }
    }
  }
  return null
}

/** Вне оснований не осталось ни одной карты. */
function emptyOutsideFoundations(state) {
  if (state.stock.length > 0 || state.waste.length > 0) return false
  return state.tableau.every((pile) => pile.length === 0)
}

/**
 * Порядок ходов, которыми оставшиеся карты уйдут в основания: панель
 * проигрывает план по одному ходу, карты в основания улетают с анимацией.
 *
 * Возвращает null, если даже так разобраться нельзя — тогда партию
 * продолжают играть вручную. Сначала пробуем всё унести в основания,
 * и только если не идёт — перекладываем стопки между рядами.
 */
export function planAutoFinish(state) {
  const sim = copyState(state)
  const plan = []

  // Каждый ход что-то убирает, а карт всего 52 — больше 104 итераций
  // зациклиться не может, но подстрахуемся.
  for (let guard = 0; guard <= 104; guard++) {
    if (emptyOutsideFoundations(sim)) return plan
    const step = takeToFoundation(sim) || takeTableau(sim)
    if (!step) return null
    plan.push(step)
  }

  return emptyOutsideFoundations(sim) ? plan : null
}

function checkWin(state) {
  state.won = state.foundations.every((pile) => pile.length === 13)
}

/** Сколько карт ещё не убрано в основание. */
export function cardsRemaining(state) {
  let n = state.stock.length + state.waste.length
  for (const pile of state.tableau) n += pile.length
  return n
}