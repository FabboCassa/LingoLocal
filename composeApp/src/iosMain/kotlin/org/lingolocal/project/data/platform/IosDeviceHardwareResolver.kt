package org.lingolocal.project.data.platform

import platform.Foundation.NSProcessInfo
import platform.UIKit.UIDevice

/**
 * Implementazione iOS di DeviceHardwareResolver.
 * Utilizza le API di sistema di Apple (NSProcessInfo e UIDevice) per rilevare
 * il modello del dispositivo e la RAM fisica.
 */
class IosDeviceHardwareResolver : DeviceHardwareResolver {
    override fun getHardwareInfo(): DeviceHardwareInfo {
        val physicalMemory = NSProcessInfo.processInfo.physicalMemory
        val totalRamGb = physicalMemory.toDouble() / (1024.0 * 1024.0 * 1024.0)

        val systemName = UIDevice.currentDevice.systemName
        val systemVersion = UIDevice.currentDevice.systemVersion
        val model = UIDevice.currentDevice.model
        val deviceName = "Apple $model ($systemName $systemVersion)"

        // Strategia: privilegiamo velocità percepita. Gemma 3 1B QAT è ottimizzato
        // per dispositivi mobili (qualità ~Q8, peso Q4).
        val (recommendedModelId, performanceTierLabel) = when {
            totalRamGb >= 4.0 -> {
                "gemma_3_1b_qat" to "Prestazioni Buone ⚡ (Consigliato Gemma 3 1B QAT)"
            }
            else -> {
                "qwen_0_5b" to "Risorse Limitate 🧪 (Consigliato Qwen 0.5B)"
            }
        }

        val cores = NSProcessInfo.processInfo.activeProcessorCount.toInt().coerceAtLeast(1)
        val recommendedThreads = ((cores * 3) / 4).coerceIn(2, 8)

        return DeviceHardwareInfo(
            deviceName = deviceName,
            totalRamGb = totalRamGb,
            recommendedModelId = recommendedModelId,
            performanceTierLabel = performanceTierLabel,
            recommendedThreads = recommendedThreads
        )
    }
}
