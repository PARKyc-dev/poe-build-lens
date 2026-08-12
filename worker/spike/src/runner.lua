local adapter_path = assert(arg[1], "adapter path is required")

package.path = "../runtime/lua/?.lua;../runtime/lua/?/init.lua;" .. package.path

local function emit(value)
	local encoded = assert(require("dkjson").encode(value))
	io.stdout:write("POE_LENS_JSON\t", encoded, "\n")
	io.stdout:flush()
end

local startup_start = os.clock()
dofile("HeadlessWrapper.lua")
local adapter = dofile(adapter_path)
local json = require("dkjson")
emit({ event = "ready", startupCpuMs = (os.clock() - startup_start) * 1000 })

local function read_file(path)
	local file, error_message = io.open(path, "rb")
	assert(file, error_message)
	local text = file:read("*a")
	file:close()
	return text
end

local function request_error(code)
	error("POE_LENS:" .. code, 0)
end

local function handle(request)
	if type(request.pobPath) ~= "string" then
		request_error("INVALID_POB_PATH")
	end
	local xml_text = read_file(request.pobPath)
	local result, timings
	if request.operation == "inspect" then
		result, timings = adapter.inspect(xml_text)
	elseif request.operation == "analyze" then
		result, timings = adapter.analyze(xml_text, request.selection)
	else
		request_error("UNKNOWN_OPERATION")
	end
	return { id = request.id, result = result, timingsCpuMs = timings }
end

for line in io.lines() do
	local request = json.decode(line)
	if type(request) ~= "table" then
		emit({ error = { code = "INVALID_REQUEST" } })
	elseif request.operation == "shutdown" then
		break
	else
		local ok, response = xpcall(function()
			return handle(request)
		end, function(error_message)
			return tostring(error_message):match("^(POE_LENS:[A-Z_]+)") or "POE_LENS:RUNTIME_ERROR"
		end)
		if ok then
			emit(response)
		else
			emit({ id = request.id, error = { code = response:sub(10) } })
		end
	end
end
