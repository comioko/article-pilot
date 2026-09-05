<template>
  <div class="article-detail-page">
    <div class="page-header">
      <div class="header-container">
        <div class="header-actions">
          <a-button @click="goBack" class="back-btn">
            <template #icon>
              <ArrowLeftOutlined />
            </template>
            返回
          </a-button>
          <div class="right-actions">
            <a-button v-if="article?.status === 'COMPLETED'" type="primary" @click="router.push(`/article/${article.taskId}/workbench`)">
              <EditOutlined /> 创作工作台
            </a-button>
            <a-button
              v-if="article?.status === 'FAILED'"
              type="primary"
              danger
              @click="handleRetry"
              class="retry-btn"
            >
              <template #icon>
                <RedoOutlined />
              </template>
              重新创建
            </a-button>
            <a-button type="primary" @click="exportMarkdown" class="export-btn">
              <template #icon>
                <DownloadOutlined />
              </template>
              导出 Markdown
            </a-button>
            <a-button v-if="article?.fullContent || article?.content" @click="openRefineModal">
              <template #icon><EditOutlined /></template>
              AI 精修
            </a-button>
            <a-button v-if="article?.fullContent || article?.content" @click="openRevisionModal">
              <template #icon><HistoryOutlined /></template>
              历史版本
            </a-button>
            <a-button v-if="article?.fullContent || article?.content" @click="openPublishModal">
              <template #icon><SendOutlined /></template>
              发布包
            </a-button>
          </div>
        </div>
      </div>
    </div>

    <div class="container">
      <a-spin :spinning="loading" tip="加载中...">
        <a-card :bordered="false" v-if="article" class="article-card">
          <!-- 标题 -->
          <div class="title-section">
            <h1 class="main-title">{{ article.mainTitle }}</h1>
            <p class="sub-title">{{ article.subTitle }}</p>
            <div class="meta-info">
              <a-tag :color="getStatusColor(article.status ?? '')" class="status-tag">
                {{ getStatusText(article.status ?? '') }}
              </a-tag>
              <span class="time">创建于 {{ article.createTime ? formatDate(article.createTime) : '' }}</span>
            </div>
          </div>

          <a-divider />

          <!-- 执行日志面板 -->
          <div v-if="executionStats && executionStats.logs && executionStats.logs.length > 0" class="execution-logs-section">
            <div class="logs-header" @click="showExecutionLogs = !showExecutionLogs">
              <h2 class="section-title">
                <ClockCircleOutlined class="section-icon" />
                执行日志
                <a-tag :color="getStatusColor(executionStats.overallStatus ?? '')" class="status-tag-small">
                  {{ executionStats.overallStatus ?? '' }}
                </a-tag>
              </h2>
              <ThunderboltOutlined :class="['toggle-icon', { expanded: showExecutionLogs }]" />
            </div>

            <Transition name="expand">
              <div v-show="showExecutionLogs" class="logs-content">
                <!-- 统计概览 -->
                <div class="stats-summary">
                  <div class="stat-item">
                    <span class="label">总耗时</span>
                    <span class="value">{{ executionStats.totalDurationMs ?? 0 }}ms</span>
                  </div>
                  <div class="stat-item">
                    <span class="label">智能体数量</span>
                    <span class="value">{{ executionStats.agentCount ?? 0 }}</span>
                  </div>
                  <div class="stat-item">
                    <span class="label">平均耗时</span>
                    <span class="value">
                      {{ executionStats.agentCount && executionStats.totalDurationMs ? Math.round(executionStats.totalDurationMs / executionStats.agentCount) : 0 }}ms
                    </span>
                  </div>
                </div>

                <!-- 智能体时间线 -->
                <div class="agent-timeline">
                  <div
                    v-for="log in executionStats.logs"
                    :key="log.id"
                    :class="['timeline-item', log.status?.toLowerCase()]"
                  >
                    <div class="timeline-indicator">
                      <CheckCircleOutlined v-if="log.status === 'SUCCESS'" class="icon success" />
                      <CloseCircleOutlined v-else-if="log.status === 'FAILED'" class="icon failed" />
                      <LoadingOutlined v-else class="icon running" />
                    </div>
                    <div class="timeline-content">
                      <div class="timeline-header">
                        <span class="agent-name">{{ getAgentDisplayName(log.agentName ?? '') }}</span>
                        <span class="duration">{{ log.durationMs ?? 0 }}ms</span>
                      </div>
                      <div class="timeline-time">
                        {{ log.startTime ? formatDate(log.startTime) : '' }}
                      </div>
                      <div v-if="log.errorMessage" class="error-message">
                        <CloseCircleOutlined /> {{ log.errorMessage }}
                      </div>
                    </div>
                  </div>
                </div>
              </div>
            </Transition>
          </div>

          <a-divider v-if="executionStats && executionStats.logs && executionStats.logs.length > 0" />

          <!-- 大纲 -->
          <div v-if="article.outline && article.outline.length > 0" class="outline-section">
            <h2 class="section-title">
              <OrderedListOutlined class="section-icon" />
              文章大纲
            </h2>
            <div class="outline-list">
              <div v-for="item in article.outline" :key="item.section" class="outline-item">
                <div class="outline-title">{{ item.section }}. {{ item.title }}</div>
                <ul class="outline-points">
                  <li v-for="(point, idx) in item.points" :key="idx">{{ point }}</li>
                </ul>
              </div>
            </div>
          </div>

          <a-divider v-if="article.outline && article.outline.length > 0" />

          <!-- 完整图文（优先展示） -->
          <div v-if="article.fullContent" class="content-section">
            <h2 class="section-title">
              <FileTextOutlined class="section-icon" />
              完整图文
            </h2>
            <div ref="articleContentRef" v-html="markdownToHtml(article.fullContent)" class="markdown-content" @mouseup="captureSelection"></div>
          </div>

          <!-- 普通正文（无 fullContent 时展示） -->
          <div v-else-if="article.content" class="content-section">
            <h2 class="section-title">
              <FileTextOutlined class="section-icon" />
              文章正文
            </h2>
            <div ref="articleContentRef" v-html="markdownToHtml(article.content)" class="markdown-content" @mouseup="captureSelection"></div>
          </div>

          <!-- 配图（仅在没有 fullContent 时单独展示） -->
          <div v-if="!article.fullContent && article.images && article.images.length > 0" class="images-section">
            <h2 class="section-title">
              <PictureOutlined class="section-icon" />
              文章配图
            </h2>
            <div class="images-grid">
              <div v-for="image in article.images" :key="image.position" class="image-item">
                <img :src="image.url" :alt="image.description" />
                <div class="image-info">
                  <span class="badge">{{ image.method }}</span>
                  <span class="keywords">{{ image.keywords }}</span>
                </div>
              </div>
            </div>
          </div>
        </a-card>
      </a-spin>
    </div>

    <a-modal
      v-model:open="refineModalOpen"
      title="AI 精修选中内容"
      :confirm-loading="refining || savingRefinement"
      :ok-text="refinedText ? '确认替换并保存版本' : '生成精修建议'"
      :ok-button-props="{ disabled: !selectedText }"
      cancel-text="取消"
      width="720px"
      @ok="refinedText ? applyRefinement() : generateRefinement()"
    >
      <p class="refine-tip">请先在正文中用鼠标选中一个段落，再选择精修方式。确认替换后会自动保留历史版本。</p>
      <div class="selected-text">{{ selectedText || '尚未选择内容' }}</div>
      <a-radio-group v-model:value="refineInstruction" class="refine-actions">
        <a-radio-button v-for="item in refinePresets" :key="item" :value="item">{{ item }}</a-radio-button>
      </a-radio-group>
      <a-textarea v-model:value="customInstruction" :rows="2" placeholder="或输入更具体的精修要求（填写后会优先使用）" />
      <template v-if="refinedText">
        <a-divider>精修预览（可直接修改）</a-divider>
        <a-textarea v-model:value="refinedText" :rows="10" />
      </template>
    </a-modal>

    <a-modal v-model:open="revisionModalOpen" title="历史版本" :footer="null" width="760px">
      <a-spin :spinning="revisionsLoading">
        <a-empty v-if="!revisions.length" description="尚未保存精修版本" />
        <a-list v-else :data-source="revisions" item-layout="vertical">
          <template #renderItem="{ item }">
            <a-list-item>
              <template #actions>
                <a-button type="link" @click="restoreSelectedRevision(item)">恢复此版本</a-button>
              </template>
              <a-list-item-meta>
                <template #title>版本 {{ item.revisionNumber }} · {{ item.revisionNote || '未命名版本' }}</template>
                <template #description>{{ item.createTime ? formatDate(item.createTime) : '' }}</template>
              </a-list-item-meta>
              <div class="revision-preview">{{ getRevisionPreview(item) }}</div>
            </a-list-item>
          </template>
        </a-list>
      </a-spin>
    </a-modal>

    <a-modal
      v-model:open="publishModalOpen"
      title="生成多平台发布稿"
      :confirm-loading="publishing"
      ok-text="生成发布稿"
      cancel-text="取消"
      width="760px"
      @ok="generatePackage"
    >
      <p class="refine-tip">发布稿独立生成，不会覆盖你的原始文章。</p>
      <a-radio-group v-model:value="publishChannel" class="publish-channels">
        <a-radio-button value="WECHAT">微信公众号</a-radio-button>
        <a-radio-button value="XIAOHONGSHU">小红书</a-radio-button>
      </a-radio-group>
      <template v-if="publishPackage">
        <a-divider>发布预览</a-divider>
        <a-textarea :value="publishPackage.content" :rows="14" readonly />
        <div class="publish-actions">
          <a-button @click="copyPublishPackage">复制内容</a-button>
          <a-button type="primary" @click="downloadPublishPackage">下载发布稿</a-button>
        </div>
      </template>
    </a-modal>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { message, Modal } from 'ant-design-vue'
import {
  ArrowLeftOutlined,
  DownloadOutlined,
  OrderedListOutlined,
  FileTextOutlined,
  PictureOutlined,
  ClockCircleOutlined,
  CheckCircleOutlined,
  CloseCircleOutlined,
  LoadingOutlined,
  RedoOutlined,
  ThunderboltOutlined,
  EditOutlined,
  HistoryOutlined,
  SendOutlined
} from '@ant-design/icons-vue'
import { aiRefineContent, generatePublishPackage, getArticle, getExecutionLogs, listRevisions, restoreRevision, saveRevision } from '@/api/articleController'
import { markdownToHtml } from '@/utils/markdown'
import dayjs from 'dayjs'

const router = useRouter()
const route = useRoute()

const loading = ref(false)
const article = ref<API.ArticleVO | null>(null)
const executionStats = ref<API.AgentExecutionStats | null>(null)
const logsLoading = ref(false)
const showExecutionLogs = ref(false)
const articleContentRef = ref<HTMLElement | null>(null)
const selectedText = ref('')
const refineModalOpen = ref(false)
const refineInstruction = ref('更精炼')
const customInstruction = ref('')
const refinedText = ref('')
const refining = ref(false)
const savingRefinement = ref(false)
const revisionModalOpen = ref(false)
const revisions = ref<API.ArticleRevisionVO[]>([])
const revisionsLoading = ref(false)
const refinePresets = ['更精炼', '更专业', '更口语化', '扩充细节', '增强感染力']
const publishModalOpen = ref(false)
const publishChannel = ref<'WECHAT' | 'XIAOHONGSHU'>('WECHAT')
const publishing = ref(false)
const publishPackage = ref<API.ArticlePublishPackageVO | null>(null)


// 加载文章
const loadArticle = async () => {
  const taskId = route.params.taskId as string
  if (!taskId) {
    message.error('文章ID不存在')
    return
  }

  loading.value = true
  try {
    const res = await getArticle({ taskId })
    article.value = res.data.data || null
    // 自动加载执行日志
    await loadExecutionLogs(taskId)
  } catch (error) {
    message.error((error as Error).message || '加载失败')
  } finally {
    loading.value = false
  }
}

// 加载执行日志
const loadExecutionLogs = async (taskId: string) => {
  logsLoading.value = true
  try {
    const res = await getExecutionLogs({ taskId })
    executionStats.value = res.data.data || null
  } catch (error) {
    console.error('加载执行日志失败:', error)
  } finally {
    logsLoading.value = false
  }
}

// 返回
const goBack = () => {
  router.back()
}

const captureSelection = () => {
  const selection = window.getSelection()
  if (!selection || selection.rangeCount === 0 || !articleContentRef.value) return
  const range = selection.getRangeAt(0)
  if (!articleContentRef.value.contains(range.commonAncestorContainer)) return
  const value = selection.toString().trim()
  if (value) selectedText.value = value
}

const openRefineModal = () => {
  captureSelection()
  if (!selectedText.value) {
    message.info('请先在正文区域选中需要精修的一段内容')
    return
  }
  refinedText.value = ''
  customInstruction.value = ''
  refineModalOpen.value = true
}

const getRefineInstruction = () => customInstruction.value.trim() || refineInstruction.value

const generateRefinement = async () => {
  if (!article.value?.taskId) return
  refining.value = true
  try {
    const res = await aiRefineContent({
      taskId: article.value.taskId,
      selectedText: selectedText.value,
      instruction: getRefineInstruction()
    })
    refinedText.value = res.data.data || ''
    if (!refinedText.value) message.error('未生成精修内容，请重试')
  } catch (error) {
    message.error((error as Error).message || '精修生成失败')
  } finally {
    refining.value = false
  }
}

const replaceFirst = (source: string | undefined, target: string, replacement: string) => {
  if (!source) return source || ''
  const index = source.indexOf(target)
  return index < 0 ? source : `${source.slice(0, index)}${replacement}${source.slice(index + target.length)}`
}

const applyRefinement = async () => {
  if (!article.value?.taskId || !refinedText.value.trim()) return
  const oldContent = article.value.content || ''
  const oldFullContent = article.value.fullContent || ''
  const newContent = replaceFirst(oldContent, selectedText.value, refinedText.value.trim())
  const newFullContent = replaceFirst(oldFullContent, selectedText.value, refinedText.value.trim())
  if ((oldContent || oldFullContent) && newContent === oldContent && newFullContent === oldFullContent) {
    message.error('未能在原始 Markdown 中定位选中文本，请选中单个完整段落后重试')
    return
  }

  savingRefinement.value = true
  try {
    const res = await saveRevision({
      taskId: article.value.taskId,
      content: newContent,
      fullContent: newFullContent,
      revisionNote: `AI 精修：${getRefineInstruction()}`
    })
    article.value = res.data.data || article.value
    refineModalOpen.value = false
    selectedText.value = ''
    message.success('精修已保存，可在历史版本中随时恢复')
  } catch (error) {
    message.error((error as Error).message || '保存精修版本失败')
  } finally {
    savingRefinement.value = false
  }
}

const loadRevisions = async () => {
  if (!article.value?.taskId) return
  revisionsLoading.value = true
  try {
    const res = await listRevisions({ taskId: article.value.taskId })
    revisions.value = res.data.data || []
  } catch (error) {
    message.error((error as Error).message || '加载历史版本失败')
  } finally {
    revisionsLoading.value = false
  }
}

const openRevisionModal = async () => {
  revisionModalOpen.value = true
  await loadRevisions()
}

const getRevisionPreview = (revision: API.ArticleRevisionVO) => {
  const text = revision.fullContent || revision.content || ''
  return text.replace(/[#*_>`\[\]()!]/g, '').replace(/\s+/g, ' ').slice(0, 160) || '空内容'
}

const restoreSelectedRevision = (revision: API.ArticleRevisionVO) => {
  if (!article.value?.taskId || !revision.id) return
  Modal.confirm({
    title: `恢复版本 ${revision.revisionNumber}`,
    content: '当前内容会先自动备份为新版本，确认恢复吗？',
    okText: '恢复',
    cancelText: '取消',
    onOk: async () => {
      const res = await restoreRevision({ taskId: article.value?.taskId, revisionId: revision.id })
      article.value = res.data.data || article.value
      message.success('已恢复历史版本')
      await loadRevisions()
    }
  })
}

const openPublishModal = () => {
  publishPackage.value = null
  publishModalOpen.value = true
}

const generatePackage = async () => {
  if (!article.value?.taskId) return
  publishing.value = true
  try {
    const res = await generatePublishPackage({
      taskId: article.value.taskId,
      channel: publishChannel.value
    })
    publishPackage.value = res.data.data || null
    if (!publishPackage.value) message.error('未生成发布稿，请重试')
  } catch (error) {
    message.error((error as Error).message || '生成发布稿失败')
  } finally {
    publishing.value = false
  }
}

const copyPublishPackage = async () => {
  if (!publishPackage.value?.content) return
  try {
    await navigator.clipboard.writeText(publishPackage.value.content)
    message.success('发布稿已复制')
  } catch {
    message.error('复制失败，请手动复制内容')
  }
}

const downloadPublishPackage = () => {
  if (!publishPackage.value?.content) return
  const channelName = publishChannel.value === 'WECHAT' ? '微信公众号' : '小红书'
  const blob = new Blob([publishPackage.value.content], { type: 'text/plain;charset=utf-8' })
  const url = URL.createObjectURL(blob)
  const a = document.createElement('a')
  a.href = url
  a.download = `${article.value?.mainTitle || '文章'}-${channelName}.txt`
  a.click()
  URL.revokeObjectURL(url)
}

// 导出 Markdown
const exportMarkdown = () => {
  if (!article.value) return

  let markdown = `# ${article.value.mainTitle}\n\n`
  markdown += `> ${article.value.subTitle}\n\n`

  // 优先使用完整图文
  if (article.value.fullContent) {
    markdown += article.value.fullContent
  } else {
    if (article.value.outline && article.value.outline.length > 0) {
      markdown += `## 目录\n\n`
      article.value.outline.forEach(item => {
        markdown += `${item.section}. ${item.title}\n`
      })
      markdown += `\n---\n\n`
    }

    markdown += article.value.content || ''

    if (article.value.images && article.value.images.length > 0) {
      markdown += `\n\n## 配图\n\n`
      article.value.images.forEach(image => {
        markdown += `![${image.description}](${image.url})\n\n`
      })
    }
  }

  const blob = new Blob([markdown], { type: 'text/markdown' })
  const url = URL.createObjectURL(blob)
  const a = document.createElement('a')
  a.href = url
  a.download = `${article.value.mainTitle}.md`
  a.click()
  URL.revokeObjectURL(url)

  message.success('导出成功')
}

// 格式化日期
const formatDate = (date: string) => {
  return dayjs(date).format('YYYY-MM-DD HH:mm:ss')
}

// 获取状态颜色
const getStatusColor = (status: string) => {
  const colorMap: Record<string, string> = {
    PENDING: 'default',
    PROCESSING: 'processing',
    COMPLETED: 'success',
    FAILED: 'error',
  }
  return colorMap[status] || 'default'
}

// 获取状态文本
const getStatusText = (status: string) => {
  const textMap: Record<string, string> = {
    PENDING: '等待中',
    PROCESSING: '生成中',
    COMPLETED: '已完成',
    FAILED: '失败',
  }
  return textMap[status] || status
}

// 获取智能体显示名称
const getAgentDisplayName = (agentName: string) => {
  const nameMap: Record<string, string> = {
    'agent1_generate_titles': '生成标题',
    'agent2_generate_outline': '生成大纲',
    'agent3_generate_content': '生成正文',
    'agent4_analyze_image_requirements': '分析配图需求',
    'agent5_generate_images': '生成配图',
    'agent6_merge_content': '图文合成',
    'ai_modify_outline': 'AI修改大纲'
  }
  return nameMap[agentName] || agentName
}

// 重试（重新创建文章）
const handleRetry = () => {
  if (!article.value) return

  Modal.confirm({
    title: '确认重试',
    content: '将使用相同的选题和配置重新创建文章，是否继续？',
    okText: '确认',
    cancelText: '取消',
    onOk: () => {
      router.push({
        path: '/create',
        query: {
          topic: article.value?.topic
        }
      })
    }
  })
}

onMounted(() => {
  loadArticle()
})
</script>

<style scoped lang="scss">
.article-detail-page {
  background: var(--color-background-secondary);
  min-height: 100vh;
  padding-bottom: 60px;

  .page-header {
    background: var(--gradient-hero);
    padding: 20px;
    margin-bottom: 24px;
  }

  .header-container {
    max-width: 1200px;
    margin: 0 auto;
  }

  .header-actions {
    display: flex;
    justify-content: space-between;
    align-items: center;
  }

  .right-actions {
    display: flex;
    gap: 12px;
    flex-wrap: wrap;
  }

  .back-btn {
    background: white;
    border: 1px solid var(--color-border);
    color: var(--color-text);
    font-size: 13px;
    transition: all var(--transition-fast);
    border-radius: var(--radius-md);

    &:hover {
      background: var(--color-background-secondary);
      border-color: var(--color-border);
      color: var(--color-text);
    }
  }

  .retry-btn {
    background: #ff4d4f;
    color: white;
    border: none;
    font-weight: 600;
    font-size: 13px;
    transition: all var(--transition-fast);
    border-radius: var(--radius-md);

    &:hover {
      opacity: 0.9;
      transform: translateY(-1px);
    }
  }

  .export-btn {
    background: var(--gradient-primary);
    color: white;
    border: none;
    font-weight: 600;
    font-size: 13px;
    transition: all var(--transition-fast);
    border-radius: var(--radius-md);
    box-shadow: var(--shadow-green);

    &:hover {
      opacity: 0.9;
      transform: translateY(-1px);
    }
  }

  .refine-tip {
    color: var(--color-text-secondary);
    font-size: 13px;
  }

  .selected-text {
    max-height: 120px;
    overflow: auto;
    padding: 12px;
    margin: 12px 0;
    border-radius: var(--radius-md);
    background: var(--color-background-secondary);
    color: var(--color-text-secondary);
    white-space: pre-wrap;
  }

  .refine-actions {
    display: flex;
    flex-wrap: wrap;
    gap: 8px;
    margin: 0 0 12px;
  }

  .revision-preview {
    color: var(--color-text-secondary);
    line-height: 1.7;
  }

  .publish-channels {
    margin: 8px 0 4px;
  }

  .publish-actions {
    display: flex;
    justify-content: flex-end;
    gap: 10px;
    margin-top: 12px;
  }

  .container {
    max-width: 1200px;
    margin: 0 auto;
    padding: 0 20px;
  }

  .article-card {
    border-radius: var(--radius-xl);
    border: 1px solid var(--color-border);
    box-shadow: var(--shadow-md);
    background: white;

    :deep(.ant-card-body) {
      padding: 40px;
    }
  }

  .title-section {
    margin-bottom: 28px;
    text-align: center;

    .main-title {
      font-size: 28px;
      font-weight: 700;
      margin: 0 0 10px;
      color: var(--color-text);
      line-height: 1.3;
      letter-spacing: -0.5px;
    }

    .sub-title {
      font-size: 16px;
      color: var(--color-text-secondary);
      margin: 0 0 20px;
    }

    .meta-info {
      display: flex;
      align-items: center;
      justify-content: center;
      gap: 12px;
      color: var(--color-text-muted);
      font-size: 13px;
    }

    .status-tag {
      border-radius: var(--radius-full);
      font-size: 12px;
      padding: 2px 12px;
    }
  }

  .section-title {
    display: flex;
    align-items: center;
    gap: 8px;
    font-size: 16px;
    font-weight: 600;
    margin-bottom: 16px;
    color: var(--color-text);
  }

  .section-icon {
    font-size: 18px;
    color: var(--color-text-secondary);
  }

  .status-tag-small {
    font-size: 11px;
    padding: 2px 8px;
    margin-left: 8px;
  }

  /* 执行日志部分 */
  .execution-logs-section {
    margin-bottom: 28px;
    background: var(--color-background-secondary);
    border-radius: var(--radius-lg);
    border: 1px solid var(--color-border);
    overflow: hidden;

    .logs-header {
      padding: 16px 20px;
      display: flex;
      justify-content: space-between;
      align-items: center;
      cursor: pointer;
      transition: background var(--transition-fast);

      &:hover {
        background: rgba(0, 0, 0, 0.02);
      }

      .section-title {
        margin: 0;
        display: flex;
        align-items: center;
      }

      .toggle-icon {
        font-size: 14px;
        color: var(--color-text-secondary);
        transition: transform var(--transition-fast);

        &.expanded {
          transform: rotate(180deg);
        }
      }
    }

    .logs-content {
      padding: 0 20px 20px;
    }

    .stats-summary {
      display: grid;
      grid-template-columns: repeat(3, 1fr);
      gap: 16px;
      margin-bottom: 24px;
      padding: 16px;
      background: white;
      border-radius: var(--radius-md);
      border: 1px solid var(--color-border-light);

      .stat-item {
        text-align: center;

        .label {
          display: block;
          font-size: 12px;
          color: var(--color-text-muted);
          margin-bottom: 4px;
        }

        .value {
          display: block;
          font-size: 20px;
          font-weight: 600;
          color: var(--color-primary);
        }
      }
    }

    .agent-timeline {
      position: relative;

      &::before {
        content: '';
        position: absolute;
        left: 16px;
        top: 12px;
        bottom: 12px;
        width: 2px;
        background: var(--color-border);
      }

      .timeline-item {
        position: relative;
        padding-left: 48px;
        padding-bottom: 20px;

        &:last-child {
          padding-bottom: 0;
        }

        .timeline-indicator {
          position: absolute;
          left: 8px;
          top: 2px;
          width: 20px;
          height: 20px;
          border-radius: 50%;
          background: white;
          display: flex;
          align-items: center;
          justify-content: center;
          border: 2px solid var(--color-border);

          .icon {
            font-size: 12px;

            &.success {
              color: var(--color-success);
            }

            &.failed {
              color: var(--color-error);
            }

            &.running {
              color: var(--color-primary);
            }
          }
        }

        &.success .timeline-indicator {
          border-color: var(--color-success);
        }

        &.failed .timeline-indicator {
          border-color: var(--color-error);
        }

        .timeline-content {
          background: white;
          padding: 12px 16px;
          border-radius: var(--radius-md);
          border: 1px solid var(--color-border-light);

          .timeline-header {
            display: flex;
            justify-content: space-between;
            align-items: center;
            margin-bottom: 4px;

            .agent-name {
              font-size: 14px;
              font-weight: 600;
              color: var(--color-text);
            }

            .duration {
              font-size: 13px;
              font-weight: 600;
              color: var(--color-primary);
            }
          }

          .timeline-time {
            font-size: 12px;
            color: var(--color-text-muted);
          }

          .error-message {
            margin-top: 8px;
            padding: 8px;
            background: rgba(255, 77, 79, 0.1);
            border-radius: var(--radius-md);
            font-size: 12px;
            color: var(--color-error);
            display: flex;
            align-items: flex-start;
            gap: 6px;

            .anticon {
              flex-shrink: 0;
              margin-top: 2px;
            }
          }
        }
      }
    }
  }

  /* 展开/收起动画 */
  .expand-enter-active,
  .expand-leave-active {
    transition: all 0.3s ease;
    overflow: hidden;
  }

  .expand-enter-from,
  .expand-leave-to {
    opacity: 0;
    max-height: 0;
  }

  .expand-enter-to,
  .expand-leave-from {
    opacity: 1;
    max-height: 2000px;
  }

  .outline-section {
    margin-bottom: 28px;

    .outline-list {
      .outline-item {
        margin-bottom: 12px;
        padding: 16px;
        background: var(--color-background-secondary);
        border-radius: var(--radius-md);
        border: 1px solid var(--color-border-light);
        transition: all var(--transition-fast);

        &:hover {
          border-color: var(--color-border);
        }

        .outline-title {
          font-size: 14px;
          font-weight: 600;
          margin-bottom: 8px;
          color: var(--color-text);
        }

        .outline-points {
          margin: 0;
          padding-left: 18px;

          li {
            margin-bottom: 4px;
            color: var(--color-text-secondary);
            line-height: 1.6;
            font-size: 13px;
          }
        }
      }
    }
  }

  .content-section {
    margin-bottom: 28px;

    .markdown-content {
      line-height: 1.8;
      font-size: 15px;
      color: var(--color-text);

      :deep(h2) {
        font-size: 20px;
        font-weight: 600;
        margin: 28px 0 14px;
        padding-bottom: 10px;
        border-bottom: 1px solid var(--color-border);
        color: var(--color-text);
      }

      :deep(h3) {
        font-size: 17px;
        font-weight: 600;
        margin: 22px 0 10px;
        color: var(--color-text);
      }

      :deep(p) {
        margin-bottom: 14px;
        text-indent: 2em;
        color: var(--color-text);
      }

      :deep(ul), :deep(ol) {
        margin-bottom: 14px;
        padding-left: 2em;
      }

      :deep(li) {
        margin-bottom: 6px;
        color: var(--color-text);
      }

      :deep(img) {
        display: block;
        max-width: 100%;
        max-height: 600px;
        width: auto;
        height: auto;
        margin: 20px auto;
        border-radius: var(--radius-md);
        box-shadow: var(--shadow-md);
        object-fit: contain;
      }

      // Mermaid 图表特殊处理（SVG 格式）
      :deep(img[src$=".svg"]) {
        max-width: 800px;
        max-height: 500px;
      }
    }
  }

  .images-section {
    .images-grid {
      display: grid;
      grid-template-columns: repeat(auto-fill, minmax(240px, 1fr));
      gap: 16px;

      .image-item {
        border-radius: var(--radius-md);
        overflow: hidden;
        border: 1px solid var(--color-border);
        transition: all var(--transition-normal);
        cursor: pointer;

        &:hover {
          border-color: var(--color-text-muted);
          box-shadow: var(--shadow-md);
        }

        img {
          width: 100%;
          height: 160px;
          object-fit: cover;
        }

        .image-info {
          padding: 12px;
          background: white;
          display: flex;
          justify-content: space-between;
          align-items: center;

          .badge {
            padding: 3px 10px;
            background: var(--color-text);
            color: white;
            border-radius: var(--radius-md);
            font-size: 11px;
            font-weight: 500;
          }

          .keywords {
            font-size: 11px;
            color: var(--color-text-muted);
          }
        }
      }
    }
  }
}

@media (max-width: 768px) {
  .article-detail-page {
    .article-card {
      :deep(.ant-card-body) {
        padding: 24px;
      }
    }

    .title-section {
      .main-title {
        font-size: 22px;
      }

      .sub-title {
        font-size: 14px;
      }
    }
  }
}
</style>
