package me.yin.residencebridge.model

import java.util.*

data class ResidenceInfo(
    val residenceName: String,
    val ownerUUID: UUID,
    val ownerName: String,
    val residenceFlags: MutableMap<String, Boolean>,
    val playerFlags: MutableMap<String, MutableMap<String, Boolean>>,
    val serverName: String,
)