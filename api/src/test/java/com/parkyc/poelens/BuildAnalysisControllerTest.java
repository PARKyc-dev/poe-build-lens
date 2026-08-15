package com.parkyc.poelens;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class BuildAnalysisControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void buildsAnalysisFromTagsInsteadOfSkillOrBuffNames() throws Exception {
        mockMvc.perform(post("/api/analyses")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "gameVersion": "3.29",
                                  "buildFacts": {
                                    "offence": [{ "name": "Arbitrary Main Skill", "role": "primary", "delivery": "self-cast", "tags": ["spell", "damage-over-time", "fire", "area"] }],
                                    "defence": [{ "kind": "life", "value": 4500 }, { "kind": "fire-resistance", "value": 75 }, { "kind": "cold-resistance", "value": 75 }, { "kind": "lightning-resistance", "value": 75 }, { "kind": "block", "value": 40 }],
                                    "buffs": [{ "name": "Arbitrary Utility", "kind": "buff", "appliesTo": "player", "tags": ["shock-immunity", "spell-block"] }],
                                    "mobility": [{ "name": "Arbitrary Movement" }],
                                    "passives": [{ "name": "Arbitrary Passive", "effects": [], "tags": ["life-regeneration", "fire-resistance"] }],
                                    "items": [{ "slot": "Helmet", "tags": ["life", "fire-resistance", "block"] }]
                                  }
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("OK"))
                .andExpect(jsonPath("$.returnObject.gameVersion").value("3.29"))
                .andExpect(jsonPath("$.returnObject.offence[0].title").value("주력 공격: Arbitrary Main Skill"))
                .andExpect(jsonPath("$.returnObject.offence[0].explanation").value("직접 시전 (Self-Cast) 방식으로 지속 피해·화염·범위 태그를 활용해 주 피해를 담당합니다."))
                .andExpect(jsonPath("$.returnObject.defence[0].explanation").value("생명력을 피해를 견디는 기본 자원으로 사용합니다. 패시브와 장비 효과 태그가 이 방어 축을 보강합니다."))
                .andExpect(jsonPath("$.returnObject.buffs[0].title").value("버프 유틸리티: Arbitrary Utility"))
                .andExpect(jsonPath("$.returnObject.buffs[0].explanation").value("감전 면역·주문 막기 태그가 활성화되어 상태 이상 방지와 방어 수치를 보강합니다."))
                .andExpect(jsonPath("$.returnObject.buffs[1].title").value("이동기: Arbitrary Movement"))
                .andExpect(jsonPath("$.returnObject.passives[0].title").value("생존 핵심 패시브"))
                .andExpect(jsonPath("$.returnObject.overrides").isEmpty());
    }

    @Test
    void analysesDefenceBuffAndPassiveTagsBeyondLifeResistanceAndBlock() throws Exception {
        mockMvc.perform(post("/api/analyses")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "gameVersion": "3.29",
                                  "buildFacts": {
                                    "offence": [{ "name": "Any Fire Skill", "role": "primary", "delivery": "self-cast", "tags": ["spell", "fire"] }],
                                    "defence": [
                                      { "kind": "energy-shield", "value": 5000 },
                                      { "kind": "armour", "value": 18000 },
                                      { "kind": "evasion", "value": 12000 },
                                      { "kind": "spell-suppression", "value": 100 },
                                      { "kind": "ward", "value": 700 }
                                    ],
                                    "buffs": [{ "name": "Any Defensive Buff", "kind": "buff", "appliesTo": "player", "tags": ["freeze-immunity", "armour", "spell-suppression"] }],
                                    "passives": [],
                                    "passiveTags": ["energy-shield", "energy-shield-recovery", "armour", "spell-suppression", "damage-over-time"],
                                    "items": [{ "slot": "Body Armour", "tags": ["ward", "evasion", "fire"] }]
                                  }
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.returnObject.defence[0].title").value("생존 자원 기반 방어"))
                .andExpect(jsonPath("$.returnObject.defence[1].title").value("방어도·회피 기반 방어"))
                .andExpect(jsonPath("$.returnObject.defence[2].title").value("주문 방어"))
                .andExpect(jsonPath("$.returnObject.defence[3].title").value("보조 피해 방어"))
                .andExpect(jsonPath("$.returnObject.offence[0].explanation").value("직접 시전 (Self-Cast) 방식으로 화염 태그를 활용해 주 피해를 담당합니다. 장비 효과 태그가 이 피해 축을 보강합니다."))
                .andExpect(jsonPath("$.returnObject.buffs[0].title").value("버프 유틸리티: Any Defensive Buff"))
                .andExpect(jsonPath("$.returnObject.buffs[0].explanation").value("동결 면역·방어도·주문 억제 태그가 활성화되어 상태 이상 방지와 방어 수치를 보강합니다."))
                .andExpect(jsonPath("$.returnObject.passives[0].title").value("피해 핵심 패시브"))
                .andExpect(jsonPath("$.returnObject.passives[1].title").value("생존 핵심 패시브"))
                .andExpect(jsonPath("$.returnObject.passives[2].title").value("적중 방어 핵심 패시브"));
    }

    @Test
    void usesGenericUtilityWhenDefensiveEffectTagsAreAbsent() throws Exception {
        mockMvc.perform(post("/api/analyses")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "gameVersion": "3.29",
                                  "buildFacts": {
                                    "offence": [],
                                    "defence": [],
                                    "buffs": [{ "name": "Arbitrary Utility", "kind": "buff", "appliesTo": "player", "tags": [] }]
                                  }
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.returnObject.buffs[0].title").value("버프 (Buff): Arbitrary Utility"));
    }

    @Test
    void doesNotUseVersionSpecificSkillOverrides() throws Exception {
        mockMvc.perform(post("/api/analyses")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "gameVersion": "3.29",
                                  "buildFacts": {
                                    "offence": [{ "name": "Righteous Fire", "role": "primary", "delivery": "self-cast", "tags": ["spell", "damage-over-time"] }],
                                    "defence": [],
                                    "buffs": []
                                  }
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.returnObject.offence[0].title").value("주력 공격: Righteous Fire"))
                .andExpect(jsonPath("$.returnObject.offence[0].explanation").value("직접 시전 (Self-Cast) 방식으로 지속 피해 태그를 활용해 주 피해를 담당합니다."))
                .andExpect(jsonPath("$.returnObject.evidence").isEmpty());
    }

    @Test
    void analyzesDeliveryWithoutDependingOnSkillName() throws Exception {
        mockMvc.perform(post("/api/analyses")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "gameVersion": "3.29",
                                  "buildFacts": {
                                    "offence": [{ "name": "Unknown Totem Skill", "role": "primary", "delivery": "totem", "tags": [] }],
                                    "defence": [],
                                    "buffs": []
                                  }
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.returnObject.offence[0].title").value("주력 공격: Unknown Totem Skill"))
                .andExpect(jsonPath("$.returnObject.overrides").isEmpty());
    }

    @Test
    void returnsRelationshipInsightsFromOffenceDefenceAndPassiveTags() throws Exception {
        mockMvc.perform(post("/api/analyses")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "gameVersion": "3.29",
                                  "buildFacts": {
                                    "offence": [
                                      { "name": "Righteous Fire", "role": "primary", "delivery": "self-cast", "tags": ["damage-over-time"] },
                                      { "name": "Detonate Dead", "role": "secondary", "delivery": "self-cast", "tags": ["spell"] }
                                    ],
                                    "defence": [
                                      { "kind": "life", "value": 4500 },
                                      { "kind": "fire-resistance", "value": 75 },
                                      { "kind": "cold-resistance", "value": 75 },
                                      { "kind": "lightning-resistance", "value": 75 },
                                      { "kind": "block", "value": 60 }
                                    ],
                                    "buffs": [],
                                    "passives": [{
                                      "name": "Any Passive",
                                      "effects": ["+100% to Fire Resistance", "2% of Life Regenerated per second"],
                                      "tags": ["life-regeneration", "fire-resistance"]
                                    }, {
                                      "name": "Another Passive",
                                      "effects": ["Enemies you Kill have a chance to Explode"],
                                      "tags": []
                                    }],
                                    "items": []
                                  }
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.returnObject.offence[0].title").value("주력 공격: Righteous Fire"))
                .andExpect(jsonPath("$.returnObject.offence[0].explanation").value("직접 시전 (Self-Cast) 방식으로 지속 피해 태그를 활용해 주 피해를 담당합니다."))
                .andExpect(jsonPath("$.returnObject.offence[1].title").value("보조 공격: Detonate Dead"))
                .andExpect(jsonPath("$.returnObject.offence[1].explanation").value("직접 시전 (Self-Cast) 방식으로 주문 태그를 활용해 주력 공격을 보완합니다."))
                .andExpect(jsonPath("$.returnObject.defence[0].title").value("생존 자원 기반 방어"))
                .andExpect(jsonPath("$.returnObject.defence[0].explanation").value("생명력을 피해를 견디는 기본 자원으로 사용합니다. 패시브와 장비 효과 태그가 이 방어 축을 보강합니다."))
                .andExpect(jsonPath("$.returnObject.passives[0].title").value("생존 핵심 패시브"))
                .andExpect(jsonPath("$.returnObject.passives[0].explanation").value("패시브 효과 태그가 생명력 회복·화염 저항을 보강합니다."));
    }

    @Test
    void rejectsMissingGameVersion() throws Exception {
        mockMvc.perform(post("/api/analyses")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "primaryOffence": { "skillName": "Fireball", "delivery": "self-cast" },
                                  "summary": null
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("MISSING_GAME_VERSION"))
                .andExpect(jsonPath("$.message").value("Game version is required."));
    }
}
