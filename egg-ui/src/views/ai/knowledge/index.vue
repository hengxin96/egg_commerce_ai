<template>
  <div class="app-container">
    <el-form
      ref="queryForm"
      :model="queryParams"
      size="small"
      :inline="true"
      v-show="showSearch"
      label-width="80px"
    >
      <el-form-item label="文件名称" prop="fileName">
        <el-input
          v-model="queryParams.fileName"
          placeholder="请输入文件名称"
          clearable
          style="width: 240px"
          @keyup.enter.native="handleQuery"
        />
      </el-form-item>
      <el-form-item label="处理状态" prop="status">
        <el-select
          v-model="queryParams.status"
          placeholder="请选择处理状态"
          clearable
          style="width: 180px"
        >
          <el-option label="处理中" value="processing" />
          <el-option label="已完成" value="completed" />
          <el-option label="失败" value="failed" />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" icon="el-icon-search" size="mini" @click="handleQuery">搜索</el-button>
        <el-button icon="el-icon-refresh" size="mini" @click="resetQuery">重置</el-button>
      </el-form-item>
    </el-form>

    <el-row :gutter="10" class="mb8">
      <el-col :span="1.5">
        <el-upload
          ref="upload"
          action="#"
          :auto-upload="false"
          :limit="1"
          :file-list="fileList"
          :on-change="handleFileChange"
          :on-remove="handleFileRemove"
          :on-exceed="handleFileExceed"
          :http-request="handleUploadRequest"
          :before-upload="beforeUpload"
          v-hasPermi="['ai:knowledge:upload']"
        >
          <el-button
            type="primary"
            plain
            icon="el-icon-upload2"
            size="mini"
            :loading="uploading"
          >上传文档</el-button>
        </el-upload>
      </el-col>
      <el-col :span="1.5">
        <el-button
          type="success"
          plain
          icon="el-icon-check"
          size="mini"
          :disabled="!fileList.length"
          :loading="uploading"
          @click="submitUpload"
          v-hasPermi="['ai:knowledge:upload']"
        >开始上传</el-button>
      </el-col>
      <right-toolbar :showSearch.sync="showSearch" @queryTable="getList"></right-toolbar>
    </el-row>

    <el-table v-loading="loading" :data="knowledgeList">
      <el-table-column label="文件名称" align="left" prop="fileName" min-width="220" :show-overflow-tooltip="true" />
      <el-table-column label="文件大小" align="center" prop="fileSize" width="110">
        <template slot-scope="scope">
          <span>{{ formatFileSize(scope.row.fileSize) }}</span>
        </template>
      </el-table-column>
      <el-table-column label="状态" align="center" prop="status" width="100">
        <template slot-scope="scope">
          <el-tag :type="statusType(scope.row.status)" size="mini">{{ statusLabel(scope.row.status) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="切片数" align="center" prop="chunkCount" width="90" />
      <el-table-column label="失败原因" align="left" prop="failureReason" min-width="220" :show-overflow-tooltip="true">
        <template slot-scope="scope">
          <span>{{ scope.row.failureReason || '-' }}</span>
        </template>
      </el-table-column>
      <el-table-column label="更新时间" align="center" prop="updatedAt" width="180">
        <template slot-scope="scope">
          <span>{{ parseTime(scope.row.updatedAt) }}</span>
        </template>
      </el-table-column>
      <el-table-column label="操作" align="center" width="150" class-name="small-padding fixed-width">
        <template slot-scope="scope">
          <el-button
            v-if="scope.row.status === 'failed'"
            size="mini"
            type="text"
            icon="el-icon-refresh"
            @click="handleRetry(scope.row)"
            v-hasPermi="['ai:knowledge:retry']"
          >重试</el-button>
          <el-button
            size="mini"
            type="text"
            icon="el-icon-delete"
            @click="handleDelete(scope.row)"
            v-hasPermi="['ai:knowledge:remove']"
          >删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <pagination
      v-show="total>0"
      :total="total"
      :page.sync="queryParams.pageNum"
      :limit.sync="queryParams.pageSize"
      @pagination="getList"
    />
  </div>
</template>

<script>
import { delKnowledge, listKnowledge, retryKnowledge, uploadKnowledge } from '@/api/ai/knowledge'

export default {
  name: 'AiKnowledge',
  data() {
    return {
      loading: true,
      uploading: false,
      showSearch: true,
      total: 0,
      knowledgeList: [],
      fileList: [],
      queryParams: {
        pageNum: 1,
        pageSize: 10,
        fileName: undefined,
        status: undefined
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
        this.loading = false
      }).catch(() => {
        this.loading = false
      })
    },
    handleQuery() {
      this.queryParams.pageNum = 1
      this.getList()
    },
    resetQuery() {
      this.resetForm('queryForm')
      this.handleQuery()
    },
    beforeUpload(file) {
      if (!file || file.size <= 0) {
        this.$modal.msgError('请选择有效的知识库文件')
        return false
      }
      return true
    },
    handleUploadRequest(option) {
      const formData = new FormData()
      formData.append('file', option.file)
      this.uploading = true
      uploadKnowledge(formData).then(() => {
        this.$modal.msgSuccess('上传成功，系统将异步解析并向量化')
        this.fileList = []
        this.$refs.upload.clearFiles()
        this.getList()
      }).finally(() => {
        this.uploading = false
      })
    },
    submitUpload() {
      this.$refs.upload.submit()
    },
    handleFileChange(file, fileList) {
      this.fileList = fileList.slice(-1)
    },
    handleFileRemove(file, fileList) {
      this.fileList = fileList
    },
    handleFileExceed(files) {
      this.$refs.upload.clearFiles()
      const file = files[0]
      if (file) {
        this.$refs.upload.handleStart(file)
        this.fileList = [{ name: file.name, raw: file }]
      }
    },
    handleRetry(row) {
      this.$modal.confirm('是否确认重新处理文件"' + row.fileName + '"？').then(() => {
        return retryKnowledge(row.id)
      }).then(() => {
        this.$modal.msgSuccess('已提交重试')
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
    formatFileSize(size) {
      const value = Number(size || 0)
      if (value < 1024) {
        return value + ' B'
      }
      if (value < 1024 * 1024) {
        return (value / 1024).toFixed(1) + ' KB'
      }
      return (value / 1024 / 1024).toFixed(1) + ' MB'
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
    }
  }
}
</script>
