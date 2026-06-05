
<template>
  <div class="tag-manager">
    <div class="manager-header">
      <div class="back-btn" @click="goBack">← 返回</div>
      <h1>标签管理</h1>
    </div>
    
    <div class="tags-container">
      <div v-if="tags.length > 0" class="tags-grid">
        <div v-for="tag in tags" :key="tag.id" class="tag-card">
          <div class="tag-color" :style="{ backgroundColor: tag.color }"></div>
          <div class="tag-info">
            <h3 class="tag-name">{{ tag.name }}</h3>
            <p class="tag-date">{{ formatDate(tag.createdAt) }}</p>
          </div>
          <div class="tag-actions">
            <button @click="editTag(tag)" class="edit-btn">编辑</button>
            <button @click="deleteTag(tag.id)" class="delete-btn">删除</button>
          </div>
        </div>
      </div>
      
      <div v-else class="empty-state">
        <div class="empty-icon">🏷️</div>
        <p>暂无标签，点击下方按钮创建</p>
      </div>
    </div>
    
    <button @click="showAddModal = true" class="add-tag-btn">+ 添加标签</button>

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
}

.tag-color {
  width: 16px;
  height: 16px;
  border-radius: 50%;
  flex-shrink: 0;
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
  max-width: 400px;
}

.modal-content h2 {
  margin-bottom: 1.5rem;
  color: #333;
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
</style>
