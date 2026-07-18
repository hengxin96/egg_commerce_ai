import { readFileSync } from 'node:fs'
import { test } from 'node:test'
import assert from 'node:assert/strict'

test('front-stage primary navigation hides style clone entry', () => {
  const nav = readFileSync(new URL('../data/mock.js', import.meta.url), 'utf8')
  const primaryNavBlock = nav.match(/export const primaryNavItems = \[[\s\S]*?\]/)?.[0] || ''

  assert.match(primaryNavBlock, /\u5168\u54c1\u7c7b\u5546\u54c1\u56fe/)
  assert.match(primaryNavBlock, /\u5546\u54c1\u5e93/)
  assert.match(primaryNavBlock, /\u4efb\u52a1\u4e2d\u5fc3/)
  assert.doesNotMatch(primaryNavBlock, /\u98ce\u683c\u590d\u523b/)
  assert.doesNotMatch(primaryNavBlock, /\/generate\/style-clone/)
})
