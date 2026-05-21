package org.lingolocal.project.data.platform

/**
 * Data class che rappresenta il report hardware dinamico del dispositivo.
 * Contiene informazioni sul nome del dispositivo, la memoria RAM rilevata,
 * il modello consigliato, il numero di thread consigliato per llama.cpp
 * e l'etichetta per la UI.
 */
data class DeviceHardwareInfo(
    val deviceName: String,
    val totalRamGb: Double,
    val recommendedModelId: String,
    val performanceTierLabel: String,
    val recommendedThreads: Int = 4
)
