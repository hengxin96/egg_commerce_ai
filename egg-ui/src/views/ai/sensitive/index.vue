<template>
  <div class="app-container">
    <el-tabs v-model="activeTab" @tab-click="handleTabClick">
      <el-tab-pane label="分类管理" name="category">
        <el-form
          ref="categoryQueryForm"
          :model="categoryQueryParams"
          size="small"
          :inline="true"
          v-show="showSearch"
          label-width="80px"
        >
          <el-form-item label="分类名称" prop="categoryName">
            <el-input
              v-model="categoryQueryParams.categoryName"
              placeholder="请输入分类名称"
              clearable
              style="width: 240px"
              @keyup.enter.native="handleCategoryQuery"
            />
          </el-form-item>
          <el-form-item label="状态" prop="status">
            <el-select
              v-model="categoryQueryParams.status"
              placeholder="请选择状态"
              clearable
              style="width: 180px"
            >
              <el-option
                v-for="item in statusOptions"
                :key="item.value"
                :label="item.label"
                :value="item.value"
              />
            </el-select>
          </el-form-item>
          <el-form-item>
            <el-button type="primary" icon="el-icon-search" size="mini" @click="handleCategoryQuery">搜索</el-button>
            <el-button icon="el-icon-refresh" size="mini" @click="resetCategoryQuery">重置</el-button>
          </el-form-item>
        </el-form>

        <el-row :gutter="10" class="mb8">
          <el-col :span="1.5">
            <el-button
              type="primary"
              plain
              icon="el-icon-plus"
              size="mini"
              @click="handleAddCategory"
              v-hasPermi="['ai:sensitive:category:add']"
            >新增</el-button>
          </el-col>
          <el-col :span="1.5">
            <el-button
              type="success"
              plain
              icon="el-icon-edit"
              size="mini"
              :disabled="categorySingle"
              @click="handleUpdateCategory"
              v-hasPermi="['ai:sensitive:category:edit']"
            >修改</el-button>
          </el-col>
          <el-col :span="1.5">
            <el-button
              type="danger"
              plain
              icon="el-icon-delete"
              size="mini"
              :disabled="categoryMultiple"
              @click="handleDeleteCategory"
              v-hasPermi="['ai:sensitive:category:remove']"
            >删除</el-button>
          </el-col>
          <right-toolbar :showSearch.sync="showSearch" @queryTable="refreshActiveList"></right-toolbar>
        </el-row>

        <el-table v-loading="categoryLoading" :data="categoryList" @selection-change="handleCategorySelectionChange">
          <el-table-column type="selection" width="55" align="center" />
          <el-table-column label="分类编号" align="center" prop="id" width="100" />
          <el-table-column label="分类名称" align="left" prop="categoryName" :show-overflow-tooltip="true" />
          <el-table-column label="状态" align="center" prop="status" width="100">
            <template slot-scope="scope">
              <el-tag :type="statusType(scope.row.status)" size="mini">{{ statusLabel(scope.row.status) }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column label="创建时间" align="center" prop="createdAt" width="180">
            <template slot-scope="scope">
              <span>{{ parseTime(scope.row.createdAt) }}</span>
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
                size="mini"
                type="text"
                icon="el-icon-edit"
                @click="handleUpdateCategory(scope.row)"
                v-hasPermi="['ai:sensitive:category:edit']"
              >修改</el-button>
              <el-button
                size="mini"
                type="text"
                icon="el-icon-delete"
                @click="handleDeleteCategory(scope.row)"
                v-hasPermi="['ai:sensitive:category:remove']"
              >删除</el-button>
            </template>
          </el-table-column>
        </el-table>

        <pagination
          v-show="categoryTotal>0"
          :total="categoryTotal"
          :page.sync="categoryQueryParams.pageNum"
          :limit.sync="categoryQueryParams.pageSize"
          @pagination="getCategoryList"
        />
      </el-tab-pane>

      <el-tab-pane label="词条管理" name="word">
        <el-form
          ref="wordQueryForm"
          :model="wordQueryParams"
          size="small"
          :inline="true"
          v-show="showSearch"
          label-width="80px"
        >
          <el-form-item label="敏感词" prop="word">
            <el-input
              v-model="wordQueryParams.word"
              placeholder="请输入敏感词"
              clearable
              style="width: 220px"
              @keyup.enter.native="handleWordQuery"
            />
          </el-form-item>
          <el-form-item label="所属分类" prop="categoryId">
            <el-select
              v-model="wordQueryParams.categoryId"
              placeholder="请选择分类"
              clearable
              filterable
              style="width: 220px"
            >
              <el-option
                v-for="item in categoryOptions"
                :key="item.id"
                :label="item.categoryName"
                :value="item.id"
              />
            </el-select>
          </el-form-item>
          <el-form-item label="状态" prop="status">
            <el-select
              v-model="wordQueryParams.status"
              placeholder="请选择状态"
              clearable
              style="width: 180px"
            >
              <el-option
                v-for="item in statusOptions"
                :key="item.value"
                :label="item.label"
                :value="item.value"
              />
            </el-select>
          </el-form-item>
          <el-form-item>
            <el-button type="primary" icon="el-icon-search" size="mini" @click="handleWordQuery">搜索</el-button>
            <el-button icon="el-icon-refresh" size="mini" @click="resetWordQuery">重置</el-button>
          </el-form-item>
        </el-form>

        <el-row :gutter="10" class="mb8">
          <el-col :span="1.5">
            <el-button
              type="primary"
              plain
              icon="el-icon-plus"
              size="mini"
              @click="handleAddWord"
              v-hasPermi="['ai:sensitive:word:add']"
            >新增</el-button>
          </el-col>
          <el-col :span="1.5">
            <el-button
              type="success"
              plain
              icon="el-icon-edit"
              size="mini"
              :disabled="wordSingle"
              @click="handleUpdateWord"
              v-hasPermi="['ai:sensitive:word:edit']"
            >修改</el-button>
          </el-col>
          <el-col :span="1.5">
            <el-button
              type="danger"
              plain
              icon="el-icon-delete"
              size="mini"
              :disabled="wordMultiple"
              @click="handleDeleteWord"
              v-hasPermi="['ai:sensitive:word:remove']"
            >删除</el-button>
          </el-col>
          <right-toolbar :showSearch.sync="showSearch" @queryTable="refreshActiveList"></right-toolbar>
        </el-row>

        <el-table v-loading="wordLoading" :data="wordList" @selection-change="handleWordSelectionChange">
          <el-table-column type="selection" width="55" align="center" />
          <el-table-column label="词条编号" align="center" prop="id" width="100" />
          <el-table-column label="敏感词" align="left" prop="word" min-width="180" :show-overflow-tooltip="true" />
          <el-table-column label="所属分类" align="left" prop="categoryName" min-width="160" :show-overflow-tooltip="true">
            <template slot-scope="scope">
              <span>{{ scope.row.categoryName || '-' }}</span>
            </template>
          </el-table-column>
          <el-table-column label="状态" align="center" prop="status" width="100">
            <template slot-scope="scope">
              <el-tag :type="statusType(scope.row.status)" size="mini">{{ statusLabel(scope.row.status) }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column label="创建时间" align="center" prop="createdAt" width="180">
            <template slot-scope="scope">
              <span>{{ parseTime(scope.row.createdAt) }}</span>
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
                size="mini"
                type="text"
                icon="el-icon-edit"
                @click="handleUpdateWord(scope.row)"
                v-hasPermi="['ai:sensitive:word:edit']"
              >修改</el-button>
              <el-button
                size="mini"
                type="text"
                icon="el-icon-delete"
                @click="handleDeleteWord(scope.row)"
                v-hasPermi="['ai:sensitive:word:remove']"
              >删除</el-button>
            </template>
          </el-table-column>
        </el-table>

        <pagination
          v-show="wordTotal>0"
          :total="wordTotal"
          :page.sync="wordQueryParams.pageNum"
          :limit.sync="wordQueryParams.pageSize"
          @pagination="getWordList"
        />
      </el-tab-pane>
    </el-tabs>

    <el-dialog :title="categoryTitle" :visible.sync="categoryOpen" width="500px" append-to-body>
      <el-form ref="categoryForm" :model="categoryForm" :rules="categoryRules" label-width="90px">
        <el-form-item label="分类名称" prop="categoryName">
          <el-input v-model="categoryForm.categoryName" placeholder="请输入分类名称" maxlength="100" />
        </el-form-item>
        <el-form-item label="状态" prop="status">
          <el-radio-group v-model="categoryForm.status">
            <el-radio
              v-for="item in statusOptions"
              :key="item.value"
              :label="item.value"
            >{{ item.label }}</el-radio>
          </el-radio-group>
        </el-form-item>
      </el-form>
      <div slot="footer" class="dialog-footer">
        <el-button type="primary" @click="submitCategoryForm">确 定</el-button>
        <el-button @click="cancelCategory">取 消</el-button>
      </div>
    </el-dialog>

    <el-dialog :title="wordTitle" :visible.sync="wordOpen" width="520px" append-to-body>
      <el-form ref="wordForm" :model="wordForm" :rules="wordRules" label-width="90px">
        <el-form-item label="敏感词" prop="word">
          <el-input v-model="wordForm.word" placeholder="请输入敏感词" maxlength="255" />
        </el-form-item>
        <el-form-item label="所属分类" prop="categoryId">
          <el-select v-model="wordForm.categoryId" placeholder="请选择分类" filterable style="width: 100%">
            <el-option
              v-for="item in categoryOptions"
              :key="item.id"
              :label="item.categoryName"
              :value="item.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="状态" prop="status">
          <el-radio-group v-model="wordForm.status">
            <el-radio
              v-for="item in statusOptions"
              :key="item.value"
              :label="item.value"
            >{{ item.label }}</el-radio>
          </el-radio-group>
        </el-form-item>
      </el-form>
      <div slot="footer" class="dialog-footer">
        <el-button type="primary" @click="submitWordForm">确 定</el-button>
        <el-button @click="cancelWord">取 消</el-button>
      </div>
    </el-dialog>
  </div>
</template>

<script>
import {
  addSensitiveCategory,
  addSensitiveWord,
  delSensitiveCategory,
  delSensitiveWord,
  getSensitiveCategory,
  getSensitiveWord,
  listSensitiveCategory,
  listSensitiveWord,
  updateSensitiveCategory,
  updateSensitiveWord
} from '@/api/ai/sensitive'

export default {
  name: 'AiSensitive',
  data() {
    return {
      activeTab: 'category',
      showSearch: true,
      categoryLoading: true,
      wordLoading: true,
      categoryIds: [],
      wordIds: [],
      categorySingle: true,
      categoryMultiple: true,
      wordSingle: true,
      wordMultiple: true,
      categoryTotal: 0,
      wordTotal: 0,
      categoryList: [],
      wordList: [],
      categoryOptions: [],
      categoryTitle: '',
      wordTitle: '',
      categoryOpen: false,
      wordOpen: false,
      statusOptions: [
        { label: '启用', value: 'enabled', type: 'success' },
        { label: '停用', value: 'disabled', type: 'info' }
      ],
      categoryQueryParams: {
        pageNum: 1,
        pageSize: 10,
        categoryName: undefined,
        status: undefined
      },
      wordQueryParams: {
        pageNum: 1,
        pageSize: 10,
        word: undefined,
        categoryId: undefined,
        status: undefined
      },
      categoryForm: {},
      wordForm: {},
      categoryRules: {
        categoryName: [
          { required: true, message: '分类名称不能为空', trigger: 'blur' }
        ],
        status: [
          { required: true, message: '状态不能为空', trigger: 'change' }
        ]
      },
      wordRules: {
        word: [
          { required: true, message: '敏感词不能为空', trigger: 'blur' }
        ],
        categoryId: [
          { required: true, message: '所属分类不能为空', trigger: 'change' }
        ],
        status: [
          { required: true, message: '状态不能为空', trigger: 'change' }
        ]
      }
    }
  },
  created() {
    this.getCategoryList()
    this.getWordList()
    this.getCategoryOptions()
  },
  methods: {
    getCategoryList() {
      this.categoryLoading = true
      listSensitiveCategory(this.categoryQueryParams).then(response => {
        this.categoryList = response.rows || []
        this.categoryTotal = response.total || 0
        this.categoryLoading = false
      }).catch(() => {
        this.categoryLoading = false
      })
    },
    getWordList() {
      this.wordLoading = true
      listSensitiveWord(this.wordQueryParams).then(response => {
        this.wordList = response.rows || []
        this.wordTotal = response.total || 0
        this.wordLoading = false
      }).catch(() => {
        this.wordLoading = false
      })
    },
    getCategoryOptions() {
      listSensitiveCategory({ pageNum: 1, pageSize: 1000 }).then(response => {
        this.categoryOptions = response.rows || []
      })
    },
    refreshActiveList() {
      if (this.activeTab === 'category') {
        this.getCategoryList()
      } else {
        this.getWordList()
      }
    },
    handleTabClick() {
      this.refreshActiveList()
    },
    handleCategoryQuery() {
      this.categoryQueryParams.pageNum = 1
      this.getCategoryList()
    },
    resetCategoryQuery() {
      this.resetForm('categoryQueryForm')
      this.handleCategoryQuery()
    },
    handleWordQuery() {
      this.wordQueryParams.pageNum = 1
      this.getWordList()
    },
    resetWordQuery() {
      this.resetForm('wordQueryForm')
      this.handleWordQuery()
    },
    handleCategorySelectionChange(selection) {
      this.categoryIds = selection.map(item => item.id)
      this.categorySingle = selection.length !== 1
      this.categoryMultiple = !selection.length
    },
    handleWordSelectionChange(selection) {
      this.wordIds = selection.map(item => item.id)
      this.wordSingle = selection.length !== 1
      this.wordMultiple = !selection.length
    },
    resetCategory() {
      this.categoryForm = {
        id: undefined,
        categoryName: undefined,
        status: 'enabled'
      }
      this.resetForm('categoryForm')
    },
    resetWord() {
      this.wordForm = {
        id: undefined,
        word: undefined,
        categoryId: undefined,
        status: 'enabled'
      }
      this.resetForm('wordForm')
    },
    handleAddCategory() {
      this.resetCategory()
      this.categoryOpen = true
      this.categoryTitle = '添加敏感词分类'
    },
    handleUpdateCategory(row) {
      this.resetCategory()
      const id = row.id || this.categoryIds
      getSensitiveCategory(id).then(response => {
        this.categoryForm = response.data
        this.categoryOpen = true
        this.categoryTitle = '修改敏感词分类'
      })
    },
    submitCategoryForm() {
      this.$refs.categoryForm.validate(valid => {
        if (valid) {
          if (this.categoryForm.id !== undefined) {
            updateSensitiveCategory(this.categoryForm).then(() => {
              this.$modal.msgSuccess('修改成功')
              this.categoryOpen = false
              this.getCategoryList()
              this.getCategoryOptions()
              this.getWordList()
            })
          } else {
            addSensitiveCategory(this.categoryForm).then(() => {
              this.$modal.msgSuccess('新增成功')
              this.categoryOpen = false
              this.getCategoryList()
              this.getCategoryOptions()
            })
          }
        }
      })
    },
    handleDeleteCategory(row) {
      const ids = row.id || this.categoryIds
      this.$modal.confirm('是否确认删除敏感词分类编号为"' + ids + '"的数据项？').then(() => {
        return delSensitiveCategory(ids)
      }).then(() => {
        this.$modal.msgSuccess('删除成功')
        this.getCategoryList()
        this.getCategoryOptions()
      }).catch(() => {})
    },
    cancelCategory() {
      this.categoryOpen = false
      this.resetCategory()
    },
    handleAddWord() {
      this.resetWord()
      this.wordOpen = true
      this.wordTitle = '添加敏感词'
    },
    handleUpdateWord(row) {
      this.resetWord()
      const id = row.id || this.wordIds
      getSensitiveWord(id).then(response => {
        this.wordForm = response.data
        this.wordOpen = true
        this.wordTitle = '修改敏感词'
      })
    },
    submitWordForm() {
      this.$refs.wordForm.validate(valid => {
        if (valid) {
          if (this.wordForm.id !== undefined) {
            updateSensitiveWord(this.wordForm).then(() => {
              this.$modal.msgSuccess('修改成功')
              this.wordOpen = false
              this.getWordList()
            })
          } else {
            addSensitiveWord(this.wordForm).then(() => {
              this.$modal.msgSuccess('新增成功')
              this.wordOpen = false
              this.getWordList()
            })
          }
        }
      })
    },
    handleDeleteWord(row) {
      const ids = row.id || this.wordIds
      this.$modal.confirm('是否确认删除敏感词编号为"' + ids + '"的数据项？').then(() => {
        return delSensitiveWord(ids)
      }).then(() => {
        this.$modal.msgSuccess('删除成功')
        this.getWordList()
      }).catch(() => {})
    },
    cancelWord() {
      this.wordOpen = false
      this.resetWord()
    },
    statusLabel(status) {
      const item = this.statusOptions.find(option => option.value === status)
      return item ? item.label : status || '-'
    },
    statusType(status) {
      const item = this.statusOptions.find(option => option.value === status)
      return item ? item.type : 'info'
    }
  }
}
</script>
