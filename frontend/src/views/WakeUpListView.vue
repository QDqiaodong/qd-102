<template>
  <div class="wakeup-page">
    <div class="page-header">
      <div>
        <h2>📣 待回访书单</h2>
        <p class="page-subtitle">识别长期低活跃的书籍，提醒你补齐知识闭环</p>
      </div>
      <div class="threshold-selector">
        <span class="threshold-label">阈值：</span>
        <button
          v-for="opt in thresholdOptions"
          :key="opt.value"
          :class="['threshold-btn', { active: selectedDays === opt.value }]"
          @click="changeThreshold(opt.value)"
        >
          {{ opt.label }}
        </button>
      </div>
    </div>

    <div class="stats-cards" v-if="wakeUpData">
      <div class="stat-card stagnant-reading" @click="activeTab = 'stagnantReading'">
        <div class="stat-icon">📖</div>
        <div class="stat-content">
          <span class="stat-number">{{ wakeUpData.stagnantReading?.length || 0 }}</span>
          <span class="stat-label">在读停滞</span>
        </div>
      </div>
      <div class="stat-card neglected-want" @click="activeTab = 'neglectedWantToRead'">
        <div class="stat-icon">📚</div>
        <div class="stat-content">
          <span class="stat-number">{{ wakeUpData.neglectedWantToRead?.length || 0 }}</span>
          <span class="stat-label">想读尘封</span>
        </div>
      </div>
      <div class="stat-card read-no-summary" @click="activeTab = 'readWithoutSummary'">
        <div class="stat-icon">📝</div>
        <div class="stat-content">
          <span class="stat-number">{{ wakeUpData.readWithoutSummary?.length || 0 }}</span>
          <span class="stat-label">已读无总结</span>
        </div>
      </div>
    </div>

    <div class="loading" v-if="loading">
      <div class="spinner"></div>
      <span>加载中...</span>
    </div>

    <div class="empty-state" v-if="!loading && wakeUpData && wakeUpData.totalCount === 0">
      <div class="empty-icon">🎉</div>
      <p>太棒了！所有书籍都在活跃状态</p>
      <p class="empty-hint">继续保持阅读节奏，定期回访你的书单</p>
    </div>

    <div class="tabs-section" v-if="!loading && wakeUpData && wakeUpData.totalCount > 0">
      <div class="tabs-nav">
        <button
          :class="['tab-btn', { active: activeTab === 'stagnantReading' }]"
          @click="activeTab = 'stagnantReading'"
        >
          📖 在读停滞
          <span class="tab-count">{{ wakeUpData.stagnantReading?.length || 0 }}</span>
        </button>
        <button
          :class="['tab-btn', { active: activeTab === 'neglectedWantToRead' }]"
          @click="activeTab = 'neglectedWantToRead'"
        >
          📚 想读尘封
          <span class="tab-count">{{ wakeUpData.neglectedWantToRead?.length || 0 }}</span>
        </button>
        <button
          :class="['tab-btn', { active: activeTab === 'readWithoutSummary' }]"
          @click="activeTab = 'readWithoutSummary'"
        >
          📝 已读无总结
          <span class="tab-count">{{ wakeUpData.readWithoutSummary?.length || 0 }}</span>
        </button>
      </div>

      <div class="tab-content">
        <div class="book-list-section" v-if="activeTab === 'stagnantReading'">
          <div class="section-intro">
            <h3>长期在读却无新增笔记</h3>
            <p>这些书你已经开始阅读了，但最近没有记笔记。捡起来继续读吧，别让之前的努力白费！</p>
          </div>
          <div class="books-grid">
            <div
              v-for="item in wakeUpData.stagnantReading"
              :key="item.book.id"
              class="wakeup-book-card"
              @click="goToBook(item.book.id)"
            >
              <div class="book-cover">
                <img :src="item.book.coverUrl || defaultCover" :alt="item.book.title" />
                <div class="days-badge">
                  {{ item.daysInactive }}天
                </div>
              </div>
              <div class="book-info">
                <h4 class="book-title">{{ item.book.title }}</h4>
                <p class="book-author">{{ item.book.author || '未知作者' }}</p>
                <div class="book-meta">
                  <span :class="['status-badge', item.book.status?.toLowerCase()]">
                    {{ getStatusLabel(item.book.status) }}
                  </span>
                  <span v-if="item.book.progress != null" class="progress-text">
                    {{ item.book.progress }}%
                  </span>
                </div>
                <div class="note-count" v-if="item.book.noteCount > 0">
                  📝 {{ item.book.noteCount }} 条笔记
                </div>
                <div class="wakeup-reason">
                  ⚠️ {{ item.reason }}
                </div>
              </div>
              <div class="action-btn-row">
                <button class="action-btn primary" @click.stop="goToBook(item.book.id)">
                  继续阅读
                </button>
              </div>
            </div>
          </div>
        </div>

        <div class="book-list-section" v-if="activeTab === 'neglectedWantToRead'">
          <div class="section-intro">
            <h3>长期想读未启动</h3>
            <p>这些书加入想读很久了，却一直没开始。挑一本今天就开读吧！</p>
          </div>
          <div class="books-grid">
            <div
              v-for="item in wakeUpData.neglectedWantToRead"
              :key="item.book.id"
              class="wakeup-book-card"
              @click="goToBook(item.book.id)"
            >
              <div class="book-cover">
                <img :src="item.book.coverUrl || defaultCover" :alt="item.book.title" />
                <div class="days-badge want-read">
                  {{ item.daysInactive }}天
                </div>
              </div>
              <div class="book-info">
                <h4 class="book-title">{{ item.book.title }}</h4>
                <p class="book-author">{{ item.book.author || '未知作者' }}</p>
                <div class="book-meta">
                  <span :class="['status-badge', item.book.status?.toLowerCase()]">
                    {{ getStatusLabel(item.book.status) }}
                  </span>
                </div>
                <div v-if="item.book.category" class="book-category">
                  <span class="category-tag">{{ item.book.category }}</span>
                </div>
                <div class="wakeup-reason">
                  ⏰ {{ item.reason }}
                </div>
              </div>
              <div class="action-btn-row">
                <button class="action-btn primary" @click.stop="goToBook(item.book.id)">
                  开始阅读
                </button>
              </div>
            </div>
          </div>
        </div>

        <div class="book-list-section" v-if="activeTab === 'readWithoutSummary'">
          <div class="section-intro">
            <h3>已读但无总结</h3>
            <p>这些书你已经读完了，但还没有写下总结。花点时间整理一下，把书里的知识内化成自己的吧！</p>
            <div class="tip-box">
              💡 <strong>小提示：</strong>笔记标题包含"总结"、"读后感"、"书评"、"感悟"，或打了"总结"标签，即视为总结笔记
            </div>
          </div>
          <div class="books-grid">
            <div
              v-for="item in wakeUpData.readWithoutSummary"
              :key="item.book.id"
              class="wakeup-book-card"
              @click="goToBook(item.book.id)"
            >
              <div class="book-cover">
                <img :src="item.book.coverUrl || defaultCover" :alt="item.book.title" />
                <div class="days-badge read-summary">
                  {{ item.daysInactive }}天
                </div>
              </div>
              <div class="book-info">
                <h4 class="book-title">{{ item.book.title }}</h4>
                <p class="book-author">{{ item.book.author || '未知作者' }}</p>
                <div class="book-meta">
                  <span :class="['status-badge', item.book.status?.toLowerCase()]">
                    {{ getStatusLabel(item.book.status) }}
                  </span>
                  <span v-if="item.book.noteCount > 0" class="note-count-inline">
                    📝 {{ item.book.noteCount }} 条笔记
                  </span>
                </div>
                <div class="wakeup-reason">
                  📌 {{ item.reason }}
                </div>
              </div>
              <div class="action-btn-row">
                <button class="action-btn primary" @click.stop="goToWriteSummary(item.book.id)">
                  写总结
                </button>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { activityApi } from '../api'

const router = useRouter()
const wakeUpData = ref(null)
const loading = ref(false)
const selectedDays = ref(30)
const activeTab = ref('stagnantReading')

const defaultCover = 'https://via.placeholder.com/150x200?text=No+Cover'

const thresholdOptions = [
  { label: '7天', value: 7 },
  { label: '15天', value: 15 },
  { label: '30天', value: 30 },
  { label: '60天', value: 60 },
  { label: '90天', value: 90 }
]

const getStatusLabel = (status) => {
  const labels = {
    READING: '在读',
    READ: '已读',
    WANT_TO_READ: '想读'
  }
  return labels[status] || status
}

const loadWakeUpList = async () => {
  loading.value = true
  try {
    const res = await activityApi.getWakeUpList(selectedDays.value)
    wakeUpData.value = res.data
  } catch (e) {
    console.error('加载唤醒清单失败:', e)
  } finally {
    loading.value = false
  }
}

const changeThreshold = (days) => {
  selectedDays.value = days
  loadWakeUpList()
}

const goToBook = (id) => {
  router.push(`/book/${id}`)
}

const goToWriteSummary = (bookId) => {
  router.push(`/book/${bookId}/note/new`)
}

onMounted(loadWakeUpList)
</script>

<style scoped>
.wakeup-page {
  width: 100%;
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 1.5rem;
}

.page-header h2 {
  font-size: 1.5rem;
  color: #333;
  margin: 0 0 0.5rem 0;
}

.page-subtitle {
  color: #888;
  font-size: 0.9rem;
  margin: 0;
}

.threshold-selector {
  display: flex;
  align-items: center;
  gap: 0.5rem;
}

.threshold-label {
  font-size: 0.85rem;
  color: #666;
}

.threshold-btn {
  padding: 0.4rem 0.9rem;
  border: 1px solid #ddd;
  border-radius: 20px;
  background: white;
  cursor: pointer;
  font-size: 0.85rem;
  transition: all 0.3s;
  color: #666;
}

.threshold-btn.active {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
  border-color: transparent;
}

.stats-cards {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 1rem;
  margin-bottom: 2rem;
}

.stat-card {
  background: white;
  border-radius: 12px;
  padding: 1.5rem;
  display: flex;
  align-items: center;
  gap: 1rem;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.04);
  cursor: pointer;
  transition: all 0.3s;
  border-left: 4px solid transparent;
}

.stat-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 6px 20px rgba(0, 0, 0, 0.08);
}

.stat-card.stagnant-reading {
  border-left-color: #ff9800;
}

.stat-card.neglected-want {
  border-left-color: #9c27b0;
}

.stat-card.read-no-summary {
  border-left-color: #2196f3;
}

.stat-icon {
  font-size: 2.5rem;
}

.stat-content {
  display: flex;
  flex-direction: column;
}

.stat-number {
  font-size: 2rem;
  font-weight: 700;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
  line-height: 1.2;
}

.stat-label {
  font-size: 0.85rem;
  color: #888;
  margin-top: 0.25rem;
}

.loading {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 0.75rem;
  padding: 3rem;
  color: #999;
}

.spinner {
  width: 20px;
  height: 20px;
  border: 3px solid #e0e0e0;
  border-top-color: #667eea;
  border-radius: 50%;
  animation: spin 0.8s linear infinite;
}

@keyframes spin {
  to {
    transform: rotate(360deg);
  }
}

.empty-state {
  text-align: center;
  padding: 4rem 2rem;
  background: white;
  border-radius: 12px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.04);
}

.empty-state .empty-icon {
  font-size: 3.5rem;
  margin-bottom: 1rem;
}

.empty-state p {
  color: #666;
  font-size: 1rem;
  margin: 0.5rem 0;
}

.empty-state .empty-hint {
  color: #bbb;
  font-size: 0.85rem;
}

.tabs-section {
  background: white;
  border-radius: 12px;
  padding: 1.5rem;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.04);
}

.tabs-nav {
  display: flex;
  gap: 0.5rem;
  border-bottom: 2px solid #f0f0f0;
  margin-bottom: 1.5rem;
  padding-bottom: 0;
}

.tab-btn {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  padding: 0.75rem 1.25rem;
  background: none;
  border: none;
  border-bottom: 3px solid transparent;
  cursor: pointer;
  font-size: 0.95rem;
  color: #666;
  margin-bottom: -2px;
  transition: all 0.2s;
}

.tab-btn:hover {
  color: #667eea;
}

.tab-btn.active {
  color: #667eea;
  border-bottom-color: #667eea;
  font-weight: 600;
}

.tab-count {
  background: #f0f0f0;
  color: #888;
  padding: 0.15rem 0.6rem;
  border-radius: 12px;
  font-size: 0.8rem;
}

.tab-btn.active .tab-count {
  background: rgba(102, 126, 234, 0.1);
  color: #667eea;
}

.section-intro {
  margin-bottom: 1.5rem;
  padding: 1rem 1.25rem;
  background: #fafafa;
  border-radius: 8px;
}

.section-intro h3 {
  margin: 0 0 0.5rem 0;
  font-size: 1.1rem;
  color: #333;
}

.section-intro p {
  margin: 0;
  color: #777;
  font-size: 0.9rem;
}

.tip-box {
  margin-top: 0.75rem;
  padding: 0.6rem 1rem;
  background: #fff8e1;
  border-radius: 6px;
  font-size: 0.85rem;
  color: #f57f17;
}

.tip-box strong {
  color: #e65100;
}

.books-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(320px, 1fr));
  gap: 1.25rem;
}

.wakeup-book-card {
  display: flex;
  flex-direction: column;
  border: 1px solid #eee;
  border-radius: 12px;
  overflow: hidden;
  cursor: pointer;
  transition: all 0.3s;
  background: #fff;
}

.wakeup-book-card:hover {
  border-color: #667eea;
  box-shadow: 0 4px 16px rgba(102, 126, 234, 0.15);
  transform: translateY(-2px);
}

.book-cover {
  position: relative;
  height: 140px;
  overflow: hidden;
  background: #f5f5f5;
}

.book-cover img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.days-badge {
  position: absolute;
  top: 0.75rem;
  right: 0.75rem;
  background: rgba(255, 152, 0, 0.95);
  color: white;
  padding: 0.25rem 0.6rem;
  border-radius: 12px;
  font-size: 0.75rem;
  font-weight: 600;
  backdrop-filter: blur(4px);
}

.days-badge.want-read {
  background: rgba(156, 39, 176, 0.95);
}

.days-badge.read-summary {
  background: rgba(33, 150, 243, 0.95);
}

.book-info {
  padding: 1rem;
  flex: 1;
}

.book-title {
  font-size: 1rem;
  font-weight: 600;
  color: #333;
  margin: 0 0 0.4rem 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.book-author {
  font-size: 0.85rem;
  color: #888;
  margin: 0 0 0.75rem 0;
}

.book-meta {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  margin-bottom: 0.5rem;
  flex-wrap: wrap;
}

.status-badge {
  padding: 0.2rem 0.6rem;
  border-radius: 10px;
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
  font-size: 0.8rem;
}

.note-count,
.note-count-inline {
  font-size: 0.8rem;
  color: #667eea;
  margin-bottom: 0.5rem;
}

.note-count-inline {
  margin-bottom: 0;
}

.book-category {
  margin-bottom: 0.5rem;
}

.category-tag {
  display: inline-block;
  padding: 0.15rem 0.5rem;
  background: #f5f0ff;
  color: #764ba2;
  border-radius: 6px;
  font-size: 0.75rem;
}

.wakeup-reason {
  font-size: 0.85rem;
  color: #f57f17;
  padding: 0.4rem 0.6rem;
  background: #fff8e1;
  border-radius: 6px;
  margin-top: 0.5rem;
}

.action-btn-row {
  padding: 0 1rem 1rem 1rem;
}

.action-btn {
  width: 100%;
  padding: 0.65rem;
  border: none;
  border-radius: 8px;
  cursor: pointer;
  font-size: 0.9rem;
  font-weight: 500;
  transition: all 0.2s;
}

.action-btn.primary {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
}

.action-btn.primary:hover {
  opacity: 0.9;
  transform: translateY(-1px);
}

@media (max-width: 768px) {
  .page-header {
    flex-direction: column;
    gap: 1rem;
  }

  .stats-cards {
    grid-template-columns: 1fr;
  }

  .tabs-nav {
    overflow-x: auto;
  }

  .books-grid {
    grid-template-columns: 1fr;
  }
}
</style>
