<script setup>
import { computed, shallowRef } from 'vue'
import { View } from '@element-plus/icons-vue'
import ProductImageAuditBadge from '@/components/product/ProductImageAuditBadge.vue'

const emit = defineEmits(['update:selectedIds'])

const props = defineProps({
  images: { type: Array, default: () => [] },
  selectedIds: { type: Array, default: () => [] },
  emptyText: { type: String, default: '暂无生成图' },
  selectable: { type: Boolean, default: true }
})

const normalizedImages = computed(() => props.images
  .map((image, index) => ({
    ...image,
    id: image.id || image.imageId || `${index}`,
    url: image.ossUrl || image.url || image.temporaryUrl,
    label: image.label || image.name || `生成图 ${index + 1}`
  }))
  .filter((image) => image.id && image.url))

const selectedSet = computed(() => new Set(props.selectedIds))
const selectedCount = computed(() => normalizedImages.value.filter((image) => selectedSet.value.has(image.id)).length)
const allSelected = computed(() => normalizedImages.value.length > 0 && selectedCount.value === normalizedImages.value.length)
const isIndeterminate = computed(() => selectedCount.value > 0 && !allSelected.value)
const previewImageIndex = shallowRef(-1)
const previewUrls = computed(() => normalizedImages.value.map((image) => image.url))
const isPreviewVisible = computed(() => previewImageIndex.value >= 0)

const updateSelection = (ids) => {
  emit('update:selectedIds', ids)
}

const toggleImage = (imageId, checked) => {
  const next = new Set(props.selectedIds)
  if (checked) {
    next.add(imageId)
  } else {
    next.delete(imageId)
  }
  updateSelection([...next])
}

const toggleAll = (checked) => {
  updateSelection(checked ? normalizedImages.value.map((image) => image.id) : [])
}

const openImagePreview = (index) => {
  previewImageIndex.value = index
}

const closeImagePreview = () => {
  previewImageIndex.value = -1
}
</script>

<template>
  <div class="selectable-image-gallery">
    <div v-if="normalizedImages.length" class="gallery-toolbar">
      <el-checkbox
        v-if="props.selectable"
        :model-value="allSelected"
        :indeterminate="isIndeterminate"
        @change="toggleAll"
      >
        全选
      </el-checkbox>
      <span class="gallery-count">已选 {{ selectedCount }} / 共 {{ normalizedImages.length }} 张</span>
    </div>

    <div v-if="normalizedImages.length" class="gallery-grid">
      <figure
        v-for="(image, index) in normalizedImages"
        :key="image.id"
        class="gallery-item"
        :class="{ 'is-selected': selectedSet.has(image.id) }"
      >
        <label v-if="props.selectable" class="gallery-check" :aria-label="`选择 ${image.label}`">
          <input
            type="checkbox"
            :checked="selectedSet.has(image.id)"
            @change="toggleImage(image.id, $event.target.checked)"
          />
        </label>
        <button
          type="button"
          class="gallery-preview"
          :aria-label="`预览 ${image.label}`"
          title="预览图片"
          @click.stop="openImagePreview(index)"
        >
          <el-icon><View /></el-icon>
        </button>
        <img :src="image.url" :alt="image.label" />
        <ProductImageAuditBadge v-if="image.audit" :audit="image.audit" />
      </figure>
    </div>

    <el-empty v-else :description="props.emptyText" />
    <el-image-viewer
      v-if="isPreviewVisible"
      :url-list="previewUrls"
      :initial-index="previewImageIndex"
      hide-on-click-modal
      @close="closeImagePreview"
    />
  </div>
</template>

<style scoped>
.selectable-image-gallery {
  min-width: 0;
}

.gallery-toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 12px;
}

.gallery-count {
  color: var(--muted);
  font-size: 13px;
}

.gallery-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(180px, 1fr));
  gap: 14px;
}

.gallery-item {
  position: relative;
  margin: 0;
  min-width: 0;
  border: 1px solid var(--line);
  border-radius: 8px;
  background: var(--panel-muted);
  overflow: hidden;
}

.gallery-item.is-selected {
  border-color: #409eff;
  box-shadow: 0 0 0 2px rgba(64, 158, 255, 0.18);
}

.gallery-item img {
  width: 100%;
  aspect-ratio: 1 / 1;
  display: block;
  object-fit: contain;
}

.gallery-check {
  position: absolute;
  left: 10px;
  top: 10px;
  z-index: 1;
  width: 28px;
  height: 28px;
  display: grid;
  place-items: center;
  border-radius: 8px;
  background: rgba(255, 255, 255, 0.94);
  box-shadow: 0 4px 12px rgba(17, 18, 23, 0.12);
}

.gallery-check input {
  width: 16px;
  height: 16px;
  margin: 0;
}

.gallery-preview {
  position: absolute;
  right: 10px;
  top: 10px;
  z-index: 2;
  width: 34px;
  height: 34px;
  display: grid;
  place-items: center;
  border: 1px solid rgba(255, 255, 255, 0.24);
  border-radius: 999px;
  color: #fff;
  background: rgba(17, 18, 23, 0.72);
  box-shadow: 0 6px 16px rgba(17, 18, 23, 0.22);
  backdrop-filter: blur(8px);
  cursor: pointer;
  transition: transform 0.16s ease, background 0.16s ease;
}

.gallery-preview:hover {
  transform: translateY(-1px);
  background: rgba(17, 18, 23, 0.86);
}
</style>
