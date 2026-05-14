# Specifiche Architetturali: Progetto "LingoLocal"
## Piattaforma di Apprendimento Linguistico Iper-Personalizzata, Multimodale e 100% On-Device

### 1. Visione del Progetto
**LingoLocal** è un'applicazione mobile innovativa per l'apprendimento delle lingue che garantisce **privacy totale** e funzionamento offline. Il cuore dell'applicazione è un motore AI unificato (LLM multimodale) in esecuzione localmente sul dispositivo dell'utente. Il sistema abbandona i percorsi di studio predefiniti per generare dinamicamente quiz, flashcard e conversazioni basate sui materiali personali dell'utente (libri, appunti fotografati, file PDF).

---

### 2. Stack Tecnologico Aggiornato
L'architettura è stata ottimizzata per sfruttare modelli Edge multimodali, eliminando la necessità di librerie separate per OCR e Speech-to-Text.

*   **Framework UI & Core Logic:** Kotlin Multiplatform (KMP) con **Compose Multiplatform** per una singola codebase su Android e iOS.
*   **Motore di Inferenza AI:** `llama.cpp` integrato via JNI (Android) e C/Objective-C interop (iOS), sfruttando l'accelerazione hardware nativa (Vulkan/Metal).
*   **Core AI (LLM Multimodale):**
    *   **Modello Target:** **Gemma 4 E2B** (Edge 2 Billion parameters) in formato GGUF con quantizzazione a 4-bit.
    *   *Opzione High-End:* Gemma 4 E4B per dispositivi con >8GB di RAM.
    *   *Capacità:* Gestione nativa di input testuale, audio raw (comprensione vocale) e visivo (analisi immagini).
*   **Audio Engine (Text-to-Speech):** Librerie C++ ultraleggere come **Piper** o **VITS** per la generazione della voce dell'insegnante virtuale a bassa latenza.
*   **Database & Vector RAG (Retrieval-Augmented Generation):**
    *   **Dati Strutturati:** SQLite tramite SQLDelight (gestione SRS, progressi, mazzi di carte).
    *   **Dati Vettoriali:** Estensione `sqlite-vec` o **ObjectBox** per l'archiviazione degli embedding testuali estratti dai materiali dell'utente.

---

### 3. Flussi Funzionali e Multimodalità Nativa

#### 3.1. Ingestione dei Contenuti (Vision-to-Text & RAG)
Il processo di estrazione dai materiali dell'utente non richiede più OCR esterno:
1.  L'utente scatta una foto a una pagina di grammatica o carica un PDF.
2.  L'immagine viene passata direttamente a Gemma 4 con un prompt specifico (es. *"Analizza questa pagina, estrai i concetti grammaticali chiave e restituiscili in un array JSON"*).
3.  Il testo estratto viene diviso in chunk, vettorizzato tramite l'embedding on-device e salvato nel Vector DB per le interrogazioni future.

#### 3.2. Valutazione Orale e Conversazione (Audio-to-Text)
L'app sfrutta le capacità audio native del modello Edge, rimuovendo le dipendenze da STT esterni:
1.  L'utente registra una risposta vocale.
2.  L'audio viene fornito al modello LLM.
3.  Il modello trascrive l'audio, valuta l'accuratezza della pronuncia e della grammatica, e fornisce un feedback contestuale generato dinamicamente.

#### 3.3. Motore di Generazione Esercizi (JSON Function Calling)
Tutte le modalità di apprendimento (Quiz a risposta multipla, completamento "cloze", Flashcard Spaced Repetition) sono alimentate dal *Function Calling* di Gemma 4. L'LLM è istruito tramite System Prompt per restituire l'output rigorosamente in formati JSON validi, che il layer Kotlin decodifica per renderizzare dinamicamente la UI in Compose.

---

### 4. Gestione delle Risorse e Limitazioni Mobile
Per mantenere l'app scalabile e rispettare i vincoli dei dispositivi mobili:
*   **Modularity On-Demand:** L'app base peserà meno di 100 MB. Al primo avvio (o al cambio di lingua), l'app scaricherà dal server i modelli GGUF (circa 2-3 GB per Gemma 4 E2B) e i modelli vocali TTS specifici per la lingua scelta.
*   **Memory Management:** Il consumo di RAM è limitato a un massimo di 3 GB durante l'inferenza, garantendo fluidità anche sui dispositivi di fascia media.
*   **Thermal Throttling:** Implementazione di logiche a livello di KMP per monitorare la temperatura del dispositivo, pausando l'inferenza in background o riducendo la velocità di generazione se necessario.

---

### 5. Fasi di Sviluppo (Roadmap)
1.  **Fase 1 (Core & UI):** Setup KMP, Compose Multiplatform e bridge per `llama.cpp`. Implementazione del download asincrono dei modelli.
2.  **Fase 2 (Text & RAG):** Integrazione del Vector DB, caricamento testo semplice e generazione di quiz testuali via JSON.
3.  **Fase 3 (Multimodality - Vision):** Implementazione dell'input tramite fotocamera per l'ingestione di immagini dirette verso l'LLM.
4.  **Fase 4 (Multimodality - Audio & TTS):** Integrazione dell'input vocale e del Text-to-Speech (Piper) per le sessioni di conversazione simulata.