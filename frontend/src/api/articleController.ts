// @ts-ignore
/* eslint-disable */
import request from '@/request'

/** 获取文章详情 GET /article/${param0} */
export async function getArticle(
  // 叠加生成的Param类型 (非body参数swagger默认没有生成对象)
  params: API.getArticleParams,
  options?: { [key: string]: any }
) {
  const { taskId: param0, ...queryParams } = params
  return request<API.BaseResponseArticleVO>(`/article/${param0}`, {
    method: 'GET',
    params: { ...queryParams },
    ...(options || {}),
  })
}

/** AI 修改大纲 POST /article/ai-modify-outline */
export async function aiModifyOutline(
  body: API.ArticleAiModifyOutlineRequest,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponseListOutlineSection>('/article/ai-modify-outline', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
    },
    data: body,
    ...(options || {}),
  })
}

/** AI 精修选中内容 POST /article/ai-refine-content */
export async function aiRefineContent(
  body: API.ArticleAiRefineRequest,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponseString>('/article/ai-refine-content', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    data: body,
    ...(options || {}),
  })
}

/** 保存文章精修版本 POST /article/save-revision */
export async function saveRevision(
  body: API.ArticleSaveRevisionRequest,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponseArticleVO>('/article/save-revision', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    data: body,
    ...(options || {}),
  })
}

/** 查询文章历史版本 GET /article/revisions/${param0} */
export async function listRevisions(
  params: API.listRevisionsParams,
  options?: { [key: string]: any }
) {
  const { taskId: param0, ...queryParams } = params
  return request<API.BaseResponseListArticleRevisionVO>(`/article/revisions/${param0}`, {
    method: 'GET',
    params: { ...queryParams },
    ...(options || {}),
  })
}

/** 恢复文章历史版本 POST /article/restore-revision */
export async function restoreRevision(
  body: API.ArticleRestoreRevisionRequest,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponseArticleVO>('/article/restore-revision', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    data: body,
    ...(options || {}),
  })
}

/** 生成多平台发布稿 POST /article/publish-package */
export async function generatePublishPackage(
  body: API.ArticlePublishPackageRequest,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponseArticlePublishPackageVO>('/article/publish-package', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    data: body,
    ...(options || {}),
  })
}

/** 确认大纲 POST /article/confirm-outline */
export async function confirmOutline(
  body: API.ArticleConfirmOutlineRequest,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponseVoid>('/article/confirm-outline', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
    },
    data: body,
    ...(options || {}),
  })
}

/** 确认标题并输入补充描述 POST /article/confirm-title */
export async function confirmTitle(
  body: API.ArticleConfirmTitleRequest,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponseVoid>('/article/confirm-title', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
    },
    data: body,
    ...(options || {}),
  })
}

/** 创建文章任务 POST /article/create */
export async function createArticle(
  body: API.ArticleCreateRequest,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponseString>('/article/create', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
    },
    data: body,
    ...(options || {}),
  })
}

/** 删除文章 POST /article/delete */
export async function deleteArticle(body: API.DeleteRequest, options?: { [key: string]: any }) {
  return request<API.BaseResponseBoolean>('/article/delete', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
    },
    data: body,
    ...(options || {}),
  })
}

/** 获取任务执行日志 GET /article/execution-logs/${param0} */
export async function getExecutionLogs(
  // 叠加生成的Param类型 (非body参数swagger默认没有生成对象)
  params: API.getExecutionLogsParams,
  options?: { [key: string]: any }
) {
  const { taskId: param0, ...queryParams } = params
  return request<API.BaseResponseAgentExecutionStats>(`/article/execution-logs/${param0}`, {
    method: 'GET',
    params: { ...queryParams },
    ...(options || {}),
  })
}

/** 分页查询文章列表 POST /article/list */
export async function listArticle(body: API.ArticleQueryRequest, options?: { [key: string]: any }) {
  return request<API.BaseResponsePageArticleVO>('/article/list', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
    },
    data: body,
    ...(options || {}),
  })
}

/** 获取文章生成进度(SSE) GET /article/progress/${param0} */
export async function getProgress(
  // 叠加生成的Param类型 (非body参数swagger默认没有生成对象)
  params: API.getProgressParams,
  options?: { [key: string]: any }
) {
  const { taskId: param0, ...queryParams } = params
  return request<API.SseEmitter>(`/article/progress/${param0}`, {
    method: 'GET',
    params: { ...queryParams },
    ...(options || {}),
  })
}
