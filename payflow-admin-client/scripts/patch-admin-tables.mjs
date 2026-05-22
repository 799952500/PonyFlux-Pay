/**
 * 为所有 data-table 启用 table-layout="auto"，并规范化常见列宽写法
 */
import fs from 'fs'
import path from 'path'

const srcDir = path.join(import.meta.dirname, '..', 'src')

function walk(dir, out = []) {
  for (const ent of fs.readdirSync(dir, { withFileTypes: true })) {
    const p = path.join(dir, ent.name)
    if (ent.isDirectory()) walk(p, out)
    else if (ent.name.endsWith('.vue')) out.push(p)
  }
  return out
}

function patch(content) {
  let s = content
  s = s.replace(/class="data-table data-table--auto"/g, 'class="data-table"')

  s = s.replace(/<el-table\b([^>]*)>/g, (tag, attrs) => {
    if (!/class="[^"]*data-table/.test(attrs)) return tag
    if (/table-layout=/.test(attrs)) return tag
    return `<el-table table-layout="auto"${attrs}>`
  })

  const datetimeLabels = ['创建时间', '时间', '决策时间', '变更时间', '申请时间', '处理时间', '最近扫描']
  for (const label of datetimeLabels) {
    const re = new RegExp(
      `<el-table-column label="${label}"([^>]*?) width="172"`,
      'g',
    )
    s = s.replace(
      re,
      `<el-table-column label="${label}"$1 min-width="168" class-name="col-datetime" show-overflow-tooltip`,
    )
    const re170 = new RegExp(
      `<el-table-column label="${label}"([^>]*?) width="170"`,
      'g',
    )
    s = s.replace(
      re170,
      `<el-table-column label="${label}"$1 min-width="168" class-name="col-datetime" show-overflow-tooltip`,
    )
  }

  s = s.replace(
    /<el-table-column label="操作" width="(\d+)"([^>]*?)fixed="right"/g,
    '<el-table-column label="操作" min-width="$1" class-name="col-actions"$2fixed="right"',
  )
  s = s.replace(
    /<el-table-column label="操作" min-width="(\d+)"(?![^>]*class-name="col-actions")([^>]*?)fixed="right"/g,
    '<el-table-column label="操作" min-width="$1" class-name="col-actions"$2fixed="right"',
  )
  s = s.replace(/class-name="col-actions" class-name="col-actions"/g, 'class-name="col-actions"')

  return s
}

let changed = 0
for (const file of walk(srcDir)) {
  const raw = fs.readFileSync(file, 'utf8')
  const next = patch(raw)
  if (next !== raw) {
    fs.writeFileSync(file, next, 'utf8')
    changed++
    console.log('patched', path.relative(srcDir, file))
  }
}
console.log('done, files:', changed)
