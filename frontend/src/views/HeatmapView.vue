<template>
  <div class="heatmap-page">
    <div class="page-header">
      <h2>阅读活跃度</h2>
      <div class="month-selector">
        <button
          v-for="opt in monthOptions"
          :key="opt.value"
          :class="['month-btn', { active: selectedMonths === opt.value }]"
          @click="changeMonths(opt.value)"
        >
          {{ opt.label }}
        </button>
      </div>
    </div>

    <div class="stats-bar" v-if="heatmapData && heatmapData.summary">
      <div class="stat-item">
        <span class="stat-value">{{ heatmapData.summary.totalNotes || 0 }}</span>
        <span class="stat-label">笔记</span>
      </div>
      <div class="stat-item">
        <span class="stat-value">{{ heatmapData.summary.totalProgress || 0 }}</span>
        <span class="stat-label">进度更新</span>
      </div>
      <div class="stat-item">
        <span class="stat-value">{{ heatmapData.summary.totalCompleted || 0 }}</span>
        <span class="stat-label">读完</span>
      </div>
      <div class="stat-item">
        <span class="stat-value">{{ heatmapData.summary.activeDays || 0 }}</span>
        <span class="stat-label">活跃天数</span>
      </div>
    </div>

    <div class="heatmap-container" v-if="heatmapData && heatmapData.days && heatmapData.days.length">
      <div class="heatmap-scroll">
        <div class="heatmap-wrapper">
          <div class="month-label-row">
            <div class="day-label-spacer"></div>
            <div class="month-labels-row">
              <div
                v-for="m in monthLabels"
                :key="m.key"
                class="month-label"
                :style="{ width: m.width + 'px' }"
              >
                {{ m.label }}
              </div>
            </div>
          </div>
          <div class="heatmap-grid">
            <div class="day-labels">
              <div class="day-label">一</div>
              <div class="day-label"></div>
              <div class="day-label">三</div>
              <div class="day-label"></div>
              <div class="day-label">五</div>
              <div class="day-label"></div>
              <div class="day-label">日</div>
            </div>
            <div class="heatmap-cells">
              <div
                v-for="(week, wi) in weeks"
                :key="wi"
                class="heatmap-week"
              >
                <div
                  v-for="(day, di) in week"
                  :key="di"
                  :class="['heatmap-cell', `level-${day ? day.level : 0}`, { selected: selectedDate === (day && day.date), clickable: day && day.totalActivity > 0 }]"
                  :title="day ? cellTooltip(day) : ''"
                  @click="day && day.totalActivity > 0 && selectDate(day.date)"
                ></div>
              </div>
            </div>
          </div>
        </div>
      </div>
      <div class="heatmap-legend">
        <span class="legend-label">少</span>
        <div class="legend-cell level-0"></div>
        <div class="legend-cell level-1"></div>
        <div class="legend-cell level-2"></div>
        <div class="legend-cell level-3"></div>
        <div class="legend-cell level-4"></div>
        <span class="legend-label">多</span>
      </div>
    </div>

    <div class="loading" v-if="loading">
      <div class="spinner"></div>
      <span>加载中...</span>
    </div>

    <div class="empty-heatmap" v-if="!loading && !heatmapData">
      <div class="empty-icon">📊</div>
      <p>暂无活跃度数据</p>
      <p class="empty-hint">开始记录阅读笔记，您的阅读节奏将在这里可视化呈现</p>
    </div>

    <div class="detail-panel" v-if="dateDetail">
      <div class="detail-header">
        <h3>{{ formatDate(dateDetail.date) }}</h3>
        <button class="close-btn" @click="dateDetail = null">&times;</button>
      </div>
      <div class="detail-stats">
        <span class="detail-stat" v-if="dateDetail.noteCount > 0">
          📝 {{ dateDetail.noteCount }} 条笔记
        </span>
        <span class="detail-stat" v-if="dateDetail.progressCount > 0">
          📊 {{ dateDetail.progressCount }} 次进度更新
        </span>
        <span class="detail-stat" v-if="dateDetail.completedCount > 0">
          ✅ {{ dateDetail.completedCount }} 本书读完
        </span>
        <span class="detail-stat no-activity" v-if="dateDetail.noteCount === 0 && dateDetail.progressCount === 0 && dateDetail.completedCount === 0">
          这天没有阅读活动
        </span>
      </div>

      <div class="detail-section" v-if="dateDetail.books.length > 0">
        <h4>关联书籍</h4>
        <div class="detail-books">
          <div
            v-for="book in dateDetail.books"
            :key="book.id"
            class="detail-book-card"
            @click="goToBook(book.id)"
          >
            <div class="detail-book-cover">
              <img :src="book.coverUrl || 'https://via.placeholder.com/60x80?text=No+Cover'" :alt="book.title" />
            </div>
            <div class="detail-book-info">
              <div class="detail-book-title">{{ book.title }}</div>
              <div class="detail-book-author">{{ book.author }}</div>
              <div class="detail-book-meta">
                <span :class="['status-dot', book.status.toLowerCase()]"></span>
                <span class="status-text">{{ getStatusLabel(book.status) }}</span>
                <span v-if="book.progress != null" class="progress-text">{{ book.progress }}%</span>
              </div>
            </div>
          </div>
        </div>
      </div>

      <div class="detail-section" v-if="dateDetail.notes.length > 0">
        <h4>当天笔记</h4>
        <div class="detail-notes">
          <div
            v-for="note in dateDetail.notes"
            :key="note.id"
            class="detail-note-card"
            @click="goToNote(note.id)"
          >
            <div class="detail-note-title">{{ note.title }}</div>
            <div class="detail-note-book">📖 {{ note.bookTitle }}</div>
            <div class="detail-note-preview">{{ stripHtml(note.content) }}</div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { activityApi } from '../api'

const router = useRouter()
const heatmapData = ref(null)
const dateDetail = ref(null)
const selectedDate = ref(null)
const loading = ref(false)
const selectedMonths = ref(12)

const monthOptions = [
  { label: '近3月', value: 3 },
  { label: '近6月', value: 6 },
  { label: '近12月', value: 12 }
]

const weeks = computed(() => {
  if (!heatmapData.value || !heatmapData.value.days.length) return []
  const days = heatmapData.value.days
  const firstDate = new Date(days[0].date)
  const firstDow = firstDate.getDay()
  const offset = firstDow === 0 ? 6 : firstDow - 1

  const padded = []
  for (let i = 0; i < offset; i++) {
    padded.push(null)
  }
  days.forEach(d => padded.push(d))

  while (padded.length % 7 !== 0) {
    padded.push(null)
  }

  const result = []
  for (let i = 0; i < padded.length; i += 7) {
    result.push(padded.slice(i, i + 7))
  }
  return result
})

const monthLabels = computed(() => {
  if (!weeks.value.length) return []
  const labels = []
  let lastMonth = -1
  let startWeekIdx = 0
  let lastLabel = ''
  const cellSize = 15
  const gap = 3
  weeks.value.forEach((week, i) => {
    const firstDay = week.find(d => d !== null)
    if (firstDay) {
      const month = new Date(firstDay.date).getMonth()
      const label = new Date(firstDay.date).toLocaleDateString('zh-CN', { month: 'short' })
      if (month !== lastMonth) {
        if (lastMonth !== -1) {
          const weekCount = i - startWeekIdx
          labels.push({
            key: `${lastMonth}-${startWeekIdx}`,
            label: lastLabel,
            width: weekCount * (cellSize + gap)
          })
        }
        lastMonth = month
        lastLabel = label
        startWeekIdx = i
      }
    }
  })
  if (lastMonth !== -1) {
    const weekCount = weeks.value.length - startWeekIdx
    labels.push({
      key: `${lastMonth}-${startWeekIdx}`,
      label: lastLabel,
      width: weekCount * (cellSize + gap)
    })
  }
  return labels
})

const cellTooltip = (day) => {
  if (!day || day.totalActivity === 0) return `${day.date}: 无活动`
  const parts = []
  if (day.noteCount > 0) parts.push(`${day.noteCount} 条笔记`)
  if (day.progressCount > 0) parts.push(`${day.progressCount} 次进度更新`)
  if (day.completedCount > 0) parts.push(`${day.completedCount} 本读完`)
  return `${day.date}: ${parts.join('，')}`
}

const formatDate = (dateStr) => {
  const d = new Date(dateStr)
  return d.toLocaleDateString('zh-CN', { year: 'numeric', month: 'long', day: 'numeric', weekday: 'long' })
}

const getStatusLabel = (status) => {
  const labels = { READING: '在读', READ: '已读', WANT_TO_READ: '想读' }
  return labels[status] || status
}

const stripHtml = (html) => {
  if (!html) return ''
  const tmp = document.createElement('div')
  tmp.innerHTML = html
  const text = tmp.textContent || tmp.innerText || ''
  return text.length > 100 ? text.substring(0, 100) + '...' : text
}

const loadHeatmap = async () => {
  loading.value = true
  try {
    const res = await activityApi.getHeatmap(selectedMonths.value)
    heatmapData.value = res.data
  } catch (e) {
    console.error('加载活跃度数据失败:', e)
  } finally {
    loading.value = false
  }
}

const selectDate = async (date) => {
  selectedDate.value = date
  try {
    const res = await activityApi.getDateDetail(date)
    dateDetail.value = res.data
  } catch (e) {
    console.error('加载日期详情失败:', e)
  }
}

const changeMonths = (val) => {
  selectedMonths.value = val
  dateDetail.value = null
  selectedDate.value = null
  loadHeatmap()
}

const goToBook = (id) => {
  router.push(`/book/${id}`)
}

const goToNote = (id) => {
  router.push(`/note/${id}/edit`)
}

onMounted(loadHeatmap)
</script>

<style scoped>
.heatmap-page {
  width: 100%;
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 1.5rem;
}

.page-header h2 {
  font-size: 1.5rem;
  color: #333;
}

.month-selector {
  display: flex;
  gap: 0.5rem;
}

.month-btn {
  padding: 0.4rem 1rem;
  border: 1px solid #ddd;
  border-radius: 20px;
  background: white;
  cursor: pointer;
  font-size: 0.85rem;
  transition: all 0.3s;
}

.month-btn.active {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
  border-color: transparent;
}

.stats-bar {
  display: flex;
  gap: 1.5rem;
  margin-bottom: 1.5rem;
  padding: 1rem 1.5rem;
  background: white;
  border-radius: 12px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.04);
}

.stat-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 0.5rem 1rem;
}

.stat-value {
  font-size: 1.6rem;
  font-weight: 700;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
}

.stat-label {
  font-size: 0.8rem;
  color: #999;
  margin-top: 0.25rem;
}

.heatmap-container {
  background: white;
  border-radius: 12px;
  padding: 1.5rem;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.04);
  margin-bottom: 1.5rem;
}

.heatmap-scroll {
  overflow-x: auto;
  padding-bottom: 0.5rem;
}

.heatmap-wrapper {
  display: inline-block;
  min-width: 100%;
}

.month-label-row {
  display: flex;
  margin-bottom: 0.5rem;
}

.day-label-spacer {
  width: 30px;
  flex-shrink: 0;
}

.month-labels-row {
  display: flex;
}

.month-label {
  font-size: 0.75rem;
  color: #999;
  overflow: hidden;
  white-space: nowrap;
  text-overflow: ellipsis;
}

.heatmap-grid {
  display: flex;
  gap: 0;
}

.day-labels {
  display: grid;
  grid-template-rows: repeat(7, 15px);
  gap: 3px;
  margin-right: 6px;
  flex-shrink: 0;
}

.day-label {
  font-size: 0.7rem;
  color: #aaa;
  line-height: 15px;
  text-align: right;
  width: 24px;
}

.heatmap-cells {
  display: flex;
  gap: 3px;
}

.heatmap-week {
  display: grid;
  grid-template-rows: repeat(7, 15px);
  gap: 3px;
}

.heatmap-cell {
  width: 15px;
  height: 15px;
  border-radius: 3px;
  transition: all 0.15s;
}

.heatmap-cell.level-0 {
  background: #ebedf0;
}

.heatmap-cell.level-1 {
  background: #c6e48b;
}

.heatmap-cell.level-2 {
  background: #7bc96f;
}

.heatmap-cell.level-3 {
  background: #449e48;
}

.heatmap-cell.level-4 {
  background: #196127;
}

.heatmap-cell.clickable {
  cursor: pointer;
}

.heatmap-cell.clickable:hover {
  outline: 2px solid #667eea;
  outline-offset: 1px;
}

.heatmap-cell.selected {
  outline: 2px solid #667eea;
  outline-offset: 1px;
}

.heatmap-legend {
  display: flex;
  align-items: center;
  gap: 4px;
  justify-content: flex-end;
  margin-top: 0.75rem;
}

.legend-label {
  font-size: 0.75rem;
  color: #999;
}

.legend-cell {
  width: 15px;
  height: 15px;
  border-radius: 3px;
}

.legend-cell.level-0 {
  background: #ebedf0;
}

.legend-cell.level-1 {
  background: #c6e48b;
}

.legend-cell.level-2 {
  background: #7bc96f;
}

.legend-cell.level-3 {
  background: #449e48;
}

.legend-cell.level-4 {
  background: #196127;
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

.empty-heatmap {
  text-align: center;
  padding: 4rem 2rem;
  background: white;
  border-radius: 12px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.04);
}

.empty-heatmap .empty-icon {
  font-size: 3rem;
  margin-bottom: 1rem;
}

.empty-heatmap p {
  color: #999;
  font-size: 0.95rem;
}

.empty-heatmap .empty-hint {
  color: #ccc;
  font-size: 0.85rem;
  margin-top: 0.5rem;
}

.detail-panel {
  background: white;
  border-radius: 12px;
  padding: 1.5rem;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.04);
  animation: slideUp 0.3s ease;
}

@keyframes slideUp {
  from {
    opacity: 0;
    transform: translateY(10px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

.detail-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 1rem;
}

.detail-header h3 {
  font-size: 1.2rem;
  color: #333;
}

.close-btn {
  background: none;
  border: none;
  font-size: 1.5rem;
  color: #999;
  cursor: pointer;
  padding: 0 0.5rem;
  line-height: 1;
  transition: color 0.2s;
}

.close-btn:hover {
  color: #333;
}

.detail-stats {
  display: flex;
  gap: 1rem;
  margin-bottom: 1.25rem;
  flex-wrap: wrap;
}

.detail-stat {
  padding: 0.4rem 0.8rem;
  background: #f5f5f5;
  border-radius: 16px;
  font-size: 0.85rem;
  color: #555;
}

.detail-stat.no-activity {
  background: #fafafa;
  color: #bbb;
}

.detail-section {
  margin-top: 1.25rem;
}

.detail-section h4 {
  font-size: 1rem;
  color: #555;
  margin-bottom: 0.75rem;
  padding-bottom: 0.5rem;
  border-bottom: 1px solid #f0f0f0;
}

.detail-books {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(260px, 1fr));
  gap: 0.75rem;
}

.detail-book-card {
  display: flex;
  gap: 0.75rem;
  padding: 0.75rem;
  border: 1px solid #f0f0f0;
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.2s;
}

.detail-book-card:hover {
  border-color: #667eea;
  box-shadow: 0 2px 8px rgba(102, 126, 234, 0.1);
}

.detail-book-cover {
  flex-shrink: 0;
  width: 45px;
  height: 60px;
  border-radius: 4px;
  overflow: hidden;
}

.detail-book-cover img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.detail-book-info {
  flex: 1;
  min-width: 0;
}

.detail-book-title {
  font-weight: 600;
  font-size: 0.9rem;
  color: #333;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.detail-book-author {
  font-size: 0.8rem;
  color: #999;
  margin-top: 0.2rem;
}

.detail-book-meta {
  display: flex;
  align-items: center;
  gap: 0.4rem;
  margin-top: 0.4rem;
  font-size: 0.75rem;
}

.status-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
}

.status-dot.reading {
  background: #4caf50;
}

.status-dot.read {
  background: #2196f3;
}

.status-dot.want_to_read {
  background: #ff9800;
}

.status-text {
  color: #777;
}

.progress-text {
  color: #aaa;
}

.detail-notes {
  display: flex;
  flex-direction: column;
  gap: 0.75rem;
}

.detail-note-card {
  padding: 0.75rem;
  border: 1px solid #f0f0f0;
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.2s;
}

.detail-note-card:hover {
  border-color: #667eea;
  box-shadow: 0 2px 8px rgba(102, 126, 234, 0.1);
}

.detail-note-title {
  font-weight: 600;
  font-size: 0.9rem;
  color: #333;
  margin-bottom: 0.25rem;
}

.detail-note-book {
  font-size: 0.8rem;
  color: #999;
  margin-bottom: 0.4rem;
}

.detail-note-preview {
  font-size: 0.8rem;
  color: #777;
  line-height: 1.5;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}
</style>
