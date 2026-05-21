package org.lingolocal.project.data.platform

import android.app.ActivityManager
import android.content.Context
import android.os.Build

/**
 * Implementazione Android di DeviceHardwareResolver.
 * Utilizza le API del sistema operativo Android per rilevare il nome del dispositivo (es. "Google Pixel 8 Pro")
 * e la RAM fisica totale installata.
 */
class AndroidDeviceHardwareResolver(private val context: Context) : DeviceHardwareResolver {
    override fun getHardwareInfo(): DeviceHardwareInfo {
        val rawMan = Build.MANUFACTURER ?: "Dispositivo"
        val manufacturer = if (rawMan.isNotEmpty()) {
            rawMan.substring(0, 1).uppercase() + rawMan.substring(1)
        } else {
            "Dispositivo"
        }
        val model = Build.MODEL ?: "Android"
        val deviceName = if (model.startsWith(manufacturer, ignoreCase = true)) {
            model
        } else {
            "$manufacturer $model"
        }

        val actManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val memInfo = ActivityManager.MemoryInfo()
        actManager.getMemoryInfo(memInfo)

        // Conversione byte -> Gigabyte
        val totalRamGb = memInfo.totalMem.toDouble() / (1024.0 * 1024.0 * 1024.0)

        // Classificazione intelligente delle prestazioni locali per Pixel 8 Pro e altri modelli
        val (recommendedModelId, performanceTierLabel) = when {
            totalRamGb >= 10.0 -> {
                "gemma_4_e2b" to "Prestazioni Eccellenti 🚀 (Consigliato Gemma 4 E2B)"
            }
            totalRamGb >= 6.0 -> {
                "gemma_2_2b" to "Prestazioni Buone ⚡ (Consigliato Gemma 2 2B)"
            }
            else -> {
                "qwen_0_5b" to "Risorse Limitate 🧪 (Consigliato Qwen 0.5B)"
            }
        }

        // Thread llama.cpp: usa ~75% dei core fisici, cappato a 8.
        // I core "efficiency" non aiutano: meglio non sovra-allocare.
        // Pixel 8 Pro Tensor G3 = 9 core → 6 thread è il sweet-spot.
        val cores = Runtime.getRuntime().availableProcessors().coerceAtLeast(1)
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
