
<template>
  <div class="tag-graph-view">
    <div class="graph-header">
      <div class="back-btn" @click="goBack">← 返回</div>
      <h1>标签知识图谱</h1>
      <div class="view-toggle">
        <button 
          :class="{ active: viewMode === 'global' }" 
          @click="switchViewMode('global')"
        >
          全局视图
        </button>
        <button 
          :class="{ active: viewMode === 'focus' }" 
          @click="switchViewMode('focus')"
        >
          聚焦视图
        </button>
      </div>
    </div>

    <div class="graph-container">
      <canvas 
        ref="canvasRef" 
        @mousedown="onMouseDown"
        @mousemove="onMouseMove"
        @mouseup="onMouseUp"
        @dblclick="onDoubleClick"
      ></canvas>
      
      <div v-if="loading" class="loading-overlay">
        <div class="loading-spinner"></div>
        <p>加载中...</p>
      </div>

      <div v-if="!loading && nodes.length === 0" class="empty-state">
        <div class="empty-icon">🕸️</div>
        <p>暂无标签关联数据</p>
        <p class="hint">请先创建带有多个标签的笔记</p>
      </div>

      <div v-if="selectedNode" class="node-info-panel">
        <div class="panel-header">
          <span 
            class="tag-color-dot" 
            :style="{ backgroundColor: selectedNode.color }"
          ></span>
          <h3>{{ selectedNode.name }}</h3>
          <button class="close-btn" @click="selectedNode = null">×</button>
        </div>
        <div class="panel-content">
          <p><strong>笔记数量：</strong>{{ selectedNode.noteCount }}</p>
          <p><strong>关联标签：</strong>{{ getNeighborCount(selectedNode.id) }}</p>
          <button class="expand-btn" @click="expandTag(selectedNode.id)">
            展开周边标签
          </button>
        </div>
      </div>

      <div class="graph-legend">
        <div class="legend-item">
          <div class="legend-icon node"></div>
          <span>标签节点</span>
        </div>
        <div class="legend-item">
          <div class="legend-icon edge"></div>
          <span>标签关联</span>
        </div>
        <div class="legend-item">
          <div class="legend-icon highlight"></div>
          <span>选中标签</span>
        </div>
      </div>

      <div class="zoom-controls">
        <button @click="zoomIn">+</button>
        <span>{{ Math.round(scale * 100) }}%</span>
        <button @click="zoomOut">−</button>
        <button @click="resetView">⟲</button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, onUnmounted, nextTick } from 'vue'
import { tagApi } from '../api'

const canvasRef = ref(null)
const loading = ref(false)
const nodes = ref([])
const edges = ref([])
const selectedNode = ref(null)
const viewMode = ref('global')
const scale = ref(1)

let ctx = null
let animationId = null
let draggingNode = null
let mousePos = { x: 0, y: 0 }
let offsetX = 0
let offsetY = 0
let isDragging = false

const forceParams = {
  repulsion: 8000,
  attraction: 0.01,
  damping: 0.9,
  centerForce: 0.02
}

const loadTagGraph = async () => {
  loading.value = true
  try {
    const response = await tagApi.getTagGraph()
    const data = response.data
    initGraph(data.nodes, data.edges)
  } catch (error) {
    console.error('加载标签图谱失败:', error)
  } finally {
    loading.value = false
  }
}

const initGraph = (nodeData, edgeData) => {
  const canvas = canvasRef.value
  const centerX = canvas.width / 2
  const centerY = canvas.height / 2
  
  nodes.value = nodeData.map((node, index) => {
    const angle = (2 * Math.PI * index) / Math.max(nodeData.length, 1)
    const radius = 100 + Math.random() * 100
    return {
      ...node,
      x: centerX + radius * Math.cos(angle),
      y: centerY + radius * Math.sin(angle),
      vx: 0,
      vy: 0,
      radius: Math.min(40, 20 + node.noteCount * 2)
    }
  })
  
  edges.value = edgeData.map(edge => ({
    ...edge,
    sourceNode: null,
    targetNode: null
  }))
  
  edges.value.forEach(edge => {
    edge.sourceNode = nodes.value.find(n => n.id === edge.source)
    edge.targetNode = nodes.value.find(n => n.id === edge.target)
  })
}

const expandTag = async (tagId) => {
  loading.value = true
  try {
    const response = await tagApi.getTagNeighbors(tagId)
    const data = response.data
    
    const existingIds = new Set(nodes.value.map(n => n.id))
    const canvas = canvasRef.value
    const centerX = canvas.width / 2
    const centerY = canvas.height / 2
    
    const centerNode = nodes.value.find(n => n.id === tagId)
    if (centerNode) {
      centerNode.expanded = true
      centerNode.x = centerX
      centerNode.y = centerY
    }
    
    const newNodes = data.nodes
      .filter(n => !existingIds.has(n.id))
      .map((node, index, arr) => {
        const angle = (2 * Math.PI * index) / Math.max(arr.length, 1)
        const radius = 150
        return {
          ...node,
          x: centerX + radius * Math.cos(angle),
          y: centerY + radius * Math.sin(angle),
          vx: 0,
          vy: 0,
          radius: Math.min(40, 20 + node.noteCount * 2)
        }
      })
    
    nodes.value = [...nodes.value, ...newNodes]
    
    const existingEdgeIds = new Set(edges.value.map(e => e.id))
    const newEdges = data.edges
      .filter(e => !existingEdgeIds.has(e.id))
      .map(edge => ({
        ...edge,
        sourceNode: null,
        targetNode: null
      }))
    
    newEdges.forEach(edge => {
      edge.sourceNode = nodes.value.find(n => n.id === edge.source)
      edge.targetNode = nodes.value.find(n => n.id === edge.target)
    })
    
    edges.value = [...edges.value, ...newEdges]
    viewMode.value = 'focus'
  } catch (error) {
    console.error('展开标签失败:', error)
  } finally {
    loading.value = false
  }
}

const getNeighborCount = (nodeId) => {
  const neighbors = new Set()
  edges.value.forEach(edge => {
    if (edge.source === nodeId) neighbors.add(edge.target)
    if (edge.target === nodeId) neighbors.add(edge.source)
  })
  return neighbors.size
}

const applyForces = () => {
  const nodeCount = nodes.value.length
  if (nodeCount < 2) return
  
  const canvas = canvasRef.value
  const centerX = canvas.width / 2
  const centerY = canvas.height / 2
  
  for (let i = 0; i < nodeCount; i++) {
    const nodeA = nodes.value[i]
    if (nodeA === draggingNode) continue
    
    for (let j = i + 1; j < nodeCount; j++) {
      const nodeB = nodes.value[j]
      if (nodeB === draggingNode) continue
      
      const dx = nodeA.x - nodeB.x
      const dy = nodeA.y - nodeB.y
      const dist = Math.sqrt(dx * dx + dy * dy) || 1
      const force = forceParams.repulsion / (dist * dist)
      
      const fx = (dx / dist) * force
      const fy = (dy / dist) * force
      
      nodeA.vx += fx
      nodeA.vy += fy
      nodeB.vx -= fx
      nodeB.vy -= fy
    }
  }
  
  edges.value.forEach(edge => {
    if (!edge.sourceNode || !edge.targetNode) return
    if (edge.sourceNode === draggingNode || edge.targetNode === draggingNode) return
    
    const dx = edge.targetNode.x - edge.sourceNode.x
    const dy = edge.targetNode.y - edge.sourceNode.y
    const dist = Math.sqrt(dx * dx + dy * dy) || 1
    const force = dist * forceParams.attraction * edge.weight
    
    const fx = (dx / dist) * force
    const fy = (dy / dist) * force
    
    edge.sourceNode.vx += fx
    edge.sourceNode.vy += fy
    edge.targetNode.vx -= fx
    edge.targetNode.vy -= fy
  })
  
  nodes.value.forEach(node => {
    if (node === draggingNode) return
    
    const dx = centerX - node.x
    const dy = centerY - node.y
    node.vx += dx * forceParams.centerForce
    node.vy += dy * forceParams.centerForce
    
    node.vx *= forceParams.damping
    node.vy *= forceParams.damping
    
    node.x += node.vx
    node.y += node.vy
    
    const padding = 50
    node.x = Math.max(padding, Math.min(canvas.width - padding, node.x))
    node.y = Math.max(padding, Math.min(canvas.height - padding, node.y))
  })
}

const draw = () => {
  if (!ctx) return
  
  const canvas = canvasRef.value
  ctx.clearRect(0, 0, canvas.width, canvas.height)
  
  ctx.save()
  ctx.translate(offsetX, offsetY)
  ctx.scale(scale.value, scale.value)
  
  edges.value.forEach(edge => {
    if (!edge.sourceNode || !edge.targetNode) return
    
    const maxWeight = Math.max(...edges.value.map(e => e.weight), 1)
    const lineWidth = 1 + (edge.weight / maxWeight) * 3
    
    ctx.beginPath()
    ctx.moveTo(edge.sourceNode.x, edge.sourceNode.y)
    ctx.lineTo(edge.targetNode.x, edge.targetNode.y)
    ctx.strokeStyle = 'rgba(150, 150, 180, 0.4)'
    ctx.lineWidth = lineWidth
    ctx.stroke()
    
    const midX = (edge.sourceNode.x + edge.targetNode.x) / 2
    const midY = (edge.sourceNode.y + edge.targetNode.y) / 2
    ctx.fillStyle = 'rgba(100, 100, 130, 0.8)'
    ctx.font = '10px sans-serif'
    ctx.textAlign = 'center'
    ctx.fillText(edge.weight, midX, midY - 5)
  })
  
  nodes.value.forEach(node => {
    const isSelected = selectedNode.value && selectedNode.value.id === node.id
    
    if (isSelected) {
      ctx.beginPath()
      ctx.arc(node.x, node.y, node.radius + 8, 0, 2 * Math.PI)
      ctx.fillStyle = 'rgba(102, 126, 234, 0.3)'
      ctx.fill()
    }
    
    ctx.beginPath()
    ctx.arc(node.x, node.y, node.radius, 0, 2 * Math.PI)
    ctx.fillStyle = node.color || '#667eea'
    ctx.fill()
    ctx.strokeStyle = isSelected ? '#4c51bf' : 'white'
    ctx.lineWidth = isSelected ? 3 : 2
    ctx.stroke()
    
    ctx.fillStyle = 'white'
    ctx.font = `bold ${Math.min(12, node.radius / 2)}px sans-serif`
    ctx.textAlign = 'center'
    ctx.textBaseline = 'middle'
    
    const maxChars = Math.floor(node.radius / 6)
    const displayName = node.name.length > maxChars 
      ? node.name.substring(0, maxChars) + '...' 
      : node.name
    ctx.fillText(displayName, node.x, node.y)
    
    if (node.noteCount > 0) {
      ctx.fillStyle = 'rgba(0, 0, 0, 0.7)'
      ctx.font = '10px sans-serif'
      ctx.fillText(`${node.noteCount}`, node.x, node.y + node.radius + 12)
    }
  })
  
  ctx.restore()
}

const animate = () => {
  applyForces()
  draw()
  animationId = requestAnimationFrame(animate)
}

const getMousePos = (e) => {
  const canvas = canvasRef.value
  const rect = canvas.getBoundingClientRect()
  return {
    x: (e.clientX - rect.left - offsetX) / scale.value,
    y: (e.clientY - rect.top - offsetY) / scale.value
  }
}

const findNodeAt = (x, y) => {
  for (let i = nodes.value.length - 1; i >= 0; i--) {
    const node = nodes.value[i]
    const dx = x - node.x
    const dy = y - node.y
    if (dx * dx + dy * dy <= node.radius * node.radius) {
      return node
    }
  }
  return null
}

const onMouseDown = (e) => {
  const pos = getMousePos(e)
  const node = findNodeAt(pos.x, pos.y)
  
  if (node) {
    draggingNode = node
    selectedNode.value = node
    isDragging = true
  } else {
    selectedNode.value = null
  }
}

const onMouseMove = (e) => {
  mousePos = getMousePos(e)
  
  if (draggingNode && isDragging) {
    draggingNode.x = mousePos.x
    draggingNode.y = mousePos.y
    draggingNode.vx = 0
    draggingNode.vy = 0
  }
}

const onMouseUp = () => {
  draggingNode = null
  isDragging = false
}

const onDoubleClick = (e) => {
  const pos = getMousePos(e)
  const node = findNodeAt(pos.x, pos.y)
  if (node) {
    expandTag(node.id)
  }
}

const zoomIn = () => {
  scale.value = Math.min(2, scale.value + 0.1)
}

const zoomOut = () => {
  scale.value = Math.max(0.5, scale.value - 0.1)
}

const resetView = () => {
  scale.value = 1
  offsetX = 0
  offsetY = 0
  if (viewMode.value === 'global') {
    loadTagGraph()
  }
}

const switchViewMode = (mode) => {
  viewMode.value = mode
  if (mode === 'global') {
    loadTagGraph()
  }
}

const goBack = () => {
  window.location.href = '/'
}

const initCanvas = () => {
  const canvas = canvasRef.value
  const container = canvas.parentElement
  
  canvas.width = container.clientWidth
  canvas.height = container.clientHeight
  
  ctx = canvas.getContext('2d')
}

const handleResize = () => {
  initCanvas()
}

onMounted(async () => {
  await nextTick()
  initCanvas()
  await loadTagGraph()
  animate()
  
  window.addEventListener('resize', handleResize)
})

onUnmounted(() => {
  if (animationId) {
    cancelAnimationFrame(animationId)
  }
  window.removeEventListener('resize', handleResize)
})
</script>

<style scoped>
.tag-graph-view {
  width: 100%;
  height: 100vh;
  display: flex;
  flex-direction: column;
  background: linear-gradient(135deg, #f5f7fa 0%, #e4e8ec 100%);
}

.graph-header {
  display: flex;
  align-items: center;
  gap: 1rem;
  padding: 1rem 2rem;
  background: white;
  box-shadow: 0 2px 10px rgba(0, 0, 0, 0.05);
  z-index: 10;
}

.back-btn {
  padding: 0.5rem 1rem;
  background: #f0f0f0;
  border-radius: 8px;
  cursor: pointer;
  font-weight: 500;
  transition: all 0.2s;
}

.back-btn:hover {
  background: #e0e0e0;
}

.graph-header h1 {
  font-size: 1.5rem;
  color: #333;
  margin: 0;
  flex: 1;
}

.view-toggle {
  display: flex;
  gap: 0.5rem;
  background: #f0f0f0;
  padding: 4px;
  border-radius: 8px;
}

.view-toggle button {
  padding: 0.5rem 1rem;
  border: none;
  background: transparent;
  border-radius: 6px;
  cursor: pointer;
  font-size: 0.9rem;
  transition: all 0.2s;
}

.view-toggle button.active {
  background: white;
  box-shadow: 0 2px 5px rgba(0, 0, 0, 0.1);
  font-weight: 500;
}

.graph-container {
  flex: 1;
  position: relative;
  overflow: hidden;
}

.graph-container canvas {
  display: block;
  cursor: grab;
}

.graph-container canvas:active {
  cursor: grabbing;
}

.loading-overlay {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(255, 255, 255, 0.8);
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  z-index: 100;
}

.loading-spinner {
  width: 40px;
  height: 40px;
  border: 3px solid #e0e0e0;
  border-top-color: #667eea;
  border-radius: 50%;
  animation: spin 1s linear infinite;
}

@keyframes spin {
  to { transform: rotate(360deg); }
}

.empty-state {
  position: absolute;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  text-align: center;
  color: #999;
}

.empty-icon {
  font-size: 4rem;
  margin-bottom: 1rem;
}

.empty-state .hint {
  font-size: 0.9rem;
  color: #bbb;
}

.node-info-panel {
  position: absolute;
  top: 1rem;
  right: 1rem;
  width: 280px;
  background: white;
  border-radius: 12px;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.1);
  overflow: hidden;
  z-index: 50;
}

.panel-header {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  padding: 1rem;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
}

.tag-color-dot {
  width: 12px;
  height: 12px;
  border-radius: 50%;
  border: 2px solid white;
}

.panel-header h3 {
  flex: 1;
  margin: 0;
  font-size: 1rem;
}

.close-btn {
  background: none;
  border: none;
  color: white;
  font-size: 1.5rem;
  cursor: pointer;
  line-height: 1;
  opacity: 0.8;
}

.close-btn:hover {
  opacity: 1;
}

.panel-content {
  padding: 1rem;
}

.panel-content p {
  margin: 0.5rem 0;
  color: #555;
}

.expand-btn {
  width: 100%;
  padding: 0.75rem;
  margin-top: 1rem;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
  border: none;
  border-radius: 8px;
  cursor: pointer;
  font-size: 0.9rem;
  font-weight: 500;
  transition: transform 0.2s;
}

.expand-btn:hover {
  transform: translateY(-1px);
}

.graph-legend {
  position: absolute;
  bottom: 1rem;
  left: 1rem;
  background: white;
  padding: 1rem;
  border-radius: 12px;
  box-shadow: 0 2px 10px rgba(0, 0, 0, 0.05);
  z-index: 50;
}

.legend-item {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  margin-bottom: 0.5rem;
  font-size: 0.85rem;
  color: #666;
}

.legend-item:last-child {
  margin-bottom: 0;
}

.legend-icon {
  width: 16px;
  height: 16px;
}

.legend-icon.node {
  background: #667eea;
  border-radius: 50%;
}

.legend-icon.edge {
  background: linear-gradient(to right, transparent 40%, #999 40%, #999 60%, transparent 60%);
  height: 2px;
}

.legend-icon.highlight {
  background: rgba(102, 126, 234, 0.3);
  border-radius: 50%;
  border: 2px solid #667eea;
}

.zoom-controls {
  position: absolute;
  bottom: 1rem;
  right: 1rem;
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
  z-index: 50;
}

.zoom-controls button {
  width: 36px;
  height: 36px;
  border: none;
  background: white;
  border-radius: 8px;
  box-shadow: 0 2px 5px rgba(0, 0, 0, 0.1);
  cursor: pointer;
  font-size: 1.2rem;
  transition: all 0.2s;
}

.zoom-controls button:hover {
  background: #f0f0f0;
}

.zoom-controls span {
  text-align: center;
  font-size: 0.8rem;
  color: #666;
  background: white;
  padding: 4px;
  border-radius: 4px;
}
</style>
