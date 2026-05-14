# Fase 4: Multimodality - Audio & TTS

## Obiettivo
Arricchire l'app con l'interazione vocale bidirezionale (insegnante AI): l'utente parla (Audio-to-Text via LLM) e l'app risponde con voce sintetizzata in locale (Text-to-Speech).

## Task
- [ ] **Task 4.1: Registrazione e Gestione Microfono**
  - Gestione permessi Microfono in KMP.
  - Implementare il registratore audio nativo e convertire lo stream nei parametri previsti dal modello (es. PCM mono 16kHz).
  - *Test:* Registrare un file audio temporaneo di 5 secondi e riprodurlo.
- [ ] **Task 4.2: Inference Audio-to-Text**
  - Iniettare il flusso audio nel motore di inferenza (o `llama.cpp` se il modello nativo lo supporta, o `whisper.cpp` come engine parallelo).
  - *Test:* Ricevere la trascrizione accurata di una registrazione di test con misurazione delle tempistiche.
- [ ] **Task 4.3: Motore Text-to-Speech (TTS)**
  - Selezionare e integrare un motore TTS in C++ commerciale e off-grid (es. Piper TTS) per il voice synthesis a bassa latenza.
  - Creare il bridge JNI/CInterop e il ViewModel/UseCase per passare la stringa di testo al motore e ricevere il playback audio.
  - *Test:* Passare una stringa "Hello World" generata e ascoltare l'output vocale, verificando l'assenza di latenze bloccanti.
- [ ] **Task 4.4: UI e Gestione Conversazionale (MVVM)**
  - Creare la UI Conversazionale con pulsante "Hold to Talk" e animazioni delle onde sonore (Voice Visualizer).
  - Orchestrazione complessa nel ViewModel: Registra -> Trascrivi -> RAG -> Genera Risposta Testo -> Passa a TTS -> Riproduci.
  - Mantenere la logica orchestrata tramite Coroutines Flow all'interno della clean architecture.
  - *Test:* Completamento di un ciclo di iterazione vocale completo (Turn-taking user-AI) con aggiornamenti di stato sulla View.
