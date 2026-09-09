import { useState, type FocusEvent, type MouseEvent } from 'react'

import type { BrowserEquipmentItem, BrowserJewelItem } from '../pob/browserPob'
import { resolveItemImage } from '../pob/itemAssetResolver'
import type { DetailTooltip } from './detailTooltip'
import { EquipmentSection } from './EquipmentSection'
import './BuildPage.css'
import './EquipmentPreviewPage.css'

function previewItem(slot: string, name: string, baseName: string, rarity: string): BrowserEquipmentItem {
  const item = { slot, name, baseName, rarity, modifiers: ['디자인 확인용 예시 아이템'] }
  return { ...item, imageUrl: resolveItemImage(item) }
}

const previewEquipment = [
  previewItem('Weapon 1', 'Woe Chant', 'Kinetic Wand', 'RARE'),
  previewItem('Weapon 2', 'Dawnbreaker', 'Colossal Tower Shield', 'UNIQUE'),
  previewItem('Helmet', 'Fate Star', 'Divine Crown', 'RARE'),
  previewItem('Body Armour', 'Foulborn The Iron Fortress', 'Crusader Plate', 'UNIQUE'),
  previewItem('Gloves', 'Phoenix Clutches', 'Leviathan Gauntlets', 'RARE'),
  previewItem('Boots', "Replica Alberon's Warpath", 'Soldier Boots', 'UNIQUE'),
  previewItem('Amulet', 'Agony Gorget', 'Great Maw Talisman', 'RARE'),
  previewItem('Ring 1', 'Horror Band', 'Cogwork Ring', 'RARE'),
  previewItem('Ring 2', 'Rune Whorl', 'Cogwork Ring', 'RARE'),
  previewItem('Belt', 'Armageddon Clasp', 'Stygian Vise', 'RARE'),
  previewItem('Flask 1', 'Stormblood', 'Iron Flask', 'UNIQUE'),
  previewItem('Flask 2', "Physician's Diamond Flask", 'Diamond Flask', 'MAGIC'),
  previewItem('Flask 3', "Flagellant's Ruby Flask", 'Ruby Flask', 'MAGIC'),
  previewItem('Flask 4', "Specialist's Silver Flask", 'Silver Flask', 'MAGIC'),
  previewItem('Flask 5', "Medic's Quicksilver Flask", 'Quicksilver Flask', 'MAGIC'),
]

function previewJewel(socket: string, name: string, baseName: string, rarity: string, kind: BrowserJewelItem['kind']): BrowserJewelItem {
  const item = { socket, name, baseName, rarity, kind, modifiers: ['디자인 확인용 예시 주얼'] }
  return { ...item, imageUrl: resolveItemImage(item) }
}

const previewJewels = [
  previewJewel('preview-1', 'Forbidden Flame', 'Crimson Jewel', 'UNIQUE', 'jewel'),
  previewJewel('preview-2', "Watcher's Eye", 'Prismatic Jewel', 'UNIQUE', 'jewel'),
  previewJewel('preview-3', 'Unnatural Instinct', 'Viridian Jewel', 'UNIQUE', 'jewel'),
  previewJewel('preview-4', 'Large Cluster Jewel', 'Large Cluster Jewel', 'RARE', 'cluster'),
  previewJewel('preview-5', 'Bramble Spark', 'Large Cluster Jewel', 'RARE', 'cluster'),
  previewJewel('preview-6', 'Spirit Hope', 'Large Cluster Jewel', 'RARE', 'cluster'),
]

export function EquipmentPreviewPage() {
  const [tooltip, setTooltip] = useState<DetailTooltip | null>(null)
  const showTooltip = (event: MouseEvent<HTMLButtonElement> | FocusEvent<HTMLButtonElement>, nextTooltip: Omit<DetailTooltip, 'rect'>) => setTooltip({ ...nextTooltip, rect: event.currentTarget.getBoundingClientRect() })
  const tooltipLeft = tooltip && tooltip.rect.right + 12 + 300 > window.innerWidth ? Math.max(12, tooltip.rect.left - 312) : (tooltip?.rect.right ?? 0) + 12
  const tooltipTop = tooltip ? Math.max(12, Math.min(tooltip.rect.top, window.innerHeight - 180)) : 0

  return <main className="build-detail equipment-preview" aria-label="장비 디자인 미리보기">
    <header className="detail-header">
      <div><p className="eyebrow">POE LENS · DESIGN PREVIEW</p><h1>장비 디자인 <span>미리보기</span></h1><p className="detail-subtitle">PoB 및 AI 분석 없이 장비 배치와 이미지 크기를 확인합니다.</p></div>
      <a className="secondary-button" href="/">PoB 검사로 돌아가기</a>
    </header>
    <EquipmentSection result={{ equipment: previewEquipment, jewels: previewJewels }} tooltip={tooltip} onShow={showTooltip} onHide={() => setTooltip(null)} />
    {tooltip && <aside id="item-tooltip" className="detail-tooltip" style={{ position: 'fixed', left: tooltipLeft, top: tooltipTop }} role="tooltip" aria-label={`${tooltip.title} 장비 정보`}><small>{tooltip.label}</small><h3>{tooltip.title}</h3><div>{tooltip.details.map((detail) => <p key={detail}>{detail}</p>)}</div></aside>}
  </main>
}
