<script setup>
import { computed, nextTick, onMounted, shallowRef, useTemplateRef } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { createChatSession, deleteChatSession, listChatMessages, listChatSessions, sendChatMessageStream } from '@/api/chat'
import { handleAuthFailure } from '@/utils/authGuard'

const WELCOME_MESSAGE = {
  id: 'local-welcome-message',
  role: 'assistant',
  content: '你好！我是“龙蛋 AI”的智能知识库问答助手。很高兴为您服务！\n\n您可以向我询问平台规则、商品内容规范、审核标准、风险排查或是文案生成建议等相关问题。请问今天有什么我可以帮您的？',
  ragUsed: false,
  sources: []
}

const sessions = shallowRef([])
const activeSessionId = shallowRef('')
const messages = shallowRef([])
const draft = shallowRef('')
const loadingSessions = shallowRef(false)
const loadingMessages = shallowRef(false)
const sending = shallowRef(false)
const creatingSession = shallowRef(false)
const deletingSessionIds = shallowRef(new Set())
const messagesRef = useTemplateRef('messagesRef')
const router = useRouter()

const activeSession = computed(() => sessions.value.find(session => session.id === activeSessionId.value))
const newSessionBusy = computed(() => loadingSessions.value || creatingSession.value)
const displayMessages = computed(() => {
  if (messages.value.length > 0) {
    return messages.value
  }
  return activeSession.value ? [WELCOME_MESSAGE] : []
})

const handleRequestError = (error, fallbackMessage) => {
  if (handleAuthFailure(error, router)) {
    return
  }

  ElMessage.error(error.message || fallbackMessage)
}

const loadSessions = async ({ keepActive = false } = {}) => {
  loadingSessions.value = true
  try {
    const nextSessions = await listChatSessions()
    sessions.value = Array.isArray(nextSessions) ? nextSessions : []

    if (sessions.value.length === 0) {
      activeSessionId.value = ''
      messages.value = []
      return
    }

    const stillActive = sessions.value.some(session => session.id === activeSessionId.value)
    if (!keepActive || !stillActive) {
      await selectSession(sessions.value[0])
    }
  } catch (error) {
    handleRequestError(error, '会话加载失败')
  } finally {
    loadingSessions.value = false
  }
}

const handleNewSession = async () => {
  if (creatingSession.value) {
    return
  }

  const emptySession = findEmptySession()
  if (emptySession) {
    await selectSession(emptySession, { force: true })
    return
  }

  creatingSession.value = true
  try {
    const session = await createChatSession('新建对话')
    sessions.value = [session, ...sessions.value.filter(item => item.id !== session.id)]
    activeSessionId.value = session.id
    messages.value = []
    await scrollToBottom()
  } catch (error) {
    handleRequestError(error, '新建会话失败')
  } finally {
    creatingSession.value = false
  }
}

const selectSession = async (session, { force = false } = {}) => {
  if (!session || (!force && session.id === activeSessionId.value)) {
    return
  }
  activeSessionId.value = session.id
  await loadMessages(session.id)
}

const isEmptySession = (session) => Number(session?.messageCount || 0) === 0
  && !String(session?.lastMessagePreview || '').trim()

const findEmptySession = () => sessions.value.find(isEmptySession)

const loadMessages = async (sessionId) => {
  if (!sessionId) {
    messages.value = []
    return
  }
  loadingMessages.value = true
  try {
    const history = await listChatMessages(sessionId)
    messages.value = Array.isArray(history) ? history : []
    await scrollToBottom()
  } catch (error) {
    handleRequestError(error, '消息加载失败')
  } finally {
    loadingMessages.value = false
  }
}

const sendCurrentMessage = async () => {
  const content = draft.value.trim()
  if (!content || sending.value) {
    return
  }

  let sessionId = activeSessionId.value
  if (!sessionId) {
    await handleNewSession()
    sessionId = activeSessionId.value
  }
  if (!sessionId) {
    return
  }

  const userMessage = {
    id: `local-user-${Date.now()}`,
    role: 'user',
    content,
    sources: []
  }
  const assistantMessageId = `local-assistant-${Date.now()}`
  const assistantMessage = {
    id: assistantMessageId,
    role: 'assistant',
    content: '',
    ragUsed: false,
    sources: [],
    streaming: true
  }
  messages.value = [...messages.value, userMessage, assistantMessage]
  draft.value = ''
  sending.value = true
  await scrollToBottom()

  try {
    const response = await sendChatMessageStream(sessionId, content, {
      onDelta: (chunk) => appendAssistantDelta(assistantMessageId, chunk)
    })
    updateMessage(assistantMessageId, (message) => ({
      ...message,
      id: response.messageId || message.id,
      content: response.answer || message.content,
      ragUsed: Array.isArray(response.sources) && response.sources.length > 0,
      sources: response.sources || [],
      streaming: false
    }))
    await loadSessions({ keepActive: true })
    await scrollToBottom()
  } catch (error) {
    messages.value = messages.value.filter(message => ![userMessage.id, assistantMessageId].includes(message.id))
    draft.value = content
    handleRequestError(error, '发送失败')
  } finally {
    sending.value = false
  }
}

const updateMessage = (messageId, updater) => {
  messages.value = messages.value.map((message) => (message.id === messageId ? updater(message) : message))
}

const setSessionDeleting = (sessionId, deleting) => {
  const nextIds = new Set(deletingSessionIds.value)
  if (deleting) {
    nextIds.add(sessionId)
  } else {
    nextIds.delete(sessionId)
  }
  deletingSessionIds.value = nextIds
}

const isDeletingSession = (sessionId) => deletingSessionIds.value.has(sessionId)

const confirmDeleteSession = async (session, event) => {
  event?.stopPropagation()
  if (!session || isDeletingSession(session.id)) {
    return
  }

  try {
    await ElMessageBox.confirm('删除后会清除该会话的所有消息和知识库来源记录，无法恢复。', '删除会话', {
      confirmButtonText: '删除',
      cancelButtonText: '取消',
      type: 'warning',
      confirmButtonClass: 'el-button--danger'
    })
  } catch (error) {
    if (['cancel', 'close'].includes(error)) {
      return
    }
    throw error
  }

  setSessionDeleting(session.id, true)
  try {
    await deleteChatSession(session.id)
    const wasActive = session.id === activeSessionId.value
    sessions.value = sessions.value.filter(item => item.id !== session.id)
    ElMessage.success('已删除会话')

    if (wasActive) {
      activeSessionId.value = ''
      messages.value = []
      const nextSession = sessions.value[0]
      if (nextSession) {
        await selectSession(nextSession, { force: true })
      }
    }
  } catch (error) {
    handleRequestError(error, '删除会话失败')
  } finally {
    setSessionDeleting(session.id, false)
  }
}

const appendAssistantDelta = (messageId, chunk) => {
  if (!chunk) {
    return
  }
  updateMessage(messageId, (message) => ({
    ...message,
    content: `${message.content || ''}${chunk}`
  }))
  scrollToBottom()
}

const scrollToBottom = async () => {
  await nextTick()
  if (messagesRef.value) {
    messagesRef.value.scrollTop = messagesRef.value.scrollHeight
  }
}

const sessionTitle = (session) => {
  if (isEmptySession(session)) {
    return '未开始的对话'
  }
  return session?.title || '未命名对话'
}

const sessionPreview = (session) => session?.lastMessagePreview || '暂无消息'

const messageClass = (message) => ({
  'is-user': message.role === 'user',
  'is-assistant': message.role !== 'user'
})

const displaySources = (sources = []) => {
  const seen = new Set()
  return sources.filter((source) => {
    const key = source?.documentId || source?.sourceTitle
    if (!key || seen.has(key)) {
      return false
    }
    seen.add(key)
    return true
  })
}

onMounted(() => {
  loadSessions()
})
</script>

<template>
  <div class="page chat-page">
    <section class="chat-panel chat-workbench">
      <aside class="chat-sidebar session-sidebar">
        <el-button class="new-session-button" type="primary" :loading="newSessionBusy" :disabled="newSessionBusy" @click="handleNewSession">
          <el-icon><Plus /></el-icon>
          <span>新建会话</span>
        </el-button>

        <div class="session-list" v-loading="loadingSessions">
          <div
            v-for="session in sessions"
            :key="session.id"
            role="button"
            tabindex="0"
            :class="['session-item', { 'is-active': session.id === activeSessionId }]"
            @click="selectSession(session)"
            @keydown.enter.prevent="selectSession(session)"
            @keydown.space.prevent="selectSession(session)"
          >
            <span class="session-item-copy">
              <span class="session-title">{{ sessionTitle(session) }}</span>
              <span class="session-preview">{{ sessionPreview(session) }}</span>
            </span>
            <button
              class="session-delete-button"
              type="button"
              :disabled="isDeletingSession(session.id)"
              :aria-label="`删除会话：${sessionTitle(session)}`"
              title="删除会话"
              @click="confirmDeleteSession(session, $event)"
            >
              <el-icon><Delete /></el-icon>
            </button>
          </div>
          <el-empty v-if="!loadingSessions && sessions.length === 0" description="暂无会话" :image-size="92" />
        </div>
      </aside>

      <main class="chat-main conversation-main">
        <div ref="messagesRef" class="messages chat-messages" v-loading="loadingMessages">
          <div v-if="displayMessages.length === 0" class="conversation-empty">
            <h2>{{ activeSession ? '开始智能聊天' : '正在准备会话' }}</h2>
            <p>请输入你想查询的规则、规范或知识库内容。</p>
          </div>

          <article
            v-for="message in displayMessages"
            :key="message.id"
            :class="['chat-message-row', messageClass(message)]"
          >
            <div class="message chat-bubble">
              <p>{{ message.content }}</p>

              <span v-if="message.streaming && !message.content" class="typing-cursor">正在生成</span>

              <div v-if="message.role !== 'user' && displaySources(message.sources).length" class="source-list">
                <div class="source-list-title">知识库来源</div>
                <ul class="source-list-items">
                  <li v-for="source in displaySources(message.sources)" :key="source.documentId || source.sourceTitle" class="source-item">
                    {{ source.sourceTitle || '未命名文档' }}
                  </li>
                </ul>
              </div>
            </div>
          </article>
        </div>

        <div class="composer chat-composer">
          <el-input
            v-model="draft"
            type="textarea"
            :autosize="{ minRows: 1, maxRows: 4 }"
            maxlength="1000"
            show-word-limit
            resize="none"
            placeholder="询问知识库中的平台规则、详情页结构、图片风险或生成建议"
            @keydown.enter.exact.prevent="sendCurrentMessage"
          />
          <el-button type="primary" :loading="sending" :disabled="!draft.trim()" @click="sendCurrentMessage">
            发送
          </el-button>
        </div>
      </main>
    </section>
  </div>
</template>

<style scoped>
.chat-page {
  padding: 0;
}

.chat-workbench {
  height: calc(100vh - 144px);
  min-height: 520px;
  overflow: hidden;
}

.session-sidebar {
  display: flex;
  flex-direction: column;
  gap: 16px;
  min-height: 0;
  overflow: hidden;
}

.new-session-button {
  width: 100%;
  height: 42px;
  border-radius: 6px;
  font-weight: 700;
}

.new-session-button :deep(.el-icon) {
  margin-right: 6px;
}

.session-list {
  min-height: 0;
  flex: 1;
  overflow: auto;
  padding-right: 2px;
}

.session-item {
  width: 100%;
  display: grid;
  grid-template-columns: minmax(0, 1fr) 30px;
  align-items: center;
  gap: 5px;
  padding: 12px 13px;
  margin-bottom: 8px;
  border: 1px solid transparent;
  border-radius: 6px;
  background: transparent;
  color: var(--text);
  text-align: left;
  cursor: pointer;
}

.session-item-copy {
  min-width: 0;
  display: grid;
  gap: 5px;
}

.session-delete-button {
  width: 30px;
  height: 30px;
  display: grid;
  place-items: center;
  border: 1px solid transparent;
  border-radius: 6px;
  color: #8a93a3;
  background: transparent;
  cursor: pointer;
  opacity: 0;
}

.session-item:hover .session-delete-button,
.session-item:focus-within .session-delete-button,
.session-item.is-active .session-delete-button {
  opacity: 1;
}

.session-delete-button:hover,
.session-delete-button:focus-visible {
  color: #d92d20;
  border-color: rgba(217, 45, 32, 0.18);
  background: rgba(217, 45, 32, 0.08);
}

.session-delete-button:disabled {
  cursor: wait;
  opacity: 0.5;
}

.session-item:hover,
.session-item.is-active {
  border-color: var(--line);
  background: #f4f5f7;
}

.session-title,
.session-preview {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.session-title {
  font-size: 15px;
  font-weight: 700;
}

.session-preview {
  color: var(--muted);
  font-size: 12px;
}

.conversation-main {
  min-width: 0;
  min-height: 0;
  overflow: hidden;
}

.chat-messages {
  min-height: 0;
  overflow: auto;
  gap: 14px;
  scrollbar-gutter: stable;
}

.conversation-empty {
  align-self: center;
  justify-self: center;
  max-width: 430px;
  color: var(--muted);
  text-align: center;
}

.conversation-empty h2 {
  margin: 0 0 10px;
  color: var(--text);
  font-size: 28px;
}

.conversation-empty p {
  margin: 0;
}

.chat-message-row {
  display: flex;
}

.chat-message-row.is-user {
  justify-content: flex-end;
}

.chat-message-row.is-assistant {
  justify-content: flex-start;
}

.chat-bubble {
  border-radius: 18px;
}

.chat-bubble p {
  margin: 0;
  white-space: pre-wrap;
  word-break: break-word;
}

.typing-cursor {
  color: #7b8494;
  font-size: 14px;
}

.chat-message-row.is-assistant .chat-bubble {
  background: #f3f4f6;
}

.source-list {
  margin-top: 14px;
  padding-top: 12px;
  border-top: 1px solid rgba(17, 24, 39, 0.12);
}

.source-list-title {
  margin-bottom: 8px;
  color: #5b6472;
  font-size: 12px;
  font-weight: 700;
}

.source-list-items {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  padding: 0;
  margin: 0;
  list-style: none;
}

.source-item {
  max-width: 100%;
  padding: 6px 9px;
  border: 1px solid rgba(17, 24, 39, 0.12);
  border-radius: 6px;
  background: rgba(255, 255, 255, 0.72);
  color: #4d5665;
  font-size: 12px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.chat-composer {
  align-items: flex-end;
}

.chat-composer :deep(.el-textarea__inner) {
  min-height: 44px !important;
  border-radius: 6px;
}

.chat-composer .el-button {
  min-width: 78px;
  height: 44px;
  border-radius: 6px;
}

@media (max-width: 900px) {
  .chat-workbench {
    height: auto;
    min-height: auto;
  }

  .session-sidebar {
    border-right: none;
    border-bottom: 1px solid var(--line);
  }

  .session-list {
    max-height: 260px;
  }
}
</style>
