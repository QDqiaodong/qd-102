
<template>
  <div class="app-container">
    <header class="app-header">
      <div class="header-content">
        <h1 @click="$router.push('/')" class="logo">📚 读书笔记</h1>
        <div class="header-right">
          <div class="search-box">
            <input 
              type="text" 
              v-model="searchKeyword" 
              placeholder="搜索书籍、笔记..."
              @keyup.enter="handleSearch"
              class="search-input"
            />
            <button @click="handleSearch" class="search-btn">搜索</button>
          </div>
          <nav class="nav-links">
            <a href="/" class="nav-link">书籍库</a>
            <a href="/heatmap" class="nav-link">阅读活跃度</a>
            <a href="/tags" class="nav-link">标签管理</a>
          </nav>
        </div>
      </div>
    </header>
    <main class="app-main">
      <router-view></router-view>
    </main>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { useRouter } from 'vue-router'

const router = useRouter()
const searchKeyword = ref('')

const handleSearch = () => {
  if (searchKeyword.value.trim()) {
    router.push(`/search?keyword=${encodeURIComponent(searchKeyword.value.trim())}`)
    searchKeyword.value = ''
  }
}
</script>

<style>
* {
  margin: 0;
  padding: 0;
  box-sizing: border-box;
}

body {
  font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, 'Helvetica Neue', Arial, sans-serif;
  background-color: #f5f5f5;
  min-height: 100vh;
}

.app-container {
  min-height: 100vh;
  display: flex;
  flex-direction: column;
}

.app-header {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
  padding: 1rem 2rem;
  box-shadow: 0 2px 10px rgba(0,0,0,0.1);
}

.header-content {
  max-width: 1400px;
  margin: 0 auto;
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.logo {
  font-size: 1.5rem;
  cursor: pointer;
  font-weight: 600;
}

.header-right {
  display: flex;
  align-items: center;
  gap: 2rem;
}

.search-box {
  display: flex;
  gap: 0.5rem;
}

.search-input {
  padding: 0.5rem 1rem;
  border: none;
  border-radius: 20px;
  width: 250px;
  font-size: 0.9rem;
}

.search-btn {
  padding: 0.5rem 1rem;
  background: rgba(255,255,255,0.2);
  border: 1px solid rgba(255,255,255,0.3);
  border-radius: 20px;
  color: white;
  cursor: pointer;
  transition: all 0.3s;
}

.search-btn:hover {
  background: rgba(255,255,255,0.3);
}

.nav-links {
  display: flex;
  gap: 1.5rem;
}

.nav-link {
  color: white;
  text-decoration: none;
  padding: 0.5rem 1rem;
  border-radius: 8px;
  transition: background 0.3s;
}

.nav-link:hover {
  background: rgba(255,255,255,0.1);
}

.app-main {
  flex: 1;
  max-width: 1400px;
  width: 100%;
  margin: 0 auto;
  padding: 2rem;
}
</style>
