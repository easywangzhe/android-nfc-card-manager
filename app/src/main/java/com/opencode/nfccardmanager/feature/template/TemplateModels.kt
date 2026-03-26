package com.opencode.nfccardmanager.feature.template

data class WriteTemplate(
    val id: String,
    val name: String,
    val version: String,
    val content: String,
    val description: String,
)
