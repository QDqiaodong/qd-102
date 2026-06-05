
<template>
  <div class="card-wall">
    <div class="wall-header">
      <div class="header-left">
        <h2 class="wall-title">📜 书摘卡片墙</h2>
        <p class="wall-subtitle">翻阅你的思想素材库</p>
      </div>
      <div class="summary-stats">
        <div class="stat-item">
          <span class="stat-number">{{ summary.totalNotes || 0 }}</span>
          <span class="stat-label">条书摘</span>
        </div>
        <div class="stat-divider"></div>
        <div class="stat-item">
          <span class="stat-number">{{ summary.totalBooks || 0 }}</span>
          <span class="stat-label">本书</span>
        </div>
        <div class="stat-divider"></div>
        <div class="stat-item">
          <span class="stat-number">{{ summary.totalTags || 0 }}</span>
          <span class="stat-label">个标签</span>
        </div>
      </div>
    </div>

    <div class="filter-bar">
      <div class="filter-group">
        <label class="filter-label">📚 按书籍</label>
        <select v-model="selectedBookId" @change="handleBookChange" class="filter-select">
          <option :value="null">全部书籍</option>
          <option v-for="book in books" :key="book.id" :value="book.id">{{ book.title }}</option>
        </select>
      </div>

      <div class="filter-group">
        <label class="filter-label">🏷️ 按标签</label>
        <select v-model="selectedTagId" @change="loadCards" class="filter-select">
          <option :value="null">全部标签</option>
          <option v-for="tag in tags" :key="tag.id" :value="tag.id">{{ tag.name }}</option>
        </select>
      </div>

      <div class="filter-group">
        <label class="filter-label">📄 按页码</label>
        <select v-model="selectedPage" @change="loadCards" class="filter-select" :disabled="!selectedBookId">
          <option :value="null">全部页码</option>
          <option v-for="page in pageNumbers" :key="page" :value="page">第 {{ page }} 页</option>
        </select>
      </div>

      <div class="filter-group sort-group">
        <label class="filter-label">↕️ 排序</label>
        <select v-model="sortBy" @change="sortCards" class="filter-select">
          <option value="newest">最新创建</option>
          <option value="oldest">最早创建</option>
          <option value="page">页码顺序</option>
        </select>
      </div>

      <button @click="resetFilters" class="reset-btn">重置筛选</button>
    </div>

    <div v-if="loading" class="loading-state">
      <div class="loading-spinner"></div>
      <p>正在加载书摘卡片...</p>
    </div>

    <div v-else-if="cards.length === 0" class="empty-state">
      <div class="empty-icon">📝</div>
      <h3>暂无书摘卡片</h3>
      <p>还没有符合条件的书摘，快去添加笔记吧~</p>
    </div>

    <div v-else class="cards-grid">
      <div
        v-for="(card, index) in sortedCards"
        :key="card.id"
        class="card-item"
        :style="{ animationDelay: `${index * 0.05}s` }"
        @click="openCardDetail(card)"
      >
        <div class="card-inner">
          <div class="card-header">
            <span class="card-book" :title="card.bookTitle">{{ card.bookTitle || '未知书籍' }}</span>
            <span v-if="card.pageNumber" class="card-page">P.{{ card.pageNumber }}</span>
          </div>

          <div class="card-content">
            <p class="card-text">{{ truncateText(card.content || card.title, 120) }}</p>
          </div>

          <div v-if="card.tags && card.tags.length > 0" class="card-tags">
            <span
              v-for="tag in card.tags.slice(0, 3)"
              :key="tag.id"
              class="tag-chip"
              :style="{ backgroundColor: tag.color + '20', color: tag.color, borderColor: tag.color + '40' }"
            >
              #{{ tag.name }}
            </span>
            <span v-if="card.tags.length > 3" class="tag-more">+{{ card.tags.length - 3 }}</span>
          </div>

          <div class="card-footer">
            <span class="card-date">{{ formatDate(card.createdAt) }}</span>
            <span class="card-action">查看详情 →</span>
          </div>
        </div>
      </div>
    </div>

    <div v-if="totalPages > 1" class="pagination">
      <button @click="prevPage" :disabled="currentPage === 0" class="page-btn">← 上一页</button>
      <span class="page-info">第 {{ currentPage + 1 }} / {{ totalPages }} 页</span>
      <button @click="nextPage" :disabled="currentPage >= totalPages - 1" class="page-btn">下一页 →</button>
    </div>

    <div v-if="showDetail && selectedCard" class="detail-modal" @click.self="closeDetail">
      <div class="detail-content">
        <button @click="closeDetail" class="close-btn">✕</button>

        <div class="detail-header">
          <h3 class="detail-title">{{ selectedCard.title || '书摘卡片' }}</h3>
          <div class="detail-meta">
            <span class="detail-book">📚 {{ selectedCard.bookTitle }}</span>
            <span v-if="selectedCard.pageNumber" class="detail-page">📄 第 {{ selectedCard.pageNumber }} 页</span>
          </div>
        </div>

        <div class="detail-body">
          <p class="detail-text">{{ selectedCard.content }}</p>
        </div>

        <div v-if="selectedCard.tags && selectedCard.tags.length > 0" class="detail-tags">
          <span
            v-for="tag in selectedCard.tags"
            :key="tag.id"
            class="tag-chip large"
            :style="{ backgroundColor: tag.color + '20', color: tag.color, borderColor: tag.color + '40' }"
          >
            #{{ tag.name }}
          </span>
        </div>

        <div class="detail-footer">
          <span class="detail-date">创建于 {{ formatDate(selectedCard.createdAt) }}</span>
          <button @click="goToNote" class="view-note-btn">查看完整笔记 →</button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, computed } from 'vue'
import { useRouter } from 'vue-router'
import { cardApi } from '../api'

const router = useRouter()

const cards = ref([])
const books = ref([])
const tags = ref([])
const pageNumbers = ref([])
const summary = ref({})
const loading = ref(false)

const selectedBookId = ref(null)
const selectedTagId = ref(null)
const selectedPage = ref(null)
const sortBy = ref('newest')

const currentPage = ref(0)
const pageSize = ref(12)
const totalPages = ref(0)
const totalElements = ref(0)

const showDetail = ref(false)
const selectedCard = ref(null)

const sortedCards = computed(() => {
  const list = [...cards.value]
  switch (sortBy.value) {
    case 'newest':
      return list.sort((a, b) => new Date(b.createdAt) - new Date(a.createdAt))
    case 'oldest':
      return list.sort((a, b) => new Date(a.createdAt) - new Date(b.createdAt))
    case 'page':
      return list.sort((a, b) => (a.pageNumber || 0) - (b.pageNumber || 0))
    default:
      return list
  }
})

const loadSummary = async () => {
  try {
    const response = await cardApi.getCardSummary()
    summary.value = response.data
  } catch (error) {
    console.error('加载统计数据失败:', error)
  }
}

const loadBooks = async () => {
  try {
    const response = await cardApi.getCardBooks()
    books.value = response.data
  } catch (error) {
    console.error('加载书籍列表失败:', error)
  }
}

const loadTags = async () => {
  try {
    const response = await cardApi.getCardTags()
    tags.value = response.data
  } catch (error) {
    console.error('加载标签列表失败:', error)
  }
}

const loadPageNumbers = async (bookId) => {
  if (!bookId) {
    pageNumbers.value = []
    return
  }
  try {
    const response = await cardApi.getPageNumbers(bookId)
    pageNumbers.value = response.data
  } catch (error) {
    console.error('加载页码列表失败:', error)
  }
}

const loadCards = async () => {
  loading.value = true
  try {
    const params = {
      page: currentPage.value,
      size: pageSize.value
    }
    if (selectedBookId.value) params.bookId = selectedBookId.value
    if (selectedTagId.value) params.tagId = selectedTagId.value
    if (selectedPage.value) params.pageNumber = selectedPage.value

    const response = await cardApi.getCardsPaged(params)
    cards.value = response.data.content
    totalPages.value = response.data.totalPages
    totalElements.value = response.data.totalElements
  } catch (error) {
    console.error('加载卡片失败:', error)
  } finally {
    loading.value = false
  }
}

const handleBookChange = async () => {
  selectedPage.value = null
  await loadPageNumbers(selectedBookId.value)
  currentPage.value = 0
  loadCards()
}

const resetFilters = () => {
  selectedBookId.value = null
  selectedTagId.value = null
  selectedPage.value = null
  pageNumbers.value = []
  currentPage.value = 0
  sortBy.value = 'newest'
  loadCards()
}

const sortCards = () => {
}

const prevPage = () => {
  if (currentPage.value > 0) {
    currentPage.value--
    loadCards()
  }
}

const nextPage = () => {
  if (currentPage.value < totalPages.value - 1) {
    currentPage.value++
    loadCards()
  }
}

const openCardDetail = (card) => {
  selectedCard.value = card
  showDetail.value = true
}

const closeDetail = () => {
  showDetail.value = false
  selectedCard.value = null
}

const goToNote = () => {
  if (selectedCard.value) {
    router.push(`/note/${selectedCard.value.id}/edit`)
  }
}

const truncateText = (text, maxLength) => {
  if (!text) return ''
  if (text.length <= maxLength) return text
  return text.substring(0, maxLength) + '...'
}

const formatDate = (dateStr) => {
  if (!dateStr) return ''
  const date = new Date(dateStr)
  return date.toLocaleDateString('zh-CN', {
    year: 'numeric',
    month: 'long',
    day: 'numeric'
  })
}

onMounted(async () => {
  await Promise.all([
    loadSummary(),
    loadBooks(),
    loadTags()
  ])
  loadCards()
})
</script>

<style scoped>
.card-wall {
  width: 100%;
}

.wall-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-end;
  margin-bottom: 2rem;
  padding-bottom: 1.5rem;
  border-bottom: 1px solid #eee;
}

.wall-title {
  font-size: 1.75rem;
  color: #333;
  margin-bottom: 0.5rem;
}

.wall-subtitle {
  color: #888;
  font-size: 0.95rem;
}

.summary-stats {
  display: flex;
  align-items: center;
  gap: 1.5rem;
  padding: 1rem 1.5rem;
  background: linear-gradient(135deg, #667eea10 0%, #764ba210 100%);
  border-radius: 12px;
}

.stat-item {
  display: flex;
  flex-direction: column;
  align-items: center;
}

.stat-number {
  font-size: 1.5rem;
  font-weight: 700;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
}

.stat-label {
  font-size: 0.85rem;
  color: #888;
  margin-top: 0.25rem;
}

.stat-divider {
  width: 1px;
  height: 30px;
  background: #ddd;
}

.filter-bar {
  display: flex;
  flex-wrap: wrap;
  gap: 1rem;
  align-items: flex-end;
  padding: 1.25rem;
  background: white;
  border-radius: 12px;
  box-shadow: 0 2px 8px rgba(0,0,0,0.04);
  margin-bottom: 2rem;
}

.filter-group {
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
  flex: 1;
  min-width: 140px;
}

.filter-label {
  font-size: 0.85rem;
  color: #666;
  font-weight: 500;
}

.filter-select {
  padding: 0.6rem 0.75rem;
  border: 1px solid #ddd;
  border-radius: 8px;
  font-size: 0.9rem;
  background: white;
  cursor: pointer;
  transition: border-color 0.3s;
}

.filter-select:hover {
  border-color: #667eea;
}

.filter-select:focus {
  outline: none;
  border-color: #667eea;
  box-shadow: 0 0 0 3px rgba(102, 126, 234, 0.1);
}

.filter-select:disabled {
  background: #f5f5f5;
  cursor: not-allowed;
  opacity: 0.6;
}

.sort-group {
  max-width: 140px;
}

.reset-btn {
  padding: 0.6rem 1.25rem;
  border: 1px solid #ddd;
  border-radius: 8px;
  background: white;
  color: #666;
  cursor: pointer;
  font-size: 0.9rem;
  transition: all 0.3s;
  height: fit-content;
}

.reset-btn:hover {
  border-color: #667eea;
  color: #667eea;
  background: #f8f9ff;
}

.loading-state,
.empty-state {
  text-align: center;
  padding: 4rem 2rem;
}

.loading-spinner {
  width: 40px;
  height: 40px;
  border: 3px solid #f0f0f0;
  border-top-color: #667eea;
  border-radius: 50%;
  margin: 0 auto 1rem;
  animation: spin 1s linear infinite;
}

@keyframes spin {
  to { transform: rotate(360deg); }
}

.loading-state p,
.empty-state p {
  color: #999;
}

.empty-icon {
  font-size: 4rem;
  margin-bottom: 1rem;
}

.empty-state h3 {
  color: #555;
  margin-bottom: 0.5rem;
}

.cards-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(300px, 1fr));
  gap: 1.5rem;
  margin-bottom: 2rem;
}

.card-item {
  animation: fadeInUp 0.5s ease both;
}

@keyframes fadeInUp {
  from {
    opacity: 0;
    transform: translateY(20px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

.card-inner {
  background: white;
  border-radius: 12px;
  padding: 1.25rem;
  box-shadow: 0 2px 10px rgba(0,0,0,0.05);
  cursor: pointer;
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  height: 100%;
  display: flex;
  flex-direction: column;
  border: 1px solid transparent;
  position: relative;
  overflow: hidden;
}

.card-inner::before {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  height: 4px;
  background: linear-gradient(90deg, #667eea, #764ba2);
  transform: scaleX(0);
  transform-origin: left;
  transition: transform 0.3s;
}

.card-inner:hover {
  transform: translateY(-4px);
  box-shadow: 0 12px 24px rgba(102, 126, 234, 0.15);
  border-color: #e0e0ff;
}

.card-inner:hover::before {
  transform: scaleX(1);
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 0.75rem;
  padding-bottom: 0.75rem;
  border-bottom: 1px dashed #eee;
}

.card-book {
  font-size: 0.85rem;
  color: #667eea;
  font-weight: 500;
  max-width: 70%;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.card-page {
  font-size: 0.75rem;
  color: #999;
  background: #f5f5f5;
  padding: 0.2rem 0.6rem;
  border-radius: 10px;
}

.card-content {
  flex: 1;
  margin-bottom: 1rem;
}

.card-text {
  color: #444;
  font-size: 0.95rem;
  line-height: 1.7;
  display: -webkit-box;
  -webkit-line-clamp: 4;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.card-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 0.5rem;
  margin-bottom: 1rem;
}

.tag-chip {
  font-size: 0.75rem;
  padding: 0.25rem 0.6rem;
  border-radius: 12px;
  border: 1px solid;
  font-weight: 500;
}

.tag-chip.large {
  font-size: 0.85rem;
  padding: 0.4rem 0.8rem;
}

.tag-more {
  font-size: 0.75rem;
  color: #999;
  padding: 0.25rem 0;
}

.card-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding-top: 0.75rem;
  border-top: 1px solid #f0f0f0;
}

.card-date {
  font-size: 0.8rem;
  color: #bbb;
}

.card-action {
  font-size: 0.85rem;
  color: #667eea;
  font-weight: 500;
  opacity: 0;
  transform: translateX(-10px);
  transition: all 0.3s;
}

.card-inner:hover .card-action {
  opacity: 1;
  transform: translateX(0);
}

.pagination {
  display: flex;
  justify-content: center;
  align-items: center;
  gap: 1.5rem;
  padding: 1rem;
}

.page-btn {
  padding: 0.6rem 1.25rem;
  border: 1px solid #ddd;
  border-radius: 8px;
  background: white;
  color: #666;
  cursor: pointer;
  font-size: 0.9rem;
  transition: all 0.3s;
}

.page-btn:hover:not(:disabled) {
  border-color: #667eea;
  color: #667eea;
  background: #f8f9ff;
}

.page-btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.page-info {
  color: #888;
  font-size: 0.9rem;
}

.detail-modal {
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
  padding: 1rem;
  animation: fadeIn 0.2s ease;
}

@keyframes fadeIn {
  from { opacity: 0; }
  to { opacity: 1; }
}

.detail-content {
  background: white;
  border-radius: 16px;
  width: 90%;
  max-width: 600px;
  max-height: 85vh;
  overflow-y: auto;
  padding: 2rem;
  position: relative;
  animation: slideUp 0.3s ease;
}

@keyframes slideUp {
  from {
    opacity: 0;
    transform: translateY(30px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

.close-btn {
  position: absolute;
  top: 1rem;
  right: 1rem;
  width: 36px;
  height: 36px;
  border: none;
  background: #f5f5f5;
  border-radius: 50%;
  cursor: pointer;
  font-size: 1rem;
  color: #888;
  transition: all 0.3s;
}

.close-btn:hover {
  background: #eee;
  color: #555;
}

.detail-header {
  margin-bottom: 1.5rem;
  padding-bottom: 1rem;
  border-bottom: 2px solid #f0f0f0;
}

.detail-title {
  font-size: 1.4rem;
  color: #333;
  margin-bottom: 0.75rem;
}

.detail-meta {
  display: flex;
  gap: 1rem;
  flex-wrap: wrap;
}

.detail-book,
.detail-page {
  font-size: 0.9rem;
  color: #667eea;
  background: #f0f4ff;
  padding: 0.4rem 0.8rem;
  border-radius: 8px;
}

.detail-body {
  margin-bottom: 1.5rem;
}

.detail-text {
  color: #444;
  line-height: 2;
  font-size: 1rem;
  white-space: pre-wrap;
  word-wrap: break-word;
}

.detail-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 0.75rem;
  margin-bottom: 1.5rem;
  padding: 1rem;
  background: #fafafa;
  border-radius: 10px;
}

.detail-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding-top: 1rem;
  border-top: 1px solid #eee;
}

.detail-date {
  font-size: 0.85rem;
  color: #aaa;
}

.view-note-btn {
  padding: 0.6rem 1.25rem;
  border: none;
  border-radius: 8px;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
  cursor: pointer;
  font-size: 0.9rem;
  font-weight: 500;
  transition: transform 0.2s;
}

.view-note-btn:hover {
  transform: translateY(-2px);
}

@media (max-width: 768px) {
  .wall-header {
    flex-direction: column;
    align-items: flex-start;
    gap: 1rem;
  }

  .summary-stats {
    width: 100%;
    justify-content: space-around;
  }

  .filter-bar {
    flex-direction: column;
    align-items: stretch;
  }

  .filter-group {
    min-width: 100%;
  }

  .cards-grid {
    grid-template-columns: 1fr;
  }
}
</style>
