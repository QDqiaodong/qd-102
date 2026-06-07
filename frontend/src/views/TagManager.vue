
<template>
  <div class="tag-manager">
    <div class="manager-header">
      <div class="back-btn" @click="goBack">← 返回</div>
      <h1>标签管理</h1>
      <button v-if="tags.length > 1" @click="toggleSelectMode" class="select-mode-btn">
        {{ selectMode ? '取消选择' : '合并标签' }}
      </button>
    </div>
    
    <div v-if="selectMode && selectedTags.length > 0" class="merge-bar">
      <span>已选择 {{ selectedTags.length }} 个标签</span>
      <button @click="showMergeModal = true" class="merge-btn" :disabled="selectedTags.length < 2">
        合并选中标签
      </button>
    </div>
    
    <div class="tags-container">
      <div v-if="tags.length > 0" class="tags-grid">
        <div v-for="tag in tags" :key="tag.id" 
             class="tag-card"
             :class="{ 'selected': isSelected(tag.id) }"
             @click="handleTagClick(tag)">
          <div v-if="selectMode" class="tag-checkbox">
            <input type="checkbox" :checked="isSelected(tag.id)" @click.stop="toggleSelect(tag.id)" />
          </div>
          <div class="tag-color" :style="{ backgroundColor: tag.color }"></div>
          <div class="tag-info">
            <h3 class="tag-name">{{ tag.name }}</h3>
            <p class="tag-date">{{ formatDate(tag.createdAt) }}</p>
          </div>
          <div v-if="!selectMode" class="tag-actions">
            <button @click.stop="editTag(tag)" class="edit-btn">编辑</button>
            <button @click.stop="deleteTag(tag.id)" class="delete-btn">删除</button>
          </div>
        </div>
      </div>
      
      <div v-else class="empty-state">
        <div class="empty-icon">🏷️</div>
        <p>暂无标签，点击下方按钮创建</p>
      </div>
    </div>
    
    <button v-if="!selectMode" @click="showAddModal = true" class="add-tag-btn">+ 添加标签</button>

    <div v-if="showAddModal" class="modal-overlay" @click.self="closeModal">
      <div class="modal-content">
        <h2>{{ editingTag ? '编辑标签' : '新建标签' }}</h2>
        <form @submit.prevent="saveTag">
          <div class="form-group">
            <label>标签名称 *</label>
            <input v-model="formData.name" type="text" required placeholder="请输入标签名称" />
          </div>
          <div class="form-group">
            <label>标签颜色</label>
            <input v-model="formData.color" type="color" />
          </div>
          <div class="form-actions">
            <button type="button" @click="closeModal" class="cancel-btn">取消</button>
            <button type="submit" class="submit-btn">保存</button>
          </div>
        </form>
      </div>
    </div>

    <div v-if="showMergeModal" class="modal-overlay" @click.self="closeMergeModal">
      <div class="modal-content">
        <h2>合并标签</h2>
        <p class="merge-desc">将以下标签合并到一个主标签，原有笔记关联会保留：</p>
        
        <div class="selected-tags-list">
          <div v-for="tagId in selectedTags" :key="tagId" class="selected-tag-item">
            <div class="tag-color-small" :style="{ backgroundColor: getTagById(tagId)?.color }"></div>
            <span>{{ getTagById(tagId)?.name }}</span>
          </div>
        </div>
        
        <div class="form-group">
          <label>选择主标签（保留） *</label>
          <select v-model="targetTagId" class="target-select">
            <option value="">请选择主标签</option>
            <option v-for="tagId in selectedTags" :key="tagId" :value="tagId">
              {{ getTagById(tagId)?.name }}
            </option>
          </select>
        </div>
        
        <p v-if="targetTagId" class="merge-warning">
          ⚠️ 合并后，其他标签将被删除，关联的笔记会保留主标签。此操作不可撤销。
        </p>
        
        <div class="form-actions">
          <button type="button" @click="closeMergeModal" class="cancel-btn">取消</button>
          <button type="button" @click="confirmMerge" class="submit-btn merge-confirm-btn" 
                  :disabled="!targetTagId || merging">
            {{ merging ? '合并中...' : '确认合并' }}
          </button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { tagApi } from '../api'

const tags = ref([])
const showAddModal = ref(false)
const editingTag = ref(null)
const formData = ref({
  name: '',
  color: '#667eea'
})

const selectMode = ref(false)
const selectedTags = ref([])
const showMergeModal = ref(false)
const targetTagId = ref('')
const merging = ref(false)

const formatDate = (dateStr) => {
  if (!dateStr) return ''
  const date = new Date(dateStr)
  return date.toLocaleDateString('zh-CN')
}

const loadTags = async () => {
  try {
    const response = await tagApi.getAllTags()
    tags.value = response.data
  } catch (error) {
    console.error('加载标签失败:', error)
  }
}

const getTagById = (id) => {
  return tags.value.find(t => t.id === id)
}

const toggleSelectMode = () => {
  selectMode.value = !selectMode.value
  selectedTags.value = []
}

const isSelected = (id) => {
  return selectedTags.value.includes(id)
}

const toggleSelect = (id) => {
  const index = selectedTags.value.indexOf(id)
  if (index > -1) {
    selectedTags.value.splice(index, 1)
  } else {
    selectedTags.value.push(id)
  }
}

const handleTagClick = (tag) => {
  if (selectMode.value) {
    toggleSelect(tag.id)
  }
}

const editTag = (tag) => {
  editingTag.value = tag
  formData.value = {
    name: tag.name,
    color: tag.color || '#667eea'
  }
  showAddModal.value = true
}

const deleteTag = async (id) => {
  if (!confirm('确定删除这个标签吗？')) return
  try {
    await tagApi.deleteTag(id)
    await loadTags()
  } catch (error) {
    console.error('删除标签失败:', error)
    alert('删除标签失败：' + (error.response?.data?.message || error.message))
  }
}

const saveTag = async () => {
  try {
    if (editingTag.value) {
      await tagApi.updateTag(editingTag.value.id, formData.value)
    } else {
      await tagApi.createTag(formData.value)
    }
    closeModal()
    await loadTags()
  } catch (error) {
    console.error('保存标签失败:', error)
    alert('保存标签失败：' + (error.response?.data?.message || error.message))
  }
}

const closeModal = () => {
  showAddModal.value = false
  editingTag.value = null
  formData.value = {
    name: '',
    color: '#667eea'
  }
}

const closeMergeModal = () => {
  showMergeModal.value = false
  targetTagId.value = ''
}

const confirmMerge = async () => {
  if (!targetTagId.value) return
  
  const sourceIds = selectedTags.value.filter(id => id !== Number(targetTagId.value))
  if (sourceIds.length === 0) {
    alert('请至少选择一个要合并的源标签')
    return
  }
  
  if (!confirm(`确定要将 ${sourceIds.length} 个标签合并到「${getTagById(targetTagId.value)?.name}」吗？`)) {
    return
  }
  
  merging.value = true
  try {
    await tagApi.mergeTags(Number(targetTagId.value), sourceIds)
    closeMergeModal()
    selectMode.value = false
    selectedTags.value = []
    await loadTags()
    alert('标签合并成功！')
  } catch (error) {
    console.error('合并标签失败:', error)
    alert('合并标签失败：' + (error.response?.data?.message || error.message))
  } finally {
    merging.value = false
  }
}

const goBack = () => {
  window.location.href = '/'
}

onMounted(loadTags)
</script>

<style scoped>
.tag-manager {
  width: 100%;
}

.manager-header {
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

.manager-header h1 {
  font-size: 1.5rem;
  color: #333;
  flex: 1;
}

.select-mode-btn {
  padding: 0.5rem 1rem;
  background: #f0f0f0;
  border: none;
  border-radius: 8px;
  cursor: pointer;
  font-size: 0.9rem;
  color: #555;
}

.select-mode-btn:hover {
  background: #e0e0e0;
}

.merge-bar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 1rem 1.5rem;
  background: #e3f2fd;
  border-radius: 8px;
  margin-bottom: 1.5rem;
  color: #1565c0;
  font-weight: 500;
}

.merge-btn {
  padding: 0.5rem 1.25rem;
  background: #1976d2;
  color: white;
  border: none;
  border-radius: 6px;
  cursor: pointer;
  font-size: 0.9rem;
}

.merge-btn:disabled {
  background: #90caf9;
  cursor: not-allowed;
}

.tags-container {
  background: white;
  padding: 2rem;
  border-radius: 12px;
  box-shadow: 0 2px 10px rgba(0,0,0,0.05);
  min-height: 300px;
  margin-bottom: 2rem;
}

.tags-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(300px, 1fr));
  gap: 1rem;
}

.tag-card {
  display: flex;
  align-items: center;
  gap: 1rem;
  padding: 1rem;
  background: #fafafa;
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.2s;
  border: 2px solid transparent;
}

.tag-card:hover {
  background: #f0f0f0;
}

.tag-card.selected {
  background: #e3f2fd;
  border-color: #1976d2;
}

.tag-checkbox {
  flex-shrink: 0;
}

.tag-checkbox input {
  width: 18px;
  height: 18px;
  cursor: pointer;
}

.tag-color {
  width: 16px;
  height: 16px;
  border-radius: 50%;
  flex-shrink: 0;
}

.tag-color-small {
  width: 12px;
  height: 12px;
  border-radius: 50%;
  display: inline-block;
}

.tag-info {
  flex: 1;
}

.tag-name {
  font-size: 1rem;
  font-weight: 600;
  margin-bottom: 0.25rem;
}

.tag-date {
  color: #999;
  font-size: 0.85rem;
}

.tag-actions {
  display: flex;
  gap: 0.5rem;
}

.edit-btn, .delete-btn {
  padding: 0.35rem 0.75rem;
  border: none;
  border-radius: 6px;
  cursor: pointer;
  font-size: 0.85rem;
}

.edit-btn {
  background: #f0f0f0;
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
  font-size: 3rem;
  margin-bottom: 1rem;
}

.empty-state p {
  color: #999;
}

.add-tag-btn {
  width: 100%;
  padding: 1rem;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
  border: none;
  border-radius: 8px;
  cursor: pointer;
  font-size: 1rem;
  font-weight: 500;
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
  max-width: 450px;
  max-height: 80vh;
  overflow-y: auto;
}

.modal-content h2 {
  margin-bottom: 1.5rem;
  color: #333;
}

.merge-desc {
  color: #666;
  margin-bottom: 1rem;
  font-size: 0.95rem;
}

.selected-tags-list {
  background: #f5f5f5;
  border-radius: 8px;
  padding: 0.75rem;
  margin-bottom: 1.5rem;
  max-height: 150px;
  overflow-y: auto;
}

.selected-tag-item {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  padding: 0.4rem 0.5rem;
  font-size: 0.9rem;
  color: #444;
}

.target-select {
  width: 100%;
  padding: 0.75rem;
  border: 1px solid #ddd;
  border-radius: 8px;
  font-size: 1rem;
  background: white;
  cursor: pointer;
}

.merge-warning {
  color: #e65100;
  font-size: 0.85rem;
  margin-top: 0.5rem;
  padding: 0.5rem;
  background: #fff8e1;
  border-radius: 6px;
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

.form-group input {
  width: 100%;
  padding: 0.75rem;
  border: 1px solid #ddd;
  border-radius: 8px;
  font-size: 1rem;
  box-sizing: border-box;
}

.form-group input[type="color"] {
  width: 60px;
  height: 40px;
  padding: 0;
  border: none;
  cursor: pointer;
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

.submit-btn:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.merge-confirm-btn {
  background: #d32f2f;
}

.merge-confirm-btn:not(:disabled):hover {
  background: #b71c1c;
}
</style>
