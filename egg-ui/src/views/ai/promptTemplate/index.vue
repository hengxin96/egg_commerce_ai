<template>
  <div class="app-container">
    <el-form :model="queryParams" ref="queryForm" size="small" :inline="true" v-show="showSearch" label-width="68px">
      <el-form-item label="模板名称" prop="name">
        <el-input
          v-model="queryParams.name"
          placeholder="请输入模板名称"
          clearable
          style="width: 240px"
          @keyup.enter.native="handleQuery"
        />
      </el-form-item>
      <el-form-item label="分类" prop="category">
        <el-input
          v-model="queryParams.category"
          placeholder="请输入分类"
          clearable
          style="width: 200px"
          @keyup.enter.native="handleQuery"
        />
      </el-form-item>
      <el-form-item label="状态" prop="status">
        <el-select v-model="queryParams.status" placeholder="请选择状态" clearable style="width: 160px">
          <el-option
            v-for="item in statusOptions"
            :key="item.value"
            :label="item.label"
            :value="item.value"
          />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" icon="el-icon-search" size="mini" @click="handleQuery">搜索</el-button>
        <el-button icon="el-icon-refresh" size="mini" @click="resetQuery">重置</el-button>
      </el-form-item>
    </el-form>

    <el-row :gutter="10" class="mb8">
      <el-col :span="1.5">
        <el-button
          type="primary"
          plain
          icon="el-icon-plus"
          size="mini"
          @click="handleAdd"
          v-hasPermi="['ai:promptTemplate:add']"
        >新增</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button
          type="success"
          plain
          icon="el-icon-edit"
          size="mini"
          :disabled="single"
          @click="handleUpdate"
          v-hasPermi="['ai:promptTemplate:edit']"
        >修改</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button
          type="danger"
          plain
          icon="el-icon-delete"
          size="mini"
          :disabled="multiple"
          @click="handleDelete"
          v-hasPermi="['ai:promptTemplate:remove']"
        >删除</el-button>
      </el-col>
      <right-toolbar :showSearch.sync="showSearch" @queryTable="getList"></right-toolbar>
    </el-row>

    <el-table v-loading="loading" :data="promptTemplateList" @selection-change="handleSelectionChange">
      <el-table-column type="selection" width="55" align="center" />
      <el-table-column label="模板名称" align="left" prop="name" min-width="180" :show-overflow-tooltip="true" />
      <el-table-column label="分类" align="center" prop="category" width="140" :show-overflow-tooltip="true" />
      <el-table-column label="状态" align="center" prop="status" width="110">
        <template slot-scope="scope">
          <el-tag :type="getStatusType(scope.row.status)" size="mini">{{ getStatusLabel(scope.row.status) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="版本" align="center" prop="version" width="90" />
      <el-table-column label="创建人" align="center" prop="createdBy" width="110" />
      <el-table-column label="更新时间" align="center" prop="updatedAt" width="180">
        <template slot-scope="scope">
          <span>{{ parseTime(scope.row.updatedAt) }}</span>
        </template>
      </el-table-column>
      <el-table-column label="创建时间" align="center" prop="createdAt" width="180">
        <template slot-scope="scope">
          <span>{{ parseTime(scope.row.createdAt) }}</span>
        </template>
      </el-table-column>
      <el-table-column label="操作" align="center" class-name="small-padding fixed-width" width="210">
        <template slot-scope="scope">
          <el-button
            size="mini"
            type="text"
            icon="el-icon-view"
            @click="handleView(scope.row)"
            v-hasPermi="['ai:promptTemplate:query']"
          >详情</el-button>
          <el-button
            size="mini"
            type="text"
            icon="el-icon-edit"
            @click="handleUpdate(scope.row)"
            v-hasPermi="['ai:promptTemplate:edit']"
          >修改</el-button>
          <el-button
            size="mini"
            type="text"
            icon="el-icon-delete"
            @click="handleDelete(scope.row)"
            v-hasPermi="['ai:promptTemplate:remove']"
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

    <el-dialog :title="title" :visible.sync="open" width="980px" append-to-body>
      <el-form ref="form" :model="form" :rules="rules" label-width="96px">
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="模板名称" prop="name">
              <el-input v-model="form.name" placeholder="请输入模板名称" maxlength="255" show-word-limit />
            </el-form-item>
          </el-col>
          <el-col :span="6">
            <el-form-item label="分类" prop="category">
              <el-input v-model="form.category" placeholder="自由输入" maxlength="100" />
            </el-form-item>
          </el-col>
          <el-col :span="6">
            <el-form-item label="状态" prop="status">
              <el-select v-model="form.status" placeholder="请选择状态" style="width: 100%">
                <el-option
                  v-for="item in statusOptions"
                  :key="item.value"
                  :label="item.label"
                  :value="item.value"
                />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="描述" prop="description">
          <el-input v-model="form.description" type="textarea" :rows="2" placeholder="请输入模板用途或适用场景" />
        </el-form-item>
        <el-form-item label="Markdown" prop="body">
          <el-input
            v-model="form.body"
            type="textarea"
            :rows="18"
            class="prompt-markdown-editor"
            placeholder="# 角色&#10;你是一位专业助手。&#10;&#10;用户姓名：{name}"
          />
        </el-form-item>
        <el-form-item label="变量定义" prop="variables">
          <el-input
            v-model="form.variables"
            type="textarea"
            :rows="6"
            class="prompt-json-editor"
            placeholder='非必填，例如：[{"key":"name","label":"用户姓名","type":"text","required":false,"defaultValue":""}]'
          />
        </el-form-item>
      </el-form>
      <div slot="footer" class="dialog-footer">
        <el-button type="primary" @click="submitForm">确 定</el-button>
        <el-button @click="cancel">取 消</el-button>
      </div>
    </el-dialog>

    <el-dialog title="提示词模板详情" :visible.sync="detailOpen" width="980px" append-to-body>
      <el-descriptions :column="2" border size="small">
        <el-descriptions-item label="模板名称">{{ detail.name }}</el-descriptions-item>
        <el-descriptions-item label="分类">{{ detail.category }}</el-descriptions-item>
        <el-descriptions-item label="状态">{{ getStatusLabel(detail.status) }}</el-descriptions-item>
        <el-descriptions-item label="版本">{{ detail.version }}</el-descriptions-item>
        <el-descriptions-item label="创建人">{{ detail.createdBy }}</el-descriptions-item>
        <el-descriptions-item label="更新时间">{{ parseTime(detail.updatedAt) }}</el-descriptions-item>
        <el-descriptions-item label="描述" :span="2">{{ detail.description }}</el-descriptions-item>
      </el-descriptions>
      <div class="detail-block">
        <div class="detail-title">Markdown 正文</div>
        <pre class="detail-pre">{{ detail.body }}</pre>
      </div>
      <div class="detail-block">
        <div class="detail-title">变量定义</div>
        <pre class="detail-pre">{{ detail.variables }}</pre>
      </div>
    </el-dialog>
  </div>
</template>

<script>
import {
  listPromptTemplate,
  getPromptTemplate,
  delPromptTemplate,
  addPromptTemplate,
  updatePromptTemplate
} from '@/api/ai/promptTemplate'

export default {
  name: 'PromptTemplate',
  data() {
    return {
      loading: true,
      ids: [],
      single: true,
      multiple: true,
      showSearch: true,
      total: 0,
      promptTemplateList: [],
      title: '',
      open: false,
      detailOpen: false,
      queryParams: {
        pageNum: 1,
        pageSize: 10,
        name: undefined,
        category: undefined,
        status: undefined
      },
      statusOptions: [
        { label: '草稿', value: 'draft', type: 'info' },
        { label: '已发布', value: 'published', type: 'success' },
        { label: '已归档', value: 'archived', type: 'warning' }
      ],
      form: {},
      detail: {},
      rules: {
        name: [
          { required: true, message: '模板名称不能为空', trigger: 'blur' }
        ],
        status: [
          { required: true, message: '模板状态不能为空', trigger: 'change' }
        ],
        body: [
          { required: true, message: 'Markdown 正文不能为空', trigger: 'blur' }
        ]
      }
    }
  },
  created() {
    this.getList()
  },
  methods: {
    getList() {
      this.loading = true
      listPromptTemplate(this.queryParams).then(response => {
        this.promptTemplateList = response.rows
        this.total = response.total
        this.loading = false
      })
    },
    cancel() {
      this.open = false
      this.reset()
    },
    reset() {
      this.form = {
        id: undefined,
        name: undefined,
        description: undefined,
        category: undefined,
        status: 'draft',
        body: '',
        variables: ''
      }
      this.resetForm('form')
    },
    handleQuery() {
      this.queryParams.pageNum = 1
      this.getList()
    },
    resetQuery() {
      this.resetForm('queryForm')
      this.handleQuery()
    },
    handleAdd() {
      this.reset()
      this.open = true
      this.title = '新增提示词模板'
    },
    handleSelectionChange(selection) {
      this.ids = selection.map(item => item.id)
      this.single = selection.length !== 1
      this.multiple = !selection.length
    },
    handleView(row) {
      getPromptTemplate(row.id).then(response => {
        this.detail = this.toViewModel(response.data || {})
        this.detailOpen = true
      })
    },
    handleUpdate(row) {
      this.reset()
      const id = row.id || this.ids
      getPromptTemplate(id).then(response => {
        this.form = this.toViewModel(response.data || {})
        this.open = true
        this.title = '修改提示词模板'
      })
    },
    submitForm() {
      this.$refs.form.validate(valid => {
        if (!valid) {
          return
        }

        const variables = this.normalizeVariables(this.form.variables)
        if (variables === false) {
          return
        }

        const data = {
          id: this.form.id,
          name: this.form.name,
          description: this.form.description,
          category: this.form.category,
          status: this.form.status,
          // 后端保存 JSONB；后台人员只编辑 Markdown 正文。
          content: JSON.stringify({ format: 'markdown', body: this.form.body }),
          variables: variables
        }

        if (data.id !== undefined) {
          updatePromptTemplate(data).then(() => {
            this.$modal.msgSuccess('修改成功')
            this.open = false
            this.getList()
          })
        } else {
          addPromptTemplate(data).then(() => {
            this.$modal.msgSuccess('新增成功')
            this.open = false
            this.getList()
          })
        }
      })
    },
    handleDelete(row) {
      const ids = row.id || this.ids
      this.$modal.confirm('是否确认删除提示词模板编号为"' + ids + '"的数据项？').then(function() {
        return delPromptTemplate(ids)
      }).then(() => {
        this.getList()
        this.$modal.msgSuccess('删除成功')
      }).catch(() => {})
    },
    toViewModel(data) {
      const content = this.parseJson(data.content, {})
      return {
        id: data.id,
        name: data.name,
        description: data.description,
        category: data.category,
        status: data.status || 'draft',
        version: data.version,
        createdBy: data.createdBy,
        createdAt: data.createdAt,
        updatedAt: data.updatedAt,
        body: content.body || '',
        variables: this.formatJsonText(data.variables)
      }
    },
    parseJson(value, fallback) {
      if (!value) {
        return fallback
      }
      if (typeof value === 'object') {
        return value
      }
      try {
        return JSON.parse(value)
      } catch (e) {
        return fallback
      }
    },
    formatJsonText(value) {
      if (!value) {
        return ''
      }
      try {
        return JSON.stringify(this.parseJson(value, []), null, 2)
      } catch (e) {
        return value
      }
    },
    normalizeVariables(value) {
      if (!value || !value.trim()) {
        return '[]'
      }
      try {
        JSON.parse(value)
        return value
      } catch (e) {
        this.$modal.msgError('变量定义必须是合法 JSON')
        return false
      }
    },
    getStatusLabel(status) {
      const option = this.statusOptions.find(item => item.value === status)
      return option ? option.label : status
    },
    getStatusType(status) {
      const option = this.statusOptions.find(item => item.value === status)
      return option ? option.type : ''
    }
  }
}
</script>

<style scoped>
.prompt-markdown-editor ::v-deep textarea,
.prompt-json-editor ::v-deep textarea {
  font-family: Consolas, "Courier New", monospace;
  line-height: 1.6;
}

.prompt-markdown-editor ::v-deep textarea {
  min-height: 420px;
}

.detail-block {
  margin-top: 16px;
}

.detail-title {
  margin-bottom: 8px;
  color: #303133;
  font-weight: 600;
}

.detail-pre {
  min-height: 120px;
  max-height: 420px;
  margin: 0;
  padding: 12px;
  overflow: auto;
  border: 1px solid #dcdfe6;
  border-radius: 4px;
  background: #f8f8f9;
  color: #303133;
  font-family: Consolas, "Courier New", monospace;
  line-height: 1.6;
  white-space: pre-wrap;
  word-break: break-word;
}
</style>
