<template>
  <div class="page product-assets-page">
    <section class="section-title">
      <div>
        <h2>商品库</h2>
        <p class="muted">查看已成功生成的商品图资产，并继续复用原图、平台和生成要求。</p>
      </div>
      <el-button :loading="isLoading" @click="loadAssets">刷新</el-button>
    </section>

    <div class="table-panel">
      <el-table v-loading="isLoading" :data="assets" style="width:100%">
        <el-table-column label="商品" min-width="260">
          <template #default="{ row }">
            <router-link class="product-asset-link" :to="`/products/${row.id}`">
              <strong>{{ row.name || '未命名商品' }}</strong>
            </router-link>
            <p class="muted">{{ row.userRequirement || '暂无图片要求记录' }}</p>
          </template>
        </el-table-column>
        <el-table-column label="类型" width="120">
          <template #default="{ row }">{{ assetTypeLabel(row.assetType) }}</template>
        </el-table-column>
        <el-table-column label="平台" width="140">
          <template #default="{ row }">{{ row.targetPlatform || '抖音' }}</template>
        </el-table-column>
        <el-table-column label="最近生成" width="180">
          <template #default="{ row }">{{ formatDate(row.updatedAt || row.createdAt) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="320" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="goDetail(row)">详情</el-button>
            <el-button link type="primary" @click="openRenameDialog(row)">重命名</el-button>
            <el-button
              link
              type="primary"
              :loading="Boolean(reusingAssetIds[row.id])"
              @click="reuseAsset(row)"
            >
              继续使用
            </el-button>
            <el-button link type="primary" @click="downloadAsset(row)">下载</el-button>
            <el-button
              link
              type="danger"
              :loading="Boolean(deletingAssetIds[row.id])"
              @click="deleteAsset(row)"
            >
              删除
            </el-button>
          </template>
        </el-table-column>
      </el-table>
    </div>

    <el-dialog
      v-model="isRenameDialogVisible"
      title="重命名商品"
      width="420px"
      destroy-on-close
    >
      <el-input v-model="renameName" maxlength="80" show-word-limit placeholder="请输入商品名称" />
      <template #footer>
        <el-button @click="isRenameDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="isRenaming" @click="confirmRename">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  deleteProductAsset,
  downloadProductAsset,
  listProductAssets,
  renameProductAsset,
  reuseProductAsset
} from '@/api/productImage'
import { handleAuthFailure } from '@/utils/authGuard'

const router = useRouter()
const assets = ref([])
const isLoading = ref(false)
const isRenameDialogVisible = ref(false)
const isRenaming = ref(false)
const renameTarget = ref(null)
const renameName = ref('')
const reusingAssetIds = ref({})
const deletingAssetIds = ref({})

const loadAssets = async () => {
  isLoading.value = true
  try {
    assets.value = await listProductAssets()
  } catch (error) {
    if (!handleAuthFailure(error, router)) {
      ElMessage.error(error.message || '商品库加载失败')
    }
  } finally {
    isLoading.value = false
  }
}

const goDetail = (asset) => {
  router.push(`/products/${asset.id}`)
}

const openRenameDialog = (asset) => {
  renameTarget.value = asset
  renameName.value = asset.name || ''
  isRenameDialogVisible.value = true
}

const confirmRename = async () => {
  const name = renameName.value.trim()
  if (!renameTarget.value || !name) {
    ElMessage.warning('请输入商品名称')
    return
  }

  isRenaming.value = true
  try {
    const renamed = await renameProductAsset(renameTarget.value.id, name)
    assets.value = assets.value.map((asset) => asset.id === renamed.id ? renamed : asset)
    isRenameDialogVisible.value = false
    ElMessage.success('已重命名')
  } catch (error) {
    if (!handleAuthFailure(error, router)) {
      ElMessage.error(error.message || '重命名失败')
    }
  } finally {
    isRenaming.value = false
  }
}

const reuseAsset = async (asset) => {
  reusingAssetIds.value = { ...reusingAssetIds.value, [asset.id]: true }
  try {
    await reuseProductAsset(asset.id)
    ElMessage.success('已复制商品数据，可继续生成')
    router.push('/generate/product-images')
  } catch (error) {
    if (!handleAuthFailure(error, router)) {
      ElMessage.error(error.message || '继续使用失败')
    }
  } finally {
    const next = { ...reusingAssetIds.value }
    delete next[asset.id]
    reusingAssetIds.value = next
  }
}

const downloadAsset = async (asset) => {
  try {
    await downloadProductAsset(asset.id, `${asset.name || '商品图'}.png`)
  } catch (error) {
    if (!handleAuthFailure(error, router)) {
      ElMessage.error(error.message || '下载失败')
    }
  }
}

const deleteAsset = async (asset) => {
  try {
    await ElMessageBox.confirm(
      `删除「${asset.name || '未命名商品'}」后，数据库记录和 OSS 文件都会被删除，是否继续？`,
      '删除商品',
      { type: 'warning', confirmButtonText: '删除', cancelButtonText: '取消' }
    )
  } catch {
    return
  }

  deletingAssetIds.value = { ...deletingAssetIds.value, [asset.id]: true }
  try {
    await deleteProductAsset(asset.id)
    assets.value = assets.value.filter((item) => item.id !== asset.id)
    ElMessage.success('已删除商品和 OSS 文件')
  } catch (error) {
    if (!handleAuthFailure(error, router)) {
      ElMessage.error(error.message || '删除失败')
    }
  } finally {
    const next = { ...deletingAssetIds.value }
    delete next[asset.id]
    deletingAssetIds.value = next
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
  loadAssets()
})
</script>

<style scoped>
.product-asset-link {
  display: inline-flex;
  max-width: 100%;
  color: var(--text);
  text-decoration: none;
}

.product-asset-link:hover,
.product-asset-link:focus-visible {
  color: var(--black);
  text-decoration: underline;
}
</style>
