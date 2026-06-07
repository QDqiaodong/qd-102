
import { createRouter, createWebHistory } from 'vue-router'
import BookList from '../views/BookList.vue'
import BookDetail from '../views/BookDetail.vue'
import NoteEditor from '../views/NoteEditor.vue'
import TagManager from '../views/TagManager.vue'
import SearchPage from '../views/SearchPage.vue'
import HeatmapView from '../views/HeatmapView.vue'
import TagGraphView from '../views/TagGraphView.vue'
import CardWall from '../views/CardWall.vue'
import SimilarNotes from '../views/SimilarNotes.vue'
import WakeUpListView from '../views/WakeUpListView.vue'

const routes = [
  {
    path: '/',
    name: 'BookList',
    component: BookList
  },
  {
    path: '/wake-up',
    name: 'WakeUpList',
    component: WakeUpListView
  },
  {
    path: '/cards',
    name: 'CardWall',
    component: CardWall
  },
  {
    path: '/book/:id',
    name: 'BookDetail',
    component: BookDetail
  },
  {
    path: '/book/:id/note/new',
    name: 'NoteEditor',
    component: NoteEditor
  },
  {
    path: '/note/:id/edit',
    name: 'NoteEdit',
    component: NoteEditor
  },
  {
    path: '/tags',
    name: 'TagManager',
    component: TagManager
  },
  {
    path: '/tags/graph',
    name: 'TagGraph',
    component: TagGraphView
  },
  {
    path: '/search',
    name: 'SearchPage',
    component: SearchPage
  },
  {
    path: '/heatmap',
    name: 'HeatmapView',
    component: HeatmapView
  },
  {
    path: '/similar-notes',
    name: 'SimilarNotes',
    component: SimilarNotes
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

export default router
