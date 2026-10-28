import client from './client'

export function getGameStats() {
  return client.get('/games/stats')
}

export function submitMinesweeperResult(seconds, correctFlags) {
  return client.post('/games/minesweeper', { seconds, correctFlags })
}

export function submitSolitaireResult(seconds, moves) {
  return client.post('/games/solitaire', { seconds, moves })
}
