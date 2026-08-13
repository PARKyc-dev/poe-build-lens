import { createHash } from 'node:crypto'
import { cp, mkdir, readFile, readdir, rm, stat, writeFile } from 'node:fs/promises'
import { join, relative } from 'node:path'
import { fileURLToPath } from 'node:url'
import { zipSync } from 'fflate'

const webRoot = fileURLToPath(new URL('..', import.meta.url))
const pobRoot = join(webRoot, 'pob/.cache/PathOfBuilding')
const outputRoot = join(webRoot, 'public/pob')
const lock = Object.fromEntries((await readFile(join(webRoot, 'pob/pob.lock'), 'utf8'))
  .trim().split('\n').map((line) => line.split('=')))
const latestTree = '3_29'

async function filesAt(root, current = root) {
  const entries = await readdir(current, { withFileTypes: true })
  const files = await Promise.all(entries.map(async (entry) => {
    const path = join(current, entry.name)
    return entry.isDirectory() ? filesAt(root, path) : [path]
  }))
  return files.flat()
}

async function makeArchive(files) {
  const entries = await Promise.all(files.map(async ({ path, target }) => [target, new Uint8Array(await readFile(path))]))
  return zipSync(Object.fromEntries(entries), { level: 9 })
}

function descriptor(path, bytes) {
  return {
    url: path,
    byteSize: bytes.byteLength,
    sha256: createHash('sha256').update(bytes).digest('hex'),
  }
}

const sourceRoot = join(pobRoot, 'src')
const sourceFiles = (await filesAt(sourceRoot)).filter((path) => {
  const sourcePath = relative(sourceRoot, path)
  return !sourcePath.startsWith('TreeData/')
    && !(sourcePath.startsWith('Data/TimelessJewelData/') && !sourcePath.endsWith('.lua'))
})
const runtimeFiles = await filesAt(join(pobRoot, 'runtime/lua'))
const coreFiles = [
  ...sourceFiles.map((path) => ({ path, target: relative(sourceRoot, path) })),
  ...runtimeFiles.map((path) => ({ path, target: relative(pobRoot, path) })),
  { path: join(pobRoot, 'manifest.xml'), target: 'manifest.xml' },
  { path: join(sourceRoot, `TreeData/${latestTree}/tree.lua`), target: `TreeData/${latestTree}/tree.lua` },
  { path: join(sourceRoot, `TreeData/${latestTree}/sprites.lua`), target: `TreeData/${latestTree}/sprites.lua` },
  { path: join(sourceRoot, 'TreeData/3_19/Assets.lua'), target: 'TreeData/3_19/Assets.lua' },
  { path: join(sourceRoot, 'TreeData/legion/tree-legion.lua'), target: 'TreeData/legion/tree-legion.lua' },
]

await rm(outputRoot, { recursive: true, force: true })
await mkdir(join(outputRoot, 'trees'), { recursive: true })

const core = await makeArchive(coreFiles)
await writeFile(join(outputRoot, 'core.zip'), core)

const trees = {}
for (const entry of await readdir(join(sourceRoot, 'TreeData'), { withFileTypes: true })) {
  if (!entry.isDirectory() || entry.name === 'legion') continue
  const treeRoot = join(sourceRoot, 'TreeData', entry.name)
  const treeFile = join(treeRoot, 'tree.lua')
  try {
    await stat(treeFile)
  } catch {
    continue
  }
  const treeFiles = [{ path: treeFile, target: relative(sourceRoot, treeFile) }]
  const spriteFile = join(treeRoot, 'sprites.lua')
  try {
    await stat(spriteFile)
    treeFiles.push({ path: spriteFile, target: relative(sourceRoot, spriteFile) })
  } catch {}
  for (const shared of [
    join(sourceRoot, 'TreeData/3_19/Assets.lua'),
    join(sourceRoot, 'TreeData/legion/tree-legion.lua'),
  ]) {
    try {
      await stat(shared)
      treeFiles.push({ path: shared, target: relative(sourceRoot, shared) })
    } catch {}
  }
  const bytes = await makeArchive(treeFiles)
  const file = `trees/${entry.name}.zip`
  await writeFile(join(outputRoot, file), bytes)
  trees[entry.name] = descriptor(`/pob/${file}`, bytes)
}

await cp(join(pobRoot, 'LICENSE.md'), join(outputRoot, 'LICENSE.md'))
await writeFile(join(outputRoot, 'manifest.json'), JSON.stringify({
  schemaVersion: 1,
  pobTag: lock.POB_TAG,
  pobCommit: lock.POB_COMMIT,
  sourceLicense: '/pob/LICENSE.md',
  core: descriptor('/pob/core.zip', core),
  latestTree,
  trees,
}, null, 2) + '\n')
