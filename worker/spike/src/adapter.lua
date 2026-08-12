local adapter = { }

local function title_or_default(value)
	if value == nil or value == "" then
		return "Default"
	end
	return value
end

local function entries_from_ids(ids, values)
	local entries = { }
	for _, id in ipairs(ids) do
		local value = values[id]
		table.insert(entries, { id = id, title = title_or_default(value and value.title) })
	end
	return entries
end

local function validate_selection(selection)
	if type(selection) ~= "table"
		or type(selection.activeSpec) ~= "number" or selection.activeSpec % 1 ~= 0
		or type(selection.activeSkillSet) ~= "number" or selection.activeSkillSet % 1 ~= 0
		or type(selection.activeItemSet) ~= "number" or selection.activeItemSet % 1 ~= 0 then
		error("POE_LENS:INVALID_SELECTION", 0)
	end
	if not build.treeTab.specList[selection.activeSpec] then
		error("POE_LENS:UNKNOWN_SPEC", 0)
	end
	if not build.skillsTab.skillSets[selection.activeSkillSet] then
		error("POE_LENS:UNKNOWN_SKILL_SET", 0)
	end
	if not build.itemsTab.itemSets[selection.activeItemSet] then
		error("POE_LENS:UNKNOWN_ITEM_SET", 0)
	end
end

function adapter.inspect(xml_text)
	local import_start = os.clock()
	loadBuildFromXML(xml_text, "poe-lens-spike")
	local import_ms = (os.clock() - import_start) * 1000

	local inspect_start = os.clock()
	local specs = { }
	for id, spec in ipairs(build.treeTab.specList) do
		table.insert(specs, { id = id, title = title_or_default(spec.title) })
	end
	local result = {
		specs = specs,
		skillSets = entries_from_ids(build.skillsTab.skillSetOrderList, build.skillsTab.skillSets),
		itemSets = entries_from_ids(build.itemsTab.itemSetOrderList, build.itemsTab.itemSets),
		activeSpec = build.treeTab.activeSpec,
		activeSkillSet = build.skillsTab.activeSkillSetId,
		activeItemSet = build.itemsTab.activeItemSetId,
	}

	return result, {
		import = import_ms,
		inspect = (os.clock() - inspect_start) * 1000,
	}
end

function adapter.analyze(xml_text, selection)
	local import_start = os.clock()
	loadBuildFromXML(xml_text, "poe-lens-spike")
	local import_ms = (os.clock() - import_start) * 1000

	local analyze_start = os.clock()
	validate_selection(selection)
	build.treeTab:SetActiveSpec(selection.activeSpec)
	build.itemsTab:SetActiveItemSet(selection.activeItemSet)
	build.skillsTab:SetActiveSkillSet(selection.activeSkillSet)
	wipeGlobalCache()
	build.buildFlag = true
	for _ = 1, 20 do
		runCallback("OnFrame")
		if not build.buildFlag then
			break
		end
	end
	assert(not build.buildFlag, "POE_LENS:CALCULATION_DID_NOT_SETTLE")

	local output = build.calcsTab.mainOutput
	local null = require("dkjson").null
	local result = {
		selection = {
			activeSpec = build.treeTab.activeSpec,
			activeSkillSet = build.skillsTab.activeSkillSetId,
			activeItemSet = build.itemsTab.activeItemSetId,
		},
		summary = {
			totalDps = output.TotalDPS or null,
			combinedDps = output.CombinedDPS or null,
			life = output.Life or null,
			energyShield = output.EnergyShield or null,
			mana = output.Mana or null,
			armour = output.Armour or null,
			evasion = output.Evasion or null,
			totalEhp = output.TotalEHP or null,
		},
	}

	return result, {
		import = import_ms,
		analyze = (os.clock() - analyze_start) * 1000,
	}
end

return adapter
