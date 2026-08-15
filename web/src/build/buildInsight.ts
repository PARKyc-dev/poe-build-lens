export type BuildSummary = {
  totalDps?: number | null
  combinedDps?: number | null
  life?: number | null
  energyShield?: number | null
  mana?: number | null
  armour?: number | null
  evasion?: number | null
  totalEhp?: number | null
}

export type BuildInsightItem = {
  title: string
  description: string
  isPending?: boolean
}

export type BuildInsight = {
  cautions: BuildInsightItem[]
  priorities: BuildInsightItem[]
}

function hasValue(value: number | null | undefined): value is number {
  return typeof value === 'number' && Number.isFinite(value)
}

export function createBuildInsight({ summary }: {
  summary: BuildSummary
}): BuildInsight {
  const cautions: BuildInsightItem[] = []
  const priorities: BuildInsightItem[] = []
  const life = summary.life
  const armour = summary.armour
  const evasion = summary.evasion

  if (hasValue(life) && life < 3500) {
    cautions.push({
      title: '생명력 여유가 부족합니다',
      description: `현재 생명력은 ${Math.round(life).toLocaleString('ko-KR')}입니다. 맵 진행 전 생명력 접두어와 패시브 생명력 노드를 우선 확보하세요.`,
    })
    priorities.push({
      title: '생명력 기반 방어를 먼저 보강하세요',
      description: '장비의 최대 생명력과 패시브 트리의 생명력 노드를 먼저 확보한 뒤 피해 투자 비중을 늘리세요.',
    })
  }

  if (hasValue(armour) && hasValue(evasion) && armour < 5000 && evasion < 5000) {
    cautions.push({
      title: '물리 피해 대응 수단을 확인하세요',
      description: '방어도와 회피가 모두 낮습니다. 주력 방어 축 하나를 정하고 플라스크·오라·장비 접두어를 맞추세요.',
    })
    priorities.push({
      title: '주력 방어 축을 한 가지 정하세요',
      description: '방어도 또는 회피 중 빌드에 맞는 축을 선택해 장비와 오라의 투자 방향을 일치시키세요.',
    })
  }

  if (priorities.length === 0) {
    priorities.push({
      title: '피해와 방어의 다음 병목을 확인하세요',
      description: '현재 요약 수치만으로 뚜렷한 약점이 감지되지 않았습니다. 원소 저항, 주문 억제, 플라스크 상태는 후속 분석에서 추가됩니다.',
    })
  }

  return {
    cautions: cautions.length > 0 ? cautions : [{
      title: '즉시 감지된 방어 경고가 없습니다',
      description: '현재는 생명력·방어도·회피 기준만 점검했습니다. 원소 저항과 상태 이상 대응은 준비 중입니다.',
      isPending: true,
    }],
    priorities,
  }
}
