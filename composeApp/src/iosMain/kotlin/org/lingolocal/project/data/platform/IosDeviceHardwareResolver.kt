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

        // I dispositivi iOS hanno una gestione della RAM molto aggressiva,
        // quindi 6 GB di RAM sono ottimali per modelli da 2B parametri
        val (recommendedModelId, performanceTierLabel) = when {
            totalRamGb >= 6.0 -> {
                "gemma_4_e2b" to "Prestazioni Eccellenti 🚀 (Consigliato Gemma 4 E2B)"
            }
            totalRamGb >= 4.0 -> {
                "gemma_2_2b" to "Prestazioni Buone ⚡ (Consigliato Gemma 2 2B)"
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
