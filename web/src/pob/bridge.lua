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
local function jsonEncode(value)
  local valueType = jsonType(value)
  if valueType == "nil" then return "null" end
  if valueType == "boolean" then return value and "true" or "false" end
  if valueType == "number" then return tostring(value) end
  if valueType == "string" then
    local encoded = jsonFormat("%q", value):gsub("\\\n", "\\n")
    return encoded
  end
  local isArray = #value > 0
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

local equipmentSlots = { "Weapon 1", "Weapon 2", "Helmet", "Body Armour", "Gloves", "Boots", "Amulet", "Ring 1", "Ring 2", "Belt" }

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

function inspectBuild(xmlText, specId)
  loadBuildFromXML(xmlText, "poe-lens-browser")
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
  local mainSkill = build.calcsTab.mainEnv and build.calcsTab.mainEnv.player and build.calcsTab.mainEnv.player.mainSkill
  return jsonEncode({
    specs = specs,
    skillSets = entries(build.skillsTab.skillSetOrderList, build.skillsTab.skillSets),
    itemSets = entries(build.itemsTab.itemSetOrderList, build.itemsTab.itemSets),
    activeSpec = build.treeTab.activeSpec,
    activeSkillSet = build.skillsTab.activeSkillSetId,
    activeItemSet = build.itemsTab.activeItemSetId,
    activeSkillName = mainSkill and mainSkill.activeEffect and mainSkill.activeEffect.grantedEffect and mainSkill.activeEffect.grantedEffect.name or nil,
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
    tree = {
      version = spec.treeVersion,
      nodes = nodes,
      links = links,
    },
  })
end
