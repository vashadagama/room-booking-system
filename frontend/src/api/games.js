import client from './client'

/** Статистика по всем играм для блока в профиле. */
export function getGameStats() {
  return client.get('/games/stats')
}

/** Победа в сапёре: сервер сам посчитает очки. */
export function submitMinesweeperResult(seconds, correctFlags) {
  return client.post('/games/minesweeper', { seconds, correctFlags })
}

/** Победа в пасьянсе: сервер сам посчитает очки. */
export function submitSolitaireResult(seconds, moves) {
  return client.post('/games/solitaire', { seconds, moves })
}
