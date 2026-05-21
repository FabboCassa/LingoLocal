package org.lingolocal.project.data.platform

/**
 * Interfaccia multipiattaforma per la scansione hardware del dispositivo in tempo reale.
 * Fornisce le informazioni sulle specifiche fisiche per suggerire il modello ottimale.
 */
interface DeviceHardwareResolver {
    /**
     * Esegue la scansione e restituisce i dati hardware dinamici del telefono corrente.
     */
    fun getHardwareInfo(): DeviceHardwareInfo
}
