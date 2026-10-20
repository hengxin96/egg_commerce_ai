<script setup>
import { computed } from 'vue'

const props = defineProps({
  audit: { type: [Object, String], default: null }
})

const auditObject = computed(() => {
  if (!props.audit) {
    return null
  }
  if (typeof props.audit === 'string') {
    try {
      return JSON.parse(props.audit)
    } catch {
      return null
    }
  }
  return props.audit
})

const visionIncomplete = computed(() => auditObject.value?.visionAudit?.completed === false
  || auditObject.value?.completed === false)

const status = computed(() => {
  if (visionIncomplete.value) {
    return 'incomplete'
  }
  return auditObject.value?.auditSummary?.status
    || auditObject.value?.status
    || auditObject.value?.visionAudit?.status
    || auditObject.value?.ruleAudit?.status
    || ''
})

const statusMeta = computed(() => ({
  pass: { label: '通过', type: 'success' },
  warning: { label: '注意', type: 'warning' },
  high_risk: { label: '高风险', type: 'danger' },
  incomplete: { label: '视觉审计未完成', type: 'info' }
}[status.value] || { label: '待审计', type: 'info' }))

const riskItems = computed(() => [
  ...(auditObject.value?.risks || []),
  ...(auditObject.value?.ruleAudit?.risks || []),
  ...(auditObject.value?.visionAudit?.risks || [])
].filter((risk) => risk?.reason || risk?.type))
</script>

<template>
  <div v-if="auditObject" class="audit-status" :class="`is-${status || 'unknown'}`">
    <el-tag size="small" :type="statusMeta.type">{{ statusMeta.label }}</el-tag>
    <details v-if="riskItems.length" class="audit-risks">
      <summary>{{ riskItems.length }} 条风险</summary>
      <ul>
        <li v-for="(risk, index) in riskItems" :key="`${risk.type || 'risk'}-${index}`">
          {{ risk.reason || risk.type }}
        </li>
      </ul>
    </details>
  </div>
</template>

<style scoped>
.audit-status {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 8px;
  margin: 8px 0 10px;
}

.audit-risks {
  color: var(--muted);
  font-size: 12px;
  line-height: 1.5;
}

.audit-risks summary {
  cursor: pointer;
}

.audit-risks ul {
  margin: 6px 0 0;
  padding-left: 18px;
}
</style>
