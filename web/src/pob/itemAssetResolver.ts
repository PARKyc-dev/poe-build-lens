import amulets from '../assets-data/amulets.json'
import belts from '../assets-data/belts.json'
import bodyArmours from '../assets-data/body-armours.json'
import boots from '../assets-data/boots.json'
import clusterJewels from '../assets-data/cluster-jewels.json'
import fishingRods from '../assets-data/fishing-rods.json'
import flasks from '../assets-data/flasks.json'
import gloves from '../assets-data/gloves.json'
import grafts from '../assets-data/grafts.json'
import helmets from '../assets-data/helmets.json'
import jewels from '../assets-data/jewels.json'
import quivers from '../assets-data/quivers.json'
import rings from '../assets-data/rings.json'
import shields from '../assets-data/shields.json'
import tinctures from '../assets-data/tinctures.json'
import weapons from '../assets-data/weapons.json'

type ItemIdentity = {
  name: string
  baseName: string | null
}

type Asset = { iconUrl: string | null }
type AssetCatalog = Record<string, Asset>

const catalogs: AssetCatalog[] = [amulets, belts, bodyArmours, boots, clusterJewels, fishingRods, flasks, gloves, grafts, helmets, jewels, quivers, rings, shields, tinctures, weapons]

export function resolveItemImage(item: ItemIdentity): string | null {
  for (const key of [item.baseName, item.name]) {
    if (!key) continue
    for (const catalog of catalogs) {
      const iconUrl = catalog[key]?.iconUrl
      if (iconUrl) return iconUrl
    }
  }
  return null
}
