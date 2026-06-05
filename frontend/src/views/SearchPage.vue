
<template>
  <div class="search-page">
    <div class="search-header">
      <div class="back-btn" @click="goBack">← 返回</div>
      <div class="search-box-container">
        <input 
          v-model="keyword" 
          type="text" 
          placeholder="搜索书籍、笔记、标签..."
          @keyup.enter="performSearch"
          class="search-input"
        />
        <button @click="performSearch" class="search-btn">搜索</button>
      </div>
    </div>
    
    <div v-if="keyword" class="search-results">
      <div v-if="resultsLoading" class="loading">搜索中...</div>
      
      <div v-else>
        <div v-if="books.length > 0" class="result-section">
          <h2>📚 书籍</h2>
          <div class="books-results">
            <div v-for="book in books" :key="book.id" class="book-result-item" @click="viewBook(book.id)">
              <img :src="book.coverUrl || 'https://via.placeholder.com/80x100?text=No+Cover'" :alt="book.title" />
              <div class="book-info">
                <h3 v-html="highlightText(book.highlightedTitle || book.title)"></h3>
                <p>{{ book.author }}</p>
                <p v-if="book.highlightedContent" class="book-highlight-desc" v-html="highlightText(book.highlightedContent)"></p>
              </div>
            </div>
          </div>
        </div>
        
        <div v-if="notes.length > 0" class="result-section">
          <h2>📝 笔记</h2>
          <div class="notes-results">
            <div v-for="note in notes" :key="note.id" class="note-result-item" @click="viewNote(note)">
              <div class="note-info">
                <h3 v-html="highlightText(note.highlightedTitle || note.title)"></h3>
                <p v-if="note.highlightedContent" v-html="highlightText(note.highlightedContent)" class="highlight-content"></p>
                <p v-else>{{ getPreview(note.content) }}</p>
                <div class="note-meta">
                  <span>书籍：{{ note.bookTitle }}</span>
                  <div class="note-tags">
                    <span 
                      v-for="tag in note.tags" 
                      :key="tag.id" 
                      class="note-tag"
                      :class="{ 'tag-highlight': isTagHighlighted(tag.name, note.highlightedTags) }"
                      :style="{ backgroundColor: tag.color || '#e0e0e0' }"
                    >
                      {{ tag.name }}
                    </span>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </div>
        
        <div v-if="books.length === 0 && notes.length === 0" class="no-results">
          <div class="empty-icon">🔍</div>
          <p>未找到相关结果</p>
        </div>
      </div>
    </div>
    
    <div v-else class="search-hint">
      <div class="hint-icon">🔍</div>
      <p>输入关键词开始搜索</p>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, watch } from 'vue'
import { searchApi } from '../api'

const keyword = ref('')
const books = ref([])
const notes = ref([])
const resultsLoading = ref(false)

const highlightText = (text) => {
  if (!text) return ''
  return text
    .replace(/\[\[HIGHLIGHT\]\]/g, '<span class="search-highlight">')
    .replace(/\[\[\/HIGHLIGHT\]\]/g, '</span>')
}

const isTagHighlighted = (tagName, highlightedTags) => {
  if (!highlightedTags || !tagName) return false
  return highlightedTags.toLowerCase().includes(tagName.toLowerCase())
}

const getPreview = (content) => {
  if (!content) return ''
  const text = content.replace(/<[^>]*>/g, '').substring(0, 150)
  return text + (content.length > 150 ? '...' : '')
}

const performSearch = async () => {
  if (!keyword.value.trim()) return
  
  resultsLoading.value = true
  books.value = []
  notes.value = []
  
  try {
    const response = await searchApi.fullTextSearch(keyword.value.trim())
    books.value = response.data.books || []
    notes.value = response.data.notes || []
  } catch (error) {
    console.error('搜索失败:', error)
  } finally {
    resultsLoading.value = false
  }
}

const viewBook = (id) => {
  window.location.href = `/book/${id}`
}

const viewNote = (note) => {
  window.location.href = `/book/${note.bookId}`
}

const goBack = () => {
  window.location.href = '/'
}

onMounted(() => {
  const params = new URLSearchParams(window.location.search)
  const queryKeyword = params.get('keyword')
  if (queryKeyword) {
    keyword.value = queryKeyword
    performSearch()
  }
})
</script>

<style scoped>
.search-page {
  width: 100%;
}

.search-header {
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
  flex-shrink: 0;
}

.search-box-container {
  flex: 1;
  display: flex;
  gap: 0.5rem;
}

.search-input {
  flex: 1;
  padding: 0.75rem 1rem;
  border: 1px solid #ddd;
  border-radius: 8px;
  font-size: 1rem;
}

.search-btn {
  padding: 0.75rem 1.5rem;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
  border: none;
  border-radius: 8px;
  cursor: pointer;
}

.search-results {
  background: white;
  padding: 2rem;
  border-radius: 12px;
  box-shadow: 0 2px 10px rgba(0,0,0,0.05);
}

.result-section {
  margin-bottom: 2rem;
}

.result-section:last-child {
  margin-bottom: 0;
}

.result-section h2 {
  font-size: 1.2rem;
  margin-bottom: 1rem;
  color: #333;
}

.books-results {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(200px, 1fr));
  gap: 1rem;
}

.book-result-item {
  display: flex;
  gap: 1rem;
  padding: 1rem;
  background: #fafafa;
  border-radius: 8px;
  cursor: pointer;
  transition: background 0.3s;
}

.book-result-item:hover {
  background: #f0f0f0;
}

.book-result-item img {
  width: 80px;
  height: 100px;
  object-fit: cover;
  border-radius: 6px;
}

.book-info h3 {
  font-size: 1rem;
  margin-bottom: 0.25rem;
}

.book-info p {
  color: #666;
  font-size: 0.9rem;
}

.book-highlight-desc {
  margin-top: 0.5rem;
  font-size: 0.85rem;
  color: #888;
  line-height: 1.5;
}

.notes-results {
  display: flex;
  flex-direction: column;
  gap: 1rem;
}

.note-result-item {
  padding: 1rem;
  background: #fafafa;
  border-radius: 8px;
  cursor: pointer;
  transition: background 0.3s;
}

.note-result-item:hover {
  background: #f0f0f0;
}

.note-info h3 {
  font-size: 1.1rem;
  margin-bottom: 0.5rem;
}

.note-info p {
  line-height: 1.5;
  color: #555;
  margin-bottom: 0.75rem;
}

.note-meta {
  display: flex;
  align-items: center;
  gap: 1rem;
  color: #999;
  font-size: 0.9rem;
}

.note-tags {
  display: flex;
  gap: 0.35rem;
}

.note-tag {
  padding: 0.2rem 0.5rem;
  border-radius: 8px;
  font-size: 0.75rem;
  color: white;
}

.loading {
  text-align: center;
  padding: 3rem;
  color: #999;
}

.no-results {
  text-align: center;
  padding: 3rem;
}

.empty-icon {
  font-size: 3rem;
  margin-bottom: 1rem;
}

.no-results p {
  color: #999;
}

.search-hint {
  text-align: center;
  padding: 4rem;
}

.search-hint p {
  color: #999;
}

.search-highlight {
  background: #fff3cd;
  color: #856404;
  padding: 0 2px;
  border-radius: 2px;
  font-weight: 500;
}

.highlight-content {
  line-height: 1.6;
  color: #555;
  margin-bottom: 0.75rem;
}

.tag-highlight {
  box-shadow: 0 0 0 2px #fff3cd, 0 0 0 3px #ffc107;
}
</style>
