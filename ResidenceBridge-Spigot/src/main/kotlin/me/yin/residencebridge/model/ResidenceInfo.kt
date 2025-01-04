package me.yin.residencebridge.model

import java.util.*

data class ResidenceInfo(
    var residenceName: String,
    var ownerUUID: UUID,
    var ownerName: String,
    var residenceFlags: MutableMap<String, Boolean>,
    var playerFlags: MutableMap<String, MutableMap<String, Boolean>>,
    var serverName: String,
)