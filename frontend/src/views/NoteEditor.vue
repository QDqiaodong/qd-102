
<template>
  <div class="note-editor">
    <div class="editor-header">
      <div class="back-btn" @click="handleGoBack">← 返回</div>
      <h1>{{ isEdit ? '编辑笔记' : '新建笔记' }}</h1>
      <div v-if="lastSaved" class="auto-save-status">已保存: {{ lastSaved }}</div>
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
import { ref, onMounted, onBeforeUnmount, watch } from 'vue'
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
      }
    } catch (error) {
      console.error('加载笔记失败:', error)
    }
  } else {
    bookId.value = path.split('/')[2]
    // 尝试加载草稿
    await loadDraft()
  }
}

// 加载草稿
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
      }
    }
  } catch (error) {
    // 草稿不存在是正常的
  }
}

// 保存草稿
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

// 监听内容变化，延迟自动保存
const handleDraftChange = () => {
  hasUnsavedChanges.value = true
  if (autoSaveTimer) clearTimeout(autoSaveTimer)
  autoSaveTimer = setTimeout(saveDraft, 2000) // 2秒后自动保存
}

// Quill内容变化监听
const setupEditorListeners = () => {
  if (quillInstance) {
    quillInstance.on('text-change', () => {
      handleDraftChange()
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
      // 删除草稿
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
  await loadNote()
  
  quillInstance = new Quill(document.querySelector('.editor-container'), {
    theme: 'snow',
    modules: {
      toolbar: [
        ['bold', 'italic', 'underline', 'strike'],
        ['blockquote', 'code-block'],
        [{ 'header': 1 }, { 'header': 2 }],
        [{ 'list': 'ordered'}, { 'list': 'bullet' }],
        [{ 'color': [] }, { 'background': [] }],
        ['link', 'image'],
        ['clean']
      ]
    },
    placeholder: '开始记录你的读书笔记...'
  })
  
  if (formData.value.content) {
    quillInstance.root.innerHTML = formData.value.content
  }
  
  setupEditorListeners()
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

.editor-form {
  background: white;
  padding: 2rem;
  border-radius: 12px;
  box-shadow: 0 2px 10px rgba(0,0,0,0.05);
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
  min-height: 300px;
  border: 1px solid #ddd;
  border-radius: 8px;
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
