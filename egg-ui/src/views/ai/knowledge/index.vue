<template>
  <div class="knowledge-hub-page app-container">
    <section class="knowledge-upload-panel">
      <el-upload
        ref="upload"
        class="knowledge-upload"
        drag
        multiple
        action="#"
        accept=".pdf,.doc,.docx,.md,.xls,.xlsx,.txt"
        :auto-upload="false"
        :file-list="fileList"
        :before-upload="beforeUpload"
        :on-change="handleFileChange"
        :on-remove="handleFileRemove"
        :disabled="uploading"
        v-hasPermi="['ai:knowledge:upload']"
      >
        <i class="el-icon-upload knowledge-upload__icon"></i>
        <div class="el-upload__text">拖拽文件至此或<em>点击选择文件</em>进行上传</div>
        <div slot="tip" class="knowledge-upload__tip">
          文件支持 <i>pdf、doc、md、excel、text</i> 等，最大可上传 <em>100MB</em>
        </div>
      </el-upload>
    </section>

    <el-form ref="queryForm" class="knowledge-toolbar" :model="queryParams" :inline="true" size="small">
      <div class="knowledge-toolbar__left">
        <el-button
          type="danger"
          size="small"
          :disabled="selectedRows.length === 0"
          @click="handleBatchDelete"
          v-hasPermi="['ai:knowledge:remove']"
        >批量删除</el-button>
        <el-button
          type="primary"
          size="small"
          :disabled="selectedRows.length === 0 || downloading"
          :loading="downloading"
          @click="handleBatchDownload"
        >批量下载</el-button>
        <el-button
          type="success"
          size="small"
          icon="el-icon-user"
          :disabled="selectedRows.length === 0"
          @click="handleOpenAccessDialog"
          v-hasPermi="['ai:knowledge:upload']"
        >设置权限</el-button>
        <el-form-item label="文件名:" prop="fileName">
          <el-input
            v-model="queryParams.fileName"
            class="knowledge-toolbar__input"
            placeholder="请输入文件名称"
            clearable
            @keyup.enter.native="handleQuery"
          />
        </el-form-item>
        <el-form-item label="访问角色:" prop="accessRoles">
          <el-select
            v-model="queryParams.accessRoles"
            class="knowledge-toolbar__select"
            placeholder="全部"
            clearable
            @change="handleQuery"
          >
            <el-option label="管理员" value="admin" />
            <el-option label="普通用户" value="common" />
          </el-select>
        </el-form-item>
        <el-button type="primary" size="small" :loading="loading" @click="handleQuery">搜索</el-button>
      </div>
      <el-button
        type="warning"
        size="small"
        :disabled="fileList.length === 0"
        :loading="uploading"
        @click="submitUpload"
        v-hasPermi="['ai:knowledge:upload']"
      >全部上传</el-button>
    </el-form>

    <el-table
      ref="knowledgeTable"
      class="knowledge-table"
      v-loading="loading"
      :data="knowledgeList"
      border
      height="calc(100vh - 430px)"
      @selection-change="handleSelectionChange"
    >
      <el-table-column type="selection" width="55" align="center" />
      <el-table-column label="序号" width="80" align="center">
        <template slot-scope="scope">
          {{ (queryParams.pageNum - 1) * queryParams.pageSize + scope.$index + 1 }}
        </template>
      </el-table-column>
      <el-table-column label="文件名" prop="fileName" min-width="360" :show-overflow-tooltip="true" />
      <el-table-column label="状态" width="110" align="center">
        <template slot-scope="scope">
          <el-tag :type="statusType(scope.row.status)" size="mini">{{ statusLabel(scope.row.status) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="切片数" prop="chunkCount" width="90" align="center">
        <template slot-scope="scope">
          <span>{{ scope.row.chunkCount || 0 }}</span>
        </template>
      </el-table-column>
      <el-table-column label="访问角色" width="170" align="center">
        <template slot-scope="scope">
          <el-tag
            v-for="role in parseAccessRoles(scope.row.accessRoles)"
            :key="role"
            class="knowledge-access-tag"
            :type="accessRoleType(role)"
            size="mini"
          >
            {{ accessRoleLabel(role) }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="上传时间" width="180">
        <template slot-scope="scope">
          <span>{{ parseTime(scope.row.createdAt) }}</span>
        </template>
      </el-table-column>
      <el-table-column label="更新时间" width="180">
        <template slot-scope="scope">
          <span>{{ parseTime(scope.row.updatedAt) }}</span>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="220" fixed="right">
        <template slot-scope="scope">
          <el-button
            v-if="scope.row.status === 'failed'"
            type="warning"
            size="mini"
            @click="handleRetry(scope.row)"
            v-hasPermi="['ai:knowledge:retry']"
          >重试</el-button>
          <el-button type="danger" size="mini" @click="handleDelete(scope.row)" v-hasPermi="['ai:knowledge:remove']">
            删除
          </el-button>
          <el-button type="primary" size="mini" :loading="downloadingId === scope.row.id" @click="handleDownload(scope.row)">
            下载
          </el-button>
        </template>
      </el-table-column>
    </el-table>

    <pagination
      v-show="total > 0"
      :total="total"
      :page.sync="queryParams.pageNum"
      :limit.sync="queryParams.pageSize"
      @pagination="getList"
    />

    <el-dialog title="设置访问权限" :visible.sync="accessDialog.open" width="420px" append-to-body>
      <el-form label-width="96px" size="small">
        <el-form-item label="已选文档">
          <span>{{ selectedRows.length }} 个</span>
        </el-form-item>
        <el-form-item label="访问角色">
          <el-checkbox-group v-model="accessDialog.accessRoles">
            <el-checkbox label="admin" disabled>管理员</el-checkbox>
            <el-checkbox label="common">普通用户</el-checkbox>
          </el-checkbox-group>
        </el-form-item>
      </el-form>
      <div slot="footer" class="dialog-footer">
        <el-button size="small" @click="accessDialog.open = false">取消</el-button>
        <el-button
          type="primary"
          size="small"
          :loading="accessDialog.submitting"
          @click="submitAccessRoles"
        >确定</el-button>
      </div>
    </el-dialog>
  </div>
</template>

<script>
import {
  delKnowledge,
  getKnowledgeDownloadUrls,
  listKnowledge,
  retryKnowledge,
  updateKnowledgeAccessRoles,
  uploadKnowledgeBatch
} from '@/api/ai/knowledge'

const MAX_FILE_SIZE = 100 * 1024 * 1024
const SUPPORTED_EXTENSIONS = ['pdf', 'doc', 'docx', 'md', 'xls', 'xlsx', 'txt']

export default {
  name: 'AiKnowledge',
  data() {
    return {
      loading: true,
      uploading: false,
      downloading: false,
      downloadingId: undefined,
      total: 0,
      knowledgeList: [],
      fileList: [],
      selectedRows: [],
      accessDialog: {
        open: false,
        submitting: false,
        accessRoles: ['admin']
      },
      queryParams: {
        pageNum: 1,
        pageSize: 10,
        fileName: undefined,
        status: undefined,
        accessRoles: undefined
      }
    }
  },
  created() {
    this.getList()
  },
  methods: {
    getList() {
      this.loading = true
      listKnowledge(this.queryParams).then(response => {
        this.knowledgeList = response.rows || []
        this.total = response.total || 0
      }).finally(() => {
        this.loading = false
      })
    },
    handleQuery() {
      this.queryParams.pageNum = 1
      this.getList()
    },
    beforeUpload(file) {
      const error = this.validateUploadFile(file)
      if (error) {
        this.$modal.msgError(error)
        return false
      }
      return true
    },
    handleFileChange(file, fileList) {
      const acceptedFiles = []
      const rejectedMessages = []
      fileList.forEach(item => {
        const rawFile = item.raw || item
        const error = this.validateUploadFile(rawFile)
        if (error) {
          rejectedMessages.push(error)
        } else {
          acceptedFiles.push(item)
        }
      })
      this.fileList = acceptedFiles
      if (rejectedMessages.length > 0) {
        this.$modal.msgWarning(rejectedMessages[0])
      }
    },
    handleFileRemove(file, fileList) {
      this.fileList = fileList
    },
    submitUpload() {
      if (this.fileList.length === 0) {
        this.$modal.msgWarning('请先选择要上传的知识库文件')
        return
      }
      const formData = new FormData()
      this.fileList.forEach(item => {
        // The backend expects repeated "files" parts for the batch upload endpoint.
        formData.append('files', item.raw || item)
      })
      this.uploading = true
      uploadKnowledgeBatch(formData).then(() => {
        this.$modal.msgSuccess('上传成功，系统将异步解析并向量化')
        this.clearUploadFiles()
        this.getList()
      }).finally(() => {
        this.uploading = false
      })
    },
    clearUploadFiles() {
      this.fileList = []
      if (this.$refs.upload) {
        this.$refs.upload.clearFiles()
      }
    },
    handleSelectionChange(selection) {
      this.selectedRows = selection
    },
    handleOpenAccessDialog() {
      if (this.selectedRows.length === 0) {
        this.$modal.msgWarning('请先选择要设置权限的文件')
        return
      }
      const hasCommon = this.selectedRows.some(row => this.parseAccessRoles(row.accessRoles).includes('common'))
      this.accessDialog.accessRoles = hasCommon ? ['admin', 'common'] : ['admin']
      this.accessDialog.open = true
    },
    submitAccessRoles() {
      const ids = this.selectedRows.map(row => row.id)
      if (ids.length === 0) {
        this.$modal.msgWarning('请先选择要设置权限的文件')
        return
      }
      this.accessDialog.submitting = true
      updateKnowledgeAccessRoles({
        ids,
        accessRoles: this.accessDialog.accessRoles
      }).then(() => {
        this.$modal.msgSuccess('权限已更新')
        this.accessDialog.open = false
        this.getList()
      }).finally(() => {
        this.accessDialog.submitting = false
      })
    },
    handleBatchDelete() {
      if (this.selectedRows.length === 0) {
        this.$modal.msgWarning('请先选择要删除的文件')
        return
      }
      const ids = this.selectedRows.map(row => row.id)
      this.$modal.confirm('是否确认删除选中的知识库文件？').then(() => {
        return delKnowledge(ids.join(','))
      }).then(() => {
        this.$modal.msgSuccess('删除成功')
        this.selectedRows = []
        this.getList()
      }).catch(() => {})
    },
    handleDelete(row) {
      this.$modal.confirm('是否确认删除文件"' + row.fileName + '"？').then(() => {
        return delKnowledge(row.id)
      }).then(() => {
        this.$modal.msgSuccess('删除成功')
        this.getList()
      }).catch(() => {})
    },
    handleRetry(row) {
      this.$modal.confirm('是否确认重新处理文件"' + row.fileName + '"？').then(() => {
        return retryKnowledge(row.id)
      }).then(() => {
        this.$modal.msgSuccess('已提交重试')
        this.getList()
      }).catch(() => {})
    },
    handleBatchDownload() {
      if (this.selectedRows.length === 0) {
        this.$modal.msgWarning('请先选择要下载的文件')
        return
      }
      this.downloadRows(this.selectedRows)
    },
    handleDownload(row) {
      this.downloadingId = row.id
      this.downloadRows([row]).finally(() => {
        this.downloadingId = undefined
      })
    },
    downloadRows(rows) {
      const ids = rows.map(row => row.id).join(',')
      this.downloading = true
      return getKnowledgeDownloadUrls(ids).then(response => {
        const urls = response.data || []
        if (urls.length === 0) {
          this.$modal.msgWarning('未找到可下载的 OSS 文件地址')
          return
        }
        urls.forEach((item, index) => {
          setTimeout(() => this.triggerDownload(item.url, item.fileName), index * 150)
        })
      }).finally(() => {
        this.downloading = false
      })
    },
    triggerDownload(url, fileName) {
      if (!url) {
        return
      }
      const link = document.createElement('a')
      link.href = url
      link.download = fileName || ''
      link.target = '_blank'
      link.rel = 'noopener'
      document.body.appendChild(link)
      link.click()
      document.body.removeChild(link)
    },
    validateUploadFile(file) {
      if (!file || file.size <= 0) {
        return '请选择有效的知识库文件'
      }
      const fileName = file.name || ''
      const extension = fileName.includes('.') ? fileName.split('.').pop().toLowerCase() : ''
      if (!SUPPORTED_EXTENSIONS.includes(extension)) {
        return '仅支持上传 pdf、doc、docx、md、xls、xlsx、txt 格式的知识库文件'
      }
      if (file.size > MAX_FILE_SIZE) {
        return '知识库文件不能超过 100MB'
      }
      return ''
    },
    statusLabel(status) {
      const labels = {
        processing: '处理中',
        completed: '已完成',
        failed: '失败'
      }
      return labels[status] || status || '-'
    },
    statusType(status) {
      const types = {
        processing: 'warning',
        completed: 'success',
        failed: 'danger'
      }
      return types[status] || 'info'
    },
    parseAccessRoles(accessRoles) {
      const roles = (accessRoles || 'admin').split(',')
        .map(role => role.trim())
        .filter(Boolean)
      return roles.length > 0 ? roles : ['admin']
    },
    accessRoleLabel(role) {
      const labels = {
        admin: '管理员',
        common: '普通用户'
      }
      return labels[role] || role
    },
    accessRoleType(role) {
      const types = {
        admin: 'danger',
        common: 'success'
      }
      return types[role] || 'info'
    }
  }
}
</script>

<style scoped>
.knowledge-hub-page {
  min-height: calc(100vh - 84px);
  background: #f3f3f3;
  padding: 22px 28px;
}

.knowledge-upload-panel {
  padding: 0 0 16px;
}

.knowledge-upload {
  width: 100%;
}

.knowledge-upload ::v-deep .el-upload {
  width: 100%;
}

.knowledge-upload ::v-deep .el-upload-dragger {
  width: 100%;
  height: 235px;
  border: 1px dashed #dcdfe6;
  border-radius: 6px;
  background: #fff;
}

.knowledge-upload__icon {
  margin-top: 58px;
  color: #a8adb6;
  font-size: 58px;
  line-height: 1;
}

.knowledge-upload ::v-deep .el-upload__text {
  margin-top: 18px;
  color: #606266;
  font-size: 16px;
}

.knowledge-upload ::v-deep .el-upload__text em {
  color: #409eff;
  font-style: normal;
}

.knowledge-upload__tip {
  margin-top: 6px;
  text-align: center;
  color: #606266;
  font-size: 16px;
}

.knowledge-upload__tip i {
  font-style: italic;
}

.knowledge-upload__tip em {
  color: #1f33ff;
  font-style: italic;
}

.knowledge-upload ::v-deep .el-upload-list {
  max-width: 720px;
  margin: 10px auto 0;
}

.knowledge-toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 22px;
  padding: 18px 20px;
  border-radius: 4px;
  background: #fff;
  box-shadow: 0 2px 8px rgba(31, 45, 61, 0.08);
}

.knowledge-toolbar__left {
  display: flex;
  align-items: center;
  gap: 14px;
  flex-wrap: wrap;
}

.knowledge-toolbar ::v-deep .el-form-item {
  margin-bottom: 0;
}

.knowledge-toolbar__input {
  width: 250px;
}

.knowledge-toolbar__select {
  width: 150px;
}

.knowledge-table {
  background: #fff;
}

.knowledge-access-tag + .knowledge-access-tag {
  margin-left: 6px;
}

.knowledge-table ::v-deep th {
  background: #f5f7fa;
  color: #737b86;
  font-weight: 600;
}

.knowledge-table ::v-deep .el-table__cell {
  padding: 12px 0;
}

.knowledge-table ::v-deep .el-button--mini {
  padding: 7px 12px;
}
</style>
