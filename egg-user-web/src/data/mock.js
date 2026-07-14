export const primaryNavItems = [
  { label: '全品类商品图', path: '/generate/product-images', icon: 'Box' },
  { label: '商品库', path: '/products', icon: 'Goods' },
  { label: '风格复刻', path: '/generate/style-clone', icon: 'Picture' },
  { label: '智能聊天', path: '/chat', icon: 'ChatDotRound' },
  { label: '剧情生成', path: '/generate/story', icon: 'DocumentAdd' },
  { label: '任务中心', path: '/tasks', icon: 'Clock' }
]

export const moreToolItems = [
  { label: '批量精修', path: '/generate/refinement', icon: 'Brush' },
  { label: '万能画布', path: '/canvas', icon: 'Crop', badge: 'Beta' },
  { label: '电商视频', path: '/ecommerce-video', icon: 'VideoPlay', badge: 'Beta' },
  { label: '价格', path: '/pricing', icon: 'Tickets' },
  { label: '配置说明', path: '/settings', icon: 'Setting' },
  { label: 'A2A', path: '/a2a', icon: 'Share' }
]

export const products = [
  {
    id: 'p-1001',
    name: '磁吸无线充电支架',
    category: '数码配件',
    platform: '抖音 / 天猫',
    status: '已分析',
    updatedAt: '2026-06-27 15:20',
    image: 'linear-gradient(135deg, #f8f1e7, #d9ecff)',
    highlights: ['强磁吸附', '桌面收纳', '快充安全']
  },
  {
    id: 'p-1002',
    name: '便携榨汁杯',
    category: '小家电',
    platform: '小红书 / 拼多多',
    status: '生成中',
    updatedAt: '2026-06-27 14:42',
    image: 'linear-gradient(135deg, #e7f9ee, #fff3c4)',
    highlights: ['户外便携', '食品级材质', '一杯一餐']
  },
  {
    id: 'p-1003',
    name: '夏季防晒冰丝外套',
    category: '服装',
    platform: '淘宝 / TikTok Shop',
    status: '待审核',
    updatedAt: '2026-06-27 13:58',
    image: 'linear-gradient(135deg, #e8f2ff, #f8e8ff)',
    highlights: ['轻薄透气', '通勤防晒', '多色套图']
  }
]

export const assets = [
  {
    id: 'a-001',
    title: '磁吸支架主图 1:1',
    type: '主图',
    product: '磁吸无线充电支架',
    audit: '通过',
    version: 'v3',
    model: 'image-gen-primary',
    tone: 'clean'
  },
  {
    id: 'a-002',
    title: '便携榨汁杯场景图',
    type: '场景图',
    product: '便携榨汁杯',
    audit: '待审核',
    version: 'v1',
    model: 'image-gen-primary',
    tone: 'fresh'
  },
  {
    id: 'a-003',
    title: '冰丝外套风格复刻',
    type: '风格复刻',
    product: '夏季防晒冰丝外套',
    audit: '风险',
    version: 'v2',
    model: 'style-clone',
    tone: 'soft'
  }
]

export const tasks = [
  {
    id: 't-9001',
    name: '生成磁吸支架详情页组图',
    type: '详情页组图',
    product: '磁吸无线充电支架',
    status: '执行中',
    progress: 68,
    reason: ''
  },
  {
    id: 't-9002',
    name: '榨汁杯 RAG 合规问答',
    type: '智能聊天',
    product: '便携榨汁杯',
    status: '成功',
    progress: 100,
    reason: ''
  },
  {
    id: 't-9003',
    name: '冰丝外套模特图审核',
    type: '审核',
    product: '夏季防晒冰丝外套',
    status: '失败',
    progress: 100,
    reason: '主体袖口细节与商品原图不一致'
  }
]

export const generationModes = [
  {
    title: '全品类商品图',
    desc: '主图、场景图、卖点图、详情页组图一站式生成。',
    route: '/generate/product-images',
    icon: 'PictureFilled',
    color: '#2f6fed'
  },
  {
    title: '风格复刻',
    desc: '上传参考图，保留商品主体，复刻构图、光线和质感。',
    route: '/generate/style-clone',
    icon: 'MagicStick',
    color: '#7a5af8'
  },
  {
    title: '服装组图',
    desc: '支持模特图、人台图、平铺图、细节图的套图规划。',
    route: '/generate/apparel',
    icon: 'UserFilled',
    color: '#17a673'
  }
]

export const moduleConfigs = [
  {
    name: 'PostgreSQL + pgvector',
    why: '保存商品、素材、任务、聊天记录、Prompt 版本、模型日志和向量索引。',
    how: 'Docker 运行 pgvector 镜像；后端通过 EGG_DATASOURCE_URL 连接。',
    file: 'compose.postgres.yml / application-druid.yml'
  },
  {
    name: 'Redis',
    why: '支撑登录缓存、AI 任务队列、进度、限流、防重复提交和短期聊天上下文。',
    how: 'Docker 暴露 6380 到本机；后端 spring.data.redis 使用 EGG_REDIS_*。',
    file: 'compose.postgres.yml / application.yml'
  },
  {
    name: 'MinIO',
    why: '保存商品原图、AI 生成图、编辑图和导出包，避免依赖模型临时 URL。',
    how: 'Docker 暴露 9000 API 与 9001 控制台；后端 egg.ai.storage 使用 EGG_MINIO_*。',
    file: 'compose.postgres.yml / application.yml'
  },
  {
    name: '模型网关',
    why: '统一文本、视觉理解、图片生成、Embedding、Coze 和后续 A2A 调用日志。',
    how: '配置 provider、endpoint、model、timeout、retry 和 mock 开关。',
    file: 'application.yml'
  },
  {
    name: 'RAG',
    why: '让智能聊天和审核使用平台规则、品牌规则、商品资料与运营知识。',
    how: '配置向量库 pgvector、embedding 模型、chunk 大小和 topK。',
    file: 'application.yml'
  }
]
