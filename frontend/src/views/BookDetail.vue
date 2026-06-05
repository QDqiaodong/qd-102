
<template>
  <div class="book-detail">
    <div v-if="book" class="detail-content">
      <div class="book-header">
        <div class="back-btn" @click="goBack">← 返回</div>
        <h1 class="book-title">{{ book.title }}</h1>
      </div>
      
      <div class="book-info-section">
        <div class="book-cover-large">
          <img :src="book.coverUrl || 'https://via.placeholder.com/200x280?text=No+Cover'" :alt="book.title" />
        </div>
        <div class="book-meta-detail">
          <p class="author">作者：{{ book.author || '-' }}</p>
          <p class="category">分类：{{ book.category || '-' }}</p>
          <p :class="['status', book.status.toLowerCase()]">状态：{{ getStatusLabel(book.status) }}</p>
          <div class="progress-section">
            <div class="progress-label">阅读进度</div>
            <div class="progress-bar">
              <div class="progress-fill" :style="{ width: book.progress + '%' }"></div>
            </div>
            <div class="progress-value">{{ book.progress }}%</div>
          </div>
          <p v-if="book.description" class="description">{{ book.description }}</p>
        </div>
      </div>

      <div class="notes-section">
        <div class="section-header">
          <h2>读书笔记</h2>
          <button @click="goToNewNote" class="add-note-btn">+ 写笔记</button>
        </div>

        <div v-if="notes.length > 0" class="notes-summary">
          已加载 {{ notes.length }} / {{ totalNotes }} 条
        </div>

        <div v-if="notes.length > 0" class="notes-list">
          <div v-for="note in notes" :key="note.id" class="note-item">
            <h3 class="note-title">{{ note.title }}</h3>
            <p v-if="note.pageNumber" class="note-page">第 {{ note.pageNumber }} 页</p>
            <p class="note-preview">{{ getPreview(note.content) }}</p>
            <div class="note-tags">
              <span v-for="tag in note.tags" :key="tag.id" class="note-tag" :style="{ backgroundColor: tag.color || '#e0e0e0' }">
                {{ tag.name }}
              </span>
            </div>
            <div class="note-meta">
              <span class="note-date">{{ formatDate(note.createdAt) }}</span>
              <div class="note-actions">
                <button @click="goToEditNote(note.id)" class="edit-note-btn">编辑</button>
                <button @click="deleteNote(note.id)" class="delete-note-btn">删除</button>
              </div>
            </div>
          </div>
        </div>

        <div v-if="hasMoreNotes" class="load-more-wrap">
          <button @click="loadMoreNotes" :disabled="notesLoading" class="load-more-btn">
            {{ notesLoading ? '加载中...' : '加载更多笔记' }}
          </button>
        </div>

        <div v-else-if="!notesLoading && notes.length === 0" class="empty-notes">
          <div class="empty-icon">📝</div>
          <p>暂无笔记，点击上方按钮开始记录</p>
        </div>

        <div v-if="notesLoading && notes.length === 0" class="loading">笔记加载中...</div>
      </div>
    </div>
    
    <div v-else class="loading">加载中...</div>
  </div>
</template>

<script setup>
import { computed, ref, onMounted } from 'vue'
import { bookApi, noteApi } from '../api'

const book = ref(null)
const notes = ref([])
const currentPage = ref(0)
const totalPages = ref(0)
const totalNotes = ref(0)
const notesLoading = ref(false)
const pageSize = 5

const getStatusLabel = (status) => {
  const labels = {
    READING: '在读',
    READ: '已读',
    WANT_TO_READ: '想读'
  }
  return labels[status] || status
}

const getPreview = (content) => {
  if (!content) return ''
  const text = content.replace(/<[^>]*>/g, '').substring(0, 100)
  return text + (content.length > 100 ? '...' : '')
}

const formatDate = (dateStr) => {
  if (!dateStr) return ''
  const date = new Date(dateStr)
  return date.toLocaleDateString('zh-CN')
}

const hasMoreNotes = computed(() => currentPage.value + 1 < totalPages.value)

const loadNotes = async (reset = false) => {
  const id = window.location.pathname.split('/')[2]
  const nextPage = reset ? 0 : currentPage.value + 1

  notesLoading.value = true
  try {
    const response = await noteApi.getNotesByBook(id, {
      page: nextPage,
      size: pageSize
    })
    const pageData = response.data
    const nextNotes = pageData.content || []

    notes.value = reset ? nextNotes : [...notes.value, ...nextNotes]
    currentPage.value = nextPage
    totalPages.value = pageData.totalPages || 0
    totalNotes.value = pageData.totalElements || 0
  } catch (error) {
    console.error('加载笔记失败:', error)
  } finally {
    notesLoading.value = false
  }
}

const loadBook = async () => {
  const id = window.location.pathname.split('/')[2]
  try {
    const bookRes = await bookApi.getBookById(id)
    book.value = bookRes.data
    await loadNotes(true)
  } catch (error) {
    console.error('加载失败:', error)
  }
}

const loadMoreNotes = async () => {
  if (!hasMoreNotes.value || notesLoading.value) return
  await loadNotes(false)
}

const goBack = () => {
  window.location.href = '/'
}

const goToNewNote = () => {
  const id = window.location.pathname.split('/')[2]
  window.location.href = `/book/${id}/note/new`
}

const goToEditNote = (noteId) => {
  window.location.href = `/note/${noteId}/edit`
}

const deleteNote = async (id) => {
  if (!confirm('确定删除这条笔记吗？')) return
  try {
    await noteApi.deleteNote(id)
    currentPage.value = 0
    totalPages.value = 0
    totalNotes.value = 0
    await loadBook()
  } catch (error) {
    console.error('删除失败:', error)
  }
}

onMounted(loadBook)
</script>

<style scoped>
.book-detail {
  width: 100%;
}

.book-header {
  display: flex;
  align-items: center;
  gap: 1rem;
  margin-bottom: 2rem;
}

.back-btn {
  padding: 0.5rem 1rem;
  background: white;
  border-radius: 8px;
  cursor: pointer;
  box-shadow: 0 2px 5px rgba(0,0,0,0.1);
}

.book-title {
  font-size: 1.8rem;
  color: #333;
}

.book-info-section {
  display: flex;
  gap: 2rem;
  background: white;
  padding: 2rem;
  border-radius: 12px;
  box-shadow: 0 2px 10px rgba(0,0,0,0.05);
  margin-bottom: 2rem;
}

.book-cover-large {
  flex-shrink: 0;
}

.book-cover-large img {
  width: 200px;
  height: 280px;
  object-fit: cover;
  border-radius: 8px;
}

.book-meta-detail {
  flex: 1;
}

.author, .category, .status {
  margin-bottom: 0.75rem;
  font-size: 1.1rem;
}

.status.reading {
  color: #2e7d32;
}

.status.read {
  color: #1976d2;
}

.status.want_to_read {
  color: #e65100;
}

.progress-section {
  margin: 1.5rem 0;
}

.progress-label {
  margin-bottom: 0.5rem;
  color: #666;
}

.progress-bar {
  height: 12px;
  background: #f0f0f0;
  border-radius: 6px;
  overflow: hidden;
}

.progress-fill {
  height: 100%;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  border-radius: 6px;
  transition: width 0.3s;
}

.progress-value {
  margin-top: 0.5rem;
  text-align: right;
  color: #666;
}

.description {
  margin-top: 1rem;
  line-height: 1.6;
  color: #555;
}

.notes-section {
  background: white;
  padding: 2rem;
  border-radius: 12px;
  box-shadow: 0 2px 10px rgba(0,0,0,0.05);
}

.section-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 1.5rem;
}

.section-header h2 {
  font-size: 1.4rem;
  color: #333;
}

.notes-summary {
  margin-bottom: 1rem;
  color: #666;
  font-size: 0.95rem;
}

.add-note-btn {
  padding: 0.5rem 1.5rem;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
  border: none;
  border-radius: 8px;
  cursor: pointer;
  font-weight: 500;
}

.notes-list {
  display: flex;
  flex-direction: column;
  gap: 1.5rem;
}

.note-item {
  padding: 1.5rem;
  background: #fafafa;
  border-radius: 8px;
  border-left: 4px solid #667eea;
}

.load-more-wrap {
  margin-top: 1.25rem;
  display: flex;
  justify-content: center;
}

.load-more-btn {
  padding: 0.75rem 1.5rem;
  border: none;
  border-radius: 999px;
  background: #eef2ff;
  color: #3f51b5;
  cursor: pointer;
  font-weight: 500;
}

.load-more-btn:disabled {
  cursor: not-allowed;
  opacity: 0.6;
}

.note-title {
  font-size: 1.2rem;
  font-weight: 600;
  margin-bottom: 0.5rem;
}

.note-page {
  color: #999;
  font-size: 0.9rem;
  margin-bottom: 0.5rem;
}

.note-preview {
  line-height: 1.5;
  color: #555;
  margin-bottom: 1rem;
}

.note-tags {
  display: flex;
  gap: 0.5rem;
  margin-bottom: 1rem;
}

.note-tag {
  padding: 0.25rem 0.75rem;
  border-radius: 12px;
  font-size: 0.85rem;
  color: white;
}

.note-meta {
  display: flex;
  justify-content: space-between;
  align-items: center;
  color: #999;
  font-size: 0.9rem;
}

.edit-note-btn, .delete-note-btn {
  padding: 0.35rem 0.75rem;
  border: none;
  border-radius: 6px;
  cursor: pointer;
  font-size: 0.85rem;
}

.edit-note-btn {
  background: #f0f0f0;
  margin-right: 0.5rem;
}

.delete-note-btn {
  background: #ffebee;
  color: #c62828;
}

.empty-notes {
  text-align: center;
  padding: 4rem;
}

.empty-icon {
  font-size: 3rem;
  margin-bottom: 1rem;
}

.empty-notes p {
  color: #999;
}

.loading {
  text-align: center;
  padding: 4rem;
  color: #999;
}
</style>
