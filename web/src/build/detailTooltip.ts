import type { FocusEvent, MouseEvent } from 'react'

export type DetailTooltip = { id: string; label: string; title: string; details: string[]; rect: DOMRect }
export type ShowDetailTooltip = (event: MouseEvent<HTMLButtonElement> | FocusEvent<HTMLButtonElement>, tooltip: Omit<DetailTooltip, 'rect'>) => void
