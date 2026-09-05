import test from 'node:test'
import assert from 'node:assert/strict'
import { readFileSync } from 'node:fs'
import ts from 'typescript'

// Use the project's TypeScript compiler, so tests run with the existing Node 22 runtime.
const js = ts
  .transpileModule(
    readFileSync(new URL('../src/pages/article/workbench/editor.ts', import.meta.url), 'utf8'),
    {
      compilerOptions: { module: ts.ModuleKind.ES2022, target: ts.ScriptTarget.ES2022 },
    },
  )
  .outputText.replace("from 'marked'", `from '${import.meta.resolve('marked')}'`)
const { replaceSelection, getHeadings, changedSpan, responseData } = await import(
  `data:text/javascript;base64,${Buffer.from(js).toString('base64')}`
)

test('only the selected occurrence is replaced, preserving Markdown and images', () => {
  const source = '重复段落。\n\n![配图](https://example.com/a.png)\n\n重复段落。'
  const start = source.lastIndexOf('重复段落。')
  assert.equal(
    replaceSelection(source, { start, end: source.length, text: '重复段落。', source }, '新段落。'),
    '重复段落。\n\n![配图](https://example.com/a.png)\n\n新段落。',
  )
})
test('stale selection cannot overwrite a changed draft', () => {
  assert.throws(
    () =>
      replaceSelection('修改后的文章', { start: 0, end: 2, text: '原文', source: '原文' }, '候选'),
    /重新选择/,
  )
})
test('outline excludes fenced code headings and tracks source offsets', () => {
  const source = '# 开头\n\n```md\n# 不是真正章节\n```\n\n## 结尾\n\n正文'
  const headings = getHeadings(source)
  assert.deepEqual(
    headings.map((h) => h.title),
    ['开头', '结尾'],
  )
  assert.equal(source.slice(headings[1].start, headings[1].end).trim(), '## 结尾')
})
test('comparison preserves emoji and highlights changed content', () => {
  assert.deepEqual(changedSpan('你好🌱旧观点！', '你好🌱新观点！'), {
    prefix: '你好🌱',
    removed: '旧',
    added: '新',
    suffix: '观点！',
  })
})
test('HTTP 200 business errors are rejected rather than shown as successful saves', () => {
  assert.throws(() => responseData({ code: 50001, message: '版本冲突' }), /版本冲突/)
  assert.throws(() => responseData({ code: 0 }), /操作失败/)
  assert.equal(responseData({ code: 0, data: '已保存' }), '已保存')
})
