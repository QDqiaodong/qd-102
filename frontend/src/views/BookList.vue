
<template>
  <div class="book-list">
    <div class="filter-section">
      <div class="filter-bar">
        <div class="filter-item">
          <label class="filter-label">状态</label>
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

        <div class="filter-item progress-filter">
          <label class="filter-label">进度区间</label>
          <div class="progress-inputs">
            <input 
              v-model.number="filters.minProgress" 
              type="number" 
              min="0" 
              max="100" 
              placeholder="最小"
              class="progress-input"
              @input="validateProgress('min')"
            />
            <span class="progress-separator">~</span>
            <input 
              v-model.number="filters.maxProgress" 
              type="number" 
              min="0" 
              max="100" 
              placeholder="最大"
              class="progress-input"
              @input="validateProgress('max')"
            />
            <span class="progress-unit">%</span>
          </div>
        </div>

        <div class="filter-item">
          <label class="filter-label">笔记</label>
          <select v-model="filters.hasNotes" class="filter-select">
            <option value="">不限</option>
            <option value="true">有笔记</option>
            <option value="false">无笔记</option>
          </select>
        </div>

        <button @click="resetFilters" class="reset-btn">重置</button>
      </div>

      <div class="active-filters" v-if="hasActiveFilters">
        <span class="active-filters-label">当前筛选：</span>
        <span v-if="filters.status" class="filter-tag">
          {{ getStatusLabel(filters.status) }}
          <span class="remove-tag" @click="filters.status = ''">×</span>
        </span>
        <span v-if="filters.category" class="filter-tag">
          {{ filters.category }}
          <span class="remove-tag" @click="filters.category = ''">×</span>
        </span>
        <span v-if="filters.minProgress !== null || filters.maxProgress !== null" class="filter-tag">
          {{ filters.minProgress !== null ? filters.minProgress : 0 }}% ~ {{ filters.maxProgress !== null ? filters.maxProgress : 100 }}%
          <span class="remove-tag" @click="clearProgressFilter">×</span>
        </span>
        <span v-if="filters.hasNotes !== ''" class="filter-tag">
          {{ filters.hasNotes === 'true' ? '有笔记' : '无笔记' }}
          <span class="remove-tag" @click="filters.hasNotes = ''">×</span>
        </span>
        <span class="result-count">共 {{ books.length }} 本</span>
      </div>
    </div>

    <div class="list-header">
      <h2 class="section-title">我的书架</h2>
      <button @click="showAddModal = true" class="add-btn">+ 添加书籍</button>
    </div>
    
    <div class="books-grid">
      <div v-for="book in books" :key="book.id" class="book-card" @click="viewBook(book.id)">
        <div class="book-cover">
          <img :src="book.coverUrl || 'https://via.placeholder.com/150x200?text=No+Cover'" :alt="book.title" />
          <div v-if="book.noteCount > 0" class="note-badge">
            📝 {{ book.noteCount }}
          </div>
        </div>
        <div class="book-info">
          <h3 class="book-title">{{ book.title }}</h3>
          <p class="book-author">{{ book.author }}</p>
          <div class="book-meta">
            <span :class="['status-badge', book.status.toLowerCase()]">{{ getStatusLabel(book.status) }}</span>
            <span v-if="book.progress !== null" class="progress-text">{{ book.progress }}%</span>
          </div>
          <div v-if="book.category" class="book-category">
            <span class="category-tag">{{ book.category }}</span>
          </div>
        </div>
        <div class="book-actions">
          <button @click.stop="editBook(book)" class="action-btn edit-btn">编辑</button>
          <button @click.stop="deleteBook(book.id)" class="action-btn delete-btn">删除</button>
        </div>
      </div>
    </div>

    <div v-if="books.length === 0" class="empty-state">
      <div class="empty-icon">📖</div>
      <p>{{ hasActiveFilters ? '没有符合条件的书籍' : '暂无书籍，点击上方按钮添加' }}</p>
      <button v-if="hasActiveFilters" @click="resetFilters" class="reset-filters-btn">清除筛选条件</button>
    </div>

    <div v-if="showAddModal" class="modal-overlay" @click.self="closeModal">
      <div class="modal-content">
        <h2>{{ editingBook ? '编辑书籍' : '添加书籍' }}</h2>
        <form @submit.prevent="saveBook">
          <div class="form-group">
            <label>书名 *</label>
            <input v-model="formData.title" type="text" required placeholder="请输入书名" />
          </div>
          <div class="form-group">
            <label>作者</label>
            <input v-model="formData.author" type="text" placeholder="请输入作者" />
          </div>
          <div class="form-group">
            <label>封面URL</label>
            <input v-model="formData.coverUrl" type="text" placeholder="请输入封面图片URL" />
          </div>
          <div class="form-group">
            <label>分类</label>
            <input v-model="formData.category" type="text" placeholder="请输入分类" />
          </div>
          <div class="form-group">
            <label>阅读状态</label>
            <select v-model="formData.status">
              <option value="READING">在读</option>
              <option value="READ">已读</option>
              <option value="WANT_TO_READ">想读</option>
            </select>
          </div>
          <div class="form-group">
            <label>阅读进度 (%)</label>
            <input v-model.number="formData.progress" type="number" min="0" max="100" />
          </div>
          <div class="form-group">
            <label>简介</label>
            <textarea v-model="formData.description" rows="3" placeholder="请输入书籍简介"></textarea>
          </div>
          <div class="form-actions">
            <button type="button" @click="closeModal" class="cancel-btn">取消</button>
            <button type="submit" class="submit-btn">保存</button>
          </div>
        </form>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, watch, computed } from 'vue'
import { bookApi } from '../api'

const books = ref([])
const categories = ref([])
const showAddModal = ref(false)
const editingBook = ref(null)
const formData = ref({
  title: '',
  author: '',
  coverUrl: '',
  category: '',
  status: 'READING',
  progress: 0,
  description: ''
})

const filters = reactive({
  status: '',
  category: '',
  minProgress: null,
  maxProgress: null,
  hasNotes: ''
})

const hasActiveFilters = computed(() => {
  return filters.status !== '' || 
         filters.category !== '' || 
         filters.minProgress !== null || 
         filters.maxProgress !== null ||
         filters.hasNotes !== ''
})

const getStatusLabel = (status) => {
  const labels = {
    READING: '在读',
    READ: '已读',
    WANT_TO_READ: '想读'
  }
  return labels[status] || status
}

const loadCategories = async () => {
  try {
    const response = await bookApi.getAllCategories()
    categories.value = response.data
  } catch (error) {
    console.error('加载分类失败:', error)
  }
}

const loadBooks = async () => {
  try {
    const params = {}
    if (filters.status) params.status = filters.status
    if (filters.category) params.category = filters.category
    if (filters.minProgress !== null) params.minProgress = filters.minProgress
    if (filters.maxProgress !== null) params.maxProgress = filters.maxProgress
    if (filters.hasNotes !== '') params.hasNotes = filters.hasNotes
    
    const response = await bookApi.getAllBooks(params)
    books.value = response.data
  } catch (error) {
    console.error('加载书籍失败:', error)
  }
}

const validateProgress = (type) => {
  if (type === 'min') {
    if (filters.minProgress !== null && filters.minProgress < 0) filters.minProgress = 0
    if (filters.minProgress !== null && filters.minProgress > 100) filters.minProgress = 100
    if (filters.maxProgress !== null && filters.minProgress !== null && filters.minProgress > filters.maxProgress) {
      filters.maxProgress = filters.minProgress
    }
  } else {
    if (filters.maxProgress !== null && filters.maxProgress < 0) filters.maxProgress = 0
    if (filters.maxProgress !== null && filters.maxProgress > 100) filters.maxProgress = 100
    if (filters.minProgress !== null && filters.maxProgress !== null && filters.maxProgress < filters.minProgress) {
      filters.minProgress = filters.maxProgress
    }
  }
}

const clearProgressFilter = () => {
  filters.minProgress = null
  filters.maxProgress = null
}

const resetFilters = () => {
  filters.status = ''
  filters.category = ''
  filters.minProgress = null
  filters.maxProgress = null
  filters.hasNotes = ''
}

const viewBook = (id) => {
  window.location.href = `/book/${id}`
}

const editBook = (book) => {
  editingBook.value = book
  formData.value = {
    title: book.title,
    author: book.author || '',
    coverUrl: book.coverUrl || '',
    category: book.category || '',
    status: book.status || 'READING',
    progress: book.progress !== null ? book.progress : 0,
    description: book.description || ''
  }
  showAddModal.value = true
}

const deleteBook = async (id) => {
  if (!confirm('确定删除这本书吗？')) return
  try {
    await bookApi.deleteBook(id)
    await loadBooks()
    await loadCategories()
  } catch (error) {
    console.error('删除书籍失败:', error)
  }
}

const saveBook = async () => {
  try {
    if (editingBook.value) {
      await bookApi.updateBook(editingBook.value.id, formData.value)
    } else {
      await bookApi.createBook(formData.value)
    }
    closeModal()
    await loadBooks()
    await loadCategories()
  } catch (error) {
    console.error('保存书籍失败:', error)
  }
}

const closeModal = () => {
  showAddModal.value = false
  editingBook.value = null
  formData.value = {
    title: '',
    author: '',
    coverUrl: '',
    category: '',
    status: 'READING',
    progress: 0,
    description: ''
  }
}

onMounted(() => {
  loadCategories()
  loadBooks()
})

watch(filters, () => {
  loadBooks()
}, { deep: true })
</script>

<style scoped>
.book-list {
  width: 100%;
}

.filter-section {
  background: white;
  border-radius: 12px;
  padding: 1.5rem;
  margin-bottom: 2rem;
  box-shadow: 0 2px 10px rgba(0,0,0,0.05);
}

.filter-bar {
  display: flex;
  flex-wrap: wrap;
  gap: 1.5rem;
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
  transition: border-color 0.2s;
}

.filter-select:focus {
  outline: none;
  border-color: #667eea;
}

.progress-filter {
  flex: 1;
  min-width: 200px;
}

.progress-inputs {
  display: flex;
  align-items: center;
  gap: 0.5rem;
}

.progress-input {
  width: 80px;
  padding: 0.6rem 0.75rem;
  border: 1px solid #ddd;
  border-radius: 8px;
  font-size: 0.95rem;
  text-align: center;
  transition: border-color 0.2s;
}

.progress-input:focus {
  outline: none;
  border-color: #667eea;
}

.progress-separator {
  color: #999;
  font-size: 0.9rem;
}

.progress-unit {
  color: #666;
  font-size: 0.9rem;
}

.reset-btn {
  padding: 0.6rem 1.25rem;
  background: #f0f0f0;
  color: #666;
  border: none;
  border-radius: 8px;
  cursor: pointer;
  font-size: 0.9rem;
  transition: all 0.2s;
  height: fit-content;
}

.reset-btn:hover {
  background: #e0e0e0;
  color: #333;
}

.active-filters {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 0.5rem;
  margin-top: 1rem;
  padding-top: 1rem;
  border-top: 1px solid #f0f0f0;
}

.active-filters-label {
  font-size: 0.85rem;
  color: #999;
}

.filter-tag {
  display: inline-flex;
  align-items: center;
  gap: 0.5rem;
  padding: 0.25rem 0.75rem;
  background: linear-gradient(135deg, rgba(102, 126, 234, 0.1) 0%, rgba(118, 75, 162, 0.1) 100%);
  color: #667eea;
  border-radius: 12px;
  font-size: 0.85rem;
}

.remove-tag {
  cursor: pointer;
  font-size: 1.1rem;
  line-height: 1;
  opacity: 0.7;
  transition: opacity 0.2s;
}

.remove-tag:hover {
  opacity: 1;
}

.result-count {
  margin-left: auto;
  font-size: 0.85rem;
  color: #999;
}

.list-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 1.5rem;
}

.section-title {
  font-size: 1.5rem;
  font-weight: 600;
  color: #333;
  margin: 0;
}

.add-btn {
  padding: 0.5rem 1.5rem;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
  border: none;
  border-radius: 8px;
  cursor: pointer;
  font-weight: 500;
  transition: transform 0.2s;
}

.add-btn:hover {
  transform: translateY(-2px);
}

.books-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(280px, 1fr));
  gap: 1.5rem;
}

.book-card {
  background: white;
  border-radius: 12px;
  overflow: hidden;
  box-shadow: 0 2px 10px rgba(0,0,0,0.05);
  cursor: pointer;
  transition: all 0.3s;
  position: relative;
}

.book-card:hover {
  transform: translateY(-5px);
  box-shadow: 0 10px 20px rgba(0,0,0,0.1);
}

.book-cover {
  height: 200px;
  overflow: hidden;
  position: relative;
}

.book-cover img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.note-badge {
  position: absolute;
  top: 0.75rem;
  right: 0.75rem;
  background: rgba(0, 0, 0, 0.7);
  color: white;
  padding: 0.25rem 0.6rem;
  border-radius: 12px;
  font-size: 0.8rem;
  backdrop-filter: blur(4px);
}

.book-info {
  padding: 1rem;
}

.book-title {
  font-size: 1.1rem;
  font-weight: 600;
  margin-bottom: 0.5rem;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.book-author {
  color: #666;
  font-size: 0.9rem;
  margin-bottom: 0.75rem;
}

.book-meta {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  margin-bottom: 0.5rem;
}

.status-badge {
  padding: 0.25rem 0.75rem;
  border-radius: 12px;
  font-size: 0.75rem;
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

.progress-text {
  color: #999;
  font-size: 0.85rem;
}

.book-category {
  margin-top: 0.5rem;
}

.category-tag {
  display: inline-block;
  padding: 0.2rem 0.6rem;
  background: #f5f0ff;
  color: #764ba2;
  border-radius: 8px;
  font-size: 0.75rem;
}

.book-actions {
  display: none;
  position: absolute;
  bottom: 0;
  left: 0;
  right: 0;
  background: rgba(255,255,255,0.95);
  padding: 1rem;
  gap: 0.5rem;
}

.book-card:hover .book-actions {
  display: flex;
}

.action-btn {
  flex: 1;
  padding: 0.5rem;
  border: none;
  border-radius: 6px;
  cursor: pointer;
  font-size: 0.9rem;
}

.edit-btn {
  background: #f0f0f0;
  color: #333;
}

.delete-btn {
  background: #ffebee;
  color: #c62828;
}

.empty-state {
  text-align: center;
  padding: 4rem;
}

.empty-icon {
  font-size: 4rem;
  margin-bottom: 1rem;
}

.empty-state p {
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

.modal-overlay {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0,0,0,0.5);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1000;
}

.modal-content {
  background: white;
  border-radius: 12px;
  padding: 2rem;
  width: 90%;
  max-width: 500px;
  max-height: 90vh;
  overflow-y: auto;
}

.modal-content h2 {
  margin-bottom: 1.5rem;
  color: #333;
}

.form-group {
  margin-bottom: 1rem;
}

.form-group label {
  display: block;
  margin-bottom: 0.5rem;
  font-weight: 500;
  color: #555;
}

.form-group input,
.form-group select,
.form-group textarea {
  width: 100%;
  padding: 0.75rem;
  border: 1px solid #ddd;
  border-radius: 8px;
  font-size: 1rem;
}

.form-actions {
  display: flex;
  gap: 1rem;
  margin-top: 1.5rem;
}

.cancel-btn,
.submit-btn {
  flex: 1;
  padding: 0.75rem;
  border: none;
  border-radius: 8px;
  cursor: pointer;
  font-size: 1rem;
}

.cancel-btn {
  background: #f0f0f0;
  color: #333;
}

.submit-btn {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
}
</style>
