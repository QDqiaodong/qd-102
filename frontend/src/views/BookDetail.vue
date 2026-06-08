
<template>
  <div class="book-detail">
    <div v-if="book" class="detail-content">
      <div class="book-header">
        <div class="back-btn" @click="handleBack">← 返回</div>
        <h1 class="book-title">{{ book.title }}</h1>
      </div>
      
      <div v-if="!viewingNote" class="book-info-section">
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

      <div v-if="viewingNote" class="note-viewer-section">
        <div class="note-viewer-main">
          <div class="toc-sidebar" :class="{ collapsed: !showToc }">
            <div class="toc-header">
              <span>目录</span>
              <button @click="toggleAllFold" class="toc-fold-btn">
                {{ allFolded ? '展开全部' : '折叠全部' }}
              </button>
            </div>
            <div class="toc-content">
              <div v-if="currentTocItems.length === 0" class="toc-empty">
                暂无标题
              </div>
              <div 
                v-for="(item, index) in visibleTocItems" 
                :key="index"
                class="toc-item"
                :class="[`toc-level-${item.level}`, { active: activeTocIndex === item.originalIndex }]"
                :style="{ paddingLeft: (item.level - 1) * 12 + 8 + 'px' }"
                @click="scrollToHeading(item.originalIndex)"
              >
                <span 
                  v-if="hasSubItems(item.originalIndex)" 
                  class="toc-toggle"
                  @click.stop="toggleFold(item.originalIndex)"
                >
                  {{ foldedIndexes.includes(item.originalIndex) ? '▶' : '▼' }}
                </span>
                <span class="toc-text">{{ item.text }}</span>
              </div>
            </div>
          </div>
          
          <div class="note-viewer-content">
            <div class="note-viewer-header">
              <button class="toc-toggle-btn" @click="showToc = !showToc" :class="{ active: showToc }">
                📑
              </button>
              <h2 class="note-title">{{ viewingNote.title }}</h2>
              <button @click="closeNoteViewer" class="close-note-btn">关闭</button>
            </div>
            <div v-if="viewingNote.pageNumber" class="note-page">第 {{ viewingNote.pageNumber }} 页</div>
            <div class="note-tags">
              <span v-for="tag in viewingNote.tags" :key="tag.id" class="note-tag" :style="{ backgroundColor: tag.color || '#e0e0e0' }">
                {{ tag.name }}
              </span>
            </div>
            <div ref="noteContentRef" class="note-full-content" v-html="viewingNote.content"></div>
            <div class="note-viewer-actions">
              <button 
                v-if="isNoteInRepeatList" 
                @click="handleRemoveFromRepeat(viewingNote.id)" 
                class="repeat-btn active"
              >
                🔁 已加入复读
              </button>
              <button 
                v-else 
                @click="handleAddToRepeat(viewingNote.id)" 
                class="repeat-btn"
              >
                ➕ 加入复读清单
              </button>
              <button @click="goToEditNote(viewingNote.id)" class="edit-note-btn">编辑笔记</button>
              <button @click="deleteNote(viewingNote.id)" class="delete-note-btn">删除笔记</button>
            </div>
          </div>
        </div>
      </div>

      <div v-if="!viewingNote" class="reading-plan-section">
        <div class="section-header">
          <h2>📖 阅读计划</h2>
          <div class="section-header-right">
            <button 
              v-if="!readingPlan && !showPlanForm" 
              @click="showPlanForm = true" 
              class="add-note-btn"
            >
              + 创建计划
            </button>
            <button 
              v-if="readingPlan && !showPlanForm" 
              @click="showSegmentForm = true" 
              class="add-note-btn"
            >
              + 添加分段
            </button>
          </div>
        </div>

        <div v-if="!readingPlan && !showPlanForm" class="empty-plan">
          <div class="empty-plan-icon">📋</div>
          <p>还没有阅读计划，创建一个分段计划让阅读更有节奏</p>
          <button @click="showPlanForm = true" class="create-plan-btn">创建阅读计划</button>
        </div>

        <div v-if="showPlanForm" class="plan-form-card">
          <h3>{{ editingPlan ? '编辑计划' : '创建阅读计划' }}</h3>
          <div class="form-row">
            <div class="form-item">
              <label>计划名称</label>
              <input v-model="planForm.title" type="text" placeholder="例如：第一阶段阅读计划" />
            </div>
            <div class="form-item">
              <label>总页数</label>
              <input v-model.number="planForm.totalPages" type="number" min="1" placeholder="书籍总页数" />
            </div>
          </div>
          <div class="form-actions-row">
            <button @click="cancelPlanForm" class="cancel-btn-sm">取消</button>
            <button @click="submitPlanForm" class="submit-btn-sm">{{ editingPlan ? '保存' : '创建' }}</button>
          </div>
          <div class="quick-generate">
            <span class="quick-label">快速生成：</span>
            <div class="quick-options">
              <input v-model.number="quickPages" type="number" min="10" placeholder="每段页数" class="quick-input" />
              <span class="quick-sep">页 / 段</span>
              <button @click="generatePlan" class="quick-btn">自动生成</button>
            </div>
          </div>
        </div>

        <div v-if="readingPlan" class="plan-overview">
          <div class="plan-header-row">
            <div class="plan-title-wrap">
              <h3 class="plan-title">{{ readingPlan.title }}</h3>
              <span class="plan-meta">共 {{ readingPlan.totalSegments }} 段 · {{ readingPlan.totalPages }} 页</span>
            </div>
            <div class="plan-actions">
              <button @click="editPlan" class="icon-btn" title="编辑">✏️</button>
              <button @click="deletePlan" class="icon-btn" title="删除">🗑️</button>
            </div>
          </div>
          <div class="plan-progress-row">
              <div class="plan-progress-info">
                <span class="progress-num">{{ readingPlan.overallProgress || 0 }}%</span>
                <span class="progress-text">整体进度</span>
              </div>
              <div class="plan-progress-bar">
                <div class="plan-progress-fill" :style="{ width: (readingPlan.overallProgress || 0) + '%' }"></div>
              </div>
              <div class="plan-segment-count">
                <span class="count-done">{{ readingPlan.completedSegments || 0 }}</span>
                <span class="count-sep">/</span>
                <span class="count-total">{{ readingPlan.totalSegments || 0 }}</span>
                <span class="count-label">段已完成</span>
              </div>
            </div>
        </div>

        <div v-if="readingPlan && readingPlan.segments && readingPlan.segments.length > 0" class="segments-list">
          <div 
            v-for="(segment, index) in readingPlan.segments" 
            :key="segment.id" 
            class="segment-card"
            :class="[`status-${segment.status?.toLowerCase()}`]"
          >
            <div class="segment-header">
              <div class="segment-num">{{ index + 1 }}</div>
              <div class="segment-info">
                <h4 class="segment-title">{{ segment.segmentTitle || '分段 ' + (index + 1) }}</h4>
                <div class="segment-pages">
                  <span class="page-range">P.{{ segment.startPage }} - P.{{ segment.endPage }}</span>
                  <span class="page-count">（{{ segment.pageCount }} 页）</span>
                </div>
              </div>
              <div class="segment-status" :class="segment.status?.toLowerCase()">
                {{ getSegmentStatusLabel(segment.status) }}
              </div>
            </div>

            <div class="segment-progress">
              <div class="seg-progress-bar">
                <div class="seg-progress-fill" :style="{ width: (segment.progressPercent || 0) + '%' }"></div>
              </div>
              <span class="seg-progress-text">{{ segment.progressPercent || 0 }}%</span>
            </div>

            <div class="segment-dates">
              <div v-if="segment.estimatedCompletionDate" class="date-item">
                <span class="date-label">预计完成：</span>
                <span class="date-value">{{ formatDate(segment.estimatedCompletionDate) }}</span>
              </div>
              <div v-if="segment.actualCompletionDate" class="date-item actual">
                <span class="date-label">实际完成：</span>
                <span class="date-value">{{ formatDate(segment.actualCompletionDate) }}</span>
              </div>
            </div>

            <div v-if="updatingSegmentId === segment.id" class="segment-update-form">
              <div class="update-input-row">
                <label>当前读到第</label>
                <input 
                  v-model.number="updateCurrentPage" 
                  type="number" 
                  :min="segment.startPage" 
                  :max="segment.endPage"
                  class="update-input"
                />
                <label>页</label>
              </div>
              <div class="update-actions">
                <button @click="cancelUpdateSegment" class="cancel-btn-sm">取消</button>
                <button @click="submitUpdateSegment(segment.id)" class="submit-btn-sm">更新</button>
              </div>
            </div>

            <div class="segment-actions">
              <button @click="startUpdateSegment(segment)" class="seg-action-btn">
                📝 更新进度
              </button>
              <button @click="editSegment(segment)" class="seg-action-btn">
                ✏️ 编辑
              </button>
              <button @click="deleteSegment(segment.id)" class="seg-action-btn danger">
                🗑️ 删除
              </button>
            </div>
          </div>
        </div>

        <div v-if="showSegmentForm" class="segment-form-modal">
          <div class="modal-backdrop" @click="showSegmentForm = false"></div>
          <div class="segment-form-card">
            <h3>{{ editingSegment ? '编辑分段' : '添加分段' }}</h3>
            <div class="form-item full">
              <label>分段标题</label>
              <input v-model="segmentForm.segmentTitle" type="text" placeholder="例如：第一章 引言" />
            </div>
            <div class="form-row">
              <div class="form-item">
                <label>起始页</label>
                <input v-model.number="segmentForm.startPage" type="number" min="1" placeholder="起始页码" />
              </div>
              <div class="form-item">
                <label>结束页</label>
                <input v-model.number="segmentForm.endPage" type="number" min="1" placeholder="结束页码" />
              </div>
            </div>
            <div class="form-item full">
              <label>预计完成日期</label>
              <input v-model="segmentForm.estimatedCompletionDate" type="date" />
            </div>
            <div class="form-item full" v-if="editingSegment">
              <label>当前页码</label>
              <input v-model.number="segmentForm.currentPage" type="number" min="0" placeholder="当前读到的页码" />
            </div>
            <div class="form-actions-row">
              <button @click="showSegmentForm = false" class="cancel-btn-sm">取消</button>
              <button @click="submitSegmentForm" class="submit-btn-sm">{{ editingSegment ? '保存' : '添加' }}</button>
            </div>
          </div>
        </div>
      </div>

      <div v-if="!viewingNote" class="notes-section">
        <div class="section-header">
          <h2>读书笔记</h2>
          <div class="section-header-right">
            <div class="view-toggle">
              <button 
                :class="['view-toggle-btn', { active: noteViewMode === 'card' }]" 
                @click="noteViewMode = 'card'"
                title="卡片视图"
              >
                <span class="view-icon">▦</span>
                <span class="view-label">卡片</span>
              </button>
              <button 
                :class="['view-toggle-btn', { active: noteViewMode === 'timeline' }]" 
                @click="noteViewMode = 'timeline'"
                title="时间线视图"
              >
                <span class="view-icon">☰</span>
                <span class="view-label">时间线</span>
              </button>
            </div>
            <button @click="goToNewNote" class="add-note-btn">+ 写笔记</button>
          </div>
        </div>

        <div v-if="notes.length > 0" class="notes-summary">
          已加载 {{ notes.length }} / {{ totalNotes }} 条
        </div>

        <div v-if="showContinueTip && readingProgress" class="continue-tip">
          <div class="continue-tip-content">
            <span class="continue-icon">📖</span>
            <span class="continue-text">上次读到第 {{ readingProgress.notePageIndex + 1 }} 页，共 {{ totalNotes }} 条笔记</span>
          </div>
          <div class="continue-tip-actions">
            <button @click="continueReading" class="continue-btn">继续阅读</button>
            <button @click="dismissContinueTip" class="dismiss-btn">×</button>
          </div>
        </div>

        <transition name="fade" mode="out-in">
          <div v-if="notes.length > 0 && noteViewMode === 'card'" key="card" class="notes-card-grid">
            <div v-for="note in notes" :key="note.id" class="note-card" @click="openNoteViewer(note)">
              <div class="note-card-header">
                <h3 class="note-card-title">{{ note.title }}</h3>
                <span v-if="note.pageNumber" class="note-card-page">P.{{ note.pageNumber }}</span>
              </div>
              <p class="note-card-preview">{{ getPreview(note.content) }}</p>
              <div class="note-card-tags">
                <span v-for="tag in note.tags" :key="tag.id" class="note-tag" :style="{ backgroundColor: tag.color || '#e0e0e0' }">
                  {{ tag.name }}
                </span>
              </div>
              <div class="note-card-footer">
                <span class="note-card-date">{{ formatDate(note.createdAt) }}</span>
                <div class="note-card-actions">
                  <button @click.stop="goToEditNote(note.id)" class="edit-note-btn">编辑</button>
                  <button @click.stop="deleteNote(note.id)" class="delete-note-btn">删除</button>
                </div>
              </div>
            </div>
          </div>

          <div v-if="notes.length > 0 && noteViewMode === 'timeline'" key="timeline" class="notes-timeline">
            <div v-for="(group, dateKey) in timelineGroups" :key="dateKey" class="timeline-group">
              <div class="timeline-date-label">
                <span class="timeline-dot"></span>
                <span class="timeline-date-text">{{ dateKey }}</span>
                <span class="timeline-count">{{ group.length }} 条笔记</span>
              </div>
              <div class="timeline-items">
                <div v-for="note in group" :key="note.id" class="timeline-item" @click="openNoteViewer(note)">
                  <div class="timeline-item-marker"></div>
                  <div class="timeline-item-content">
                    <div class="timeline-item-header">
                      <h3 class="timeline-item-title">{{ note.title }}</h3>
                      <span v-if="note.pageNumber" class="timeline-item-page">第 {{ note.pageNumber }} 页</span>
                    </div>
                    <p class="timeline-item-preview">{{ getPreview(note.content) }}</p>
                    <div class="timeline-item-tags">
                      <span v-for="tag in note.tags" :key="tag.id" class="note-tag" :style="{ backgroundColor: tag.color || '#e0e0e0' }">
                        {{ tag.name }}
                      </span>
                    </div>
                    <div class="timeline-item-actions">
                      <button @click.stop="goToEditNote(note.id)" class="edit-note-btn">编辑</button>
                      <button @click.stop="deleteNote(note.id)" class="delete-note-btn">删除</button>
                    </div>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </transition>

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
import { computed, ref, onMounted, onBeforeUnmount, nextTick } from 'vue'
import { bookApi, noteApi, repeatedNoteApi, readingPlanApi } from '../api'

const book = ref(null)
const notes = ref([])
const currentPage = ref(0)
const totalPages = ref(0)
const totalNotes = ref(0)
const notesLoading = ref(false)
const pageSize = 5
const noteViewMode = ref('card')

const readingProgress = ref(null)
const showContinueTip = ref(false)
const isContinuing = ref(false)
let saveProgressTimer = null

const viewingNote = ref(null)
const isNoteInRepeatList = ref(false)
const noteContentRef = ref(null)
const showToc = ref(true)
const currentTocItems = ref([])
const activeTocIndex = ref(-1)
const foldedIndexes = ref([])
const allFolded = ref(false)
let scrollTimer = null

const readingPlan = ref(null)
const showPlanForm = ref(false)
const showSegmentForm = ref(false)
const editingPlan = ref(false)
const editingSegment = ref(null)
const updatingSegmentId = ref(null)
const updateCurrentPage = ref(0)
const quickPages = ref(50)

const planForm = ref({
  title: '',
  totalPages: null
})

const segmentForm = ref({
  segmentTitle: '',
  startPage: null,
  endPage: null,
  estimatedCompletionDate: '',
  currentPage: null
})

const visibleTocItems = computed(() => {
  const result = []
  let skipUntilLevel = Infinity
  
  currentTocItems.value.forEach((item, index) => {
    if (item.level <= skipUntilLevel) {
      skipUntilLevel = Infinity
    }
    
    if (skipUntilLevel === Infinity) {
      result.push({ ...item, originalIndex: index })
      
      if (foldedIndexes.value.includes(index) && hasSubItems(index)) {
        skipUntilLevel = item.level
      }
    }
  })
  
  return result
})

const extractToc = (content) => {
  const items = []
  const tempDiv = document.createElement('div')
  tempDiv.innerHTML = content
  
  const headers = tempDiv.querySelectorAll('h1, h2, h3, h4, h5, h6')
  headers.forEach(header => {
    const level = parseInt(header.tagName.substring(1))
    items.push({
      level: level,
      text: header.textContent.trim()
    })
  })
  
  currentTocItems.value = items
}

const scrollToHeading = (index) => {
  const item = currentTocItems.value[index]
  if (!item || !noteContentRef.value) return
  
  const headers = noteContentRef.value.querySelectorAll('h1, h2, h3, h4, h5, h6')
  let headerMatchIndex = 0
  
  for (let i = 0; i < headers.length; i++) {
    const header = headers[i]
    const headerLevel = parseInt(header.tagName.substring(1))
    if (headerLevel === item.level) {
      if (header.textContent.trim() === item.text && headerMatchIndex === getHeaderMatchIndex(index)) {
        header.scrollIntoView({ behavior: 'smooth', block: 'start' })
        header.style.transition = 'background-color 0.3s'
        header.style.backgroundColor = '#fff3cd'
        setTimeout(() => {
          header.style.backgroundColor = ''
        }, 1500)
        break
      }
      headerMatchIndex++
    }
  }
  
  activeTocIndex.value = index
}

const getHeaderMatchIndex = (tocIndex) => {
  const targetItem = currentTocItems.value[tocIndex]
  let matchIndex = 0
  
  for (let i = 0; i < tocIndex; i++) {
    if (currentTocItems.value[i].level === targetItem.level && currentTocItems.value[i].text === targetItem.text) {
      matchIndex++
    }
  }
  
  return matchIndex
}

const hasSubItems = (index) => {
  const currentItem = currentTocItems.value[index]
  if (!currentItem) return false
  
  for (let i = index + 1; i < currentTocItems.value.length; i++) {
    if (currentTocItems.value[i].level > currentItem.level) {
      return true
    }
    if (currentTocItems.value[i].level <= currentItem.level) {
      break
    }
  }
  return false
}

const toggleFold = (index) => {
  const idx = foldedIndexes.value.indexOf(index)
  if (idx > -1) {
    foldedIndexes.value.splice(idx, 1)
  } else {
    foldedIndexes.value.push(index)
  }
  updateAllFolded()
}

const toggleAllFold = () => {
  if (allFolded.value) {
    foldedIndexes.value = []
  } else {
    foldedIndexes.value = currentTocItems.value.map((_, i) => i).filter(i => hasSubItems(i))
  }
  allFolded.value = !allFolded.value
}

const updateAllFolded = () => {
  const foldableCount = currentTocItems.value.filter((_, i) => hasSubItems(i)).length
  allFolded.value = foldedIndexes.value.length === foldableCount
}

const updateActiveHeading = () => {
  if (!showToc.value || currentTocItems.value.length === 0 || !noteContentRef.value) return
  
  const headers = noteContentRef.value.querySelectorAll('h1, h2, h3, h4, h5, h6')
  const scrollTop = noteContentRef.value.scrollTop
  
  let activeIndex = -1
  let minDistance = Infinity
  
  currentTocItems.value.forEach((item, index) => {
    let headerMatchIndex = 0
    for (let i = 0; i < headers.length; i++) {
      const header = headers[i]
      const headerLevel = parseInt(header.tagName.substring(1))
      if (headerLevel === item.level) {
        if (header.textContent.trim() === item.text && headerMatchIndex === getHeaderMatchIndex(index)) {
          const distance = Math.abs(header.offsetTop - scrollTop - 50)
          if (distance < minDistance) {
            minDistance = distance
            activeIndex = index
          }
          break
        }
        headerMatchIndex++
      }
    }
  })
  
  if (activeIndex !== activeTocIndex.value) {
    activeTocIndex.value = activeIndex
  }
}

const openNoteViewer = async (note) => {
  viewingNote.value = note
  extractToc(note.content)
  activeTocIndex.value = -1
  foldedIndexes.value = []
  allFolded.value = false
  
  try {
    await repeatedNoteApi.getByNoteId(note.id)
    isNoteInRepeatList.value = true
  } catch (error) {
    isNoteInRepeatList.value = false
  }
  
  nextTick(() => {
    if (noteContentRef.value) {
      noteContentRef.value.addEventListener('scroll', handleNoteScroll)
    }
  })
}

const closeNoteViewer = () => {
  viewingNote.value = null
  currentTocItems.value = []
  if (noteContentRef.value) {
    noteContentRef.value.removeEventListener('scroll', handleNoteScroll)
  }
}

const handleNoteScroll = () => {
  if (scrollTimer) clearTimeout(scrollTimer)
  scrollTimer = setTimeout(updateActiveHeading, 100)
}

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

const getSegmentStatusLabel = (status) => {
  const labels = {
    NOT_STARTED: '未开始',
    IN_PROGRESS: '进行中',
    COMPLETED: '已完成'
  }
  return labels[status] || status
}

const loadReadingPlan = async () => {
  const id = window.location.pathname.split('/')[2]
  try {
    const response = await readingPlanApi.getLatestPlanByBook(id)
    readingPlan.value = response.data
  } catch (error) {
    if (error.response && error.response.status === 404) {
      readingPlan.value = null
    } else {
      console.error('加载阅读计划失败:', error)
    }
  }
}

const submitPlanForm = async () => {
  const id = window.location.pathname.split('/')[2]
  if (!planForm.value.totalPages || planForm.value.totalPages <= 0) {
    alert('请输入有效的总页数')
    return
  }
  if (!planForm.value.title) {
    planForm.value.title = book.value.title + ' - 阅读计划'
  }
  try {
    if (editingPlan.value && readingPlan.value) {
      await readingPlanApi.updatePlan(readingPlan.value.id, planForm.value)
    } else {
      await readingPlanApi.createPlan(id, planForm.value)
    }
    await loadReadingPlan()
    await loadBook()
    cancelPlanForm()
  } catch (error) {
    console.error('保存计划失败:', error)
    alert('保存失败，请重试')
  }
}

const cancelPlanForm = () => {
  showPlanForm.value = false
  editingPlan.value = false
  planForm.value = {
    title: '',
    totalPages: null
  }
}

const editPlan = () => {
  if (!readingPlan.value) return
  planForm.value = {
    title: readingPlan.value.title,
    totalPages: readingPlan.value.totalPages
  }
  editingPlan.value = true
  showPlanForm.value = true
}

const deletePlan = async () => {
  if (!confirm('确定要删除这个阅读计划吗？所有分段也将被删除。')) return
  try {
    await readingPlanApi.deletePlan(readingPlan.value.id)
    readingPlan.value = null
    await loadBook()
  } catch (error) {
    console.error('删除计划失败:', error)
    alert('删除失败，请重试')
  }
}

const generatePlan = async () => {
  const id = window.location.pathname.split('/')[2]
  if (!quickPages.value || quickPages.value <= 0) {
    alert('请输入有效的每段页数')
    return
  }
  const totalPages = planForm.value.totalPages
  if (!totalPages || totalPages <= 0) {
    alert('请先输入总页数')
    return
  }
  try {
    const response = await readingPlanApi.generatePlan(id, quickPages.value, totalPages)
    readingPlan.value = response.data
    cancelPlanForm()
    await loadBook()
  } catch (error) {
    console.error('生成计划失败:', error)
    alert('生成失败，请重试')
  }
}

const submitSegmentForm = async () => {
  if (!segmentForm.value.startPage || !segmentForm.value.endPage) {
    alert('请填写起始页和结束页')
    return
  }
  if (segmentForm.value.startPage >= segmentForm.value.endPage) {
    alert('结束页必须大于起始页')
    return
  }
  try {
    if (editingSegment.value) {
      await readingPlanApi.updateSegment(editingSegment.value.id, segmentForm.value)
    } else {
      await readingPlanApi.addSegment(readingPlan.value.id, segmentForm.value)
    }
    await loadReadingPlan()
    showSegmentForm.value = false
    editingSegment.value = null
    resetSegmentForm()
  } catch (error) {
    console.error('保存分段失败:', error)
    alert('保存失败，请重试')
  }
}

const resetSegmentForm = () => {
  segmentForm.value = {
    segmentTitle: '',
    startPage: null,
    endPage: null,
    estimatedCompletionDate: '',
    currentPage: null
  }
}

const editSegment = (segment) => {
  editingSegment.value = segment
  segmentForm.value = {
    segmentTitle: segment.segmentTitle || '',
    startPage: segment.startPage,
    endPage: segment.endPage,
    estimatedCompletionDate: segment.estimatedCompletionDate || '',
    currentPage: segment.currentPage
  }
  showSegmentForm.value = true
}

const deleteSegment = async (segmentId) => {
  if (!confirm('确定要删除这个分段吗？')) return
  try {
    await readingPlanApi.deleteSegment(segmentId)
    await loadReadingPlan()
  } catch (error) {
    console.error('删除分段失败:', error)
    alert('删除失败，请重试')
  }
}

const startUpdateSegment = (segment) => {
  updatingSegmentId.value = segment.id
  updateCurrentPage.value = segment.currentPage != null ? segment.currentPage : segment.startPage
}

const cancelUpdateSegment = () => {
  updatingSegmentId.value = null
  updateCurrentPage.value = 0
}

const submitUpdateSegment = async (segmentId) => {
  if (updateCurrentPage.value == null || updateCurrentPage.value < 0) {
    alert('请输入有效的页码')
    return
  }
  try {
    await readingPlanApi.updateSegmentProgress(segmentId, updateCurrentPage.value)
    await loadReadingPlan()
    await loadBook()
    cancelUpdateSegment()
  } catch (error) {
    console.error('更新进度失败:', error)
    alert('更新失败，请重试')
  }
}

const hasMoreNotes = computed(() => currentPage.value + 1 < totalPages.value)

const timelineGroups = computed(() => {
  const groups = {}
  notes.value.forEach(note => {
    const dateKey = formatDate(note.createdAt)
    if (!groups[dateKey]) {
      groups[dateKey] = []
    }
    groups[dateKey].push(note)
  })
  return groups
})

const loadNotes = async (reset = false, targetPage = null) => {
  const id = window.location.pathname.split('/')[2]
  const nextPage = targetPage !== null ? targetPage : (reset ? 0 : currentPage.value + 1)

  notesLoading.value = true
  try {
    const response = await noteApi.getNotesByBook(id, {
      page: nextPage,
      size: pageSize
    })
    const pageData = response.data
    const nextNotes = pageData.content || []

    if (reset || targetPage !== null) {
      notes.value = nextNotes
    } else {
      notes.value = [...notes.value, ...nextNotes]
    }
    currentPage.value = nextPage
    totalPages.value = pageData.totalPages || 0
    totalNotes.value = pageData.totalElements || 0
  } catch (error) {
    console.error('加载笔记失败:', error)
  } finally {
    notesLoading.value = false
  }
}

const loadReadingProgress = async () => {
  const id = window.location.pathname.split('/')[2]
  try {
    const response = await noteApi.getReadingProgress(id)
    readingProgress.value = response.data
    if (readingProgress.value && readingProgress.value.notePageIndex != null && readingProgress.value.notePageIndex > 0) {
      showContinueTip.value = true
    }
  } catch (error) {
    if (error.response && error.response.status === 404) {
      readingProgress.value = null
    } else {
      console.error('加载阅读进度失败:', error)
    }
  }
}

const saveReadingProgress = async () => {
  const id = window.location.pathname.split('/')[2]
  if (!id || notes.value.length === 0) return

  const scrollTop = window.scrollY || window.pageYOffset
  const lastNoteId = notes.value.length > 0 ? notes.value[notes.value.length - 1].id : null

  const progress = {
    lastNoteId: lastNoteId,
    notePageIndex: currentPage.value,
    scrollTop: scrollTop
  }

  try {
    await noteApi.saveReadingProgress(id, progress)
    readingProgress.value = progress
  } catch (error) {
    console.error('保存阅读进度失败:', error)
  }
}

const debounceSaveProgress = () => {
  if (saveProgressTimer) clearTimeout(saveProgressTimer)
  saveProgressTimer = setTimeout(saveReadingProgress, 500)
}

const continueReading = async () => {
  if (!readingProgress.value || readingProgress.value.notePageIndex == null) return

  isContinuing.value = true
  showContinueTip.value = false

  const targetPage = readingProgress.value.notePageIndex
  await loadNotes(true, targetPage)

  nextTick(() => {
    if (readingProgress.value.scrollTop) {
      window.scrollTo({
        top: readingProgress.value.scrollTop,
        behavior: 'smooth'
      })
    }
    isContinuing.value = false
  })
}

const dismissContinueTip = () => {
  showContinueTip.value = false
}

const handlePageScroll = () => {
  if (!viewingNote.value) {
    debounceSaveProgress()
  }
}

const loadBook = async () => {
  const id = window.location.pathname.split('/')[2]
  try {
    const bookRes = await bookApi.getBookById(id)
    book.value = bookRes.data
    await Promise.all([
      loadNotes(true),
      loadReadingPlan(),
      loadReadingProgress()
    ])
  } catch (error) {
    console.error('加载失败:', error)
  }
}

const loadMoreNotes = async () => {
  if (!hasMoreNotes.value || notesLoading.value) return
  await loadNotes(false)
  saveReadingProgress()
}

const goBack = () => {
  window.location.href = '/'
}

const handleBack = () => {
  if (viewingNote.value) {
    closeNoteViewer()
  } else {
    goBack()
  }
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
    if (viewingNote.value && viewingNote.value.id === id) {
      closeNoteViewer()
    }
    currentPage.value = 0
    totalPages.value = 0
    totalNotes.value = 0
    await loadBook()
  } catch (error) {
    console.error('删除失败:', error)
  }
}

const handleAddToRepeat = async (noteId) => {
  try {
    await repeatedNoteApi.addToRepeatList(noteId)
    isNoteInRepeatList.value = true
  } catch (error) {
    console.error('加入复读清单失败:', error)
    alert('加入复读清单失败')
  }
}

const handleRemoveFromRepeat = async (noteId) => {
  if (!confirm('确定要从复读清单中移除这条笔记吗？')) return
  try {
    await repeatedNoteApi.removeFromRepeatList(noteId)
    isNoteInRepeatList.value = false
  } catch (error) {
    console.error('移除复读清单失败:', error)
    alert('移除失败')
  }
}

onMounted(() => {
  loadBook()
  window.addEventListener('scroll', handlePageScroll, { passive: true })
})

onBeforeUnmount(() => {
  if (scrollTimer) clearTimeout(scrollTimer)
  if (saveProgressTimer) clearTimeout(saveProgressTimer)
  if (noteContentRef.value) {
    noteContentRef.value.removeEventListener('scroll', handleNoteScroll)
  }
  window.removeEventListener('scroll', handlePageScroll)
  saveReadingProgress()
})
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

.note-viewer-section {
  margin-bottom: 2rem;
}

.note-viewer-main {
  display: flex;
  gap: 1.5rem;
  align-items: flex-start;
}

.toc-sidebar {
  width: 260px;
  flex-shrink: 0;
  background: white;
  border-radius: 12px;
  box-shadow: 0 2px 10px rgba(0,0,0,0.05);
  overflow: hidden;
  position: sticky;
  top: 20px;
  max-height: calc(100vh - 100px);
  display: flex;
  flex-direction: column;
  transition: width 0.3s, opacity 0.3s;
}

.toc-sidebar.collapsed {
  width: 0;
  opacity: 0;
  overflow: hidden;
}

.toc-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 1rem;
  border-bottom: 1px solid #eee;
  font-weight: 600;
  color: #333;
}

.toc-fold-btn {
  font-size: 0.8rem;
  padding: 0.25rem 0.5rem;
  border: 1px solid #ddd;
  border-radius: 4px;
  background: #fafafa;
  cursor: pointer;
  color: #666;
}

.toc-content {
  flex: 1;
  overflow-y: auto;
  padding: 0.5rem 0;
}

.toc-empty {
  padding: 2rem 1rem;
  text-align: center;
  color: #999;
  font-size: 0.9rem;
}

.toc-item {
  display: flex;
  align-items: center;
  gap: 0.35rem;
  padding: 0.5rem 0.75rem;
  cursor: pointer;
  border-left: 2px solid transparent;
  transition: all 0.2s;
  font-size: 0.9rem;
}

.toc-item:hover {
  background: #f5f5f5;
}

.toc-item.active {
  background: #eef2ff;
  border-left-color: #667eea;
  color: #3f51b5;
  font-weight: 500;
}

.toc-level-1 {
  font-weight: 600;
  font-size: 0.95rem;
}

.toc-level-2 {
  font-size: 0.9rem;
}

.toc-level-3 {
  font-size: 0.85rem;
  color: #666;
}

.toc-toggle {
  font-size: 0.7rem;
  color: #999;
  width: 14px;
  text-align: center;
  flex-shrink: 0;
}

.toc-text {
  flex: 1;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.note-viewer-content {
  flex: 1;
  background: white;
  padding: 2rem;
  border-radius: 12px;
  box-shadow: 0 2px 10px rgba(0,0,0,0.05);
  min-width: 0;
}

.note-viewer-header {
  display: flex;
  align-items: center;
  gap: 1rem;
  margin-bottom: 1rem;
}

.toc-toggle-btn {
  padding: 0.5rem 0.75rem;
  background: white;
  border: 1px solid #ddd;
  border-radius: 8px;
  cursor: pointer;
  font-size: 1.1rem;
  transition: all 0.3s;
  flex-shrink: 0;
}

.toc-toggle-btn.active {
  background: #667eea;
  border-color: #667eea;
  color: white;
}

.note-viewer-header .note-title {
  flex: 1;
  font-size: 1.5rem;
  color: #333;
  margin: 0;
}

.close-note-btn {
  padding: 0.5rem 1rem;
  background: #f0f0f0;
  border: none;
  border-radius: 8px;
  cursor: pointer;
  flex-shrink: 0;
}

.note-viewer-content .note-page {
  color: #999;
  font-size: 0.9rem;
  margin-bottom: 1rem;
}

.note-viewer-content .note-tags {
  display: flex;
  gap: 0.5rem;
  margin-bottom: 1.5rem;
}

.note-viewer-content .note-tag {
  padding: 0.25rem 0.75rem;
  border-radius: 12px;
  font-size: 0.85rem;
  color: white;
}

.note-full-content {
  max-height: 60vh;
  overflow-y: auto;
  padding: 1rem;
  background: #fafafa;
  border-radius: 8px;
  line-height: 1.8;
}

.note-full-content :deep(h1),
.note-full-content :deep(h2),
.note-full-content :deep(h3) {
  scroll-margin-top: 20px;
  margin-top: 1.5rem;
  margin-bottom: 0.75rem;
}

.note-full-content :deep(h1) {
  font-size: 1.5rem;
  color: #333;
}

.note-full-content :deep(h2) {
  font-size: 1.25rem;
  color: #444;
}

.note-full-content :deep(h3) {
  font-size: 1.1rem;
  color: #555;
}

.note-full-content :deep(p) {
  margin-bottom: 1rem;
  color: #444;
}

.note-viewer-actions {
  display: flex;
  gap: 1rem;
  margin-top: 1.5rem;
}

.note-viewer-actions .edit-note-btn,
.note-viewer-actions .delete-note-btn {
  padding: 0.75rem 1.5rem;
  border: none;
  border-radius: 8px;
  cursor: pointer;
  font-size: 1rem;
}

.note-viewer-actions .edit-note-btn {
  background: #667eea;
  color: white;
}

.note-viewer-actions .delete-note-btn {
  background: #ffebee;
  color: #c62828;
}

.note-viewer-actions .repeat-btn {
  padding: 0.75rem 1.5rem;
  border: 1px solid #e0e0e0;
  background: white;
  color: #555;
  border-radius: 8px;
  cursor: pointer;
  font-size: 1rem;
  transition: all 0.2s;
}

.note-viewer-actions .repeat-btn:hover {
  background: #f5f5f5;
}

.note-viewer-actions .repeat-btn.active {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
  border: none;
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

.section-header-right {
  display: flex;
  align-items: center;
  gap: 1rem;
}

.view-toggle {
  display: flex;
  background: #f0f0f0;
  border-radius: 8px;
  padding: 3px;
}

.view-toggle-btn {
  display: flex;
  align-items: center;
  gap: 0.35rem;
  padding: 0.5rem 0.9rem;
  border: none;
  background: transparent;
  border-radius: 6px;
  cursor: pointer;
  color: #666;
  font-size: 0.9rem;
  transition: all 0.2s;
}

.view-toggle-btn:hover {
  color: #333;
}

.view-toggle-btn.active {
  background: white;
  color: #667eea;
  box-shadow: 0 1px 3px rgba(0,0,0,0.1);
}

.view-icon {
  font-size: 1rem;
}

.view-label {
  font-weight: 500;
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

.notes-card-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(280px, 1fr));
  gap: 1.25rem;
}

.note-card {
  background: #fafafa;
  border-radius: 12px;
  padding: 1.25rem;
  cursor: pointer;
  transition: all 0.25s ease;
  border: 1px solid transparent;
  display: flex;
  flex-direction: column;
}

.note-card:hover {
  background: white;
  border-color: #e0e0e0;
  box-shadow: 0 4px 12px rgba(0,0,0,0.08);
  transform: translateY(-2px);
}

.note-card-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 0.75rem;
  margin-bottom: 0.75rem;
}

.note-card-title {
  font-size: 1.05rem;
  font-weight: 600;
  color: #333;
  margin: 0;
  flex: 1;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.note-card-page {
  font-size: 0.8rem;
  color: #999;
  background: #f0f0f0;
  padding: 0.2rem 0.5rem;
  border-radius: 4px;
  flex-shrink: 0;
}

.note-card-preview {
  font-size: 0.9rem;
  line-height: 1.5;
  color: #666;
  margin-bottom: 0.75rem;
  display: -webkit-box;
  -webkit-line-clamp: 3;
  -webkit-box-orient: vertical;
  overflow: hidden;
  flex: 1;
}

.note-card-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 0.4rem;
  margin-bottom: 0.75rem;
}

.note-card-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding-top: 0.75rem;
  border-top: 1px solid #eee;
}

.note-card-date {
  font-size: 0.8rem;
  color: #999;
}

.note-card-actions button {
  padding: 0.3rem 0.6rem;
  border: none;
  border-radius: 6px;
  cursor: pointer;
  font-size: 0.8rem;
}

.notes-timeline {
  position: relative;
}

.timeline-group {
  margin-bottom: 2rem;
}

.timeline-group:last-child {
  margin-bottom: 0;
}

.timeline-date-label {
  display: flex;
  align-items: center;
  gap: 0.75rem;
  margin-bottom: 1rem;
  position: relative;
}

.timeline-dot {
  width: 12px;
  height: 12px;
  border-radius: 50%;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  box-shadow: 0 0 0 4px #eef2ff;
  flex-shrink: 0;
}

.timeline-date-text {
  font-size: 1rem;
  font-weight: 600;
  color: #333;
}

.timeline-count {
  font-size: 0.85rem;
  color: #999;
  background: #f5f5f5;
  padding: 0.2rem 0.6rem;
  border-radius: 12px;
}

.timeline-items {
  position: relative;
  padding-left: 2rem;
  margin-left: 5px;
  border-left: 2px solid #e8e8e8;
  display: flex;
  flex-direction: column;
  gap: 1rem;
}

.timeline-item {
  position: relative;
  display: flex;
  gap: 1rem;
}

.timeline-item-marker {
  position: absolute;
  left: -2rem;
  top: 1.25rem;
  width: 10px;
  height: 10px;
  border-radius: 50%;
  background: white;
  border: 2px solid #667eea;
  transform: translateX(-4px);
}

.timeline-item-content {
  flex: 1;
  background: #fafafa;
  border-radius: 10px;
  padding: 1rem 1.25rem;
  cursor: pointer;
  transition: all 0.2s;
  border: 1px solid transparent;
}

.timeline-item-content:hover {
  background: white;
  border-color: #e0e0e0;
  box-shadow: 0 2px 8px rgba(0,0,0,0.06);
}

.timeline-item-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 1rem;
  margin-bottom: 0.5rem;
}

.timeline-item-title {
  font-size: 1.1rem;
  font-weight: 600;
  color: #333;
  margin: 0;
}

.timeline-item-page {
  font-size: 0.85rem;
  color: #999;
  flex-shrink: 0;
}

.timeline-item-preview {
  font-size: 0.9rem;
  line-height: 1.6;
  color: #666;
  margin-bottom: 0.75rem;
}

.timeline-item-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 0.4rem;
  margin-bottom: 0.75rem;
}

.timeline-item-actions {
  display: flex;
  justify-content: flex-end;
  gap: 0.5rem;
}

.timeline-item-actions button {
  padding: 0.35rem 0.75rem;
  border: none;
  border-radius: 6px;
  cursor: pointer;
  font-size: 0.85rem;
}

.fade-enter-active,
.fade-leave-active {
  transition: opacity 0.25s ease, transform 0.25s ease;
}

.fade-enter-from {
  opacity: 0;
  transform: translateY(8px);
}

.fade-leave-to {
  opacity: 0;
  transform: translateY(-8px);
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

.continue-tip {
  display: flex;
  align-items: center;
  justify-content: space-between;
  background: linear-gradient(135deg, rgba(102, 126, 234, 0.1) 0%, rgba(118, 75, 162, 0.1) 100%);
  border: 1px solid rgba(102, 126, 234, 0.3);
  border-radius: 12px;
  padding: 0.75rem 1rem;
  margin-bottom: 1.25rem;
}

.continue-tip-content {
  display: flex;
  align-items: center;
  gap: 0.5rem;
}

.continue-icon {
  font-size: 1.2rem;
}

.continue-text {
  color: #3f51b5;
  font-size: 0.9rem;
  font-weight: 500;
}

.continue-tip-actions {
  display: flex;
  align-items: center;
  gap: 0.5rem;
}

.continue-btn {
  padding: 0.4rem 1rem;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
  border: none;
  border-radius: 8px;
  cursor: pointer;
  font-size: 0.85rem;
  font-weight: 500;
  transition: transform 0.2s;
}

.continue-btn:hover {
  transform: translateY(-1px);
}

.dismiss-btn {
  width: 28px;
  height: 28px;
  background: transparent;
  border: none;
  cursor: pointer;
  font-size: 1.2rem;
  color: #999;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.2s;
}

.dismiss-btn:hover {
  background: rgba(0, 0, 0, 0.05);
  color: #666;
}

.note-tag {
  padding: 0.25rem 0.75rem;
  border-radius: 12px;
  font-size: 0.85rem;
  color: white;
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

.reading-plan-section {
  background: white;
  padding: 2rem;
  border-radius: 12px;
  box-shadow: 0 2px 10px rgba(0,0,0,0.05);
  margin-bottom: 2rem;
}

.empty-plan {
  text-align: center;
  padding: 3rem 2rem;
  background: linear-gradient(135deg, rgba(102, 126, 234, 0.05) 0%, rgba(118, 75, 162, 0.05) 100%);
  border-radius: 12px;
  border: 2px dashed #ddd;
}

.empty-plan-icon {
  font-size: 3rem;
  margin-bottom: 1rem;
}

.empty-plan p {
  color: #999;
  margin-bottom: 1.5rem;
}

.create-plan-btn {
  padding: 0.75rem 2rem;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
  border: none;
  border-radius: 8px;
  cursor: pointer;
  font-size: 1rem;
  font-weight: 500;
  transition: transform 0.2s;
}

.create-plan-btn:hover {
  transform: translateY(-2px);
}

.plan-form-card {
  background: #f9f9f9;
  padding: 1.5rem;
  border-radius: 10px;
  border: 1px solid #eee;
}

.plan-form-card h3 {
  margin: 0 0 1rem 0;
  color: #333;
  font-size: 1.1rem;
}

.form-row {
  display: flex;
  gap: 1rem;
  margin-bottom: 1rem;
}

.form-item {
  flex: 1;
}

.form-item.full {
  flex: 100%;
  margin-bottom: 1rem;
}

.form-item label {
  display: block;
  margin-bottom: 0.5rem;
  font-size: 0.85rem;
  font-weight: 500;
  color: #666;
}

.form-item input {
  width: 100%;
  padding: 0.6rem 0.75rem;
  border: 1px solid #ddd;
  border-radius: 8px;
  font-size: 0.95rem;
  box-sizing: border-box;
}

.form-item input:focus {
  outline: none;
  border-color: #667eea;
}

.form-actions-row {
  display: flex;
  gap: 0.75rem;
  justify-content: flex-end;
  margin-top: 1rem;
  padding-top: 1rem;
  border-top: 1px solid #eee;
}

.cancel-btn-sm,
.submit-btn-sm {
  padding: 0.5rem 1.25rem;
  border: none;
  border-radius: 6px;
  cursor: pointer;
  font-size: 0.9rem;
}

.cancel-btn-sm {
  background: #f0f0f0;
  color: #666;
}

.submit-btn-sm {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
}

.quick-generate {
  margin-top: 1rem;
  padding-top: 1rem;
  border-top: 1px dashed #ddd;
  display: flex;
  align-items: center;
  gap: 0.75rem;
  flex-wrap: wrap;
}

.quick-label {
  font-size: 0.85rem;
  color: #666;
  font-weight: 500;
}

.quick-options {
  display: flex;
  align-items: center;
  gap: 0.5rem;
}

.quick-input {
  width: 70px;
  padding: 0.4rem 0.6rem;
  border: 1px solid #ddd;
  border-radius: 6px;
  font-size: 0.9rem;
  text-align: center;
}

.quick-sep {
  font-size: 0.85rem;
  color: #999;
}

.quick-btn {
  padding: 0.4rem 1rem;
  background: #eef2ff;
  color: #667eea;
  border: none;
  border-radius: 6px;
  cursor: pointer;
  font-size: 0.85rem;
  font-weight: 500;
}

.quick-btn:hover {
  background: #e0e7ff;
}

.plan-overview {
  background: linear-gradient(135deg, rgba(102, 126, 234, 0.08) 0%, rgba(118, 75, 162, 0.08) 100%);
  padding: 1.5rem;
  border-radius: 10px;
  margin-bottom: 1.5rem;
}

.plan-header-row {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 1rem;
}

.plan-title-wrap {
  flex: 1;
}

.plan-title {
  margin: 0 0 0.5rem 0;
  font-size: 1.2rem;
  color: #333;
}

.plan-meta {
  font-size: 0.9rem;
  color: #888;
}

.plan-actions {
  display: flex;
  gap: 0.5rem;
}

.icon-btn {
  width: 36px;
  height: 36px;
  border: none;
  border-radius: 8px;
  background: white;
  cursor: pointer;
  font-size: 1rem;
  display: flex;
  align-items: center;
  justify-content: center;
  box-shadow: 0 1px 3px rgba(0,0,0,0.1);
  transition: all 0.2s;
}

.icon-btn:hover {
  transform: translateY(-1px);
  box-shadow: 0 2px 6px rgba(0,0,0,0.15);
}

.plan-progress-row {
  display: flex;
  align-items: center;
  gap: 1.5rem;
}

.plan-progress-info {
  display: flex;
  flex-direction: column;
  align-items: center;
  min-width: 80px;
}

.progress-num {
  font-size: 1.8rem;
  font-weight: 700;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
}

.progress-text {
  font-size: 0.8rem;
  color: #888;
}

.plan-progress-bar {
  flex: 1;
  height: 16px;
  background: white;
  border-radius: 8px;
  overflow: hidden;
  box-shadow: inset 0 1px 3px rgba(0,0,0,0.1);
}

.plan-progress-fill {
  height: 100%;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  border-radius: 8px;
  transition: width 0.5s ease;
}

.plan-segment-count {
  display: flex;
  align-items: baseline;
  gap: 0.25rem;
  min-width: 100px;
  justify-content: flex-end;
}

.count-done {
  font-size: 1.4rem;
  font-weight: 700;
  color: #2e7d32;
}

.count-sep {
  color: #ccc;
  font-size: 1.1rem;
}

.count-total {
  font-size: 1.1rem;
  color: #999;
}

.count-label {
  font-size: 0.8rem;
  color: #888;
  margin-left: 0.25rem;
}

.segments-list {
  display: flex;
  flex-direction: column;
  gap: 1rem;
}

.segment-card {
  background: #fafafa;
  border: 1px solid #eee;
  border-radius: 10px;
  padding: 1.25rem;
  transition: all 0.2s;
}

.segment-card:hover {
  background: white;
  box-shadow: 0 2px 8px rgba(0,0,0,0.06);
}

.segment-card.status-completed {
  border-left: 4px solid #4caf50;
}

.segment-card.status-in_progress {
  border-left: 4px solid #ff9800;
}

.segment-card.status-not_started {
  border-left: 4px solid #bdbdbd;
}

.segment-header {
  display: flex;
  align-items: center;
  gap: 1rem;
  margin-bottom: 1rem;
}

.segment-num {
  width: 32px;
  height: 32px;
  border-radius: 50%;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
  display: flex;
  align-items: center;
  justify-content: center;
  font-weight: 600;
  font-size: 0.9rem;
  flex-shrink: 0;
}

.segment-info {
  flex: 1;
  min-width: 0;
}

.segment-title {
  margin: 0 0 0.35rem 0;
  font-size: 1rem;
  color: #333;
}

.segment-pages {
  font-size: 0.85rem;
  color: #888;
}

.page-range {
  margin-right: 0.25rem;
}

.page-count {
  color: #aaa;
}

.segment-status {
  padding: 0.25rem 0.75rem;
  border-radius: 12px;
  font-size: 0.75rem;
  font-weight: 500;
  flex-shrink: 0;
}

.segment-status.not_started {
  background: #f5f5f5;
  color: #757575;
}

.segment-status.in_progress {
  background: #fff3e0;
  color: #e65100;
}

.segment-status.completed {
  background: #e8f5e9;
  color: #2e7d32;
}

.segment-progress {
  display: flex;
  align-items: center;
  gap: 1rem;
  margin-bottom: 1rem;
}

.seg-progress-bar {
  flex: 1;
  height: 8px;
  background: #eee;
  border-radius: 4px;
  overflow: hidden;
}

.seg-progress-fill {
  height: 100%;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  border-radius: 4px;
  transition: width 0.3s;
}

.seg-progress-text {
  min-width: 50px;
  text-align: right;
  font-size: 0.85rem;
  color: #666;
  font-weight: 500;
}

.segment-dates {
  display: flex;
  gap: 1.5rem;
  margin-bottom: 1rem;
  font-size: 0.85rem;
  flex-wrap: wrap;
}

.date-item {
  display: flex;
  align-items: center;
  gap: 0.35rem;
}

.date-label {
  color: #999;
}

.date-value {
  color: #666;
}

.date-item.actual .date-value {
  color: #2e7d32;
  font-weight: 500;
}

.segment-update-form {
  background: #eef2ff;
  padding: 1rem;
  border-radius: 8px;
  margin-bottom: 1rem;
}

.update-input-row {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  margin-bottom: 0.75rem;
}

.update-input-row label {
  font-size: 0.9rem;
  color: #555;
}

.update-input {
  width: 80px;
  padding: 0.4rem 0.6rem;
  border: 1px solid #c7d2fe;
  border-radius: 6px;
  font-size: 0.95rem;
  text-align: center;
  background: white;
}

.update-actions {
  display: flex;
  gap: 0.5rem;
  justify-content: flex-end;
}

.segment-actions {
  display: flex;
  gap: 0.75rem;
  flex-wrap: wrap;
}

.seg-action-btn {
  padding: 0.4rem 0.9rem;
  border: 1px solid #e0e0e0;
  background: white;
  color: #666;
  border-radius: 6px;
  cursor: pointer;
  font-size: 0.85rem;
  transition: all 0.2s;
}

.seg-action-btn:hover {
  background: #f5f5f5;
  border-color: #ddd;
}

.seg-action-btn.danger {
  color: #c62828;
  border-color: #ffcdd2;
}

.seg-action-btn.danger:hover {
  background: #ffebee;
}

.segment-form-modal {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  z-index: 1000;
  display: flex;
  align-items: center;
  justify-content: center;
}

.modal-backdrop {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0,0,0,0.5);
}

.segment-form-card {
  position: relative;
  background: white;
  border-radius: 12px;
  padding: 2rem;
  width: 90%;
  max-width: 450px;
  max-height: 90vh;
  overflow-y: auto;
  box-shadow: 0 10px 40px rgba(0,0,0,0.2);
}

.segment-form-card h3 {
  margin: 0 0 1.5rem 0;
  color: #333;
  font-size: 1.2rem;
}
</style>
