# Fase 1: Core & UI

## Obiettivo
Impostare l'infrastruttura di base dell'applicazione KMP (Kotlin Multiplatform), la UI con Compose Multiplatform seguendo rigorosamente l'architettura MVVM, e integrare il motore di inferenza LLM (`llama.cpp`) in locale per il test iniziale.

## Linee Guida Architetturali
- **Architettura:** MVVM (Model-View-ViewModel).
- **Classi Pulite:** Nessuna logica di business o formattazione dati nelle View (Compose). Le classi devono essere piccole, specializzate e separate per dominio.
- **Iniezione delle Dipendenze:** Koin (supporto nativo KMP, licenza Apache 2.0 - ottima per scopi commerciali).
- **Risorse e Stringhe:** Compose Multiplatform Resources. Tutte le stringhe vanno centralizzate in `composeResources/values/strings.xml` per facilitare traduzioni e modifiche. Nessuna stringa "hardcoded" nel codice.

## Task
- [ ] **Task 1.1: Setup Progetto KMP e Compose**
  - Inizializzare il progetto KMP con target Android e iOS.
  - Configurare Gradle per Compose Multiplatform, Koin e Navigation.
  - Impostare il file delle stringhe centralizzato (`strings.xml`) con le stringhe di base ("Caricamento", "Errore", "Benvenuto").
  - *Test:* Compilare ed eseguire l'app su Android e iOS mostrando una schermata di test con una stringa localizzata.
- [ ] **Task 1.2: Struttura Base MVVM & DI**
  - Creare la struttura delle cartelle per separazione logica (`data`, `domain`, `presentation`, `di`).
  - Impostare Koin e definire i primi moduli.
  - Creare un BaseViewModel multipiattaforma.
  - *Test:* Creare una UI fittizia collegata a un ViewModel che recupera una stringa mock da un UseCase e la mostra (Validazione flusso completo MVVM).
- [ ] **Task 1.3: Download e Gestione Modelli AI**
  - Implementare un repository (Data Layer) per il download asincrono dei file GGUF.
  - Gestire il salvataggio sicuro e lo storage nel file system interno.
  - Esibire il progresso del download tramite ViewModel (StateFlow) per la UI.
  - *Test:* Scaricare un file fittizio, salvarlo in memoria e mostrarne la progressione percentuale in UI.
- [ ] **Task 1.4: Integrazione Core `llama.cpp`**
  - Integrare le librerie C++ di `llama.cpp` via JNI (Android) e CInterop (iOS).
  - Creare le interfacce Kotlin (Repository) per inviare prompt e ricevere risposte.
  - *Test:* Eseguire un modello LLM quantizzato molto leggero e ottenere una generazione testuale di base (verificando il consumo RAM).
- [ ] **Task 1.5: UI Base (Dashboard & Impostazioni)**
  - Configurare il sistema di navigazione e i temi (Light/Dark mode) usando variabili semantiche.
  - Creare la schermata Dashboard principale.
  - Creare la schermata Impostazioni per il download dei modelli e la gestione della lingua.
  - *Test:* Verificare il flusso di navigazione e l'adattamento corretto ai temi di sistema.
