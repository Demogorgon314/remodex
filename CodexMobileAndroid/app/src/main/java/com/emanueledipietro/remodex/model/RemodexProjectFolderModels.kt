package com.emanueledipietro.remodex.model

data class RemodexProjectLocation(
    val id: String,
    val label: String,
    val path: String,
)

data class RemodexProjectDirectoryEntry(
    val name: String,
    val path: String,
    val isSymlink: Boolean = false,
)

data class RemodexProjectDirectoryListing(
    val path: String,
    val parentPath: String?,
    val entries: List<RemodexProjectDirectoryEntry> = emptyList(),
)
