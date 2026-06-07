
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
        <button @click="showFilters = !showFilters" class="filter-toggle-btn">
          <span v-if="showFilters">收起筛选</span>
          <span v-else>高级筛选</span>
          <span v-if="hasActiveFilters" class="filter-badge">{{ activeFilterCount }}</span>
        </button>
      </div>
    </div>

    <div v-if="showFilters" class="filter-section">
      <div class="filter-bar">
        <div class="filter-item">
          <label class="filter-label">搜索类型</label>
          <select v-model="filters.searchType" class="filter-select">
            <option value="">全部</option>
            <option value="book">仅书籍</option>
            <option value="note">仅笔记</option>
          </select>
        </div>

        <div class="filter-item">
          <label class="filter-label">阅读状态</label>
          <select v-model="filters.status" class="filter-select">
            <option value="">全部状态</option>
            <option value="READING">在读</option>
            <option value="READ">已读</option>
            <option value="WANT_TO_READ">想读</option>
          </select>
        </div>

        <div class="filter-item">
          <label class="filter-label">分类</label>
          <select v-model="filters.category" class="filter-select">
            <option value="">全部分类</option>
            <option v-for="cat in categories" :key="cat" :value="cat">{{ cat }}</option>
          </select>
        </div>

        <div class="filter-item">
          <label class="filter-label">标签</label>
          <div class="tag-select-wrapper">
            <div class="selected-tags" v-if="selectedTagNames.length > 0">
              <span v-for="tag in selectedTagNames" :key="tag" class="selected-tag">
                {{ tag }}
                <span class="remove-tag" @click="removeTag(tag)">×</span>
              </span>
            </div>
            <select v-model="newTagId" @change="addTag" class="tag-select">
              <option value="">添加标签筛选</option>
              <option v-for="tag in tags" :key="tag.id" :value="tag.id">{{ tag.name }}</option>
            </select>
          </div>
        </div>

        <div class="filter-item date-filter">
          <label class="filter-label">创建时间</label>
          <div class="date-inputs">
            <input 
              v-model="filters.startDate" 
              type="date" 
              class="date-input"
            />
            <span class="date-separator">至</span>
            <input 
              v-model="filters.endDate" 
              type="date" 
              class="date-input"
            />
          </div>
        </div>

        <button @click="applyFilters" class="apply-btn">应用筛选</button>
        <button @click="resetFilters" class="reset-btn">重置</button>
      </div>
    </div>
    
    <div class="search-results">
      <div v-if="resultsLoading" class="loading">搜索中...</div>
      
      <div v-else>
        <div v-if="books.length > 0 && (filters.searchType === '' || filters.searchType === 'book')" class="result-section">
          <div class="section-header">
            <h2>📚 书籍</h2>
            <span class="result-count">{{ books.length }} 本</span>
          </div>
          <div class="books-results">
            <div v-for="book in books" :key="book.id" class="book-result-item" @click="viewBook(book.id)">
              <img :src="book.coverUrl || 'https://via.placeholder.com/80x100?text=No+Cover'" :alt="book.title" />
              <div class="book-info">
                <h3 v-html="highlightText(book.highlightedTitle || book.title)"></h3>
                <p>{{ book.author }}</p>
                <div class="book-meta">
                  <span :class="['status-badge', book.status?.toLowerCase()]">{{ getStatusLabel(book.status) }}</span>
                  <span v-if="book.category" class="category-tag">{{ book.category }}</span>
                </div>
                <p v-if="book.highlightedContent" class="book-highlight-desc" v-html="highlightText(book.highlightedContent)"></p>
              </div>
            </div>
          </div>
        </div>
        
        <div v-if="notes.length > 0 && (filters.searchType === '' || filters.searchType === 'note')" class="result-section">
          <div class="section-header">
            <h2>📝 笔记</h2>
            <span class="result-count">{{ notes.length }} 条</span>
          </div>
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
                      :style="{ backgroundColor: tag.color || '#e0e0e0' }"
                    >{{ tag.name }}</span>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </div>
        
        <div v-if="hasSearched && books.length === 0 && notes.length === 0" class="no-results">
          <div class="empty-icon">🔍</div>
          <p>未找到相关结果</p>
          <button v-if="hasActiveFilters" @click="resetFilters" class="reset-filters-btn">清除筛选条件</button>
        </div>

        <div v-if="!hasSearched" class="search-hint">
          <div class="hint-icon">🔍</div>
          <p>输入关键词开始搜索</p>
          <p class="hint-sub">可使用高级筛选按状态、分类、标签、时间范围查找</p>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { searchApi, bookApi, tagApi } from '../api'

const keyword = ref('')
const books = ref([])
const notes = ref([])
const resultsLoading = ref(false)
const hasSearched = ref(false)
const showFilters = ref(false)
const categories = ref([])
const tags = ref([])
const newTagId = ref('')

const filters = reactive({
  searchType: '',
  status: '',
  category: '',
  startDate: '',
  endDate: ''
})

const selectedTags = ref([])

const selectedTagNames = computed(() => {
  return selectedTags.value.map(t => t.name)
})

const activeFilterCount = computed(() => {
  let count = 0
  if (filters.searchType) count++
  if (filters.status) count++
  if (filters.category) count++
  if (filters.startDate || filters.endDate) count++
  if (selectedTags.value.length > 0) count++
  return count
})

const hasActiveFilters = computed(() => activeFilterCount.value > 0)

const getStatusLabel = (status) => {
  const labels = {
    READING: '在读',
    READ: '已读',
    WANT_TO_READ: '想读'
  }
  return labels[status] || status
}

const highlightText = (text) => {
  if (!text) return ''
  return text
    .replace(/\[\[HIGHLIGHT\]\]/g, '<span class="search-highlight">')
    .replace(/\[\[\/HIGHLIGHT\]\]/g, '</span>')
}

const getPreview = (content) => {
  if (!content) return ''
  const text = content.replace(/<[^>]*>/g, '').substring(0, 150)
  return text + (content.length > 150 ? '...' : '')
}

const loadCategories = async () => {
  try {
    const response = await bookApi.getAllCategories()
    categories.value = response.data
  } catch (error) {
    console.error('加载分类失败:', error)
  }
}

const loadTags = async () => {
  try {
    const response = await tagApi.getAllTags()
    tags.value = response.data
  } catch (error) {
    console.error('加载标签失败:', error)
  }
}

const addTag = () => {
  if (!newTagId.value) return
  const tag = tags.value.find(t => t.id == newTagId.value)
  if (tag && !selectedTags.value.find(t => t.id == tag.id)) {
    selectedTags.value.push(tag)
  }
  newTagId.value = ''
}

const removeTag = (tagName) => {
  selectedTags.value = selectedTags.value.filter(t => t.name !== tagName)
}

const performSearch = async () => {
  if (!keyword.value.trim() && !hasActiveFilters.value) return
  
  hasSearched.value = true
  resultsLoading.value = true
  books.value = []
  notes.value = []
  
  try {
    const params = {}
    if (keyword.value.trim()) params.keyword = keyword.value.trim()
    if (filters.searchType) params.searchType = filters.searchType
    if (filters.status) params.status = filters.status
    if (filters.category) params.category = filters.category
    if (filters.startDate) params.startDate = filters.startDate
    if (filters.endDate) params.endDate = filters.endDate
    if (selectedTags.value.length > 0) {
      params.tagIds = selectedTags.value.map(t => t.id)
    }
    
    const response = await searchApi.advancedSearch(params)
    books.value = response.data.books || []
    notes.value = response.data.notes || []
  } catch (error) {
    console.error('搜索失败:', error)
  } finally {
    resultsLoading.value = false
  }
}

const applyFilters = () => {
  if (hasSearched.value || keyword.value.trim() || hasActiveFilters.value) {
    performSearch()
  }
}

const resetFilters = () => {
  filters.searchType = ''
  filters.status = ''
  filters.category = ''
  filters.startDate = ''
  filters.endDate = ''
  selectedTags.value = []
  if (hasSearched.value) {
    performSearch()
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
  loadCategories()
  loadTags()
  
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
  margin-bottom: 1.5rem;
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
  align-items: center;
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
  flex-shrink: 0;
}

.filter-toggle-btn {
  padding: 0.75rem 1.25rem;
  background: white;
  border: 1px solid #ddd;
  border-radius: 8px;
  cursor: pointer;
  font-size: 0.9rem;
  position: relative;
  flex-shrink: 0;
  color: #555;
}

.filter-toggle-btn:hover {
  border-color: #667eea;
  color: #667eea;
}

.filter-badge {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-width: 18px;
  height: 18px;
  background: #667eea;
  color: white;
  border-radius: 9px;
  font-size: 0.7rem;
  margin-left: 0.5rem;
  padding: 0 5px;
}

.filter-section {
  background: white;
  border-radius: 12px;
  padding: 1.5rem;
  margin-bottom: 1.5rem;
  box-shadow: 0 2px 10px rgba(0,0,0,0.05);
}

.filter-bar {
  display: flex;
  flex-wrap: wrap;
  gap: 1rem;
  align-items: flex-end;
}

.filter-item {
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
}

.filter-label {
  font-size: 0.85rem;
  font-weight: 500;
  color: #666;
}

.filter-select {
  padding: 0.6rem 1rem;
  border: 1px solid #ddd;
  border-radius: 8px;
  font-size: 0.95rem;
  background: white;
  cursor: pointer;
  min-width: 120px;
}

.tag-select-wrapper {
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
  min-width: 200px;
}

.selected-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 0.35rem;
}

.selected-tag {
  display: inline-flex;
  align-items: center;
  gap: 0.35rem;
  padding: 0.2rem 0.5rem;
  background: linear-gradient(135deg, rgba(102, 126, 234, 0.1) 0%, rgba(118, 75, 162, 0.1) 100%);
  color: #667eea;
  border-radius: 12px;
  font-size: 0.8rem;
}

.remove-tag {
  cursor: pointer;
  font-size: 1rem;
  line-height: 1;
}

.tag-select {
  padding: 0.6rem 1rem;
  border: 1px solid #ddd;
  border-radius: 8px;
  font-size: 0.9rem;
  background: white;
  cursor: pointer;
}

.date-filter {
  flex: 1;
  min-width: 250px;
}

.date-inputs {
  display: flex;
  align-items: center;
  gap: 0.5rem;
}

.date-input {
  flex: 1;
  padding: 0.6rem 0.75rem;
  border: 1px solid #ddd;
  border-radius: 8px;
  font-size: 0.9rem;
}

.date-separator {
  color: #999;
  font-size: 0.9rem;
}

.apply-btn {
  padding: 0.65rem 1.5rem;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
  border: none;
  border-radius: 8px;
  cursor: pointer;
  font-size: 0.9rem;
  height: fit-content;
}

.reset-btn {
  padding: 0.65rem 1.25rem;
  background: #f0f0f0;
  color: #666;
  border: none;
  border-radius: 8px;
  cursor: pointer;
  font-size: 0.9rem;
  height: fit-content;
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

.section-header {
  display: flex;
  align-items: center;
  gap: 0.75rem;
  margin-bottom: 1rem;
}

.section-header h2 {
  font-size: 1.2rem;
  margin: 0;
  color: #333;
}

.result-count {
  color: #999;
  font-size: 0.85rem;
}

.books-results {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(280px, 1fr));
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
  width: 70px;
  height: 90px;
  object-fit: cover;
  border-radius: 6px;
  flex-shrink: 0;
}

.book-info {
  flex: 1;
  min-width: 0;
}

.book-info h3 {
  font-size: 1rem;
  margin-bottom: 0.25rem;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.book-info p {
  color: #666;
  font-size: 0.85rem;
  margin-bottom: 0.5rem;
}

.book-meta {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  flex-wrap: wrap;
  margin-bottom: 0.5rem;
}

.status-badge {
  padding: 0.15rem 0.6rem;
  border-radius: 10px;
  font-size: 0.7rem;
}

.status-badge.reading {
  background: #e8f5e9;
  color: #2e7d32;
}

.status-badge.read {
  background: #e3f2fd;
  color: #1976d2;
}

.status-badge.want_to_read {
  background: #fff3e0;
  color: #e65100;
}

.category-tag {
  display: inline-block;
  padding: 0.15rem 0.5rem;
  background: #f5f0ff;
  color: #764ba2;
  border-radius: 6px;
  font-size: 0.7rem;
}

.book-highlight-desc {
  margin-top: 0.25rem;
  font-size: 0.8rem;
  color: #888;
  line-height: 1.4;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
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
  font-size: 1.05rem;
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
  font-size: 0.85rem;
  flex-wrap: wrap;
}

.note-tags {
  display: flex;
  gap: 0.35rem;
  flex-wrap: wrap;
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
  margin-bottom: 1rem;
}

.reset-filters-btn {
  padding: 0.6rem 1.5rem;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
  border: none;
  border-radius: 8px;
  cursor: pointer;
  font-size: 0.9rem;
}

.search-hint {
  text-align: center;
  padding: 4rem;
}

.search-hint p {
  color: #999;
}

.hint-icon {
  font-size: 3rem;
  margin-bottom: 1rem;
}

.hint-sub {
  font-size: 0.85rem;
  margin-top: 0.5rem;
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
</style>
