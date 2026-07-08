<template>
  <div class="page chat-page">
    <section class="chat-panel rag-chat-panel">
      <aside class="chat-sidebar rag-sidebar">
        <div class="sidebar-actions">
          <el-button type="primary" :icon="Plus" @click="handleCreateSession">新建会话</el-button>
          <el-button :icon="Refresh" circle @click="loadSessions" />
        </div>

        <div v-loading="loadingSessions" class="session-list">
          <button
            v-for="session in sessions"
            :key="session.id"
            class="session-item"
            :class="{ 'is-active': session.id === activeSessionId }"
            type="button"
            @click="selectSession(session.id)"
          >
            <strong>{{ session.title || '未命名会话' }}</strong>
            <span>{{ session.lastMessagePreview || '开始一次 RAG 问答' }}</span>
          </button>
          <el-empty v-if="!loadingSessions && sessions.length === 0" description="暂无会话" :image-size="72" />
        </div>

        <div class="section-title compact-title">
          <h3>商品上下文</h3>
        </div>
        <el-select v-model="selectedProductId" style="width: 100%">
          <el-option v-for="item in productOptions" :key="item.id" :label="item.name" :value="item.id" />
        </el-select>

        <div class="section-title compact-title">
          <h3>RAG 来源</h3>
        </div>
        <div class="source-list">
          <article v-for="source in activeSources" :key="source.id || `${source.sourceTitle}-${source.sourceRank}`" class="source-item">
            <div>
              <strong>{{ source.sourceTitle || '知识片段' }}</strong>
              <small v-if="source.score !== undefined && source.score !== null">匹配 {{ formatScore(source.score) }}</small>
            </div>
            <p>{{ source.snippet || '暂无摘要' }}</p>
          </article>
          <el-empty v-if="activeSources.length === 0" description="暂无来源" :image-size="62" />
        </div>
      </aside>

      <main class="chat-main rag-chat-main">
        <header class="chat-main-header">
          <div>
            <span class="eyebrow-mini">Intelligent Chat</span>
            <h1>智能聊天</h1>
          </div>
          <el-tag type="success" effect="plain">
            <el-icon><Collection /></el-icon>
            RAG
          </el-tag>
        </header>

        <div ref="messagesRef" v-loading="loadingMessages" class="messages rag-messages">
          <el-empty
            v-if="!loadingMessages && messages.length === 0"
            description="选择或新建会话后开始提问"
            :image-size="96"
          />

          <article
            v-for="message in messages"
            :key="message.id"
            class="message"
            :class="message.role === 'user' ? 'user' : 'assistant'"
          >
            <div class="message-meta">
              <span>{{ message.role === 'user' ? '你' : '龙蛋 AI' }}</span>
              <small>{{ message.createdAt || '' }}</small>
            </div>
            <p>{{ message.content }}</p>
            <div v-if="message.sources && message.sources.length" class="message-sources">
              <el-tag
                v-for="source in message.sources"
                :key="source.id || `${message.id}-${source.sourceRank}`"
                size="small"
                effect="plain"
              >
                {{ source.sourceTitle || '知识来源' }}
              </el-tag>
            </div>
          </article>

          <article v-if="sending" class="message assistant is-loading">
            <div class="message-meta">
              <span>龙蛋 AI</span>
            </div>
            <p>正在检索知识库并生成回复...</p>
          </article>
        </div>

        <div v-if="errorMessage" class="chat-error">
          <el-icon><WarningFilled /></el-icon>
          <span>{{ errorMessage }}</span>
        </div>

        <form class="composer rag-composer" @submit.prevent="handleSend">
          <el-input
            v-model="draft"
            type="textarea"
            :autosize="{ minRows: 1, maxRows: 4 }"
            placeholder="询问平台规则、详情页结构、图片风险或生成建议"
            @keydown.enter.exact.prevent="handleSend"
          />
          <el-button type="primary" native-type="submit" :loading="sending" :icon="Promotion">发送</el-button>
        </form>
      </main>
    </section>
  </div>
</template>

<script setup>
import { computed, nextTick, onMounted, ref } from 'vue'
import { Collection, Plus, Promotion, Refresh, WarningFilled } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import { createChatSession, listChatMessages, listChatSessions, sendChatMessage } from '@/api/chat'

const productOptions = [
  { id: 'p-1001', name: '磁吸无线充电支架' },
  { id: 'p-1002', name: '便携榨汁杯' },
  { id: 'p-1003', name: '夏季防晒冰丝外套' }
]

const sessions = ref([])
const activeSessionId = ref('')
const messages = ref([])
const draft = ref('')
const selectedProductId = ref(productOptions[0]?.id || '')
const loadingSessions = ref(false)
const loadingMessages = ref(false)
const sending = ref(false)
const errorMessage = ref('')
const messagesRef = ref(null)

const activeSources = computed(() => {
  const lastAssistant = [...messages.value].reverse().find(message => message.role !== 'user' && message.sources?.length)
  return lastAssistant?.sources || []
})

onMounted(() => {
  loadSessions()
})

async function loadSessions() {
  loadingSessions.value = true
  errorMessage.value = ''
  try {
    sessions.value = await listChatSessions()
    if (!activeSessionId.value && sessions.value.length > 0) {
      await selectSession(sessions.value[0].id)
    }
  } catch (error) {
    errorMessage.value = normalizeError(error)
  } finally {
    loadingSessions.value = false
  }
}

async function selectSession(sessionId) {
  activeSessionId.value = sessionId
  loadingMessages.value = true
  errorMessage.value = ''
  try {
    messages.value = await listChatMessages(sessionId)
    await scrollToBottom()
  } catch (error) {
    errorMessage.value = normalizeError(error)
  } finally {
    loadingMessages.value = false
  }
}

async function handleCreateSession() {
  errorMessage.value = ''
  try {
    const session = await createChatSession('电商 RAG 会话')
    sessions.value = [session, ...sessions.value.filter(item => item.id !== session.id)]
    await selectSession(session.id)
  } catch (error) {
    errorMessage.value = normalizeError(error)
  }
}

async function handleSend() {
  const content = draft.value.trim()
  if (!content || sending.value) {
    return
  }

  sending.value = true
  errorMessage.value = ''
  draft.value = ''

  try {
    let sessionId = activeSessionId.value
    if (!sessionId) {
      const title = content.length > 18 ? `${content.slice(0, 18)}...` : content
      const session = await createChatSession(title || '电商 RAG 会话')
      sessions.value = [session, ...sessions.value]
      sessionId = session.id
      activeSessionId.value = sessionId
    }

    messages.value.push({
      id: `local-user-${Date.now()}`,
      role: 'user',
      content
    })
    await scrollToBottom()

    const response = await sendChatMessage(sessionId, content)
    messages.value.push({
      id: response.messageId || `local-assistant-${Date.now()}`,
      role: 'assistant',
      content: response.answer,
      sources: response.sources || []
    })
    await loadSessions()
    activeSessionId.value = sessionId
    await scrollToBottom()
  } catch (error) {
    draft.value = content
    errorMessage.value = normalizeError(error)
  } finally {
    sending.value = false
  }
}

async function scrollToBottom() {
  await nextTick()
  const el = messagesRef.value
  if (el) {
    el.scrollTop = el.scrollHeight
  }
}

function formatScore(score) {
  const value = Number(score)
  if (Number.isNaN(value)) {
    return '-'
  }
  return `${Math.round(value * 100)}%`
}

function normalizeError(error) {
  const message = error?.message || '智能聊天请求失败'
  if (message.includes('401') || message.includes('登录')) {
    return '请先登录后再使用智能聊天'
  }
  ElMessage.error(message)
  return message
}
</script>

<style scoped>
.chat-page {
  --line: #e5e5e8;
  --text: #111217;
  --muted: #727682;
  --black: #111217;
  max-width: 1320px;
  margin: 0 auto;
}

.rag-chat-panel {
  min-height: calc(100vh - 150px);
  display: grid;
  grid-template-columns: minmax(280px, 360px) minmax(0, 1fr);
  overflow: hidden;
  border: 1px solid var(--line);
  border-radius: 8px;
  background: #fff;
  box-shadow: 0 14px 38px rgba(17, 18, 23, 0.055);
}

.rag-sidebar {
  display: flex;
  min-width: 0;
  flex-direction: column;
  gap: 18px;
  padding: 22px;
  border-right: 1px solid var(--line);
}

.sidebar-actions {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 44px;
  gap: 10px;
}

.sidebar-actions .el-button {
  min-height: 42px;
  border-radius: 13px;
  font-weight: 760;
}

.session-list {
  min-height: 168px;
  max-height: 280px;
  display: grid;
  align-content: start;
  gap: 9px;
  overflow: auto;
}

.session-item {
  width: 100%;
  min-height: 72px;
  padding: 13px 14px;
  border: 1px solid var(--line);
  border-radius: 8px;
  background: #fff;
  color: var(--text);
  cursor: pointer;
  text-align: left;
  transition: border-color 160ms ease, background 160ms ease, box-shadow 160ms ease;
}

.session-item:hover,
.session-item.is-active {
  border-color: var(--black);
  background: #f9f9fa;
  box-shadow: 0 8px 20px rgba(17, 18, 23, 0.06);
}

.session-item strong,
.session-item span {
  display: block;
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.session-item strong {
  font-size: 14px;
}

.session-item span {
  margin-top: 6px;
  color: var(--muted);
  font-size: 12px;
}

.compact-title {
  margin: 4px 0 -4px;
}

.compact-title h3 {
  font-size: 17px;
}

.source-list {
  display: grid;
  align-content: start;
  gap: 10px;
  overflow: auto;
}

.source-item {
  padding: 12px;
  border: 1px solid var(--line);
  border-radius: 8px;
  background: #fff;
}

.source-item div {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
}

.source-item strong {
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  font-size: 13px;
}

.source-item small {
  flex: 0 0 auto;
  color: var(--muted);
}

.source-item p {
  margin: 8px 0 0;
  color: var(--muted);
  font-size: 12px;
  line-height: 1.55;
}

.rag-chat-main {
  min-width: 0;
  display: flex;
  flex-direction: column;
  background: linear-gradient(180deg, #fff, #fbfbfc);
}

.chat-main-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 18px;
  padding: 24px 28px;
  border-bottom: 1px solid var(--line);
}

.chat-main-header h1 {
  margin: 5px 0 0;
  font-size: 28px;
  line-height: 1.15;
  letter-spacing: 0;
}

.eyebrow-mini {
  color: var(--muted);
  font-size: 12px;
  font-weight: 820;
  text-transform: uppercase;
}

.rag-messages {
  flex: 1;
  min-height: 0;
  max-height: calc(100vh - 320px);
  display: grid;
  align-content: start;
  gap: 14px;
  padding: 24px;
  overflow: auto;
}

.message {
  max-width: 72%;
  display: grid;
  gap: 7px;
  padding: 15px 17px;
  border-radius: 22px;
  background: #f1f1f3;
  line-height: 1.7;
}

.message.user {
  justify-self: end;
  color: #fff;
  background: var(--black);
}

.message.assistant {
  justify-self: start;
}

.message p {
  margin: 0;
  white-space: pre-wrap;
}

.message-meta {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  opacity: 0.82;
  font-size: 12px;
  font-weight: 760;
}

.message-meta small {
  font-weight: 500;
}

.message-sources {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
}

.message.is-loading {
  opacity: 0.78;
}

.chat-error {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  margin: 0 18px;
  padding: 10px 12px;
  border: 1px solid #ffd5d5;
  border-radius: 8px;
  color: #b42318;
  background: #fff5f5;
  font-size: 13px;
}

.rag-composer {
  display: flex;
  align-items: flex-end;
  gap: 10px;
  padding: 18px;
  border-top: 1px solid var(--line);
}

.rag-composer .el-button {
  min-width: 104px;
  min-height: 46px;
  border-radius: 14px;
  font-weight: 820;
}

@media (max-width: 1080px) {
  .rag-chat-panel {
    min-height: auto;
    grid-template-columns: 1fr;
  }

  .rag-sidebar {
    border-right: 0;
    border-bottom: 1px solid var(--line);
  }

  .rag-messages {
    max-height: none;
  }

  .message {
    max-width: 92%;
  }
}
</style>
