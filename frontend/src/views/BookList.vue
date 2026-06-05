
<template>
  <div class="book-list">
    <div class="list-header">
      <div class="filter-tabs">
        <button 
          v-for="tab in filterTabs" 
          :key="tab.value"
          :class="['filter-tab', { active: activeFilter === tab.value }]"
          @click="activeFilter = tab.value"
        >
          {{ tab.label }}
        </button>
      </div>
      <button @click="showAddModal = true" class="add-btn">+ 添加书籍</button>
    </div>
    
    <div class="books-grid">
      <div v-for="book in books" :key="book.id" class="book-card" @click="viewBook(book.id)">
        <div class="book-cover">
          <img :src="book.coverUrl || 'https://via.placeholder.com/150x200?text=No+Cover'" :alt="book.title" />
        </div>
        <div class="book-info">
          <h3 class="book-title">{{ book.title }}</h3>
          <p class="book-author">{{ book.author }}</p>
          <div class="book-meta">
            <span :class="['status-badge', book.status.toLowerCase()]">{{ getStatusLabel(book.status) }}</span>
            <span v-if="book.progress" class="progress-text">{{ book.progress }}%</span>
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
      <p>暂无书籍，点击上方按钮添加</p>
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
import { ref, onMounted, watch } from 'vue'
import { bookApi } from '../api'

const books = ref([])
const activeFilter = ref('all')
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

const filterTabs = [
  { label: '全部', value: 'all' },
  { label: '在读', value: 'READING' },
  { label: '已读', value: 'READ' },
  { label: '想读', value: 'WANT_TO_READ' }
]

const getStatusLabel = (status) => {
  const labels = {
    READING: '在读',
    READ: '已读',
    WANT_TO_READ: '想读'
  }
  return labels[status] || status
}

const loadBooks = async () => {
  try {
    const params = activeFilter.value !== 'all' ? { status: activeFilter.value } : {}
    const response = await bookApi.getAllBooks(params)
    books.value = response.data
  } catch (error) {
    console.error('加载书籍失败:', error)
  }
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
    progress: book.progress || 0,
    description: book.description || ''
  }
  showAddModal.value = true
}

const deleteBook = async (id) => {
  if (!confirm('确定删除这本书吗？')) return
  try {
    await bookApi.deleteBook(id)
    await loadBooks()
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

onMounted(loadBooks)
watch(activeFilter, loadBooks)
</script>

<style scoped>
.book-list {
  width: 100%;
}

.list-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 2rem;
}

.filter-tabs {
  display: flex;
  gap: 0.5rem;
}

.filter-tab {
  padding: 0.5rem 1rem;
  border: none;
  border-radius: 20px;
  background: white;
  cursor: pointer;
  transition: all 0.3s;
}

.filter-tab.active {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
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
}

.book-cover img {
  width: 100%;
  height: 100%;
  object-fit: cover;
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
