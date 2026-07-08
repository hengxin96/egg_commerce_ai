import request from '@/utils/request'

export function listKnowledge(query) {
  return request({
    url: '/ai/knowledge/list',
    method: 'get',
    params: query
  })
}

export function uploadKnowledge(data) {
  return request({
    url: '/ai/knowledge/upload',
    method: 'post',
    data,
    headers: { 'Content-Type': 'multipart/form-data' }
  })
}

export function delKnowledge(ids) {
  return request({
    url: '/ai/knowledge/' + ids,
    method: 'delete'
  })
}

export function retryKnowledge(id) {
  return request({
    url: '/ai/knowledge/' + id + '/retry',
    method: 'post'
  })
}
