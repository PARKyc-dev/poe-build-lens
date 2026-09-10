import type { FocusEvent, MouseEvent } from 'react'

export type DetailTooltipSection = { label: string; details: string[] }
export type DetailTooltip = { id: string; label: string; title: string; baseName?: string | null; details: string[]; sections?: DetailTooltipSection[]; rect: DOMRect }
export type ShowDetailTooltip = (event: MouseEvent<HTMLButtonElement> | FocusEvent<HTMLButtonElement>, tooltip: Omit<DetailTooltip, 'rect'>) => void
