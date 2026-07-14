<template>
  <div class="page product-detail-page">
    <section class="section-title">
      <div>
        <h2>{{ asset?.name || '商品详情' }}</h2>
        <p class="muted">
          {{ assetTypeLabel(asset?.assetType) }} · {{ asset?.targetPlatform || '抖音' }} · {{ formatDate(asset?.updatedAt || asset?.createdAt) }}
        </p>
      </div>
      <div class="product-detail-actions">
        <el-button :loading="isReusing" @click="reuseAsset">继续使用</el-button>
        <el-button type="primary" @click="downloadAsset">下载图片</el-button>
        <el-button type="danger" :loading="isDeleting" @click="deleteAsset">删除</el-button>
      </div>
    </section>

    <div v-loading="isLoading" class="product-detail-layout">
      <section class="generator-panel product-detail-panel">
        <h3>生成图片</h3>
        <div v-if="generatedImages.length" class="product-image-grid is-generated">
          <figure v-for="image in generatedImages" :key="image.id">
            <img :src="image.ossUrl" :alt="asset?.name || '生成商品图'" />
          </figure>
        </div>
        <el-empty v-else description="暂无生成图片" />
      </section>

      <section class="generator-panel product-detail-panel">
        <h3>商品信息</h3>
        <el-descriptions :column="1" border>
          <el-descriptions-item label="商品名称">{{ asset?.name || '未命名商品' }}</el-descriptions-item>
          <el-descriptions-item label="类型">{{ assetTypeLabel(asset?.assetType) }}</el-descriptions-item>
          <el-descriptions-item label="平台">{{ asset?.targetPlatform || '抖音' }}</el-descriptions-item>
          <el-descriptions-item label="比例">{{ asset?.aspectRatio || '-' }}</el-descriptions-item>
          <el-descriptions-item label="清晰度">{{ asset?.quality || '-' }}</el-descriptions-item>
          <el-descriptions-item label="图片要求">{{ asset?.userRequirement || '未填写' }}</el-descriptions-item>
          <el-descriptions-item label="生成时间">{{ formatDate(asset?.updatedAt || asset?.createdAt) }}</el-descriptions-item>
        </el-descriptions>
      </section>
    </div>

    <section class="generator-panel product-detail-panel">
      <h3>上传产品图</h3>
      <div v-if="sourceImages.length" class="product-image-grid">
        <figure v-for="image in sourceImages" :key="image.id">
          <img :src="image.ossUrl" :alt="image.analysisRole || '上传产品图'" />
          <figcaption>{{ image.analysisRole || image.ossObjectKey || '产品图' }}</figcaption>
        </figure>
      </div>
      <el-empty v-else description="暂无上传产品图" />
    </section>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  deleteProductAsset,
  downloadProductAsset,
  getProductAsset,
  reuseProductAsset
} from '@/api/productImage'
import { handleAuthFailure } from '@/utils/authGuard'

const route = useRoute()
const router = useRouter()
const detail = ref(null)
const isLoading = ref(false)
const isReusing = ref(false)
const isDeleting = ref(false)

const asset = computed(() => detail.value?.asset || null)
const sourceImages = computed(() => detail.value?.sourceImages || [])
const generatedImages = computed(() => detail.value?.generatedImages || [])

const loadDetail = async () => {
  isLoading.value = true
  try {
    detail.value = await getProductAsset(route.params.id)
  } catch (error) {
    if (!handleAuthFailure(error, router)) {
      ElMessage.error(error.message || '商品详情加载失败')
      router.push('/products')
    }
  } finally {
    isLoading.value = false
  }
}

const reuseAsset = async () => {
  if (!asset.value?.id) {
    return
  }

  isReusing.value = true
  try {
    await reuseProductAsset(asset.value.id)
    ElMessage.success('已复制商品数据，可继续生成')
    router.push('/generate/product-images')
  } catch (error) {
    if (!handleAuthFailure(error, router)) {
      ElMessage.error(error.message || '继续使用失败')
    }
  } finally {
    isReusing.value = false
  }
}

const downloadAsset = async () => {
  if (!asset.value?.id) {
    return
  }

  try {
    await downloadProductAsset(asset.value.id, `${asset.value.name || '商品图'}.png`)
  } catch (error) {
    if (!handleAuthFailure(error, router)) {
      ElMessage.error(error.message || '下载失败')
    }
  }
}

const deleteAsset = async () => {
  if (!asset.value?.id) {
    return
  }

  try {
    await ElMessageBox.confirm(
      `删除「${asset.value.name || '未命名商品'}」后，数据库记录和 OSS 文件都会被删除，是否继续？`,
      '删除商品',
      { type: 'warning', confirmButtonText: '删除', cancelButtonText: '取消' }
    )
  } catch {
    return
  }

  isDeleting.value = true
  try {
    await deleteProductAsset(asset.value.id)
    ElMessage.success('已删除商品和 OSS 文件')
    router.push('/products')
  } catch (error) {
    if (!handleAuthFailure(error, router)) {
      ElMessage.error(error.message || '删除失败')
    }
  } finally {
    isDeleting.value = false
  }
}

const assetTypeLabel = (assetType) => ({
  main: '主图',
  detail: '详情图'
}[assetType] || '商品图')

const formatDate = (value) => {
  if (!value) {
    return '-'
  }
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) {
    return value
  }
  return date.toLocaleString('zh-CN', { hour12: false })
}

onMounted(() => {
  loadDetail()
})
</script>

<style scoped>
.product-detail-actions {
  display: flex;
  flex-wrap: wrap;
  justify-content: flex-end;
  gap: 10px;
}

.product-detail-layout {
  display: grid;
  grid-template-columns: minmax(0, 1.1fr) minmax(320px, 0.9fr);
  gap: 24px;
  align-items: start;
}

.product-detail-panel {
  padding: 24px;
}

.product-detail-panel h3 {
  margin: 0 0 16px;
}

.product-image-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(150px, 1fr));
  gap: 14px;
}

.product-image-grid.is-generated {
  grid-template-columns: minmax(0, 1fr);
}

.product-image-grid figure {
  margin: 0;
  min-width: 0;
}

.product-image-grid img {
  width: 100%;
  aspect-ratio: 1 / 1;
  display: block;
  object-fit: contain;
  border: 1px solid var(--line);
  border-radius: 8px;
  background: var(--panel-muted);
}

.product-image-grid.is-generated img {
  max-height: 540px;
  aspect-ratio: auto;
}

.product-image-grid figcaption {
  margin-top: 8px;
  color: var(--muted);
  font-size: 13px;
  line-height: 1.4;
  overflow-wrap: anywhere;
}

@media (max-width: 900px) {
  .product-detail-layout {
    grid-template-columns: 1fr;
  }
}
</style>
