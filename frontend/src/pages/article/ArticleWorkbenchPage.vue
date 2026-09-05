<script setup lang="ts">
import { computed, nextTick, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { onBeforeRouteLeave, onBeforeRouteUpdate, useRoute, useRouter } from 'vue-router'
import { message, Modal } from 'ant-design-vue'
import {
  ArrowLeftOutlined,
  CheckOutlined,
  DownloadOutlined,
  EditOutlined,
  HistoryOutlined,
  SendOutlined,
  ThunderboltOutlined,
} from '@ant-design/icons-vue'
import {
  aiRefineContent,
  generatePublishPackage,
  getArticle,
  listRevisions,
  restoreRevision,
  saveRevision,
} from '@/api/articleController'
import SafeMarkdown from './workbench/SafeMarkdown.vue'
import {
  changedSpan,
  getHeadings,
  replaceSelection,
  responseData,
  type TextSelection,
} from './workbench/editor'

const route = useRoute(),
  router = useRouter()
const article = ref<API.ArticleVO>()
const loading = ref(true),
  loadError = ref(''),
  draft = ref(''),
  saved = ref('')
const editor = ref<HTMLTextAreaElement>(),
  mode = ref('edit'),
  activeTab = ref('refine')
const saving = ref(false),
  refining = ref(false),
  publishing = ref(false),
  restoring = ref(false)
const note = ref(''),
  savedAt = ref('')
const selection = ref<TextSelection>(),
  candidate = ref(''),
  instruction = ref('更精炼')
const presets = ['更精炼', '更专业', '更口语化', '扩充细节', '增强感染力']
const revisionsOpen = ref(false),
  revisionsLoading = ref(false),
  revisionsError = ref('')
const revisions = ref<API.ArticleRevisionVO[]>([]),
  viewedRevision = ref<API.ArticleRevisionVO>()
const channel = ref<'WECHAT' | 'XIAOHONGSHU'>('WECHAT')
const packages = ref<Partial<Record<'WECHAT' | 'XIAOHONGSHU', { text: string; source: string }>>>(
  {},
)
const dirty = computed(() => draft.value !== saved.value)
const busy = computed(() => saving.value || restoring.value || refining.value || publishing.value)
const editable = computed(() => article.value?.status === 'COMPLETED')
const headings = computed(() => getHeadings(draft.value))
const diff = computed(() => changedSpan(selection.value?.text || '', candidate.value))
const currentPackage = computed(() => packages.value[channel.value])
const staleSelection = computed(() => selection.value && selection.value.source !== draft.value)
const chars = computed(() => draft.value.replace(/\s/g, '').length)
const errorText = (e: unknown) => (e instanceof Error ? e.message : '操作失败，请重试')
const currentText = (a: API.ArticleVO) => a.fullContent || a.content || ''
const formatTime = (time?: string) =>
  time ? new Date(time).toLocaleString('zh-CN', { hour12: false }) : ''

async function load() {
  loading.value = true
  loadError.value = ''
  article.value = undefined
  try {
    article.value = responseData((await getArticle({ taskId: String(route.params.taskId) })).data)
    draft.value = saved.value = currentText(article.value)
    selection.value = undefined
    candidate.value = ''
    packages.value = {}
    note.value = ''
    savedAt.value = ''
  } catch (e) {
    loadError.value = errorText(e)
  } finally {
    loading.value = false
  }
}

function captureSelection() {
  if (busy.value) return
  const el = editor.value
  if (!el || el.selectionStart === el.selectionEnd) return
  const text = draft.value.slice(el.selectionStart, el.selectionEnd)
  if (!text.trim()) return
  selection.value = { start: el.selectionStart, end: el.selectionEnd, text, source: draft.value }
  candidate.value = ''
  activeTab.value = 'refine'
}
async function jumpTo(start: number, end: number) {
  mode.value = 'edit'
  await nextTick()
  const el = editor.value
  if (!el) return
  el.focus()
  el.setSelectionRange(start, end)
  // Approximate the source line position without changing the draft.
  el.scrollTop = Math.max(0, draft.value.slice(0, start).split('\n').length * 30 - 120)
}
async function refine() {
  if (!selection.value || staleSelection.value || !instruction.value.trim() || busy.value) return
  refining.value = true
  const selected = selection.value
  try {
    const text = responseData(
      (
        await aiRefineContent(
          {
            taskId: article.value!.taskId,
            selectedText: selected.text,
            instruction: instruction.value,
          },
          { timeout: 120000 },
        )
      ).data,
    )
    if (!text.trim()) throw new Error('未生成有效建议，请重试')
    candidate.value = text
  } catch (e) {
    message.error(errorText(e))
  } finally {
    refining.value = false
  }
}
function accept() {
  if (!selection.value || !candidate.value.trim()) return
  try {
    draft.value = replaceSelection(draft.value, selection.value, candidate.value)
    candidate.value = ''
    selection.value = undefined
    message.success('已采纳到草稿，请保存版本')
  } catch (e) {
    message.error(errorText(e))
  }
}
async function save() {
  if (!article.value || busy.value || !dirty.value || !draft.value.trim()) return false
  saving.value = true
  const snapshot = draft.value
  try {
    article.value = responseData(
      (
        await saveRevision({
          taskId: article.value.taskId,
          content: snapshot,
          fullContent: snapshot,
          revisionNote: note.value.trim() || '工作台编辑',
          baseFingerprint: article.value.contentFingerprint,
        })
      ).data,
    )
    saved.value = currentText(article.value)
    savedAt.value = new Date().toLocaleTimeString('zh-CN', { hour12: false })
    note.value = ''
    message.success('版本已保存')
    return true
  } catch (e) {
    message.error(errorText(e))
    return false
  } finally {
    saving.value = false
  }
}
async function loadRevisions() {
  revisionsLoading.value = true
  revisionsError.value = ''
  try {
    revisions.value = responseData((await listRevisions({ taskId: article.value!.taskId! })).data)
    viewedRevision.value = revisions.value[0]
  } catch (e) {
    revisionsError.value = errorText(e)
  } finally {
    revisionsLoading.value = false
  }
}
async function openHistory() {
  revisionsOpen.value = true
  await loadRevisions()
}
function confirmAction(title: string, content: string): Promise<boolean> {
  return new Promise((resolve) =>
    Modal.confirm({
      title,
      content,
      okText: '确认',
      cancelText: '取消',
      onOk: () => {
        resolve(true)
      },
      onCancel: () => {
        resolve(false)
      },
    }),
  )
}
async function restore() {
  const revision = viewedRevision.value
  if (!revision || busy.value) return
  const confirmed = await confirmAction(
    `恢复版本 ${revision.revisionNumber}？`,
    dirty.value
      ? '尚未保存的草稿会被替换。服务器上的当前版本会自动备份。可取消后先保存草稿。'
      : '恢复后会保存为新版本，当前版本也会保留。',
  )
  if (!confirmed) return
  restoring.value = true
  try {
    article.value = responseData(
      (
        await restoreRevision({
          taskId: article.value!.taskId,
          revisionId: revision.id,
          baseFingerprint: article.value!.contentFingerprint,
        })
      ).data,
    )
    draft.value = saved.value = currentText(article.value)
    selection.value = undefined
    candidate.value = ''
    note.value = ''
    savedAt.value = ''
    revisionsOpen.value = false
    message.success('已恢复历史版本')
  } catch (e) {
    message.error(errorText(e))
  } finally {
    restoring.value = false
  }
}
async function generatePackage() {
  if (!article.value || busy.value) return
  if (dirty.value) {
    message.info('请先保存版本，再生成对应的发布稿')
    return
  }
  publishing.value = true
  const requestedChannel = channel.value,
    source = saved.value
  try {
    const result = responseData(
      (
        await generatePublishPackage(
          { taskId: article.value.taskId, channel: requestedChannel },
          { timeout: 120000 },
        )
      ).data,
    )
    if (!result.content?.trim()) throw new Error('未生成有效发布稿，请重试')
    packages.value[requestedChannel] = { text: result.content, source }
  } catch (e) {
    message.error(errorText(e))
  } finally {
    publishing.value = false
  }
}
function download(text: string, suffix: string, extension = 'md') {
  const name = (article.value?.mainTitle || '文章').replace(/[\\/:*?"<>|]/g, '_')
  const url = URL.createObjectURL(new Blob([text], { type: 'text/plain;charset=utf-8' }))
  const link = document.createElement('a')
  link.href = url
  link.download = `${name}${suffix}.${extension}`
  link.click()
  setTimeout(() => URL.revokeObjectURL(url), 1000)
}
async function copyPackage() {
  try {
    await navigator.clipboard.writeText(currentPackage.value!.text)
    message.success('发布稿已复制')
  } catch {
    message.error('复制失败，可选中文本手动复制或下载')
  }
}
function exportDraft() {
  download(`# ${article.value?.mainTitle || ''}\n\n${draft.value}`, dirty.value ? '-草稿' : '')
}
async function canLeave() {
  if (busy.value) {
    message.info('操作进行中，请稍候')
    return false
  }
  return (
    !dirty.value ||
    (await confirmAction(
      '离开创作工作台？',
      '尚未保存的修改会丢失。可取消后保存版本，或下载草稿留存。',
    ))
  )
}
function beforeUnload(e: BeforeUnloadEvent) {
  if (dirty.value || busy.value) {
    e.preventDefault()
    e.returnValue = ''
  }
}
function shortcut(e: KeyboardEvent) {
  if ((e.metaKey || e.ctrlKey) && e.key.toLowerCase() === 's') {
    e.preventDefault()
    void save()
  }
}
onBeforeRouteLeave(canLeave)
onBeforeRouteUpdate(canLeave)
watch(() => route.params.taskId, load)
onMounted(() => {
  void load()
  window.addEventListener('beforeunload', beforeUnload)
  window.addEventListener('keydown', shortcut)
})
onBeforeUnmount(() => {
  window.removeEventListener('beforeunload', beforeUnload)
  window.removeEventListener('keydown', shortcut)
})
</script>

<template>
  <a-config-provider
    :auto-insert-space-in-button="false"
    :theme="{ token: { colorPrimary: '#8b6f47', borderRadius: 8 } }"
  >
    <main class="workbench">
      <div v-if="loading" class="page-state"><a-spin tip="正在打开创作工作台…" /></div>
      <a-result v-else-if="loadError" status="error" title="文章加载失败" :sub-title="loadError">
        <template #extra
          ><a-button @click="load">重新加载</a-button
          ><a-button @click="router.push('/article/list')">返回文章列表</a-button></template
        >
      </a-result>
      <a-result
        v-else-if="!editable"
        status="info"
        title="文章生成完成后即可编辑"
        sub-title="请在文章详情查看当前进度。"
      >
        <template #extra
          ><a-button @click="router.push(`/article/${route.params.taskId}`)"
            >查看文章</a-button
          ></template
        >
      </a-result>
      <template v-else-if="article">
        <header class="workbench-header">
          <div class="header-title">
            <button
              class="back"
              aria-label="返回文章详情"
              @click="router.push(`/article/${article.taskId}`)"
            >
              <ArrowLeftOutlined aria-hidden="true" />
            </button>
            <div>
              <div class="eyebrow">ARTICLEPILOT / 创作工作台</div>
              <h1>{{ article.mainTitle || '未命名文章' }}</h1>
            </div>
          </div>
          <div class="toolbar">
            <span class="save-state" role="status"
              ><span :class="['status-dot', { dirty }]" />{{
                dirty ? '有未保存修改' : savedAt ? `${savedAt} 已保存` : '已保存'
              }}</span
            >
            <a-button :disabled="busy" @click="openHistory"
              ><HistoryOutlined aria-hidden="true" /> 版本</a-button
            >
            <a-button @click="exportDraft"><DownloadOutlined aria-hidden="true" /> 导出</a-button>
            <a-button
              type="primary"
              :loading="saving"
              :disabled="!dirty || busy || !draft.trim()"
              @click="save"
              ><CheckOutlined aria-hidden="true" /> 保存版本</a-button
            >
          </div>
        </header>
        <div class="workspace">
          <aside class="outline-panel panel">
            <div class="panel-label">
              文章目录 <span>{{ headings.length }}</span>
            </div>
            <p class="muted">从结构开始，打磨每一段。</p>
            <nav aria-label="文章章节">
              <button
                v-for="(heading, index) in headings"
                :key="heading.start"
                class="outline-item"
                :style="{ paddingLeft: `${12 + Math.min(heading.depth - 1, 3) * 8}px` }"
                @click="jumpTo(heading.start, heading.end)"
              >
                <span>{{ String(index + 1).padStart(2, '0') }}</span
                >{{ heading.title }}
              </button>
            </nav>
            <p v-if="!headings.length" class="empty-hint">添加章节标题后，目录会自动出现。</p>
            <div class="outline-footer">
              <span>正文字符</span><strong>{{ chars.toLocaleString() }}</strong
              ><small>约 {{ Math.max(1, Math.ceil(chars / 400)) }} 分钟阅读</small>
            </div>
          </aside>
          <section class="editor-panel panel">
            <div class="editor-toolbar">
              <div><EditOutlined aria-hidden="true" /> 正文</div>
              <a-radio-group v-model:value="mode" size="small" button-style="solid"
                ><a-radio-button value="edit">编辑</a-radio-button
                ><a-radio-button value="preview">预览</a-radio-button></a-radio-group
              >
            </div>
            <div class="paper-heading">
              <span class="eyebrow">让想法成为好文章</span>
              <h2>{{ article.mainTitle }}</h2>
              <p v-if="article.subTitle">{{ article.subTitle }}</p>
            </div>
            <textarea
              v-if="mode === 'edit'"
              ref="editor"
              v-model="draft"
              class="source-editor"
              aria-label="文章正文编辑器"
              :readonly="saving || restoring || refining"
              spellcheck="false"
              placeholder="从这里开始写作…"
              @select="captureSelection"
              @mouseup="captureSelection"
              @keyup="captureSelection"
            />
            <SafeMarkdown v-else class="article-preview" :content="draft" />
            <div class="editor-footer">
              <span>{{
                mode === 'edit'
                  ? '支持 Markdown · 选中文字后使用右侧 AI 精修'
                  : '预览当前草稿 · 切回编辑可选择文字'
              }}</span
              ><span>⌘ / Ctrl + S 保存</span>
            </div>
            <div class="version-note">
              <label for="version-note">版本说明</label
              ><input
                id="version-note"
                v-model="note"
                :disabled="saving"
                maxlength="500"
                placeholder="记录这次修改，例如：精简开头、补充结论"
              />
            </div>
          </section>
          <aside class="assistant-panel panel">
            <div class="assistant-heading">
              <ThunderboltOutlined aria-hidden="true" />
              <div><strong>创作助手</strong><small>把修改的决定权留给你</small></div>
            </div>
            <a-tabs v-model:active-key="activeTab">
              <a-tab-pane key="refine" tab="AI 精修">
                <p class="muted">在正文编辑器中选中一个段落，再告诉 AI 你希望如何调整。</p>
                <div class="selection-box">
                  <span class="field-label">{{
                    selection ? `已选中 ${selection.text.length} 字符` : '等待选择文字'
                  }}</span>
                  <p>{{ selection?.text || '选中的原文会显示在这里。' }}</p>
                </div>
                <a-alert
                  v-if="staleSelection"
                  type="warning"
                  message="正文已修改，请重新选择文字"
                  show-icon
                />
                <div class="presets">
                  <button
                    v-for="preset in presets"
                    :key="preset"
                    :class="{ active: instruction === preset }"
                    :disabled="busy"
                    @click="instruction = preset"
                  >
                    {{ preset }}
                  </button>
                </div>
                <label class="field-label" for="refine-instruction">修改要求</label>
                <a-textarea
                  id="refine-instruction"
                  v-model:value="instruction"
                  :rows="3"
                  :maxlength="1000"
                  :disabled="busy"
                  placeholder="例如：保留核心观点，缩短约三分之一"
                />
                <a-button
                  block
                  type="primary"
                  class="generate"
                  :loading="refining"
                  :disabled="
                    !selection ||
                    !!staleSelection ||
                    !instruction.trim() ||
                    busy ||
                    (selection?.text.length || 0) > 8000
                  "
                  @click="refine"
                  ><ThunderboltOutlined aria-hidden="true" />
                  {{ candidate ? '重新生成建议' : '生成精修建议' }}</a-button
                >
                <p v-if="(selection?.text.length || 0) > 8000" class="muted">
                  单次最多精修 8000 字符，请缩小选区。
                </p>
                <template v-if="candidate">
                  <div class="comparison">
                    <span class="field-label">修改前</span>
                    <p>
                      {{ diff.prefix }}<del>{{ diff.removed }}</del
                      >{{ diff.suffix }}
                    </p>
                    <span class="field-label">修改后</span>
                    <p>
                      {{ diff.prefix }}<ins>{{ diff.added }}</ins
                      >{{ diff.suffix }}
                    </p>
                  </div>
                  <label class="field-label" for="refine-candidate">候选内容 · 可继续修改</label
                  ><a-textarea
                    id="refine-candidate"
                    v-model:value="candidate"
                    :rows="6"
                    :disabled="busy"
                  />
                  <div class="result-actions">
                    <a-button :disabled="busy" @click="candidate = ''">放弃</a-button
                    ><a-button
                      type="primary"
                      :disabled="!!staleSelection || busy || !candidate.trim()"
                      @click="accept"
                      ><CheckOutlined aria-hidden="true" /> 采纳到草稿</a-button
                    >
                  </div>
                </template>
                <div v-else class="assistant-footnote">先预览，再采纳。你的原文由你决定。</div>
              </a-tab-pane>
              <a-tab-pane key="publish" tab="发布包">
                <p class="muted">将已保存的文章整理成适合不同平台的发布稿。</p>
                <a-radio-group v-model:value="channel" class="channel-picker" :disabled="publishing"
                  ><a-radio-button value="WECHAT">公众号</a-radio-button
                  ><a-radio-button value="XIAOHONGSHU">小红书</a-radio-button></a-radio-group
                >
                <a-alert
                  v-if="dirty"
                  message="有未保存修改，请先保存版本"
                  type="warning"
                  show-icon
                />
                <a-button
                  block
                  type="primary"
                  class="generate"
                  :loading="publishing"
                  :disabled="busy || dirty"
                  @click="generatePackage"
                  ><SendOutlined aria-hidden="true" />
                  {{ currentPackage ? '重新生成发布稿' : '生成发布稿' }}</a-button
                >
                <template v-if="currentPackage">
                  <a-alert
                    v-if="currentPackage.source !== draft"
                    message="正文已更新，建议重新生成发布稿"
                    type="warning"
                    show-icon
                  />
                  <label for="publish-text" class="field-label"
                    >{{ channel === 'WECHAT' ? '公众号' : '小红书' }}发布稿 · 可编辑</label
                  >
                  <a-textarea
                    id="publish-text"
                    v-model:value="currentPackage.text"
                    :rows="19"
                    :disabled="publishing"
                  />
                  <div class="result-actions">
                    <a-button @click="copyPackage">复制内容</a-button
                    ><a-button
                      @click="
                        download(
                          currentPackage.text,
                          channel === 'WECHAT' ? '-公众号' : '-小红书',
                          channel === 'WECHAT' ? 'md' : 'txt',
                        )
                      "
                      >下载发布稿</a-button
                    >
                  </div>
                  <p class="muted">发布稿暂存在当前页面，请复制或下载留存。</p>
                </template>
              </a-tab-pane>
            </a-tabs>
          </aside>
        </div>
      </template>
      <a-modal
        v-model:open="revisionsOpen"
        title="版本记录"
        width="900px"
        :footer="null"
        :closable="!restoring"
        :mask-closable="!restoring"
      >
        <a-spin :spinning="revisionsLoading || restoring">
          <a-alert v-if="revisionsError" type="error" :message="revisionsError"
            ><template #action
              ><a-button size="small" @click="loadRevisions">重试</a-button></template
            ></a-alert
          >
          <a-empty
            v-else-if="!revisions.length"
            description="首次保存时，将同时保留初始成稿和新版本"
          />
          <div v-else class="history-layout">
            <div class="history-list">
              <button
                v-for="revision in revisions"
                :key="revision.id"
                :class="{ active: viewedRevision?.id === revision.id }"
                :disabled="restoring"
                @click="viewedRevision = revision"
              >
                <strong>版本 {{ revision.revisionNumber }}</strong
                ><span>{{ revision.revisionNote }}</span
                ><small>{{ formatTime(revision.createTime) }}</small>
              </button>
            </div>
            <div v-if="viewedRevision" class="history-content">
              <div class="history-action">
                <span>版本 {{ viewedRevision.revisionNumber }} 预览</span
                ><a-button type="primary" :disabled="busy" @click="restore">恢复此版本</a-button>
              </div>
              <SafeMarkdown :content="viewedRevision.fullContent || viewedRevision.content || ''" />
            </div>
          </div>
        </a-spin>
      </a-modal>
    </main>
  </a-config-provider>
</template>

<style scoped>
.workbench {
  background: #f6f2eb;
  padding: 24px 28px 42px;
  min-height: calc(100vh - 76px);
  color: #3e2a1f;
}
.workbench-header {
  max-width: 1600px;
  margin: 0 auto 24px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 20px;
}
.header-title {
  display: flex;
  align-items: center;
  gap: 14px;
  min-width: 0;
}
.back {
  background: #fffdf9;
  border: 1px solid #e6ded1;
  border-radius: 10px;
  padding: 10px 12px;
  cursor: pointer;
}
.eyebrow {
  font-size: 10px;
  letter-spacing: 0.12em;
  color: #9a8060;
  font-weight: 600;
}
h1 {
  font-size: 19px;
  margin: 5px 0 0;
  line-height: 1.5;
  overflow-wrap: anywhere;
}
.toolbar {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-shrink: 0;
  flex-wrap: wrap;
}
.save-state {
  font-size: 12px;
  color: #7c7668;
  display: flex;
  align-items: center;
  gap: 6px;
  margin-right: 8px;
}
.status-dot {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: #6b7f5c;
}
.status-dot.dirty {
  background: #d59648;
}
.workspace {
  max-width: 1600px;
  margin: auto;
  display: grid;
  grid-template-columns: 195px minmax(320px, 1fr) 330px;
  gap: 18px;
  align-items: start;
}
.panel {
  background: #fffdf9;
  border: 1px solid #e9e0d3;
  border-radius: 14px;
  min-width: 0;
}
.outline-panel {
  padding: 22px 12px 14px;
  position: sticky;
  top: 92px;
}
.panel-label {
  font-size: 14px;
  font-weight: 600;
  display: flex;
  justify-content: space-between;
  padding: 0 8px;
}
.panel-label span {
  color: #9c8a73;
  font-weight: 400;
}
.muted {
  color: #958674;
  line-height: 1.8;
  font-size: 12px;
  margin: 12px 0 16px;
}
.outline-panel .muted {
  padding: 0 8px;
}
.outline-item {
  width: 100%;
  display: flex;
  gap: 9px;
  text-align: left;
  font-size: 12px;
  line-height: 1.7;
  border: 0;
  background: transparent;
  padding: 12px;
  border-radius: 8px;
  color: #685642;
  cursor: pointer;
  overflow-wrap: anywhere;
}
.outline-item:hover {
  background: #f5eee3;
  color: #805c2d;
}
.outline-item span {
  font-size: 10px;
  color: #b7a58e;
  padding-top: 2px;
}
.outline-footer {
  border-top: 1px solid #efe6da;
  margin: 24px 8px 0;
  padding-top: 18px;
  display: flex;
  flex-direction: column;
  gap: 5px;
  color: #958674;
  font-size: 11px;
}
.outline-footer strong {
  font-family: var(--font-display);
  font-size: 26px;
  font-weight: 500;
  color: #715736;
}
.editor-panel {
  overflow: hidden;
  box-shadow: 0 6px 26px #58412406;
}
.editor-toolbar {
  padding: 14px 22px;
  display: flex;
  justify-content: space-between;
  align-items: center;
  border-bottom: 1px solid #eee6da;
  font-size: 13px;
  color: #89765d;
}
.paper-heading {
  padding: 30px 34px 12px;
}
.paper-heading h2 {
  font-size: 25px;
  line-height: 1.65;
  margin: 10px 0 8px;
  overflow-wrap: anywhere;
}
.paper-heading p {
  color: #9a8b7b;
  line-height: 1.8;
  font-size: 13px;
}
.source-editor {
  display: block;
  width: 100%;
  min-height: 490px;
  height: 54vh;
  resize: vertical;
  border: 0;
  outline: 0;
  background: transparent;
  color: #514235;
  font-family: var(--font-body);
  font-size: 15px;
  line-height: 30px;
  padding: 14px 34px 30px;
  tab-size: 2;
}
.source-editor:focus {
  box-shadow: inset 3px 0 #cbb494;
}
.article-preview {
  padding: 14px 34px 30px;
  min-height: 490px;
  max-height: 70vh;
  overflow: auto;
}
.editor-footer {
  border-top: 1px solid #eee6da;
  padding: 12px 20px;
  display: flex;
  gap: 10px;
  justify-content: space-between;
  font-size: 10px;
  color: #9c8c78;
  flex-wrap: wrap;
}
.version-note {
  padding: 14px 20px;
  background: #fcf9f3;
  display: flex;
  gap: 12px;
  align-items: center;
  font-size: 11px;
  color: #99856d;
}
.version-note label {
  flex-shrink: 0;
}
.version-note input {
  width: 100%;
  min-width: 0;
  border: 0;
  background: transparent;
  outline: 0;
  color: #6a5842;
}
.assistant-panel {
  padding: 22px 20px 18px;
}
.assistant-heading {
  display: flex;
  gap: 12px;
  align-items: center;
}
.assistant-heading > span {
  font-size: 20px;
  background: #f1e6d5;
  padding: 10px;
  border-radius: 10px;
  color: #8b6f47;
}
.assistant-heading strong,
.assistant-heading small {
  display: block;
}
.assistant-heading strong {
  font-size: 15px;
}
.assistant-heading small {
  font-size: 10px;
  margin-top: 4px;
  color: #a0907b;
}
.field-label {
  display: block;
  font-size: 11px;
  color: #99846a;
  margin: 12px 0 8px;
}
.selection-box {
  border: 1px dashed #dfd1bd;
  border-radius: 10px;
  padding: 0 14px 4px;
  background: #fbf7ef;
}
.selection-box p {
  white-space: pre-wrap;
  max-height: 140px;
  overflow: auto;
  font-size: 12px;
  line-height: 1.9;
  color: #7c6950;
}
.presets {
  display: flex;
  flex-wrap: wrap;
  gap: 7px;
  margin-top: 18px;
}
.presets button {
  background: #fcf8f1;
  color: #867055;
  border: 1px solid #e9dfd0;
  border-radius: 6px;
  padding: 6px 10px;
  font-size: 11px;
  cursor: pointer;
}
.presets button.active {
  background: #eee2cf;
  border-color: #b49b77;
  color: #6f502a;
}
.generate {
  margin: 16px 0;
}
.comparison {
  font-size: 12px;
  white-space: pre-wrap;
  line-height: 1.8;
}
.comparison p {
  max-height: 180px;
  overflow: auto;
}
.comparison del {
  background: #fae6df;
  color: #9e5543;
}
.comparison ins {
  background: #e4eddb;
  color: #496537;
  text-decoration: none;
}
.result-actions {
  display: flex;
  gap: 8px;
  margin-top: 14px;
  flex-wrap: wrap;
}
.assistant-footnote {
  border-top: 1px solid #eee6da;
  padding-top: 18px;
  margin-top: 20px;
  text-align: center;
  font-size: 10px;
  color: #b19e84;
}
.channel-picker {
  margin: 5px 0 18px;
}
.empty-hint {
  color: #a18f77;
  font-size: 12px;
  padding: 12px 8px;
  line-height: 1.9;
}
.page-state {
  padding: 150px 0;
  text-align: center;
}
.history-layout {
  display: grid;
  grid-template-columns: 220px minmax(0, 1fr);
  gap: 20px;
}
.history-list {
  max-height: 60vh;
  overflow: auto;
}
.history-list button {
  display: flex;
  flex-direction: column;
  text-align: left;
  gap: 7px;
  width: 100%;
  padding: 14px;
  margin-bottom: 8px;
  border: 1px solid #e7dfd2;
  border-radius: 8px;
  background: #fffdf9;
  cursor: pointer;
  color: #76644d;
  overflow-wrap: anywhere;
}
.history-list button.active {
  border-color: #a4875f;
  background: #f7eddd;
}
.history-list small {
  color: #a18f77;
  font-size: 10px;
}
.history-content {
  max-height: 60vh;
  overflow: auto;
  padding: 0 8px;
}
.history-action {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
  margin-bottom: 20px;
}
@media (max-width: 1180px) {
  .workspace {
    grid-template-columns: minmax(320px, 1fr) 310px;
  }
  .outline-panel {
    display: none;
  }
  .workbench-header {
    align-items: flex-start;
    flex-direction: column;
  }
}
@media (max-width: 760px) {
  .workbench {
    padding: 18px 12px;
  }
  .workspace {
    grid-template-columns: minmax(0, 1fr);
  }
  .toolbar {
    gap: 6px;
  }
  .save-state {
    width: 100%;
  }
  .paper-heading {
    padding: 24px 20px 8px;
  }
  .source-editor,
  .article-preview {
    padding: 12px 20px;
  }
  .history-layout {
    grid-template-columns: 1fr;
  }
  .history-list {
    max-height: 180px;
  }
  .history-content {
    max-height: 45vh;
  }
  .paper-heading h2 {
    font-size: 22px;
  }
}
</style>
