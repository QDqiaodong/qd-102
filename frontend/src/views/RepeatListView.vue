
<template>
  <div class="repeat-list-page">
    <div class="page-header">
      <h1 class="page-title">🔁 复读清单</h1>
      <p class="page-subtitle">按艾宾浩斯遗忘曲线复习法，让重要笔记形成长期记忆</p>
    </div>

    <div class="stats-cards">
      <div class="stat-card due" :class="{ active: activeTab === 'all' }" @click="activeTab = 'all'">
        <div class="stat-number">{{ repeatList.totalCount }}</div>
        <div class="stat-label">全部笔记</div>
      </div>
      <div class="stat-card due" :class="{ active: activeTab === 'due' }" @click="activeTab = 'due'">
        <div class="stat-number">{{ repeatList.dueCount }}</div>
        <div class="stat-label">待复习</div>
      </div>
      <div class="stat-card upcoming" :class="{ active: activeTab === 'upcoming' }" @click="activeTab = 'upcoming'">
        <div class="stat-number">{{ repeatList.upcomingCount }}</div>
        <div class="stat-label">即将复习</div>
      </div>
    </div>

    <div class="tab-buttons">
      <button :class="['tab-btn', { active: activeTab === 'all' }]" @click="activeTab = 'all'">全部</button>
      <button :class="['tab-btn', { active: activeTab === 'due' }]" @click="activeTab = 'due'">
        待复习 <span v-if="repeatList.dueCount > 0" class="badge">{{ repeatList.dueCount }}</span>
      </button>
      <button :class="['tab-btn', { active: activeTab === 'upcoming' }]" @click="activeTab = 'upcoming'">即将复习</button>
    </div>

    <div v-if="loading" class="loading">加载中...</div>

    <div v-else-if="displayNotes.length === 0" class="empty-state">
      <div class="empty-icon">📚</div>
      <h3>{{ emptyMessage }}</h3>
      <p>{{ emptySubMessage }}</p>
    </div>

    <div v-else class="repeat-notes-list">
      <div v-for="item in displayNotes" :key="item.id" class="repeat-note-card" :class="item.status">
        <div class="card-left">
          <div class="note-status">
            <span v-if="item.status === 'due'" class="status-badge due">待复习</span>
            <span v-else class="status-badge upcoming">{{ getUpcomingLabel(item) }}后复习</span>
          </div>
          <h3 class="note-title" @click="goToNote(item)">{{ item.note.title }}</h3>
          <p class="note-preview">{{ getNotePreview(item.note.content) }}</p>
          <div class="note-meta">
            <span class="book-name" @click="goToBook(item)">📖 {{ item.note.bookTitle }}</span>
            <span v-if="item.note.pageNumber" class="page-number">第 {{ item.note.pageNumber }} 页</span>
          </div>
          <div class="review-info">
            <span class="review-count">已复习 {{ item.reviewCount }} 次</span>
            <span v-if="item.lastReviewTime" class="last-review">上次复习: {{ formatDate(item.lastReviewTime) }}</span>
          </div>
        </div>
        <div class="card-actions">
          <button v-if="item.status === 'due'" @click="handleMarkReviewed(item)" class="action-btn primary">
            ✓ 标记已复习
          </button>
          <button @click="goToNote(item)" class="action-btn">查看笔记</button>
          <button @click="goToBook(item)" class="action-btn">原书详情</button>
          <button @click="handleRemove(item)" class="action-btn danger">移除</button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { repeatedNoteApi } from '../api'

const repeatList = ref({
  dueNotes: [],
  upcomingNotes: [],
  dueCount: 0,
  upcomingCount: 0,
  totalCount: 0
})
const loading = ref(true)
const activeTab = ref('all')

const displayNotes = computed(() => {
  if (activeTab.value === 'due') {
    return repeatList.value.dueNotes
  } else if (activeTab.value === 'upcoming') {
    return repeatList.value.upcomingNotes
  }
  return [...repeatList.value.dueNotes, ...repeatList.value.upcomingNotes]
})

const emptyMessage = computed(() => {
  if (activeTab.value === 'due') return '暂无待复习的笔记'
  if (activeTab.value === 'upcoming') return '暂无即将复习的笔记'
  return '复读清单是空的'
})

const emptySubMessage = computed(() => {
  if (activeTab.value === 'due') return '所有笔记都复习完啦，太棒了！'
  if (activeTab.value === 'upcoming') return '新加入的笔记会出现在这里'
  return '去笔记详情页点击"加入复读清单"吧'
})

const loadRepeatList = async () => {
  loading.value = true
  try {
    const response = await repeatedNoteApi.getAllRepeatedNotes()
    repeatList.value = response.data
  } catch (error) {
    console.error('加载复读清单失败:', error)
  } finally {
    loading.value = false
  }
}

const getNotePreview = (content) => {
  if (!content) return ''
  const text = content.replace(/<[^>]*>/g, '').substring(0, 80)
  return text + (content.length > 80 ? '...' : '')
}

const formatDate = (dateStr) => {
  if (!dateStr) return '从未'
  const date = new Date(dateStr)
  return date.toLocaleDateString('zh-CN')
}

const getUpcomingLabel = (item) => {
  const days = item.daysUntilReview
  if (days === 0) return '今天'
  if (days === 1) return '明天'
  if (days < 7) return `${days} 天`
  if (days < 30) return `${Math.floor(days / 7)} 周`
  return `${Math.floor(days / 30)} 个月`
}

const goToNote = (item) => {
  window.location.href = `/note/${item.note.id}/edit`
}

const goToBook = (item) => {
  window.location.href = `/book/${item.note.bookId}`
}

const handleMarkReviewed = async (item) => {
  try {
    await repeatedNoteApi.markAsReviewed(item.note.id)
    await loadRepeatList()
  } catch (error) {
    console.error('标记已复习失败:', error)
  }
}

const handleRemove = async (item) => {
  if (!confirm('确定要从复读清单中移除这条笔记吗？')) return
  try {
    await repeatedNoteApi.removeFromRepeatList(item.note.id)
    await loadRepeatList()
  } catch (error) {
    console.error('移除失败:', error)
  }
}

onMounted(loadRepeatList)
</script>

<style scoped>
.repeat-list-page {
  width: 100%;
}

.page-header {
  margin-bottom: 2rem;
}

.page-title {
  font-size: 2rem;
  color: #333;
  margin-bottom: 0.5rem;
}

.page-subtitle {
  color: #666;
  font-size: 1rem;
}

.stats-cards {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 1.5rem;
  margin-bottom: 2rem;
}

.stat-card {
  background: white;
  padding: 1.5rem;
  border-radius: 12px;
  box-shadow: 0 2px 10px rgba(0,0,0,0.05);
  text-align: center;
  cursor: pointer;
  transition: all 0.3s;
  border: 2px solid transparent;
}

.stat-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 15px rgba(0,0,0,0.1);
}

.stat-card.active {
  border-color: #667eea;
}

.stat-card.due .stat-number {
  color: #e65100;
}

.stat-card.upcoming .stat-number {
  color: #2e7d32;
}

.stat-number {
  font-size: 2.5rem;
  font-weight: 700;
  margin-bottom: 0.5rem;
  color: #333;
}

.stat-label {
  color: #666;
  font-size: 0.95rem;
}

.tab-buttons {
  display: flex;
  gap: 0.5rem;
  margin-bottom: 1.5rem;
}

.tab-btn {
  padding: 0.6rem 1.25rem;
  border: none;
  background: white;
  border-radius: 8px;
  cursor: pointer;
  font-size: 0.95rem;
  color: #666;
  transition: all 0.2s;
  display: flex;
  align-items: center;
  gap: 0.5rem;
}

.tab-btn:hover {
  background: #f5f5f5;
}

.tab-btn.active {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
}

.badge {
  background: rgba(255,255,255,0.3);
  padding: 0.1rem 0.5rem;
  border-radius: 10px;
  font-size: 0.8rem;
}

.repeat-notes-list {
  display: flex;
  flex-direction: column;
  gap: 1rem;
}

.repeat-note-card {
  background: white;
  border-radius: 12px;
  padding: 1.5rem;
  box-shadow: 0 2px 10px rgba(0,0,0,0.05);
  display: flex;
  gap: 1.5rem;
  align-items: stretch;
  transition: all 0.3s;
  border-left: 4px solid transparent;
}

.repeat-note-card.due {
  border-left-color: #e65100;
}

.repeat-note-card.upcoming {
  border-left-color: #2e7d32;
}

.repeat-note-card:hover {
  box-shadow: 0 4px 15px rgba(0,0,0,0.08);
}

.card-left {
  flex: 1;
  min-width: 0;
}

.note-status {
  margin-bottom: 0.75rem;
}

.status-badge {
  display: inline-block;
  padding: 0.25rem 0.75rem;
  border-radius: 12px;
  font-size: 0.8rem;
  font-weight: 500;
}

.status-badge.due {
  background: #fff3e0;
  color: #e65100;
}

.status-badge.upcoming {
  background: #e8f5e9;
  color: #2e7d32;
}

.note-title {
  font-size: 1.2rem;
  color: #333;
  margin: 0 0 0.5rem 0;
  cursor: pointer;
  transition: color 0.2s;
}

.note-title:hover {
  color: #667eea;
}

.note-preview {
  color: #666;
  font-size: 0.95rem;
  line-height: 1.6;
  margin-bottom: 1rem;
}

.note-meta {
  display: flex;
  gap: 1.5rem;
  margin-bottom: 0.75rem;
  font-size: 0.9rem;
  color: #888;
}

.book-name {
  cursor: pointer;
  transition: color 0.2s;
}

.book-name:hover {
  color: #667eea;
}

.review-info {
  display: flex;
  gap: 1.5rem;
  font-size: 0.85rem;
  color: #999;
}

.card-actions {
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
  flex-shrink: 0;
  width: 140px;
}

.action-btn {
  padding: 0.6rem 1rem;
  border: 1px solid #e0e0e0;
  background: white;
  border-radius: 8px;
  cursor: pointer;
  font-size: 0.9rem;
  color: #555;
  transition: all 0.2s;
}

.action-btn:hover {
  background: #f5f5f5;
}

.action-btn.primary {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
  border: none;
}

.action-btn.primary:hover {
  opacity: 0.9;
}

.action-btn.danger {
  color: #c62828;
  border-color: #ffcdd2;
}

.action-btn.danger:hover {
  background: #ffebee;
}

.empty-state {
  text-align: center;
  padding: 4rem 2rem;
  background: white;
  border-radius: 12px;
  box-shadow: 0 2px 10px rgba(0,0,0,0.05);
}

.empty-icon {
  font-size: 4rem;
  margin-bottom: 1rem;
}

.empty-state h3 {
  color: #333;
  margin-bottom: 0.5rem;
  font-size: 1.3rem;
}

.empty-state p {
  color: #999;
  font-size: 1rem;
}

.loading {
  text-align: center;
  padding: 4rem;
  color: #999;
  font-size: 1rem;
}
</style>
