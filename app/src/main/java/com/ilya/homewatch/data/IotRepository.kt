package com.ilya.homewatch.data

import org.json.JSONArray
import org.json.JSONObject

private fun JSONArray?.objects(): List<JSONObject> =
    if (this == null) emptyList() else (0 until length()).mapNotNull { optJSONObject(it) }

private fun JSONArray?.strings(): List<String> =
    if (this == null) emptyList() else (0 until length()).map { optString(it) }

/** Официальный API умного дома Яндекса. */
class IotRepository {
    private val base = "https://api.iot.yandex.net/v1.0"

    suspend fun fetchHome(token: String): Home {
        val json = Net.getJson("$base/user/info", token)

        val roomObjects = json.optJSONArray("rooms").objects()
        val rooms = roomObjects.map { Room(it.getString("id"), it.getString("name")) }
        val roomByDevice = mutableMapOf<String, String>()
        roomObjects.forEach { r ->
            val roomId = r.getString("id")
            r.optJSONArray("devices").strings().forEach { deviceId -> roomByDevice[deviceId] = roomId }
        }

        val devices = json.optJSONArray("devices").objects().map { parseDevice(it, roomByDevice) }
        val scenarios = json.optJSONArray("scenarios").objects().map {
            Scenario(it.getString("id"), it.getString("name"))
        }
        return Home(rooms, devices, scenarios)
    }

    suspend fun sendAction(token: String, deviceId: String, type: String, instance: String, value: Any) {
        val body = JSONObject().put(
            "devices",
            JSONArray().put(
                JSONObject()
                    .put("id", deviceId)
                    .put(
                        "actions",
                        JSONArray().put(
                            JSONObject()
                                .put("type", type)
                                .put("state", JSONObject().put("instance", instance).put("value", value))
                        )
                    )
            )
        )
        Net.postJson("$base/devices/actions", token, body)
    }

    suspend fun runScenario(token: String, scenarioId: String) {
        Net.postJson("$base/scenarios/$scenarioId/actions", token, JSONObject())
    }

    private fun parseDevice(d: JSONObject, roomByDevice: Map<String, String>): Device {
        val id = d.getString("id")
        val capabilities = d.optJSONArray("capabilities").objects().mapNotNull { parseCapability(it) }
        val properties = d.optJSONArray("properties").objects().mapNotNull { p ->
            val state = p.optJSONObject("state") ?: return@mapNotNull null
            Property(
                instance = state.optString("instance"),
                value = state.opt("value"),
                unit = p.optJSONObject("parameters")?.optString("unit").orEmpty(),
            )
        }
        return Device(
            id = id,
            name = d.getString("name"),
            type = d.optString("type"),
            roomId = d.optString("room").ifBlank { roomByDevice[id] },
            capabilities = capabilities,
            properties = properties,
        )
    }

    private fun parseCapability(c: JSONObject): Capability? {
        val type = c.optString("type")
        if (type.isBlank()) return null
        val params = c.optJSONObject("parameters") ?: JSONObject()
        val state = c.optJSONObject("state")

        var min = 0.0
        var max = 100.0
        var precision = 1.0
        params.optJSONObject("range")?.let { r ->
            min = r.optDouble("min", 0.0)
            max = r.optDouble("max", 100.0)
            precision = r.optDouble("precision", 1.0)
        }
        params.optJSONObject("temperature_k")?.let { r ->
            min = r.optDouble("min", 2700.0)
            max = r.optDouble("max", 6500.0)
            precision = 100.0
        }

        var instance = state?.optString("instance").orEmpty()
        if (instance.isBlank()) instance = params.optString("instance")
        if (instance.isBlank()) {
            instance = if (type.endsWith(".color_setting")) "temperature_k" else "on"
        }

        val modes = params.optJSONArray("modes").objects().map { it.optString("value") }

        return Capability(
            type = type,
            instance = instance,
            value = state?.opt("value"),
            min = min,
            max = max,
            precision = precision,
            modes = modes,
        )
    }
}
