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

        // Strategia: privilegiamo VELOCITÀ percepita su qualità marginale.
        // Gemma 3 1B QAT è ottimizzato Google per mobile (qualità ~Q8, peso Q4)
        // e gira a ~15 token/s anche su mid-range. Gemma 2 2B solo se molta RAM.
        val (recommendedModelId, performanceTierLabel) = when {
            totalRamGb >= 8.0 -> {
                "gemma_3_1b_qat" to "Prestazioni Eccellenti 🚀 (Consigliato Gemma 3 1B QAT)"
            }
            totalRamGb >= 4.0 -> {
                "gemma_3_1b_qat" to "Prestazioni Buone ⚡ (Consigliato Gemma 3 1B QAT)"
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
