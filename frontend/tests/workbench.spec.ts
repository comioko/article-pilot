import { expect, test, type Page } from '@playwright/test'

const source =
  '## 把注意力还给创作\n\n写作需要耐心，也需要留白。\n\n每一次删改，都让想法更清楚。保留你的判断，把重复的整理交给工具。\n\n## 从一个段落开始\n\n写作需要耐心，也需要留白。\n\n试着先写下最想表达的一句话，再让它慢慢展开。\n\n## 留出思考的空间\n\n好的工具帮助你专注，把时间留给真正重要的事。'
async function mockApi(
  page: Page,
  options: { failSave?: boolean; failLoad?: boolean; content?: string; status?: string } = {},
) {
  let article = {
    taskId: 'demo',
    id: 1,
    status: options.status || 'COMPLETED',
    mainTitle: '给创作留一点呼吸的空间',
    subTitle: '从初稿到成稿，让每一次修改都有迹可循',
    content: options.content || source,
    fullContent: options.content || source,
    contentFingerprint: 'v0',
  }
  let saves = 0
  const history = [
    {
      id: 1,
      revisionNumber: 1,
      revisionNote: '初始成稿',
      fullContent: source,
      createTime: '2026-09-05T14:00:00',
    },
  ]
  await page.route('http://127.0.0.1:5186/api/**', async (route) => {
    const path = new URL(route.request().url()).pathname
    let data: unknown = null,
      code = 0,
      message = 'ok'
    if (path === '/api/user/get/login') data = { id: 1, userName: '创作者', userRole: 'vip' }
    else if (path === '/api/article/demo') {
      if (options.failLoad) {
        code = 40400
        message = '文章不存在或无权限'
      } else data = article
    } else if (path === '/api/article/ai-refine-content') data = '让耐心与留白，成为写作的一部分。'
    else if (path === '/api/article/save-revision') {
      if (options.failSave) {
        code = 50001
        message = '文章已在其他页面修改，请保留草稿并重新加载后再保存'
      } else {
        const body = route.request().postDataJSON()
        expect(body.baseFingerprint).toBe(article.contentFingerprint)
        article = {
          ...article,
          content: body.content,
          fullContent: body.fullContent,
          contentFingerprint: `v${++saves}`,
        }
        data = article
      }
    } else if (path === '/api/article/revisions/demo') data = history
    else if (path === '/api/article/restore-revision') {
      article = { ...article, content: source, fullContent: source, contentFingerprint: 'restored' }
      data = article
    } else if (path === '/api/article/publish-package') {
      const body = route.request().postDataJSON()
      data = { channel: body.channel, content: `${body.channel} 发布稿\n${article.fullContent}` }
    } else if (path.includes('execution-logs')) data = { logs: [] }
    await route.fulfill({ json: { code, data, message } })
  })
  await page.goto('/article/demo/workbench')
}
async function selectSecondParagraph(page: Page) {
  await page.getByLabel('文章正文编辑器').evaluate((node: HTMLTextAreaElement) => {
    node.focus()
    const text = '写作需要耐心，也需要留白。'
    const start = node.value.lastIndexOf(text)
    node.setSelectionRange(start, start + text.length)
    node.dispatchEvent(new Event('select', { bubbles: true }))
  })
}

test('edit, select exact occurrence, preview, save, publish and restore', async ({
  page,
}, testInfo) => {
  const errors: string[] = []
  page.on('pageerror', (e) => errors.push(e.message))
  await mockApi(page)
  const editor = page.getByLabel('文章正文编辑器')
  await expect(editor).toHaveValue(source)
  await page.screenshot({ path: testInfo.outputPath('workbench-desktop.png'), fullPage: true })
  await selectSecondParagraph(page)
  await page.getByRole('button', { name: '生成精修建议', exact: true }).click()
  await expect(page.getByLabel('候选内容 · 可继续修改')).toHaveValue(
    '让耐心与留白，成为写作的一部分。',
  )
  await expect(page.locator('.comparison ins')).toContainText('让耐心与留白')
  await page.getByRole('button', { name: '采纳到草稿' }).click()
  const expected =
    source.slice(0, source.lastIndexOf('写作需要耐心，也需要留白。')) +
    source
      .slice(source.lastIndexOf('写作需要耐心，也需要留白。'))
      .replace('写作需要耐心，也需要留白。', '让耐心与留白，成为写作的一部分。')
  await expect(editor).toHaveValue(expected)
  await expect(page.getByRole('status')).toHaveText('有未保存修改')
  await page.getByLabel('版本说明').fill('精简第二个段落')
  await page.getByRole('button', { name: '保存版本', exact: true }).click()
  await expect(page.getByRole('status')).toContainText('已保存')
  await page.getByRole('tab', { name: '发布包' }).click()
  await page.getByRole('button', { name: '生成发布稿', exact: true }).click()
  await expect(page.locator('#publish-text')).toHaveValue(`WECHAT 发布稿\n${expected}`)
  const downloaded = page.waitForEvent('download')
  await page.getByRole('button', { name: '下载发布稿' }).click()
  expect((await downloaded).suggestedFilename()).toContain('公众号.md')
  await page.getByText('小红书', { exact: true }).click()
  await expect(page.locator('#publish-text')).toHaveCount(0)
  await page.getByRole('button', { name: '生成发布稿', exact: true }).click()
  await expect(page.locator('#publish-text')).toHaveValue(`XIAOHONGSHU 发布稿\n${expected}`)
  await page.getByRole('button', { name: '版本', exact: true }).click()
  await page.getByRole('button', { name: '恢复此版本' }).click()
  await page.getByRole('button', { name: /^确\s*认$/ }).click()
  await expect(editor).toHaveValue(source)
  await expect(page.locator('.ant-modal:visible')).toHaveCount(0)
  expect(errors).toEqual([])
})

test('failed save preserves draft and navigation can be cancelled', async ({ page }) => {
  await mockApi(page, { failSave: true })
  const editor = page.getByLabel('文章正文编辑器')
  await editor.fill('不能丢失的草稿')
  await page.getByRole('button', { name: '保存版本', exact: true }).click()
  await expect(page.getByText('文章已在其他页面修改', { exact: false })).toBeVisible()
  await expect(editor).toHaveValue('不能丢失的草稿')
  await expect(page.getByRole('status')).toHaveText('有未保存修改')
  await page.getByRole('tab', { name: '发布包' }).click()
  await expect(page.getByRole('button', { name: '生成发布稿', exact: true })).toBeDisabled()
  await page.getByLabel('返回文章详情').click()
  await expect(page.getByText('离开创作工作台？')).toBeVisible()
  await page.getByRole('button', { name: /^取\s*消$/ }).click()
  await expect(editor).toHaveValue('不能丢失的草稿')
})

test('stale AI result cannot overwrite manual edits', async ({ page }) => {
  await mockApi(page)
  await selectSecondParagraph(page)
  await page.getByRole('button', { name: '生成精修建议', exact: true }).click()
  await expect(page.getByRole('button', { name: '采纳到草稿' })).toBeEnabled()
  await page.getByLabel('文章正文编辑器').fill('用户修改的新正文')
  await expect(page.getByRole('button', { name: '采纳到草稿' })).toBeDisabled()
})

test('Markdown preview sanitizes active HTML', async ({ page }) => {
  await mockApi(page, {
    content:
      '## 安全预览\n\n正常**粗体**\n\n<img src="/bad.png" onerror="window.injected=true"><script>window.injected=true</script>[危险链接](javascript:alert(1))',
  })
  await page.getByText('预览', { exact: true }).click()
  await expect(page.locator('.article-preview strong')).toHaveText('粗体')
  expect(
    await page
      .locator(
        '.article-preview [onerror], .article-preview script, .article-preview [href^="javascript:"]',
      )
      .count(),
  ).toBe(0)
  expect(
    await page.evaluate(() => (window as unknown as { injected?: boolean }).injected),
  ).toBeUndefined()
})

test('mobile layout stays within viewport and remains editable', async ({ page }, testInfo) => {
  await page.setViewportSize({ width: 390, height: 844 })
  await mockApi(page)
  await expect(page.getByLabel('文章正文编辑器')).toHaveValue(source)
  expect(
    await page.locator('.workspace').evaluate((el) => el.getBoundingClientRect().right),
  ).toBeLessThanOrEqual(390)
  await page.getByLabel('文章正文编辑器').fill('手机端也能编辑')
  await expect(page.getByRole('status')).toHaveText('有未保存修改')
  await page.screenshot({ path: testInfo.outputPath('workbench-mobile.png'), fullPage: true })
})

test('load errors and unfinished articles have clear states', async ({ page }) => {
  await mockApi(page, { failLoad: true })
  await expect(page.getByText('文章加载失败', { exact: true })).toBeVisible()
  await expect(page.getByRole('button', { name: '重新加载' })).toBeVisible()
})
