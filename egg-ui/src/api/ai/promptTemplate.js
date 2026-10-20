import request from '@/utils/request'

// 查询提示词模板列表
export function listPromptTemplate(query) {
  return request({
    url: '/ai/promptTemplate/list',
    method: 'get',
    params: query
  })
}

// 查询提示词模板详情
export function getPromptTemplate(id) {
  return request({
    url: '/ai/promptTemplate/' + id,
    method: 'get'
  })
}

// 新增提示词模板
export function addPromptTemplate(data) {
  return request({
    url: '/ai/promptTemplate',
    method: 'post',
    data: data
  })
}

// 修改提示词模板
export function updatePromptTemplate(data) {
  return request({
    url: '/ai/promptTemplate',
    method: 'put',
    data: data
  })
}

// 删除提示词模板
export function delPromptTemplate(id) {
  return request({
    url: '/ai/promptTemplate/' + id,
    method: 'delete'
  })
}
