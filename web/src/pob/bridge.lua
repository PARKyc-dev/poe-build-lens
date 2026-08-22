unpack = table.unpack
loadstring = load

if not setfenv then
  local function findenv(f)
    local level = 1
    repeat
      local name, value = debug.getupvalue(f, level)
      if name == "_ENV" then return level, value end
      level = level + 1
    until name == nil
  end
  getfenv = function(f) return select(2, findenv(f)) or _G end
  setfenv = function(f, environment)
    local level = findenv(f)
    if level then debug.setupvalue(f, level, environment) end
    return f
  end
end

local function bitFold(operator, initial, ...)
  local result = initial
  for _, value in ipairs({ ... }) do result = operator(result, value) end
  return result
end

bit = bit or {
  band = function(first, ...) return bitFold(function(a, b) return a & b end, first, ...) end,
  bor = function(first, ...) return bitFold(function(a, b) return a | b end, first, ...) end,
  bxor = function(first, ...) return bitFold(function(a, b) return a ~ b end, first, ...) end,
  bnot = function(value) return ~value end,
  lshift = function(value, shift) return value << shift end,
  rshift = function(value, shift) return (value & 0xffffffff) >> shift end,
  tobit = function(value)
    local normalized = value % 0x100000000
    return normalized >= 0x80000000 and normalized - 0x100000000 or normalized
  end,
}

package.preload["lua-utf8"] = function()
  return {
    find = string.find,
    gsub = string.gsub,
    match = string.match,
    reverse = string.reverse,
    sub = string.sub,
    next = function(text, index, direction)
      local step = direction or 1
      local nextIndex = (index or 0) + step
      return nextIndex >= 1 and nextIndex <= #text and nextIndex or nil
    end,
  }
end
arg = { }
math.pow = math.pow or function(base, exponent) return base ^ exponent end
jit = jit or { opt = { start = function() end, stop = function() end }, off = function() end }
package.path = package.path .. ";runtime/lua/?.lua;runtime/lua/?/init.lua"

local nativeGsub = string.gsub
local nativeFormat = string.format
string.gsub = function(text, pattern, replacement, limit)
  local ok, value, count = pcall(nativeGsub, text, pattern, replacement, limit)
  if ok or type(replacement) ~= "string" then return value, count end
  return nativeGsub(text, pattern, function(...)
    local captures = { ... }
    local output = { }
    local index = 1
    while index <= #replacement do
      local character = replacement:sub(index, index)
      local nextCharacter = replacement:sub(index + 1, index + 1)
      if character == "%" and nextCharacter ~= "" then
        local capture = tonumber(nextCharacter)
        table.insert(output, capture and tostring(captures[capture] or "") or "%" .. nextCharacter)
        index = index + 2
      else
        table.insert(output, character)
        index = index + 1
      end
    end
    return table.concat(output)
  end, limit)
end
string.format = function(format, ...)
  local ok, value = pcall(nativeFormat, format, ...)
  if ok then return value end
  local floatFormat = nativeGsub(format, "%%[-+ #0%d%.]*[diouxX]", "%%.0f")
  return nativeFormat(floatFormat, ...)
end

dofile("HeadlessWrapper.lua")

local jsonType = type
local jsonPairs = pairs
local jsonConcat = table.concat
local jsonFormat = nativeFormat
local function jsonArray()
  return { __jsonArray = true }
end

local function jsonEncode(value)
  local valueType = jsonType(value)
  if valueType == "nil" then return "null" end
  if valueType == "boolean" then return value and "true" or "false" end
  if valueType == "number" then return tostring(value) end
  if valueType == "string" then
    local encoded = jsonFormat("%q", value):gsub("\\\n", "\\n")
    return encoded
  end
  local isArray = value.__jsonArray or #value > 0
  local parts = { }
  if isArray then
    for index = 1, #value do table.insert(parts, jsonEncode(value[index])) end
    return "[" .. jsonConcat(parts, ",") .. "]"
  end
  for key, item in jsonPairs(value) do
    table.insert(parts, jsonEncode(tostring(key)) .. ":" .. jsonEncode(item))
  end
  return "{" .. jsonConcat(parts, ",") .. "}"
end

local function titleOrDefault(value)
  return value and value ~= "" and value or "Default"
end

local equipmentSlots = { "Weapon 1", "Weapon 2", "Helmet", "Body Armour", "Gloves", "Boots", "Amulet", "Ring 1", "Ring 2", "Belt", "Flask 1", "Flask 2", "Flask 3", "Flask 4", "Flask 5" }

local function activateEquippedFlasks()
  for _, slot in ipairs(build.itemsTab.orderedSlots or { }) do
    if slot.slotName:match("^Flask") and slot.selItemId then
      slot.active = true
      build.itemsTab.activeItemSet[slot.slotName].active = true
    end
  end
end

local function equipmentFromActiveSet()
  local result = { }
  local itemSet = build.itemsTab.activeItemSet
  for _, slotName in ipairs(equipmentSlots) do
    local slot = itemSet[slotName]
    local item = slot and slot.selItemId and build.itemsTab.items[slot.selItemId]
    if item then
      local modifiers = { }
      for index, mod in ipairs(item.explicitModLines or { }) do
        if index > 4 then break end
        table.insert(modifiers, mod.line or mod.extra)
      end
      table.insert(result, {
        slot = slotName,
        name = item.title or item.name or item.baseName,
        baseName = item.baseName,
        rarity = item.rarity,
        modifiers = modifiers,
      })
    end
  end
  return result
end

local function jewelsFromActiveSpec()
  local result = { }
  for socketId, itemId in pairs(build.spec.jewels or { }) do
    local item = build.itemsTab.items[itemId]
    if item then
      local modifiers = { }
      for index, mod in ipairs(item.explicitModLines or { }) do
        if index > 4 then break end
        table.insert(modifiers, mod.line or mod.extra)
      end
      local baseName = item.baseName
      table.insert(result, {
        socket = tostring(socketId),
        name = item.title or item.name or baseName,
        baseName = baseName,
        rarity = item.rarity,
        modifiers = modifiers,
        kind = baseName and string.find(baseName, "Cluster Jewel", 1, true) and "cluster" or "jewel",
      })
    end
  end
  table.sort(result, function(left, right) return left.socket < right.socket end)
  return result
end

local function isTriggeredMainSkill(mainSkill)
  local skillData = mainSkill.skillData or { }
  local skillTypes = mainSkill.skillTypes or { }
  local activeEffect = mainSkill.activeEffect or { }
  local grantedEffect = activeEffect.grantedEffect or { }
  local srcInstance = activeEffect.srcInstance or { }
  return (skillData.triggered
    or skillData.triggeredByUnique
    or skillTypes[SkillType.Triggered]
    or skillTypes[SkillType.InbuiltTrigger]
    or grantedEffect.triggered
    or srcInstance.triggered) and true or false
end

local function flagsFromMainSkill(mainSkill)
  if not mainSkill then return nil end
  local skillFlags = mainSkill.skillFlags or { }
  return {
    isAttack = skillFlags.attack and true or false,
    isTotem = skillFlags.totem and true or false,
    isTrap = skillFlags.trap and true or false,
    isMine = skillFlags.mine and true or false,
    isBrand = skillFlags.brand and true or false,
    isSelfCast = skillFlags.selfCast and true or false,
    isMinion = (skillFlags.haveMinion or skillFlags.minion) and true or false,
    isTriggered = isTriggeredMainSkill(mainSkill),
  }
end

local function deliveryFromSkill(skill)
  local flags = skill.skillFlags or { }
  if isTriggeredMainSkill(skill) then return "trigger" end
  if flags.haveMinion or flags.minion then return "minion" end
  if flags.totem then return "totem" end
  if flags.trap then return "trap" end
  if flags.mine then return "mine" end
  if flags.brand then return "brand" end
  if flags.attack then return "attack" end
  if flags.selfCast then return "self-cast" end
  return "unverified"
end

local function tagsFromSkill(skill)
  local flags = skill.skillFlags or { }
  local types = skill.skillTypes or { }
  local tags = jsonArray()
  local function addTag(value, present)
    if present then table.insert(tags, value) end
  end
  addTag("attack", flags.attack)
  addTag("spell", flags.spell or types[SkillType.Spell])
  addTag("damage-over-time", flags.dot)
  addTag("projectile", flags.projectile)
  addTag("area", flags.area)
  addTag("minion", flags.haveMinion or flags.minion)
  addTag("fire", types[SkillType.Fire])
  addTag("cold", types[SkillType.Cold])
  addTag("lightning", types[SkillType.Lightning])
  addTag("chaos", types[SkillType.Chaos])
  return tags
end

local modTagMap = {
  Life = "life",
  EnergyShield = "energy-shield",
  LifeRegen = "life-regeneration",
  LifeRegenPercent = "life-regeneration",
  EnergyShieldRechargeRate = "energy-shield-recovery",
  EnergyShieldRegen = "energy-shield-recovery",
  Armour = "armour",
  Evasion = "evasion",
  Ward = "ward",
  PhysicalDamageReduction = "physical-mitigation",
  FireResist = "fire-resistance",
  ColdResist = "cold-resistance",
  LightningResist = "lightning-resistance",
  ChaosResist = "chaos-resistance",
  BlockChance = "block",
  SpellBlockChance = "spell-block",
  SpellSuppressionChance = "spell-suppression",
  AttackDodgeChance = "attack-dodge",
  SpellDodgeChance = "spell-dodge",
  AvoidAllDamageFromHitsChance = "damage-avoidance",
  ShockImmune = "shock-immunity",
  AvoidShock = "shock-avoidance",
  FreezeImmune = "freeze-immunity",
  ChillImmune = "chill-immunity",
  IgniteImmune = "ignite-immunity",
  DamageOverTime = "damage-over-time",
  PhysicalDamage = "physical",
  PhysicalDamageOverTimeMultiplier = { "physical", "damage-over-time" },
  PoisonDamage = { "chaos", "damage-over-time" },
  BleedDamage = { "physical", "damage-over-time" },
  FireDamage = "fire",
  ColdDamage = "cold",
  LightningDamage = "lightning",
  ChaosDamage = "chaos",
}

local function tagsFromModList(modList)
  local tags = jsonArray()
  local seen = { }
  for index = 1, #(modList or { }) do
    local mod = modList[index]
    local tag = modTagMap[mod.name]
    local tagList = type(tag) == "table" and tag or tag and { tag } or mod.name == "ElementalResist" and { "fire-resistance", "cold-resistance", "lightning-resistance" } or { }
    for _, value in ipairs(tagList) do
      if not seen[value] then
        seen[value] = true
        table.insert(tags, value)
      end
    end
  end
  return tags
end

local function allPassiveTags(spec)
  local result = jsonArray()
  local seen = { }
  for _, node in pairs(spec.allocNodes or { }) do
    for _, tag in ipairs(tagsFromModList(node.modList)) do
      if not seen[tag] then
        seen[tag] = true
        table.insert(result, tag)
      end
    end
  end
  return result
end

local function offenceFacts(env)
  local player = env.player
  local candidates = { }
  local function addSkill(skill)
    if not skill then return end
    local flags = skill.skillFlags or { }
    local effect = skill.activeEffect or { }
    local grantedEffect = effect.grantedEffect or { }
    local isMovement = skill.skillTypes and skill.skillTypes[SkillType.Movement]
    if not flags.disable and not isMovement and not skill.buffSkill and (flags.hit or flags.dot) and grantedEffect.name then
      local delivery = deliveryFromSkill(skill)
      local key = grantedEffect.name .. "|" .. delivery
      if not candidates[key] then
        candidates[key] = {
          name = grantedEffect.name,
          delivery = delivery,
          tags = tagsFromSkill(skill),
          skill = skill,
          combinedDps = 0,
        }
      end
    end
  end
  addSkill(player.mainSkill)
  for _, skill in ipairs(player.activeSkillList or { }) do
    if skill ~= player.mainSkill then addSkill(skill) end
  end
  local selectedSkill = player.mainSkill
  for _, candidate in pairs(candidates) do
    player.mainSkill = candidate.skill
    build.calcsTab.calcs.perform(env, true)
    candidate.combinedDps = player.output.CombinedDPS or 0
    candidate.skill = nil
  end
  player.mainSkill = selectedSkill
  build.calcsTab.calcs.perform(env)
  local ranked = { }
  for _, candidate in pairs(candidates) do
    if candidate.combinedDps > 0 then table.insert(ranked, candidate) end
  end
  table.sort(ranked, function(left, right)
    if left.combinedDps == right.combinedDps then return left.name < right.name end
    return left.combinedDps > right.combinedDps
  end)
  local result = jsonArray()
  for index, candidate in ipairs(ranked) do
    if index > 2 then break end
    candidate.role = index == 1 and "primary" or "secondary"
    table.insert(result, candidate)
  end
  return result
end

local function gemName(gem)
  local grantedEffect = gem.grantedEffect or (gem.gemData and gem.gemData.grantedEffect) or { }
  return grantedEffect.name or (gem.gemData and gem.gemData.name) or gem.nameSpec
end

local function isSupportGem(gem)
  local grantedEffect = gem.grantedEffect or (gem.gemData and gem.gemData.grantedEffect) or { }
  return grantedEffect.support and true or false
end

local gemQualityTypes = { }

local function qualityType(gem)
  return gem.qualityId or (gem.gemData and gemQualityTypes[gem.gemData.gameId]) or "Default"
end

local function supportGemFact(gem, group)
  local grantedEffect = gem.grantedEffect or (gem.gemData and gem.gemData.grantedEffect) or { }
  return {
    name = gemName(gem),
    level = gem.level,
    quality = gem.quality,
    qualityType = qualityType(gem),
    enabled = gem.enabled and group.enabled and true or false,
    awakened = (grantedEffect.plusVersionOf or (gem.gemData and gem.gemData.name and gem.gemData.name:match("^Awakened "))) and true or false,
  }
end

local function skillFacts()
  local result = jsonArray()
  for _, group in ipairs(build.skillsTab.socketGroupList or { }) do
    local supports = jsonArray()
    for _, gem in ipairs(group.gemList or { }) do
      if isSupportGem(gem) then table.insert(supports, supportGemFact(gem, group)) end
    end
    for _, gem in ipairs(group.gemList or { }) do
      if not isSupportGem(gem) and gemName(gem) then
        table.insert(result, {
          name = gemName(gem),
          level = gem.level,
          quality = gem.quality,
          qualityType = qualityType(gem),
          enabled = gem.enabled and group.enabled and true or false,
          awakened = (gem.gemData and gem.gemData.name and gem.gemData.name:match("^Awakened ")) and true or false,
          supports = supports,
        })
      end
    end
  end
  return result
end

local function mobilityFacts(player)
  local result = jsonArray()
  local seen = { }
  for _, skill in ipairs(player.activeSkillList or { }) do
    local name = skill.activeEffect and skill.activeEffect.grantedEffect and skill.activeEffect.grantedEffect.name
    if name and skill.skillTypes and skill.skillTypes[SkillType.Movement] and not seen[name] then
      seen[name] = true
      table.insert(result, { name = name })
    end
  end
  return result
end

local function passiveFacts(spec)
  local result = jsonArray()
  for _, node in pairs(spec.allocNodes or { }) do
    if node.isNotable or node.isKeystone or node.isMastery then
      local effects = jsonArray()
      for _, effect in ipairs(node.sd or { }) do
        if type(effect) == "string" and effect ~= "" then table.insert(effects, effect) end
      end
      local name = node.dn or node.name
      if name then
        table.insert(result, { name = name, kind = node.isMastery and "mastery" or node.isKeystone and "keystone" or "notable", effects = effects, tags = tagsFromModList(node.modList) })
      end
    end
  end
  return result
end

local function ascendancyFacts(spec)
  local result = jsonArray()
  for _, node in pairs(spec.allocNodes or { }) do
    if node.ascendancyName and (node.isNotable or node.isKeystone) then
      local effects = jsonArray()
      for _, effect in ipairs(node.sd or { }) do
        if type(effect) == "string" and effect ~= "" then table.insert(effects, effect) end
      end
      table.insert(result, {
        ascendancyName = node.ascendancyName,
        name = node.dn or node.name,
        effects = effects,
        tags = tagsFromModList(node.modList),
      })
    end
  end
  return result
end

local function skillTooltipDetails(skill)
  local result = jsonArray()
  local effect = skill.activeEffect or { }
  local grantedEffect = effect.grantedEffect or { }
  if type(grantedEffect.description) == "string" and grantedEffect.description ~= "" then
    table.insert(result, grantedEffect.description)
  end
  if calcLib and build.data.describeStats and grantedEffect.statDescriptionScope then
    local stats = calcLib.buildSkillInstanceStats(effect, grantedEffect)
    local descriptions = build.data.describeStats(stats, grantedEffect.statDescriptionScope)
    for _, description in ipairs(descriptions or { }) do
      if type(description) == "string" and description ~= "" then table.insert(result, description) end
    end
  end
  return result
end

local function skillTooltipFacts(env)
  local result = jsonArray()
  local seen = { }
  local function add(name, skill)
    if name and not seen[name] then
      seen[name] = true
      table.insert(result, { name = name, details = skillTooltipDetails(skill) })
    end
  end
  for _, skill in ipairs(env.player.activeSkillList or { }) do
    local grantedEffect = skill.activeEffect and skill.activeEffect.grantedEffect
    add(grantedEffect and grantedEffect.name, skill)
    if skill.buffSkill then
      for _, buff in ipairs(skill.buffList or { }) do
        if buff.name and not buff.applyNotPlayer then add(buff.name, skill) end
      end
    end
  end
  return result
end

local defenceOutputKeys = {
  { kind = "life", output = "Life" },
  { kind = "energy-shield", output = "EnergyShield" },
  { kind = "mana", output = "Mana" },
  { kind = "armour", output = "Armour" },
  { kind = "evasion", output = "Evasion" },
  { kind = "fire-resistance", output = "FireResist" },
  { kind = "cold-resistance", output = "ColdResist" },
  { kind = "lightning-resistance", output = "LightningResist" },
  { kind = "chaos-resistance", output = "ChaosResist" },
  { kind = "block", output = "BlockChance" },
  { kind = "spell-block", output = "SpellBlockChance" },
  { kind = "spell-suppression", output = "SpellSuppressionChance" },
  { kind = "guard", output = "GuardSkillActive" },
  { kind = "ward", output = "Ward" },
  { kind = "attack-dodge", output = "AttackDodgeChance" },
  { kind = "spell-dodge", output = "SpellDodgeChance" },
  { kind = "damage-avoidance", output = "AvoidAllDamageFromHitsChance" },
}

local function defenceFacts(output)
  local result = jsonArray()
  for _, entry in ipairs(defenceOutputKeys) do
    local value = output[entry.output]
    if value == true then value = 1 end
    if type(value) == "number" and value > 0 then
      table.insert(result, { kind = entry.kind, value = value })
    end
  end
  return result
end

local function buffKind(skill, buff)
  if buff.type == "Guard" then return "guard" end
  if skill.skillTypes and skill.skillTypes[SkillType.Aura] then return "aura" end
  if skill.skillTypes and (skill.skillTypes[SkillType.Hex] or skill.skillTypes[SkillType.Mark]) then return "curse" end
  return "buff"
end

local function addBuff(result, seen, name, kind, appliesTo, tags)
  local key = name .. "|" .. kind .. "|" .. appliesTo
  if not seen[key] then
    seen[key] = true
    table.insert(result, {
      name = name,
      kind = kind,
      appliesTo = appliesTo,
      tags = tags,
    })
  end
end

local function buffFacts(env, output)
  local result = jsonArray()
  local seen = { }
  for _, skill in ipairs(env.player.activeSkillList or { }) do
    if skill.buffSkill then
      for _, buff in ipairs(skill.buffList or { }) do
        if buff.name and not buff.applyNotPlayer then
          addBuff(result, seen, buff.name, buffKind(skill, buff), "player", tagsFromModList(buff.modList))
        end
      end
    end
  end
  return result
end

local function itemFacts()
  local result = jsonArray()
  local itemSet = build.itemsTab.activeItemSet
  for _, slotName in ipairs(equipmentSlots) do
    local slot = itemSet[slotName]
    local item = slot and slot.selItemId and build.itemsTab.items[slot.selItemId]
    if item then
      local modifiers = jsonArray()
      for index, mod in ipairs(item.explicitModLines or { }) do
        if index > 4 then break end
        table.insert(modifiers, mod.line or mod.extra)
      end
      table.insert(result, {
        slot = slotName,
        name = item.title or item.name or item.baseName,
        baseName = item.baseName,
        rarity = item.rarity,
        modifiers = modifiers,
        tags = tagsFromModList(item.modList),
      })
    end
  end
  return result
end

local function jewelFacts(spec)
  local result = jsonArray()
  for socketId, itemId in pairs(spec.jewels or { }) do
    local item = build.itemsTab.items[itemId]
    if item then
      local modifiers = jsonArray()
      for index, mod in ipairs(item.explicitModLines or { }) do
        if index > 4 then break end
        table.insert(modifiers, mod.line or mod.extra)
      end
      local baseName = item.baseName
      table.insert(result, {
        socket = tostring(socketId),
        name = item.title or item.name or baseName,
        baseName = baseName,
        rarity = item.rarity,
        modifiers = modifiers,
        kind = baseName and string.find(baseName, "Cluster Jewel", 1, true) and "cluster" or "jewel",
        tags = tagsFromModList(item.modList),
      })
    end
  end
  return result
end

local function performanceFact(output)
  return {
    totalDps = output.TotalDPS,
    combinedDps = output.CombinedDPS,
    life = output.Life,
    energyShield = output.EnergyShield,
    mana = output.Mana,
    armour = output.Armour,
    evasion = output.Evasion,
    totalEhp = output.TotalEHP,
  }
end

local function buildFacts(env, output, spec)
  return {
    offence = offenceFacts(env),
    skills = skillFacts(),
    defence = defenceFacts(output),
    buffs = buffFacts(env, output),
    mobility = mobilityFacts(env.player),
    passives = passiveFacts(spec),
    ascendancies = ascendancyFacts(spec),
    passiveTags = allPassiveTags(spec),
    items = itemFacts(),
    jewels = jewelFacts(spec),
    performance = performanceFact(output),
  }
end

local function qualityTypesFromXml(xmlText)
  local result = { }
  for attributes in xmlText:gmatch("<Gem%s+([^>]-)/>") do
    local gemId = attributes:match('gemId="([^"]+)"')
    local qualityId = attributes:match('qualityId="([^"]+)"')
    if gemId and qualityId then result[gemId] = qualityId end
  end
  return result
end

function inspectBuild(xmlText, specId)
  loadBuildFromXML(xmlText, "poe-lens-browser")
  activateEquippedFlasks()
  gemQualityTypes = qualityTypesFromXml(xmlText)
  if specId then build.treeTab:SetActiveSpec(specId) end
  wipeGlobalCache()
  build.buildFlag = true
  for _ = 1, 20 do
    runCallback("OnFrame")
    if not build.buildFlag then break end
  end
  local spec = build.spec
  local tree = spec.tree
  local nodes = { }
  local links = { }
  for id, node in pairs(spec.allocNodes) do
    table.insert(nodes, {
      id = tostring(id),
      x = node.x,
      y = node.y,
      allocated = true,
    })
  end
  for id, node in pairs(spec.allocNodes) do
    for _, linkedId in ipairs(node.linkedId) do
      if spec.allocNodes[linkedId] and id < linkedId then
        table.insert(links, { from = tostring(id), to = tostring(linkedId) })
      end
    end
  end
  local specs = { }
  for id, candidate in ipairs(build.treeTab.specList) do
    table.insert(specs, { id = id, title = titleOrDefault(candidate.title) })
  end
  local function entries(ids, values)
    local results = { }
    for _, id in ipairs(ids) do table.insert(results, { id = id, title = titleOrDefault(values[id].title) }) end
    return results
  end
  local output = build.calcsTab.mainOutput or { }
  local mainEnv = build.calcsTab.mainEnv
  local mainSkill = mainEnv and mainEnv.player and mainEnv.player.mainSkill
  return jsonEncode({
    specs = specs,
    skillSets = entries(build.skillsTab.skillSetOrderList, build.skillsTab.skillSets),
    itemSets = entries(build.itemsTab.itemSetOrderList, build.itemsTab.itemSets),
    activeSpec = build.treeTab.activeSpec,
    activeSkillSet = build.skillsTab.activeSkillSetId,
    activeItemSet = build.itemsTab.activeItemSetId,
    activeSkillName = mainSkill and mainSkill.activeEffect and mainSkill.activeEffect.grantedEffect and mainSkill.activeEffect.grantedEffect.name or nil,
    mainSkillFlags = flagsFromMainSkill(mainSkill),
    skillTooltips = mainEnv and skillTooltipFacts(mainEnv) or { },
    buildFacts = mainEnv and buildFacts(mainEnv, output, spec) or { offence = { }, skills = { }, defence = { }, buffs = { }, mobility = { }, passives = { }, ascendancies = { }, passiveTags = { }, items = { }, jewels = { }, performance = { } },
    summary = {
      totalDps = output.TotalDPS,
      combinedDps = output.CombinedDPS,
      life = output.Life,
      energyShield = output.EnergyShield,
      mana = output.Mana,
      armour = output.Armour,
      evasion = output.Evasion,
      totalEhp = output.TotalEHP,
    },
    equipment = equipmentFromActiveSet(),
    jewels = jewelsFromActiveSpec(),
    tree = {
      version = spec.treeVersion,
      nodes = nodes,
      links = links,
    },
  })
end
