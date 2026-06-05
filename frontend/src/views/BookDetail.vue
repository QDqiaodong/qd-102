
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
              <button @click="goToEditNote(viewingNote.id)" class="edit-note-btn">编辑笔记</button>
              <button @click="deleteNote(viewingNote.id)" class="delete-note-btn">删除笔记</button>
            </div>
          </div>
        </div>
      </div>

      <div v-else class="notes-section">
        <div class="section-header">
          <h2>读书笔记</h2>
          <button @click="goToNewNote" class="add-note-btn">+ 写笔记</button>
        </div>

        <div v-if="notes.length > 0" class="notes-summary">
          已加载 {{ notes.length }} / {{ totalNotes }} 条
        </div>

        <div v-if="notes.length > 0" class="notes-list">
          <div v-for="note in notes" :key="note.id" class="note-item" @click="openNoteViewer(note)">
            <h3 class="note-title">{{ note.title }}</h3>
            <p v-if="note.pageNumber" class="note-page">第 {{ note.pageNumber }} 页</p>
            <p class="note-preview">{{ getPreview(note.content) }}</p>
            <div class="note-tags">
              <span v-for="tag in note.tags" :key="tag.id" class="note-tag" :style="{ backgroundColor: tag.color || '#e0e0e0' }">
                {{ tag.name }}
              </span>
            </div>
            <div class="note-meta">
              <span class="note-date">{{ formatDate(note.createdAt) }}</span>
              <div class="note-actions">
                <button @click.stop="goToEditNote(note.id)" class="edit-note-btn">编辑</button>
                <button @click.stop="deleteNote(note.id)" class="delete-note-btn">删除</button>
              </div>
            </div>
          </div>
        </div>

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
import { bookApi, noteApi } from '../api'

const book = ref(null)
const notes = ref([])
const currentPage = ref(0)
const totalPages = ref(0)
const totalNotes = ref(0)
const notesLoading = ref(false)
const pageSize = 5

const viewingNote = ref(null)
const noteContentRef = ref(null)
const showToc = ref(true)
const currentTocItems = ref([])
const activeTocIndex = ref(-1)
const foldedIndexes = ref([])
const allFolded = ref(false)
let scrollTimer = null

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

const openNoteViewer = (note) => {
  viewingNote.value = note
  extractToc(note.content)
  activeTocIndex.value = -1
  foldedIndexes.value = []
  allFolded.value = false
  
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

const hasMoreNotes = computed(() => currentPage.value + 1 < totalPages.value)

const loadNotes = async (reset = false) => {
  const id = window.location.pathname.split('/')[2]
  const nextPage = reset ? 0 : currentPage.value + 1

  notesLoading.value = true
  try {
    const response = await noteApi.getNotesByBook(id, {
      page: nextPage,
      size: pageSize
    })
    const pageData = response.data
    const nextNotes = pageData.content || []

    notes.value = reset ? nextNotes : [...notes.value, ...nextNotes]
    currentPage.value = nextPage
    totalPages.value = pageData.totalPages || 0
    totalNotes.value = pageData.totalElements || 0
  } catch (error) {
    console.error('加载笔记失败:', error)
  } finally {
    notesLoading.value = false
  }
}

const loadBook = async () => {
  const id = window.location.pathname.split('/')[2]
  try {
    const bookRes = await bookApi.getBookById(id)
    book.value = bookRes.data
    await loadNotes(true)
  } catch (error) {
    console.error('加载失败:', error)
  }
}

const loadMoreNotes = async () => {
  if (!hasMoreNotes.value || notesLoading.value) return
  await loadNotes(false)
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

onMounted(loadBook)

onBeforeUnmount(() => {
  if (scrollTimer) clearTimeout(scrollTimer)
  if (noteContentRef.value) {
    noteContentRef.value.removeEventListener('scroll', handleNoteScroll)
  }
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

.notes-list {
  display: flex;
  flex-direction: column;
  gap: 1.5rem;
}

.note-item {
  padding: 1.5rem;
  background: #fafafa;
  border-radius: 8px;
  border-left: 4px solid #667eea;
  cursor: pointer;
  transition: background 0.2s;
}

.note-item:hover {
  background: #f0f0f0;
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

.note-title {
  font-size: 1.2rem;
  font-weight: 600;
  margin-bottom: 0.5rem;
}

.note-page {
  color: #999;
  font-size: 0.9rem;
  margin-bottom: 0.5rem;
}

.note-preview {
  line-height: 1.5;
  color: #555;
  margin-bottom: 1rem;
}

.note-tags {
  display: flex;
  gap: 0.5rem;
  margin-bottom: 1rem;
}

.note-tag {
  padding: 0.25rem 0.75rem;
  border-radius: 12px;
  font-size: 0.85rem;
  color: white;
}

.note-meta {
  display: flex;
  justify-content: space-between;
  align-items: center;
  color: #999;
  font-size: 0.9rem;
}

.note-actions button {
  padding: 0.35rem 0.75rem;
  border: none;
  border-radius: 6px;
  cursor: pointer;
  font-size: 0.85rem;
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
</style>
