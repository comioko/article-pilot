<script setup lang="ts">
import { computed } from 'vue'
import { marked } from 'marked'
import DOMPurify from 'dompurify'
const props = defineProps<{ content: string }>()
const html = computed(() =>
  DOMPurify.sanitize(marked.parse(props.content, { async: false }), {
    USE_PROFILES: { html: true },
    FORBID_TAGS: ['input', 'button', 'form', 'style'],
    FORBID_ATTR: ['style'],
  }),
)
</script>

<template><div class="prose" v-html="html"></div></template>

<style scoped>
.prose {
  line-height: 1.95;
  overflow-wrap: anywhere;
  color: #44392e;
  font-size: 15px;
}
.prose :deep(h1),
.prose :deep(h2),
.prose :deep(h3) {
  line-height: 1.5;
  margin: 1.6em 0 0.7em;
  color: #33281e;
}
.prose :deep(p) {
  margin: 0 0 1.1em;
}
.prose :deep(img) {
  max-width: 100%;
  height: auto;
  border-radius: 10px;
}
.prose :deep(pre) {
  overflow: auto;
  padding: 16px;
  background: #f5f0e8;
  border-radius: 8px;
}
.prose :deep(blockquote) {
  border-left: 3px solid #b69568;
  margin: 20px 0;
  padding-left: 18px;
  color: #7a6b58;
}
.prose :deep(table) {
  display: block;
  overflow: auto;
  border-collapse: collapse;
}
.prose :deep(td),
.prose :deep(th) {
  border: 1px solid #e8d9c2;
  padding: 8px 12px;
}
.prose :deep(a) {
  color: #856332;
  text-decoration: underline;
}
</style>
