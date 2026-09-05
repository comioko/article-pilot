import { marked } from 'marked'

export type TextSelection = { start: number; end: number; text: string; source: string }

export function replaceSelection(
  source: string,
  selection: TextSelection,
  replacement: string,
): string {
  if (
    source !== selection.source ||
    source.slice(selection.start, selection.end) !== selection.text
  ) {
    throw new Error('正文已修改，请重新选择段落并生成建议')
  }
  return source.slice(0, selection.start) + replacement + source.slice(selection.end)
}

export function getHeadings(source: string) {
  let offset = 0
  return marked.lexer(source).flatMap((token) => {
    const start = offset
    offset += token.raw.length
    return token.type === 'heading'
      ? [{ title: token.text, depth: token.depth, start, end: offset }]
      : []
  })
}

// Highlight the changed span without interpreting either version as HTML.
export function changedSpan(before: string, after: string) {
  const a = Array.from(before),
    b = Array.from(after)
  let prefix = 0,
    suffix = 0
  while (prefix < Math.min(a.length, b.length) && a[prefix] === b[prefix]) prefix++
  while (
    suffix < Math.min(a.length, b.length) - prefix &&
    a[a.length - 1 - suffix] === b[b.length - 1 - suffix]
  )
    suffix++
  return {
    prefix: a.slice(0, prefix).join(''),
    removed: a.slice(prefix, a.length - suffix).join(''),
    added: b.slice(prefix, b.length - suffix).join(''),
    suffix: suffix ? a.slice(-suffix).join('') : '',
  }
}

export function responseData<T>(response: { code?: number; message?: string; data?: T }): T {
  if (response.code !== 0 || response.data == null)
    throw new Error(response.message || '操作失败，请重试')
  return response.data
}
