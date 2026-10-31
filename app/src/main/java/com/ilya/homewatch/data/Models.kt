package com.ilya.homewatch.data

data class Capability(
    val type: String,        // devices.capabilities.on_off / range / color_setting / mode / toggle
    val instance: String,    // on / brightness / temperature / temperature_k / ...
    val value: Any?,         // Boolean / Number / String
    val min: Double = 0.0,
    val max: Double = 100.0,
    val precision: Double = 1.0,
    val modes: List<String> = emptyList(),
)

data class Property(
    val instance: String,
    val value: Any?,
    val unit: String,
)

data class Device(
    val id: String,
    val name: String,
    val type: String,
    val roomId: String?,
    val capabilities: List<Capability>,
    val properties: List<Property>,
) {
    val onOff: Capability?
        get() = capabilities.firstOrNull { it.type.endsWith(".on_off") }

    val isOn: Boolean
        get() = onOff?.value == true
}

data class Room(val id: String, val name: String)

data class Scenario(val id: String, val name: String)

data class Home(
    val rooms: List<Room>,
    val devices: List<Device>,
    val scenarios: List<Scenario>,
)
