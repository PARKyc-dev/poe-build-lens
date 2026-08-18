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
    void combinesBuildFactsIntoMechanismNarratives() throws Exception {
        mockMvc.perform(post("/api/analyses")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "gameVersion": "3.29",
                                  "buildFacts": {
                                    "offence": [
                                      { "name": "Righteous Fire", "role": "primary", "combinedDps": 100000, "delivery": "self-cast", "tags": ["spell", "damage-over-time", "fire", "area"] },
                                      { "name": "Fire Trap", "role": "secondary", "combinedDps": 50000, "delivery": "trap", "tags": ["spell", "damage-over-time", "fire", "area"] }
                                    ],
                                    "defence": [
                                      { "kind": "life", "value": 5000 }, { "kind": "fire-resistance", "value": 85 }, { "kind": "cold-resistance", "value": 75 }, { "kind": "lightning-resistance", "value": 75 }, { "kind": "armour", "value": 20000 }, { "kind": "block", "value": 50 }
                                    ],
                                    "buffs": [
                                      { "name": "Determination", "kind": "aura", "appliesTo": "player", "tags": ["armour"] },
                                      { "name": "Tempest Shield", "kind": "buff", "appliesTo": "player", "tags": ["spell-block", "shock-immunity"] }
                                    ],
                                    "passives": [{ "name": "Growth and Decay", "effects": ["Regenerate 1% of Life per second"], "tags": ["life-regeneration", "damage-over-time"] }],
                                    "ascendancies": [{ "ascendancyName": "Chieftain", "name": "Hinekora, Death's Fury", "effects": ["Enemies you or your Totems Kill have a 5% chance to Explode"], "tags": ["fire", "area"] }]
                                  }
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.returnObject.offence[0].title").value("공격 기재"))
                .andExpect(jsonPath("$.returnObject.offence[0].explanation").value(org.hamcrest.Matchers.containsString("Righteous Fire")))
                .andExpect(jsonPath("$.returnObject.offence[0].explanation").value(org.hamcrest.Matchers.containsString("Fire Trap")))
                .andExpect(jsonPath("$.returnObject.offence[0].explanation").value(org.hamcrest.Matchers.containsString("Hinekora, Death's Fury")))
                .andExpect(jsonPath("$.returnObject.defence[0].title").value("방어 기재"))
                .andExpect(jsonPath("$.returnObject.defence[0].explanation").value(org.hamcrest.Matchers.containsString("생명력 재생")))
                .andExpect(jsonPath("$.returnObject.defence[0].explanation").value(org.hamcrest.Matchers.containsString("Determination")))
                .andExpect(jsonPath("$.returnObject.defence[0].explanation").value(org.hamcrest.Matchers.containsString("Tempest Shield")));
    }

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
                .andExpect(jsonPath("$.returnObject.offence[0].title").value("공격 기재"))
                .andExpect(jsonPath("$.returnObject.defence[0].title").value("방어 기재"))
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
                .andExpect(jsonPath("$.returnObject.defence[0].title").value("방어 기재"))
                .andExpect(jsonPath("$.returnObject.offence[0].title").value("공격 기재"))
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
                .andExpect(jsonPath("$.returnObject.offence[0].title").value("공격 기재"))
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
                .andExpect(jsonPath("$.returnObject.offence[0].title").value("공격 기재"))
                .andExpect(jsonPath("$.returnObject.overrides").isEmpty());
    }

    @Test
    void includesSupportGemsAndAscendancyNodesInAnalysis() throws Exception {
        mockMvc.perform(post("/api/analyses")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "gameVersion": "3.29",
                                  "buildFacts": {
                                    "offence": [{ "name": "Any Fire Skill", "role": "primary", "delivery": "self-cast", "tags": ["spell", "fire"] }],
                                    "skills": [{
                                      "name": "Any Fire Skill", "level": 21, "quality": 20, "qualityType": "Default", "enabled": true, "awakened": false,
                                      "supports": [{ "name": "Awakened Added Fire Damage", "level": 5, "quality": 20, "qualityType": "Default", "enabled": true, "awakened": true }]
                                    }],
                                    "ascendancies": [{
                                      "ascendancyName": "Occultist", "name": "Void Beacon",
                                      "effects": ["Nearby Enemies have -20% to Cold Resistance"], "tags": ["cold"]
                                    }]
                                  }
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.returnObject.offence[0].title").value("공격 기재"))
                .andExpect(jsonPath("$.returnObject.ascendancies[0].title").value("전직 노드: Void Beacon"))
                .andExpect(jsonPath("$.returnObject.ascendancies[0].explanation").value("Occultist 전직의 적용된 효과: Nearby Enemies have -20% to Cold Resistance"));
    }

    @Test
    void returnsAllocatedMajorPassiveNodeDetails() throws Exception {
        mockMvc.perform(post("/api/analyses")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "gameVersion": "3.29",
                                  "buildFacts": {
                                    "passives": [{
                                      "name": "Growth and Decay",
                                      "effects": ["Regenerate 1% of Life per second", "20% increased Damage over Time"],
                                      "tags": ["life-regeneration", "damage-over-time"]
                                    }]
                                  }
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.returnObject.passiveNodes[0].title").value("주요 패시브: Growth and Decay"))
                .andExpect(jsonPath("$.returnObject.passiveNodes[0].explanation").value("적용된 효과: Regenerate 1% of Life per second · 20% increased Damage over Time"));
    }

    @Test
    void returnsEquipmentJewelsAndPobPerformanceDetails() throws Exception {
        mockMvc.perform(post("/api/analyses")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "gameVersion": "3.29",
                                  "buildFacts": {
                                    "items": [{
                                      "slot": "Helmet", "name": "Mind Crown", "baseName": "Hubris Circlet", "rarity": "RARE",
                                      "modifiers": ["+90 to maximum Life", "+42% to Fire Resistance"], "tags": ["life", "fire-resistance"]
                                    }],
                                    "jewels": [{
                                      "socket": "1234", "name": "Crimson Jewel of the Fox", "baseName": "Crimson Jewel", "rarity": "MAGIC",
                                      "modifiers": ["+7% to maximum Life"], "kind": "jewel", "tags": ["life"]
                                    }],
                                    "performance": {
                                      "totalDps": 123456.0, "combinedDps": 234567.0, "life": 4500.0,
                                      "energyShield": 1200.0, "armour": 8000.0, "evasion": 3000.0, "totalEhp": 25000.0
                                    }
                                  }
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.returnObject.gear[0].title").value("장비: Helmet · Mind Crown"))
                .andExpect(jsonPath("$.returnObject.gear[0].explanation").value("옵션: +90 to maximum Life · +42% to Fire Resistance"))
                .andExpect(jsonPath("$.returnObject.gear[1].title").value("주얼: Crimson Jewel of the Fox"))
                .andExpect(jsonPath("$.returnObject.performance[0].title").value("PoB 계산 수치"))
                .andExpect(jsonPath("$.returnObject.performance[0].explanation").value("주력 DPS 123,456 · 합산 DPS 234,567 · 생명력 4,500 · 에너지 보호막 1,200 · 방어도 8,000 · 회피 3,000 · 총 EHP 25,000"));
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
                .andExpect(jsonPath("$.returnObject.offence[0].title").value("공격 기재"))
                .andExpect(jsonPath("$.returnObject.defence[0].title").value("방어 기재"))
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
