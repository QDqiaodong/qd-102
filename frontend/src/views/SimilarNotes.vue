
<template>
  <div class="similar-notes-page">
    <div class="page-header">
      <div class="header-left">
        <h2 class="page-title">🔍 相似笔记发现中心</h2>
        <p class="page-subtitle">智能扫描历史笔记，识别重复摘抄，精简你的知识库</p>
      </div>
      <div class="stats-overview">
        <div class="stat-card">
          <span class="stat-number">{{ statistics.totalNotes || 0 }}</span>
          <span class="stat-label">总笔记数</span>
        </div>
        <div class="stat-divider"></div>
        <div class="stat-card highlight">
          <span class="stat-number">{{ statistics.similarNoteCount || 0 }}</span>
          <span class="stat-label">疑似重复</span>
        </div>
        <div class="stat-divider"></div>
        <div class="stat-card">
          <span class="stat-number">{{ statistics.similarPairCount || 0 }}</span>
          <span class="stat-label">重复组合</span>
        </div>
        <div class="stat-divider"></div>
        <div class="stat-card">
          <span class="stat-number">{{ duplicateRatioPercent }}%</span>
          <span class="stat-label">重复率</span>
        </div>
      </div>
    </div>

    <div class="filter-bar">
      <div class="filter-group">
        <label class="filter-label">📚 按书籍</label>
        <select v-model="selectedBookId" @change="loadData" class="filter-select">
          <option :value="null">全部书籍（同书内比较）</option>
          <option v-for="book in books" :key="book.id" :value="book.id">{{ book.title }}</option>
        </select>
      </div>

      <div class="filter-group">
        <label class="filter-label">🎯 相似度阈值</label>
        <div class="threshold-control">
          <input 
            type="range" 
            v-model.number="threshold" 
            min="0.3" 
            max="1" 
            step="0.05" 
            @change="loadData"
            class="threshold-slider"
          />
          <span class="threshold-value">{{ Math.round(threshold * 100) }}%</span>
        </div>
      </div>

      <div class="filter-group switch-group">
        <label class="filter-label">🌐 跨书扫描</label>
        <div class="switch-wrapper" @click="toggleCrossBook">
          <div class="switch" :class="{ active: crossBook }">
            <div class="switch-dot"></div>
          </div>
          <span class="switch-label">{{ crossBook ? '已开启' : '已关闭' }}</span>
        </div>
      </div>

      <button @click="refreshScan" class="scan-btn" :disabled="scanning">
        <span v-if="scanning" class="scan-spinner"></span>
        {{ scanning ? '扫描中...' : '重新扫描' }}
      </button>
    </div>

    <div v-if="scanning" class="loading-state">
      <div class="loading-spinner"></div>
      <p>正在智能扫描笔记相似度...</p>
    </div>

    <div v-else-if="similarPairs.length === 0" class="empty-state">
      <div class="empty-icon">✨</div>
      <h3>太棒了！未发现重复笔记</h3>
      <p>当前筛选条件下没有找到相似的笔记，继续保持~</p>
      <p class="empty-tip">💡 提示：可以调低相似度阈值试试</p>
    </div>

    <div v-else class="results-summary">
      <p>
        共发现 <strong>{{ similarPairs.length }}</strong> 组相似笔记
        <span v-if="selectedBookId">（{{ getBookTitle(selectedBookId) }}）</span>
        <span v-else-if="crossBook">（跨书扫描）</span>
        <span v-else>（按书籍分组）</span>
      </p>
    </div>

    <div v-if="similarPairs.length > 0" class="similar-pairs-list">
      <div 
        v-for="(pair, index) in similarPairs" 
        :key="`${pair.note1.id}-${pair.note2.id}`"
        class="similar-pair-card"
        :style="{ animationDelay: `${index * 0.05}s` }"
      >
        <div class="pair-header">
          <div class="similarity-badge" :class="getSimilarityLevel(pair.similarityScore)">
            <span class="similarity-percent">{{ Math.round(pair.similarityScore * 100) }}%</span>
            <span class="similarity-label">相似度</span>
          </div>
          <div class="pair-meta">
            <span class="pair-reason">{{ pair.similarityReason }}</span>
          </div>
        </div>

        <div class="pair-content">
          <div class="note-preview" @click="openNoteDetail(pair.note1)">
            <div class="note-preview-header">
              <span class="note-book">📚 {{ pair.note1.bookTitle || '未知书籍' }}</span>
              <span v-if="pair.note1.pageNumber" class="note-page">P.{{ pair.note1.pageNumber }}</span>
            </div>
            <h4 class="note-title">{{ pair.note1.title || '无标题' }}</h4>
            <p class="note-excerpt">{{ truncateText(pair.note1.content, 100) }}</p>
            <div class="note-tags" v-if="pair.note1.tags && pair.note1.tags.length > 0">
              <span 
                v-for="tag in pair.note1.tags.slice(0, 3)" 
                :key="tag.id"
                class="tag-chip"
                :style="{ backgroundColor: tag.color + '20', color: tag.color, borderColor: tag.color + '40' }"
              >
                #{{ tag.name }}
              </span>
            </div>
          </div>

          <div class="vs-divider">
            <span class="vs-text">VS</span>
          </div>

          <div class="note-preview" @click="openNoteDetail(pair.note2)">
            <div class="note-preview-header">
              <span class="note-book">📚 {{ pair.note2.bookTitle || '未知书籍' }}</span>
              <span v-if="pair.note2.pageNumber" class="note-page">P.{{ pair.note2.pageNumber }}</span>
            </div>
            <h4 class="note-title">{{ pair.note2.title || '无标题' }}</h4>
            <p class="note-excerpt">{{ truncateText(pair.note2.content, 100) }}</p>
            <div class="note-tags" v-if="pair.note2.tags && pair.note2.tags.length > 0">
              <span 
                v-for="tag in pair.note2.tags.slice(0, 3)" 
                :key="tag.id"
                class="tag-chip"
                :style="{ backgroundColor: tag.color + '20', color: tag.color, borderColor: tag.color + '40' }"
              >
                #{{ tag.name }}
              </span>
            </div>
          </div>
        </div>

        <div class="pair-footer">
          <div class="similarity-breakdown">
            <span class="breakdown-item">
              标题相似度：<strong>{{ Math.round(pair.titleSimilarity * 100) }}%</strong>
            </span>
            <span class="breakdown-divider">|</span>
            <span class="breakdown-item">
              内容相似度：<strong>{{ Math.round(pair.contentSimilarity * 100) }}%</strong>
            </span>
          </div>
          <div class="pair-actions">
            <button class="action-btn view-btn" @click.stop="viewBothNotes(pair)">
              查看详情
            </button>
          </div>
        </div>
      </div>
    </div>

    <div v-if="showDetail && selectedNote" class="detail-modal" @click.self="closeDetail">
      <div class="detail-content">
        <button @click="closeDetail" class="close-btn">✕</button>

        <div class="detail-header">
          <h3 class="detail-title">{{ selectedNote.title || '笔记详情' }}</h3>
          <div class="detail-meta">
            <span class="detail-book">📚 {{ selectedNote.bookTitle }}</span>
            <span v-if="selectedNote.pageNumber" class="detail-page">📄 第 {{ selectedNote.pageNumber }} 页</span>
          </div>
        </div>

        <div class="detail-body">
          <p class="detail-text">{{ selectedNote.content }}</p>
        </div>

        <div v-if="selectedNote.tags && selectedNote.tags.length > 0" class="detail-tags">
          <span
            v-for="tag in selectedNote.tags"
            :key="tag.id"
            class="tag-chip large"
            :style="{ backgroundColor: tag.color + '20', color: tag.color, borderColor: tag.color + '40' }"
          >
            #{{ tag.name }}
          </span>
        </div>

        <div class="detail-footer">
          <span class="detail-date">创建于 {{ formatDate(selectedNote.createdAt) }}</span>
          <button @click="goToNote" class="edit-note-btn">编辑笔记 →</button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { similarNoteApi, bookApi } from '../api'

const router = useRouter()

const similarPairs = ref([])
const statistics = ref({})
const books = ref([])
const scanning = ref(false)

const selectedBookId = ref(null)
const threshold = ref(0.6)
const crossBook = ref(false)

const showDetail = ref(false)
const selectedNote = ref(null)

const duplicateRatioPercent = computed(() => {
  return Math.round((statistics.value.duplicateRatio || 0) * 100)
})

const loadBooks = async () => {
  try {
    const response = await bookApi.getAllBooks()
    books.value = response.data.content || response.data || []
  } catch (error) {
    console.error('加载书籍列表失败:', error)
  }
}

const loadStatistics = async () => {
  try {
    const params = {}
    if (selectedBookId.value) params.bookId = selectedBookId.value
    if (crossBook.value) params.crossBook = true
    const response = await similarNoteApi.getSimilarityStatistics(params)
    statistics.value = response.data
  } catch (error) {
    console.error('加载统计数据失败:', error)
  }
}

const loadSimilarPairs = async () => {
  scanning.value = true
  try {
    const params = {
      threshold: threshold.value
    }
    if (selectedBookId.value) params.bookId = selectedBookId.value
    if (crossBook.value) params.crossBook = true
    const response = await similarNoteApi.findSimilarNotes(params)
    similarPairs.value = response.data
  } catch (error) {
    console.error('加载相似笔记失败:', error)
  } finally {
    scanning.value = false
  }
}

const loadData = async () => {
  await Promise.all([
    loadStatistics(),
    loadSimilarPairs()
  ])
}

const toggleCrossBook = () => {
  crossBook.value = !crossBook.value
  loadData()
}

const refreshScan = () => {
  loadData()
}

const getSimilarityLevel = (score) => {
  if (score >= 0.9) return 'level-high'
  if (score >= 0.75) return 'level-medium-high'
  if (score >= 0.6) return 'level-medium'
  return 'level-low'
}

const getBookTitle = (bookId) => {
  const book = books.value.find(b => b.id === bookId)
  return book ? book.title : ''
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

const openNoteDetail = (note) => {
  selectedNote.value = note
  showDetail.value = true
}

const closeDetail = () => {
  showDetail.value = false
  selectedNote.value = null
}

const viewBothNotes = (pair) => {
  openNoteDetail(pair.note1)
}

const goToNote = () => {
  if (selectedNote.value) {
    router.push(`/note/${selectedNote.value.id}/edit`)
  }
}

onMounted(async () => {
  await loadBooks()
  await loadData()
})
</script>

<style scoped>
.similar-notes-page {
  width: 100%;
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-end;
  margin-bottom: 2rem;
  padding-bottom: 1.5rem;
  border-bottom: 1px solid #eee;
}

.page-title {
  font-size: 1.75rem;
  color: #333;
  margin-bottom: 0.5rem;
}

.page-subtitle {
  color: #888;
  font-size: 0.95rem;
}

.stats-overview {
  display: flex;
  align-items: center;
  gap: 1.5rem;
  padding: 1rem 1.5rem;
  background: linear-gradient(135deg, #667eea10 0%, #764ba210 100%);
  border-radius: 12px;
}

.stat-card {
  display: flex;
  flex-direction: column;
  align-items: center;
}

.stat-card.highlight .stat-number {
  background: linear-gradient(135deg, #ff6b6b 0%, #ee5a6f 100%);
  -webkit-background-clip: text;
  background-clip: text;
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
  gap: 1.5rem;
  align-items: flex-end;
  padding: 1.25rem 1.5rem;
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
  min-width: 160px;
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

.threshold-control {
  display: flex;
  align-items: center;
  gap: 0.75rem;
}

.threshold-slider {
  flex: 1;
  height: 6px;
  -webkit-appearance: none;
  background: #e0e0e0;
  border-radius: 3px;
  outline: none;
  cursor: pointer;
}

.threshold-slider::-webkit-slider-thumb {
  -webkit-appearance: none;
  width: 18px;
  height: 18px;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  border-radius: 50%;
  cursor: pointer;
  box-shadow: 0 2px 6px rgba(102, 126, 234, 0.4);
}

.threshold-value {
  font-size: 0.9rem;
  font-weight: 600;
  color: #667eea;
  min-width: 45px;
  text-align: right;
}

.switch-group {
  max-width: 140px;
}

.switch-wrapper {
  display: flex;
  align-items: center;
  gap: 0.75rem;
  cursor: pointer;
  user-select: none;
}

.switch {
  width: 44px;
  height: 24px;
  background: #ddd;
  border-radius: 12px;
  position: relative;
  transition: background 0.3s;
}

.switch.active {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
}

.switch-dot {
  width: 20px;
  height: 20px;
  background: white;
  border-radius: 50%;
  position: absolute;
  top: 2px;
  left: 2px;
  transition: transform 0.3s;
  box-shadow: 0 1px 3px rgba(0,0,0,0.2);
}

.switch.active .switch-dot {
  transform: translateX(20px);
}

.switch-label {
  font-size: 0.9rem;
  color: #666;
}

.scan-btn {
  padding: 0.65rem 1.5rem;
  border: none;
  border-radius: 8px;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
  cursor: pointer;
  font-size: 0.9rem;
  font-weight: 500;
  transition: all 0.3s;
  display: flex;
  align-items: center;
  gap: 0.5rem;
  height: fit-content;
}

.scan-btn:hover:not(:disabled) {
  transform: translateY(-2px);
  box-shadow: 0 6px 16px rgba(102, 126, 234, 0.35);
}

.scan-btn:disabled {
  opacity: 0.7;
  cursor: not-allowed;
}

.scan-spinner {
  display: inline-block;
  width: 16px;
  height: 16px;
  border: 2px solid rgba(255,255,255,0.3);
  border-top-color: white;
  border-radius: 50%;
  animation: spin 0.8s linear infinite;
}

@keyframes spin {
  to { transform: rotate(360deg); }
}

.loading-state,
.empty-state {
  text-align: center;
  padding: 5rem 2rem;
  background: white;
  border-radius: 12px;
  box-shadow: 0 2px 10px rgba(0,0,0,0.04);
}

.loading-spinner {
  width: 50px;
  height: 50px;
  border: 4px solid #f0f0f0;
  border-top-color: #667eea;
  border-radius: 50%;
  margin: 0 auto 1.5rem;
  animation: spin 1s linear infinite;
}

.loading-state p,
.empty-state p {
  color: #999;
  font-size: 1rem;
}

.empty-icon {
  font-size: 4rem;
  margin-bottom: 1rem;
}

.empty-state h3 {
  color: #555;
  margin-bottom: 0.5rem;
  font-size: 1.25rem;
}

.empty-tip {
  margin-top: 1rem;
  font-size: 0.9rem !important;
  color: #aaa !important;
}

.results-summary {
  margin-bottom: 1.5rem;
  color: #666;
  font-size: 0.95rem;
}

.results-summary strong {
  color: #667eea;
  font-size: 1.1rem;
}

.similar-pairs-list {
  display: flex;
  flex-direction: column;
  gap: 1.5rem;
}

.similar-pair-card {
  background: white;
  border-radius: 12px;
  box-shadow: 0 2px 10px rgba(0,0,0,0.05);
  overflow: hidden;
  animation: fadeInUp 0.5s ease both;
  border: 1px solid transparent;
  transition: all 0.3s;
}

.similar-pair-card:hover {
  box-shadow: 0 8px 24px rgba(0,0,0,0.08);
  border-color: #e0e0ff;
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

.pair-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 1rem 1.5rem;
  background: #fafafa;
  border-bottom: 1px solid #f0f0f0;
}

.similarity-badge {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 0.5rem 1rem;
  border-radius: 8px;
  min-width: 80px;
}

.similarity-badge.level-high {
  background: linear-gradient(135deg, #ff6b6b20 0%, #ee5a6f20 100%);
}

.similarity-badge.level-high .similarity-percent {
  color: #ee5a6f;
}

.similarity-badge.level-medium-high {
  background: linear-gradient(135deg, #ffa50220 0%, #ff7f5020 100%);
}

.similarity-badge.level-medium-high .similarity-percent {
  color: #ff7f50;
}

.similarity-badge.level-medium {
  background: linear-gradient(135deg, #ffd93d20 0%, #f9ca2420 100%);
}

.similarity-badge.level-medium .similarity-percent {
  color: #f9ca24;
}

.similarity-badge.level-low {
  background: linear-gradient(135deg, #667eea20 0%, #764ba220 100%);
}

.similarity-badge.level-low .similarity-percent {
  color: #667eea;
}

.similarity-percent {
  font-size: 1.25rem;
  font-weight: 700;
}

.similarity-label {
  font-size: 0.75rem;
  color: #999;
  margin-top: 2px;
}

.pair-meta {
  flex: 1;
  margin-left: 1.5rem;
}

.pair-reason {
  font-size: 0.9rem;
  color: #666;
  padding: 0.35rem 0.75rem;
  background: white;
  border-radius: 6px;
  display: inline-block;
  border: 1px solid #eee;
}

.pair-content {
  display: flex;
  gap: 1rem;
  padding: 1.5rem;
}

.note-preview {
  flex: 1;
  padding: 1rem;
  background: #fafafa;
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.3s;
  border: 2px solid transparent;
}

.note-preview:hover {
  background: #f0f4ff;
  border-color: #667eea;
  transform: translateY(-2px);
}

.note-preview-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 0.75rem;
}

.note-book {
  font-size: 0.8rem;
  color: #667eea;
  font-weight: 500;
  max-width: 70%;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.note-page {
  font-size: 0.75rem;
  color: #999;
  background: white;
  padding: 0.2rem 0.5rem;
  border-radius: 8px;
}

.note-title {
  font-size: 1rem;
  color: #333;
  margin-bottom: 0.5rem;
  font-weight: 600;
}

.note-excerpt {
  color: #666;
  font-size: 0.9rem;
  line-height: 1.6;
  margin-bottom: 0.75rem;
  display: -webkit-box;
  -webkit-line-clamp: 3;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.note-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 0.4rem;
}

.tag-chip {
  font-size: 0.75rem;
  padding: 0.2rem 0.5rem;
  border-radius: 10px;
  border: 1px solid;
  font-weight: 500;
}

.tag-chip.large {
  font-size: 0.85rem;
  padding: 0.4rem 0.8rem;
}

.vs-divider {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 50px;
  flex-shrink: 0;
}

.vs-text {
  font-size: 0.85rem;
  font-weight: 700;
  color: #ccc;
  background: white;
  padding: 0.25rem 0.5rem;
  border-radius: 6px;
  border: 2px dashed #e0e0e0;
}

.pair-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 0.75rem 1.5rem;
  background: #fafafa;
  border-top: 1px solid #f0f0f0;
}

.similarity-breakdown {
  display: flex;
  align-items: center;
  gap: 0.75rem;
  font-size: 0.85rem;
  color: #888;
}

.similarity-breakdown strong {
  color: #555;
}

.breakdown-divider {
  color: #ddd;
}

.pair-actions {
  display: flex;
  gap: 0.75rem;
}

.action-btn {
  padding: 0.45rem 1rem;
  border: none;
  border-radius: 6px;
  font-size: 0.85rem;
  cursor: pointer;
  transition: all 0.3s;
  font-weight: 500;
}

.view-btn {
  background: #f0f4ff;
  color: #667eea;
}

.view-btn:hover {
  background: #e0e8ff;
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

.edit-note-btn {
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

.edit-note-btn:hover {
  transform: translateY(-2px);
}

@media (max-width: 768px) {
  .page-header {
    flex-direction: column;
    align-items: flex-start;
    gap: 1rem;
  }

  .stats-overview {
    width: 100%;
    justify-content: space-around;
    flex-wrap: wrap;
  }

  .filter-bar {
    flex-direction: column;
    align-items: stretch;
  }

  .filter-group {
    min-width: 100%;
  }

  .pair-content {
    flex-direction: column;
  }

  .vs-divider {
    width: 100%;
    height: 30px;
  }

  .pair-header {
    flex-direction: column;
    align-items: flex-start;
    gap: 0.75rem;
  }

  .pair-meta {
    margin-left: 0;
  }

  .pair-footer {
    flex-direction: column;
    gap: 0.75rem;
    align-items: flex-start;
  }
}
</style>
