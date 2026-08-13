export type PassiveTreeNode = {
  id: string
  x: number
  y: number
  allocated: boolean
}

export type PassiveTreeLink = {
  from: string
  to: string
}

export type PassiveTree = {
  version: string
  nodes: PassiveTreeNode[]
  links: PassiveTreeLink[]
}

type DrawPoint = {
  x: number
  y: number
}

export type PassiveTreeDrawModel = {
  nodes: Array<PassiveTreeNode & DrawPoint>
  lines: Array<{ from: DrawPoint; to: DrawPoint }>
}

export function createPassiveTreeDrawModel(
  tree: PassiveTree,
  bounds: { width: number; height: number; padding: number },
): PassiveTreeDrawModel {
  if (tree.nodes.length === 0) return { nodes: [], lines: [] }

  const xValues = tree.nodes.map((node) => node.x)
  const yValues = tree.nodes.map((node) => node.y)
  const minX = Math.min(...xValues)
  const maxX = Math.max(...xValues)
  const minY = Math.min(...yValues)
  const maxY = Math.max(...yValues)
  const availableWidth = bounds.width - bounds.padding * 2
  const availableHeight = bounds.height - bounds.padding * 2
  const scaleX = maxX === minX ? 0 : availableWidth / (maxX - minX)
  const scaleY = maxY === minY ? 0 : availableHeight / (maxY - minY)

  const nodes = tree.nodes.map((node) => ({
    ...node,
    x: scaleX === 0 ? bounds.width / 2 : bounds.padding + (node.x - minX) * scaleX,
    y: scaleY === 0 ? bounds.height / 2 : bounds.padding + (node.y - minY) * scaleY,
  }))
  const byId = new Map(nodes.map((node) => [node.id, node]))
  const lines = tree.links.flatMap((link) => {
    const from = byId.get(link.from)
    const to = byId.get(link.to)
    return from && to ? [{ from: { x: from.x, y: from.y }, to: { x: to.x, y: to.y } }] : []
  })

  return { nodes, lines }
}
