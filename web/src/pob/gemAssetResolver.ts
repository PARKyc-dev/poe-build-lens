import gemAssets from '../assets-data/gems.json'

const images = gemAssets as Record<string, string>

export function resolveGemImage(metadataId: string | undefined): string | null {
  return metadataId ? images[metadataId] ?? null : null
}
