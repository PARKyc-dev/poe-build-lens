import { useEffect, useRef } from 'react'

import { createPassiveTreeDrawModel, type PassiveTree } from './passiveTree'

const width = 720
const height = 520

export function PassiveTreeCanvas({ tree }: { tree: PassiveTree }) {
  const canvasRef = useRef<HTMLCanvasElement>(null)
  const model = createPassiveTreeDrawModel(tree, { width, height, padding: 28 })

  useEffect(() => {
    const context = canvasRef.current?.getContext('2d')
    if (!context) return
    context.clearRect(0, 0, width, height)
    context.strokeStyle = '#50677d'
    context.lineWidth = 1.5
    for (const line of model.lines) {
      context.beginPath()
      context.moveTo(line.from.x, line.from.y)
      context.lineTo(line.to.x, line.to.y)
      context.stroke()
    }
    for (const node of model.nodes) {
      context.beginPath()
      context.fillStyle = node.allocated ? '#e4bd6d' : '#6f8398'
      context.arc(node.x, node.y, node.allocated ? 5 : 3, 0, Math.PI * 2)
      context.fill()
    }
  }, [model])

  if (model.nodes.length === 0) return <p>할당된 패시브 노드가 없습니다.</p>

  return (
    <section className="passive-tree" aria-label="할당 패시브 트리">
      <h2>할당 패시브 트리 <small>{tree.version.replace('_', '.')}</small></h2>
      <canvas ref={canvasRef} aria-label="할당 패시브 트리 캔버스" width={width} height={height} />
    </section>
  )
}
