import test from 'node:test'
import assert from 'node:assert/strict'
import { selectImageFiles } from './imageUpload.js'

const file = (name, type) => ({ name, type })

test('selectImageFiles accepts image files up to the remaining upload slots', () => {
  const files = [
    file('front.png', 'image/png'),
    file('side.jpg', 'image/jpeg'),
    file('notes.txt', 'text/plain'),
    file('scene.webp', 'image/webp')
  ]

  const selected = selectImageFiles(files, 4)

  assert.deepEqual(selected.map((item) => item.name), ['front.png', 'side.jpg'])
})

test('selectImageFiles returns no files when the upload limit has been reached', () => {
  const selected = selectImageFiles([file('extra.png', 'image/png')], 6)

  assert.deepEqual(selected, [])
})
