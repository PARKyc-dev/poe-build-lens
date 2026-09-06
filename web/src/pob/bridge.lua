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
      for _, lines in ipairs({ item.implicitModLines or { }, item.explicitModLines or { } }) do
        for _, mod in ipairs(lines) do
          table.insert(modifiers, mod.line or mod.extra)
        end
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
      for _, lines in ipairs({ item.implicitModLines or { }, item.explicitModLines or { } }) do
        for _, mod in ipairs(lines) do
          table.insert(modifiers, mod.line or mod.extra)
        end
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

local function isAttackSkill(skill)
  local flags = skill.skillFlags or { }
  local skillTypes = skill.skillTypes or { }
  return flags.attack or skillTypes[SkillType.Attack]
end

local function flagsFromMainSkill(mainSkill)
  if not mainSkill then return nil end
  local skillFlags = mainSkill.skillFlags or { }
  return {
    isAttack = isAttackSkill(mainSkill) and true or false,
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
  if skill.buffSkill and flags.dot then return "persistent" end
  if isTriggeredMainSkill(skill) then return "trigger" end
  if flags.haveMinion or flags.minion then return "minion" end
  if flags.totem then return "totem" end
  if flags.trap then return "trap" end
  if flags.mine then return "mine" end
  if flags.brand then return "brand" end
  if isAttackSkill(skill) then return "attack" end
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

local function operationName(value)
  if type(value) ~= "string" then return "" end
  return value:gsub("(%l)(%u)", "%1-%2"):gsub("(%a)(%d)", "%1-%2"):gsub("[%s_]+", "-"):lower()
end

local function operationSubject(name)
  local charge = name:match("([%a]+)%-charges?")
  if charge then return charge .. "-charge" end
  if name:find("energy%-shield") then return "energy-shield" end
  if name:find("flask%-charge") then return "flask-charge" end
  if name:find("mana") then return "mana" end
  if name:find("life") then return "life" end
  if name:match("^rage%-") or name:match("%-rage%-") or name:match("%-rage$") or name == "rage" then return "rage" end
  local from, to = name:match("([%a]+)%-damage.-to%-([%a]+)")
  if from and to then return from .. "-to-" .. to end
  return name:gsub("^chance%-to%-", ""):gsub("%-on%-[%a-]+", "")
end

local function isExcludedOperationSubject(subject)
  return subject == "life"
    or subject:find("life", 1, true)
    or subject:find("resist", 1, true)
    or subject:find("movement", 1, true)
    or subject:find("move%-speed")
    or subject:find("armour", 1, true)
end

local function hasOperationEnhancement(name)
  return name:find("damage", 1, true)
    or name:find("speed", 1, true)
    or name:find("duration", 1, true)
    or name:find("effect", 1, true)
    or name:find("area", 1, true)
    or name:find("projectile", 1, true)
    or name:find("spell", 1, true)
    or name:find("attack", 1, true)
    or name:find("trap", 1, true)
    or name:find("mine", 1, true)
    or name:find("totem", 1, true)
    or name:find("brand", 1, true)
    or name:find("ailment", 1, true)
end

local function classifyOperationFacts(mod)
  local name = operationName(mod and mod.name)
  if name == "" or name:find("^condition:") or name:find("^multiplier:") or name:find("^pvp") then return { } end
  local subject = operationSubject(name)
  if subject == "" or isExcludedOperationSubject(subject) then return { } end

  local onKill = name:find("on%-kill") or name:find("when%-you%-kill")
  local onHit = name:find("on%-hit") or name:find("when%-you%-hit")
  local onDamaged = name:find("on%-damaged") or name:find("on%-taking%-damage") or name:find("when%-hit") or name:find("when%-you%-are%-hit")
  local conditional = onKill or onHit or onDamaged
  local chargeCondition = subject:find("%-charge$") and conditional

  local actions = { }
  if name:find("convert", 1, true) or name:find("conversion", 1, true) then
    table.insert(actions, "convert")
  elseif name:find("trigger", 1, true) or (conditional and not name:find("gain", 1, true) and not name:find("lose", 1, true) and not chargeCondition) then
    table.insert(actions, "trigger")
  elseif name:find("reserve", 1, true) then
    table.insert(actions, "reserve")
  elseif name:find("cooldown", 1, true) then
    table.insert(actions, "cooldown")
  elseif name:find("cost", 1, true) or name:find("charges%-used") or name:find("lose", 1, true) then
    table.insert(actions, "consume")
  elseif name:find("gain", 1, true) or chargeCondition then
    table.insert(actions, "gain")
  elseif name:find("duration", 1, true) or name:find("sustain", 1, true) then
    table.insert(actions, "maintain")
  elseif hasOperationEnhancement(name) then
    table.insert(actions, "enhance")
  end

  if onKill then table.insert(actions, "on-kill") end
  if onHit then table.insert(actions, "on-hit") end
  if onDamaged then table.insert(actions, "on-damaged") end
  return actions, subject
end

local function addOperationFact(result, seen, sourceType, sourceName, action, subject, effect)
  local key = sourceType .. "|" .. sourceName .. "|" .. action .. "|" .. subject .. "|" .. effect
  if not seen[key] then
    seen[key] = true
    table.insert(result, {
      sourceType = sourceType,
      sourceName = sourceName,
      action = action,
      subject = subject,
      effects = { effect },
    })
  end
end

local function operationFactsFromModList(result, seen, modList, sourceType, sourceName)
  local current = modList
  while current do
    for _, mod in ipairs(current or { }) do
      local actions, subject = classifyOperationFacts(mod)
      for _, action in ipairs(actions or { }) do
        addOperationFact(result, seen, sourceType, sourceName, action, subject, mod.name)
      end
    end
    current = current.parent
  end
end

local function operationFactsFromEffects(result, seen, effects, sourceType, sourceName)
  for _, effect in ipairs(effects or { }) do
    if type(effect) == "string" and effect ~= "" then
      local actions, subject = classifyOperationFacts({ name = effect })
      for _, action in ipairs(actions or { }) do
        addOperationFact(result, seen, sourceType, sourceName, action, subject, effect)
      end
    end
  end
end

local function itemEffectLines(item)
  local effects = { }
  for _, mod in ipairs(item.explicitModLines or { }) do
    local effect = mod.line or mod.extra
    if type(effect) == "string" and effect ~= "" then table.insert(effects, effect) end
  end
  return effects
end

local function operationFacts(env, spec)
  local result = jsonArray()
  local seen = { }
  for _, skill in ipairs(env.player.activeSkillList or { }) do
    local effect = skill.activeEffect and skill.activeEffect.grantedEffect
    local name = effect and effect.name
    if name then operationFactsFromModList(result, seen, skill.skillModList, "skill", name) end
  end
  for _, slotName in ipairs(equipmentSlots) do
    local slot = build.itemsTab.activeItemSet[slotName]
    local item = slot and slot.selItemId and build.itemsTab.items[slot.selItemId]
    if item then
      operationFactsFromModList(result, seen, item.modList, "item", item.title or item.name or item.baseName)
      operationFactsFromEffects(result, seen, itemEffectLines(item), "item", item.title or item.name or item.baseName)
    end
  end
  for socketId, itemId in pairs(spec.jewels or { }) do
    local item = build.itemsTab.items[itemId]
    if item then
      operationFactsFromModList(result, seen, item.modList, "item", item.title or item.name or item.baseName)
      operationFactsFromEffects(result, seen, itemEffectLines(item), "item", item.title or item.name or item.baseName)
    end
  end
  for _, node in pairs(spec.allocNodes or { }) do
    local sourceType = node.ascendancyName and "ascendancy" or "passive"
    local sourceName = node.dn or node.name
    if sourceName then
      operationFactsFromModList(result, seen, node.modList, sourceType, sourceName)
      operationFactsFromEffects(result, seen, node.sd, sourceType, sourceName)
    end
  end
  for _, skill in ipairs(env.player.activeSkillList or { }) do
    if skill.buffSkill then
      for _, buff in ipairs(skill.buffList or { }) do
        if buff.name and not buff.applyNotPlayer then
          addOperationFact(result, seen, "buff", buff.name, "maintain", operationName(buff.name), buff.name)
          operationFactsFromModList(result, seen, buff.modList, "buff", buff.name)
        end
      end
    end
  end
  return result
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
    local isMain = skill == player.mainSkill
    local isAttack = isAttackSkill(skill)
    if (not flags.disable or (isMain and isAttack)) and (not isMovement or (isAttack and isMain)) and (not skill.buffSkill or flags.dot or (isMain and isAttack)) and (flags.hit or flags.dot or isAttack) and grantedEffect.name then
      local delivery = deliveryFromSkill(skill)
      local key = grantedEffect.name .. "|" .. delivery
      if not candidates[key] then
        candidates[key] = {
          name = grantedEffect.name,
          delivery = delivery,
          tags = tagsFromSkill(skill),
          skill = skill,
          combinedDps = 0,
          isMain = isMain,
          modifiers = jsonArray(),
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
    local seenModifiers = { }
    local modList = candidate.skill.skillModList
    while modList do
      for _, mod in ipairs(modList) do
        if mod.source and mod.source ~= "Base" then
          local modifierKey = (mod.name or "") .. "|" .. (mod.type or "") .. "|" .. mod.source
          if not seenModifiers[modifierKey] then
            seenModifiers[modifierKey] = true
            table.insert(candidate.modifiers, { name = mod.name, type = mod.type, source = mod.source, conditional = mod[1] and true or false })
          end
        end
      end
      modList = modList.parent
    end
    candidate.skill = nil
  end
  player.mainSkill = selectedSkill
  build.calcsTab.calcs.perform(env)
  local ranked = { }
  for _, candidate in pairs(candidates) do
    if candidate.combinedDps > 0 or candidate.isMain then table.insert(ranked, candidate) end
  end
  table.sort(ranked, function(left, right)
    if left.isMain ~= right.isMain then return left.isMain end
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

local gemQualityTypes = { }

local function qualityType(gem)
  return gem.qualityId or (gem.gemData and gemQualityTypes[gem.gemData.gameId]) or "Default"
end

local function effectDetails(effect, includeDescription)
  local result = jsonArray()
  local grantedEffect = effect.grantedEffect or { }
  if includeDescription and type(grantedEffect.description) == "string" and grantedEffect.description ~= "" then
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

local function skillFacts(env)
  local result = jsonArray()
  for _, skill in ipairs(env.player.activeSkillList or { }) do
    local effect = skill.activeEffect
    local grantedEffect = effect and effect.grantedEffect
    if grantedEffect and not (skill.skillFlags or { }).disable then
      local gem = effect.srcInstance or { }
      local supports = jsonArray()
      for _, support in ipairs(skill.effectList or { }) do
        if support.grantedEffect.support then
          local source = support.srcInstance or { }
          table.insert(supports, {
            name = support.grantedEffect.name,
            level = support.level,
            quality = support.quality,
            qualityType = qualityType(source),
            enabled = true,
            awakened = support.grantedEffect.plusVersionOf and true or false,
            effects = effectDetails(support, false),
          })
        end
      end
      table.insert(result, {
        name = grantedEffect.name,
        level = effect.level,
        quality = effect.quality,
        qualityType = qualityType(gem),
        enabled = true,
        awakened = false,
        effects = effectDetails(effect, true),
        supports = supports,
      })
    end
  end
  return result
end

local function mobilityFacts(player)
  local result = jsonArray()
  local seen = { }
  for _, skill in ipairs(player.activeSkillList or { }) do
    local name = skill.activeEffect and skill.activeEffect.grantedEffect and skill.activeEffect.grantedEffect.name
    local flags = skill.skillFlags or { }
    if name and skill.skillTypes and skill.skillTypes[SkillType.Movement] and not (isAttackSkill(skill) and skill.buffSkill) and not seen[name] then
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
  return effectDetails(skill.activeEffect or { }, true)
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
  if skill.skillTypes and skill.skillTypes[SkillType.Mark] then return "mark" end
  if skill.skillTypes and skill.skillTypes[SkillType.Hex] then return "curse" end
  if skill.skillTypes and skill.skillTypes[SkillType.Aura] then return "aura" end
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
    local kind = buffKind(skill, { })
    if kind == "curse" or kind == "mark" then
      local grantedEffect = skill.activeEffect and skill.activeEffect.grantedEffect
      if grantedEffect and grantedEffect.name then
        addBuff(result, seen, grantedEffect.name, kind, "enemy", tagsFromModList(skill.skillModList))
      end
    end
    if skill.buffSkill then
      for _, buff in ipairs(skill.buffList or { }) do
        if buff.name then
          local buffType = buffKind(skill, buff)
          if buff.applyNotPlayer then
            if buffType == "curse" or buffType == "mark" then
              addBuff(result, seen, buff.name, buffType, "enemy", tagsFromModList(buff.modList))
            end
          else
            addBuff(result, seen, buff.name, buffType, "player", tagsFromModList(buff.modList))
          end
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
      for _, lines in ipairs({ item.implicitModLines or { }, item.explicitModLines or { } }) do
        for _, mod in ipairs(lines) do
          table.insert(modifiers, mod.line or mod.extra)
        end
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
      for _, lines in ipairs({ item.implicitModLines or { }, item.explicitModLines or { } }) do
        for _, mod in ipairs(lines) do
          table.insert(modifiers, mod.line or mod.extra)
        end
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
  local conditions = { }
  for name, value in pairs(build.configTab.input or { }) do
    if name:match("^condition") or name:match("^buff") or name:match("^use.*Charges$") then
      if type(value) == "boolean" or type(value) == "number" then conditions[name] = value end
    end
  end
  return {
    conditions = conditions,
    offence = offenceFacts(env),
    skills = skillFacts(env),
    defence = defenceFacts(output),
    buffs = buffFacts(env, output),
    mobility = mobilityFacts(env.player),
    passives = passiveFacts(spec),
    ascendancies = ascendancyFacts(spec),
    passiveTags = allPassiveTags(spec),
    items = itemFacts(),
    jewels = jewelFacts(spec),
    operationFacts = operationFacts(env, spec),
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
    buildFacts = mainEnv and buildFacts(mainEnv, output, spec) or { offence = { }, skills = { }, defence = { }, buffs = { }, mobility = { }, passives = { }, ascendancies = { }, passiveTags = { }, items = { }, jewels = { }, operationFacts = jsonArray(), performance = { } },
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
