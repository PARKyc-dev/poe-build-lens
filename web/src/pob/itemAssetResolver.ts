import buildAssets from '../../../data/generated/pob-Z8HtSpE0xguI-assets.json'

type ItemIdentity = {
  name: string
  baseName: string | null
  rarity: string
}

type Asset = { iconUrl: string | null }
const assets = buildAssets.assets as Record<string, Asset>

export function resolveItemImage(item: ItemIdentity): string | null {
  const kind = item.rarity === 'UNIQUE' || item.rarity === 'RELIC' ? 'UNIQUE_ITEM' : 'BASE_ITEM'
  const name = kind === 'UNIQUE_ITEM' ? item.name : item.baseName
  return name ? assets[`${kind}|${name}`]?.iconUrl ?? null : null
}
