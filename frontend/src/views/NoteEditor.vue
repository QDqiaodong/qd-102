
<template>
  <div class="note-editor">
    <div class="editor-header">
      <div class="back-btn" @click="handleGoBack">← 返回</div>
      <h1>{{ isEdit ? '编辑笔记' : '新建笔记' }}</h1>
      <div v-if="lastSaved" class="auto-save-status">已保存: {{ lastSaved }}</div>
      <button 
        class="toc-toggle-btn" 
        @click="showToc = !showToc"
        :class="{ active: showToc }"
        title="显示/隐藏目录"
      >
        📑
      </button>
    </div>
    
    <div class="editor-main">
      <div class="toc-sidebar" :class="{ collapsed: !showToc }">
        <div class="toc-header">
          <span>目录</span>
          <button @click="toggleAllFold" class="toc-fold-btn">
            {{ allFolded ? '展开全部' : '折叠全部' }}
          </button>
        </div>
        <div class="toc-content">
          <div v-if="tocItems.length === 0" class="toc-empty">
            暂无标题，请使用 H1、H2 等标题格式
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
      
      <form @submit.prevent="saveNote" class="editor-form">
        <div class="form-group">
          <label>笔记标题 *</label>
          <input v-model="formData.title" type="text" required placeholder="请输入笔记标题" @input="handleDraftChange" />
        </div>
        
        <div class="form-group">
          <label>页码</label>
          <input v-model.number="formData.pageNumber" type="number" min="1" placeholder="请输入页码" @input="handleDraftChange" />
        </div>
        
        <div class="form-group">
          <label>笔记内容</label>
          <div ref="editorRef" class="editor-container"></div>
        </div>
        
        <div class="form-group">
          <label>标签</label>
          <div class="tags-section">
            <div class="selected-tags">
              <span 
                v-for="tag in formData.tags" 
                :key="tag.id || tag.name" 
                class="selected-tag"
              >
                {{ tag.name }}
                <button type="button" @click="removeTag(tag.name)" class="remove-tag-btn">×</button>
              </span>
            </div>
            <div class="tag-selector">
              <select @change="addTag">
                <option value="">选择标签</option>
                <option v-for="tag in allTags" :key="tag.id" :value="tag.id">
                  {{ tag.name }}
                </option>
              </select>
              <button type="button" @click="showNewTagModal = true" class="add-new-tag-btn">+ 新建标签</button>
            </div>
          </div>
        </div>
        
        <div class="form-actions">
          <button type="button" @click="handleGoBack" class="cancel-btn">取消</button>
          <button type="submit" class="submit-btn">保存笔记</button>
        </div>
      </form>
    </div>

    <div v-if="showNewTagModal" class="modal-overlay" @click.self="showNewTagModal = false">
      <div class="modal-content">
        <h2>新建标签</h2>
        <div class="form-group">
          <label>标签名称 *</label>
          <input v-model="newTagName" type="text" placeholder="请输入标签名称" />
        </div>
        <div class="form-group">
          <label>标签颜色</label>
          <input v-model="newTagColor" type="color" />
        </div>
        <div class="form-actions">
          <button type="button" @click="showNewTagModal = false" class="cancel-btn">取消</button>
          <button type="button" @click="createTag" class="submit-btn">创建</button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onBeforeUnmount } from 'vue'
import { noteApi, tagApi } from '../api'
import Quill from 'quill'

const isEdit = ref(false)
const noteId = ref(null)
const bookId = ref(null)
const draftId = ref(null)
const lastSaved = ref('')
const hasUnsavedChanges = ref(false)
let autoSaveTimer = null

const formData = ref({
  title: '',
  content: '',
  pageNumber: null,
  tags: []
})
const allTags = ref([])
const showNewTagModal = ref(false)
const newTagName = ref('')
const newTagColor = ref('#667eea')
let quillInstance = null

const showToc = ref(true)
const tocItems = ref([])
const activeTocIndex = ref(-1)
const foldedIndexes = ref([])
const allFolded = ref(false)

const visibleTocItems = computed(() => {
  const result = []
  let skipUntilLevel = Infinity
  
  tocItems.value.forEach((item, index) => {
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

const extractToc = () => {
  if (!quillInstance) return
  
  const contents = quillInstance.getContents()
  const items = []
  
  contents.ops.forEach((op) => {
    if (op.insert && typeof op.insert === 'string' && op.attributes && op.attributes.header) {
      items.push({
        level: op.attributes.header,
        text: op.insert.trim()
      })
    }
  })
  
  tocItems.value = items
}

const scrollToHeading = (index) => {
  if (!quillInstance) return
  
  const item = tocItems.value[index]
  if (!item) return
  
  const editorContainer = document.querySelector('.editor-container')
  const headers = editorContainer.querySelectorAll('h1, h2, h3, h4, h5, h6')
  
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
  const targetItem = tocItems.value[tocIndex]
  let matchIndex = 0
  
  for (let i = 0; i < tocIndex; i++) {
    if (tocItems.value[i].level === targetItem.level && tocItems.value[i].text === targetItem.text) {
      matchIndex++
    }
  }
  
  return matchIndex
}

const hasSubItems = (index) => {
  const currentItem = tocItems.value[index]
  if (!currentItem) return false
  
  for (let i = index + 1; i < tocItems.value.length; i++) {
    if (tocItems.value[i].level > currentItem.level) {
      return true
    }
    if (tocItems.value[i].level <= currentItem.level) {
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
    foldedIndexes.value = tocItems.value.map((_, i) => i).filter(i => hasSubItems(i))
  }
  allFolded.value = !allFolded.value
}

const updateAllFolded = () => {
  const foldableCount = tocItems.value.filter((_, i) => hasSubItems(i)).length
  allFolded.value = foldedIndexes.value.length === foldableCount
}

const updateActiveHeading = () => {
  if (!showToc.value || tocItems.value.length === 0) return
  
  const editorContainer = document.querySelector('.editor-container')
  if (!editorContainer) return
  
  const headers = editorContainer.querySelectorAll('h1, h2, h3, h4, h5, h6')
  const scrollTop = editorContainer.scrollTop
  
  let activeIndex = -1
  let minDistance = Infinity
  
  tocItems.value.forEach((item, index) => {
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

const loadTags = async () => {
  try {
    const response = await tagApi.getAllTags()
    allTags.value = response.data
  } catch (error) {
    console.error('加载标签失败:', error)
  }
}

const loadNote = async () => {
  const path = window.location.pathname
  if (path.includes('/note/')) {
    isEdit.value = true
    noteId.value = path.split('/')[2]
    try {
      const response = await noteApi.getNoteById(noteId.value)
      const note = response.data
      formData.value = {
        title: note.title,
        content: note.content || '',
        pageNumber: note.pageNumber,
        tags: note.tags || []
      }
      bookId.value = note.bookId
      if (quillInstance) {
        quillInstance.root.innerHTML = formData.value.content
        extractToc()
      }
    } catch (error) {
      console.error('加载笔记失败:', error)
    }
  } else {
    bookId.value = path.split('/')[2]
    await loadDraft()
  }
}

const loadDraft = async () => {
  if (!bookId.value || isEdit.value) return
  
  try {
    const response = await noteApi.getDraft(bookId.value)
    if (response.data && response.data.title) {
      const draft = response.data
      formData.value = {
        title: draft.title || '',
        content: draft.content || '',
        pageNumber: draft.pageNumber,
        tags: draft.tags || []
      }
      draftId.value = draft.draftId
      if (quillInstance) {
        quillInstance.root.innerHTML = formData.value.content
        extractToc()
      }
    }
  } catch (error) {
  }
}

const saveDraft = async () => {
  if (isEdit.value || !bookId.value) return
  if (!formData.value.title && !formData.value.content) return
  
  try {
    const response = await noteApi.saveDraft(bookId.value, {
      draftId: draftId.value,
      title: formData.value.title,
      content: quillInstance ? quillInstance.root.innerHTML : formData.value.content,
      pageNumber: formData.value.pageNumber,
      tags: formData.value.tags
    })
    draftId.value = response.data.draftId
    const savedAt = new Date(response.data.savedAt)
    lastSaved.value = savedAt.toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit' })
    hasUnsavedChanges.value = false
  } catch (error) {
    console.error('保存草稿失败:', error)
  }
}

const handleDraftChange = () => {
  hasUnsavedChanges.value = true
  if (autoSaveTimer) clearTimeout(autoSaveTimer)
  autoSaveTimer = setTimeout(saveDraft, 2000)
}

const setupEditorListeners = () => {
  if (quillInstance) {
    quillInstance.on('text-change', () => {
      handleDraftChange()
      extractToc()
    })
  }
}

const addTag = (event) => {
  const tagId = parseInt(event.target.value)
  if (!tagId) return
  
  const tag = allTags.value.find(t => t.id === tagId)
  if (tag && !formData.value.tags.some(t => t.id === tag.id)) {
    formData.value.tags.push(tag)
  }
  event.target.value = ''
  handleDraftChange()
}

const removeTag = (tagName) => {
  formData.value.tags = formData.value.tags.filter(t => t.name !== tagName)
  handleDraftChange()
}

const createTag = async () => {
  if (!newTagName.value.trim()) return
  
  try {
    const response = await tagApi.createTag({
      name: newTagName.value.trim(),
      color: newTagColor.value
    })
    allTags.value.push(response.data)
    formData.value.tags.push(response.data)
    showNewTagModal.value = false
    newTagName.value = ''
    newTagColor.value = '#667eea'
  } catch (error) {
    console.error('创建标签失败:', error)
    alert('标签名已存在')
  }
}

const saveNote = async () => {
  formData.value.content = quillInstance.root.innerHTML
  
  try {
    if (isEdit.value) {
      await noteApi.updateNote(noteId.value, formData.value)
    } else {
      await noteApi.createNote(bookId.value, formData.value)
      await noteApi.deleteDraft(bookId.value)
    }
    goBack()
  } catch (error) {
    console.error('保存笔记失败:', error)
  }
}

const handleGoBack = () => {
  if (!isEdit.value && hasUnsavedChanges.value) {
    if (confirm('有未保存的内容，是否保存为草稿？')) {
      saveDraft().then(goBack)
      return
    }
  }
  goBack()
}

const goBack = () => {
  if (bookId.value) {
    window.location.href = `/book/${bookId.value}`
  } else {
    window.location.href = '/'
  }
}

onMounted(async () => {
  await loadTags()
  
  quillInstance = new Quill(document.querySelector('.editor-container'), {
    theme: 'snow',
    modules: {
      toolbar: [
        ['bold', 'italic', 'underline', 'strike'],
        ['blockquote', 'code-block'],
        [{ 'header': 1 }, { 'header': 2 }, { 'header': 3 }],
        [{ 'list': 'ordered'}, { 'list': 'bullet' }],
        [{ 'color': [] }, { 'background': [] }],
        ['link', 'image'],
        ['clean']
      ]
    },
    placeholder: '开始记录你的读书笔记...'
  })
  
  await loadNote()
  setupEditorListeners()
  
  let scrollTimer = null
  const editorContainer = document.querySelector('.editor-container')
  if (editorContainer) {
    editorContainer.addEventListener('scroll', () => {
      if (scrollTimer) clearTimeout(scrollTimer)
      scrollTimer = setTimeout(updateActiveHeading, 100)
    })
  }
})

onBeforeUnmount(() => {
  if (autoSaveTimer) clearTimeout(autoSaveTimer)
})
</script>

<style scoped>
.note-editor {
  width: 100%;
}

.editor-header {
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

.editor-header h1 {
  font-size: 1.5rem;
  color: #333;
}

.auto-save-status {
  margin-left: auto;
  color: #52c41a;
  font-size: 0.85rem;
  padding: 0.25rem 0.75rem;
  background: #f6ffed;
  border-radius: 12px;
}

.toc-toggle-btn {
  padding: 0.5rem 0.75rem;
  background: white;
  border: 1px solid #ddd;
  border-radius: 8px;
  cursor: pointer;
  font-size: 1.1rem;
  transition: all 0.3s;
}

.toc-toggle-btn.active {
  background: #667eea;
  border-color: #667eea;
  color: white;
}

.editor-main {
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

.editor-form {
  flex: 1;
  background: white;
  padding: 2rem;
  border-radius: 12px;
  box-shadow: 0 2px 10px rgba(0,0,0,0.05);
  min-width: 0;
}

.form-group {
  margin-bottom: 1.5rem;
}

.form-group label {
  display: block;
  margin-bottom: 0.5rem;
  font-weight: 500;
  color: #555;
}

.form-group input {
  width: 100%;
  padding: 0.75rem;
  border: 1px solid #ddd;
  border-radius: 8px;
  font-size: 1rem;
}

.editor-container {
  min-height: 400px;
  max-height: 60vh;
  overflow-y: auto;
  border: 1px solid #ddd;
  border-radius: 8px;
}

.editor-container :deep(h1),
.editor-container :deep(h2),
.editor-container :deep(h3) {
  scroll-margin-top: 20px;
}

.tags-section {
  margin-top: 0.5rem;
}

.selected-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 0.5rem;
  margin-bottom: 1rem;
}

.selected-tag {
  display: flex;
  align-items: center;
  gap: 0.25rem;
  padding: 0.35rem 0.75rem;
  background: #667eea;
  color: white;
  border-radius: 12px;
  font-size: 0.9rem;
}

.remove-tag-btn {
  background: rgba(255,255,255,0.2);
  border: none;
  color: white;
  width: 1.25rem;
  height: 1.25rem;
  border-radius: 50%;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 1rem;
}

.tag-selector {
  display: flex;
  gap: 0.5rem;
}

.tag-selector select {
  flex: 1;
  padding: 0.75rem;
  border: 1px solid #ddd;
  border-radius: 8px;
  font-size: 1rem;
}

.add-new-tag-btn {
  padding: 0.75rem 1rem;
  background: #f0f0f0;
  border: 1px solid #ddd;
  border-radius: 8px;
  cursor: pointer;
}

.form-actions {
  display: flex;
  gap: 1rem;
  margin-top: 2rem;
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
  max-width: 400px;
}

.modal-content h2 {
  margin-bottom: 1.5rem;
  color: #333;
}

.form-group input[type="color"] {
  width: 60px;
  height: 40px;
  padding: 0;
  border: none;
  cursor: pointer;
}
</style>
