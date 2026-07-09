import request from '@/utils/request'

export function listSensitiveCategory(query) {
  return request({
    url: '/ai/sensitive/category/list',
    method: 'get',
    params: query
  })
}

export function getSensitiveCategory(id) {
  return request({
    url: '/ai/sensitive/category/' + id,
    method: 'get'
  })
}

export function addSensitiveCategory(data) {
  return request({
    url: '/ai/sensitive/category',
    method: 'post',
    data
  })
}

export function updateSensitiveCategory(data) {
  return request({
    url: '/ai/sensitive/category',
    method: 'put',
    data
  })
}

export function delSensitiveCategory(ids) {
  return request({
    url: '/ai/sensitive/category/' + ids,
    method: 'delete'
  })
}

export function listSensitiveWord(query) {
  return request({
    url: '/ai/sensitive/word/list',
    method: 'get',
    params: query
  })
}

export function getSensitiveWord(id) {
  return request({
    url: '/ai/sensitive/word/' + id,
    method: 'get'
  })
}

export function addSensitiveWord(data) {
  return request({
    url: '/ai/sensitive/word',
    method: 'post',
    data
  })
}

export function updateSensitiveWord(data) {
  return request({
    url: '/ai/sensitive/word',
    method: 'put',
    data
  })
}

export function delSensitiveWord(ids) {
  return request({
    url: '/ai/sensitive/word/' + ids,
    method: 'delete'
  })
}
