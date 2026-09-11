import { useState } from 'react'
import type { BuildFactSkill, BuildFactSupportGem, BrowserInspectResult } from '../pob/browserPob'
import './SkillGemSection.css'

function gemMeta(gem: Pick<BuildFactSkill | BuildFactSupportGem, 'level' | 'quality' | 'qualityType'>) {
  const quality = gem.quality > 0 ? ` · 퀄리티 ${gem.quality}%` : ''
  const qualityType = gem.qualityType && gem.qualityType !== 'Default' ? ` · ${gem.qualityType}` : ''
  return `레벨 ${gem.level}${quality}${qualityType}`
}

function GemImage({ gem, active = false }: { gem: Pick<BuildFactSkill | BuildFactSupportGem, 'name' | 'imageUrl'>, active?: boolean }) {
  const [isThreeFrameSprite, setIsThreeFrameSprite] = useState(false)
  const imageUrl = gem.imageUrl
  if (!imageUrl) return null
  return <span className={`gem-icon${active ? ' active-gem-icon' : ''}${isThreeFrameSprite ? ' gem-icon-sprite' : ''}`}>
    <img className="gem-icon-layer gem-icon-frame-0" src={imageUrl} alt={gem.name} onLoad={(event) => {
      const { naturalWidth, naturalHeight } = event.currentTarget
      setIsThreeFrameSprite(naturalHeight > 0 && naturalWidth === naturalHeight * 3)
    }} />
    {isThreeFrameSprite && [1, 2].map((frame) => <img className={`gem-icon-layer gem-icon-frame-${frame}`} src={imageUrl} alt="" aria-hidden="true" key={frame} />)}
  </span>
}

const defensiveTags = new Set(['life', 'energy-shield', 'life-regeneration', 'energy-shield-recovery', 'armour', 'evasion', 'ward', 'physical-mitigation', 'fire-resistance', 'cold-resistance', 'lightning-resistance', 'chaos-resistance', 'block', 'spell-block', 'spell-suppression', 'attack-dodge', 'spell-dodge', 'damage-avoidance', 'shock-immunity', 'shock-avoidance', 'freeze-immunity', 'chill-immunity', 'ignite-immunity'])

function skillPriority(skill: BuildFactSkill, result: BrowserInspectResult) {
  const offence = result.buildFacts.offence.find((entry) => entry.name === skill.name)
  if (offence) return offence.role === 'primary' ? 0 : 1
  const buff = result.buildFacts.buffs.find((entry) => entry.name === skill.name)
  const isCurseOrMark = buff?.kind === 'curse' || buff?.kind === 'mark'
  if (buff?.kind === 'guard' || (!isCurseOrMark && buff?.tags.some((tag) => defensiveTags.has(tag)))) return 2
  if (buff && !isCurseOrMark) return 3
  if (result.buildFacts.mobility.some((entry) => entry.name === skill.name)) return 4
  if (isCurseOrMark) return 5
  if (result.buildFacts.operationFacts?.some((entry) => entry.sourceName === skill.name && entry.action === 'trigger')) return 6
  return 5
}

export function SkillGemSection({ result }: { result: BrowserInspectResult }) {
  const skills = result.buildFacts.skills
    .map((skill, index) => ({ skill, index }))
    .filter(({ skill }) => skill.enabled)
    .sort((left, right) => skillPriority(left.skill, result) - skillPriority(right.skill, result) || left.index - right.index)
    .map(({ skill }) => skill)

  return <section className="skill-gem-panel" aria-label="스킬젬 상세">
    <p className="section-kicker">SKILL GEMS</p>
    <h2>스킬젬 상세</h2>
    {skills.length === 0 ? <p className="skill-gem-empty">활성 스킬 세트에서 사용 중인 스킬젬을 찾지 못했습니다.</p> : <div className="skill-gem-groups">{skills.map((skill, index) => {
      const supports = skill.supports.filter((support) => support.enabled)
      return <article className="skill-gem-group" role="group" aria-label={`${skill.name} 연결 그룹`} key={`${skill.name}-${index}`}>
        <div className="active-gem">
          <GemImage gem={skill} active />
          <div><small>활성 스킬</small><strong>{skill.name}</strong><span>{gemMeta(skill)}</span></div>
        </div>
        {supports.length > 0 ? <ul aria-label={`${skill.name}에 연결된 보조 젬`}>{supports.map((support, supportIndex) => <li key={`${support.name}-${supportIndex}`}>
          <span className="gem-link" aria-hidden="true">＋</span>
          <GemImage gem={support} />
          <div className="gem-copy">
            <small>보조 젬{support.awakened && <b>각성</b>}</small>
            <strong>{support.name}</strong>
            <span>{gemMeta(support)}</span>
          </div>
        </li>)}</ul> : <p className="unlinked-gem">연결된 보조 젬이 없습니다.</p>}
      </article>
    })}</div>}
  </section>
}
