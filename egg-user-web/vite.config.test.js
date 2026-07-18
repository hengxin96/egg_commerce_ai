import { readFileSync } from 'node:fs'
import { test } from 'node:test'
import assert from 'node:assert/strict'

const viteConfig = readFileSync(new URL('./vite.config.js', import.meta.url), 'utf8')

test('development proxy strips the frontend api prefix before forwarding to backend', () => {
  assert.match(viteConfig, /'\/api':\s*\{/)
  assert.match(viteConfig, /rewrite:\s*\(path\)\s*=>\s*path\.replace\(\s*\/\^\\\/api\/,\s*''\s*\)/)
})
