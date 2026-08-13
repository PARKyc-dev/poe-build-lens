import '@testing-library/jest-dom/vitest'

HTMLCanvasElement.prototype.getContext = (() => ({
  arc() {},
  beginPath() {},
  clearRect() {},
  fill() {},
  lineTo() {},
  moveTo() {},
  stroke() {},
} as unknown as CanvasRenderingContext2D)) as unknown as typeof HTMLCanvasElement.prototype.getContext
