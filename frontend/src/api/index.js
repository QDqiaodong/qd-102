
import axios from 'axios'

const API_BASE_URL = '/api'

const client = axios.create({
  baseURL: API_BASE_URL,
  timeout: 10000,
  headers: {
    'Content-Type': 'application/json'
  }
})

export const bookApi = {
  getAllBooks: (params = {}) => client.get('/books', { params }),
  getBookById: (id) => client.get(`/books/${id}`),
  createBook: (book) => client.post('/books', book),
  updateBook: (id, book) => client.put(`/books/${id}`, book),
  deleteBook: (id) => client.delete(`/books/${id}`),
  searchBooks: (params) => client.get('/books/search', { params }),
  getAllCategories: () => client.get('/books/categories'),
  findDuplicates: () => client.get('/books/duplicates'),
  getMergePreview: (bookIds) => client.post('/books/merge-preview', bookIds),
  mergeBooks: (targetBookId, sourceBookIds) => client.post('/books/merge', { targetBookId, sourceBookIds })
}

export const noteApi = {
  getNotesByBook: (bookId, params = {}) => client.get(`/notes/book/${bookId}`, { params }),
  getNoteById: (id) => client.get(`/notes/${id}`),
  createNote: (bookId, note) => client.post(`/notes/book/${bookId}`, note),
  updateNote: (id, note) => client.put(`/notes/${id}`, note),
  deleteNote: (id) => client.delete(`/notes/${id}`),
  searchNotes: (params) => client.get('/notes/search', { params }),
  // 草稿缓存API
  saveDraft: (bookId, draft) => client.post(`/notes/draft/${bookId}`, draft),
  getDraft: (bookId) => client.get(`/notes/draft/${bookId}`),
  deleteDraft: (bookId) => client.delete(`/notes/draft/${bookId}`)
}

export const tagApi = {
  getAllTags: () => client.get('/tags'),
  getTagById: (id) => client.get(`/tags/${id}`),
  createTag: (tag) => client.post('/tags', tag),
  updateTag: (id, tag) => client.put(`/tags/${id}`, tag),
  deleteTag: (id) => client.delete(`/tags/${id}`),
  getTagGraph: () => client.get('/tags/graph'),
  getTagNeighbors: (id) => client.get(`/tags/${id}/neighbors`),
  mergeTags: (targetTagId, sourceTagIds) => client.post('/tags/merge', { targetTagId, sourceTagIds })
}

export const activityApi = {
  getHeatmap: (months = 12) => client.get('/activity/heatmap', { params: { months } }),
  getDateDetail: (date) => client.get(`/activity/date/${date}`),
  getWakeUpList: (daysThreshold = 30) => client.get('/activity/wake-up-list', { params: { daysThreshold } })
}

export const searchApi = {
  fullTextSearch: (keyword) => client.get('/search', { params: { keyword } }),
  advancedSearch: (params = {}) => client.get('/search/advanced', { params })
}

export const cardApi = {
  getCards: (params = {}) => client.get('/cards', { params }),
  getCardsPaged: (params = {}) => client.get('/cards/paged', { params }),
  getCardBooks: () => client.get('/cards/books'),
  getCardTags: () => client.get('/cards/tags'),
  getPageNumbers: (bookId) => client.get(`/cards/pages/${bookId}`),
  getCardSummary: () => client.get('/cards/summary')
}

export const similarNoteApi = {
  findSimilarNotes: (params = {}) => client.get('/similar-notes', { params }),
  findSimilarNotesForNote: (noteId, params = {}) => client.get(`/similar-notes/note/${noteId}`, { params }),
  getSimilarityStatistics: (params = {}) => client.get('/similar-notes/statistics', { params })
}

export const repeatedNoteApi = {
  getAllRepeatedNotes: () => client.get('/repeated-notes'),
  getByNoteId: (noteId) => client.get(`/repeated-notes/note/${noteId}`),
  addToRepeatList: (noteId) => client.post(`/repeated-notes/note/${noteId}`),
  removeFromRepeatList: (noteId) => client.delete(`/repeated-notes/note/${noteId}`),
  markAsReviewed: (noteId) => client.post(`/repeated-notes/note/${noteId}/review`),
  getStats: () => client.get('/repeated-notes/stats')
}

export default client
