# Fase 2: Text & RAG (Retrieval-Augmented Generation)

## Obiettivo
Implementare il database locale per i dati strutturati (es. sistema di ripetizione spaziata, progressi utente) e il vector database per il salvataggio e la ricerca semantica (RAG) dei testi utente, sfruttando l'output strutturato in JSON generato dall'LLM.

## Task
- [ ] **Task 2.1: Setup Database Strutturato (SQLDelight)**
  - Configurare SQLDelight (licenza Apache 2.0).
  - Progettare e creare le tabelle SQL per Utenti, Sessioni di Studio, Mazzi di Carte e SRS (Spaced Repetition System).
  - Implementare Data Sources e Repository puliti per l'accesso ai dati.
  - *Test:* Eseguire operazioni CRUD su una flashcard e verificarne la persistenza locale.
- [ ] **Task 2.2: Setup Vector Database**
  - Valutare e integrare una libreria compatibile KMP e commercializzabile (es. `sqlite-vec` estensione SQLite, o ObjectBox se la licenza è in target).
  - *Test:* Inserire un vettore fittizio nel DB vettoriale ed eseguire una query di ricerca per "cosine similarity".
- [ ] **Task 2.3: Text Chunking e Generazione Embeddings**
  - Implementare Use Case per la suddivisione di testi lunghi in chunk intelligenti (basati su punteggiatura e lunghezza).
  - Sviluppare il layer per richiedere l'embedding del testo al modello on-device.
  - *Test:* Fornire in input un testo, osservare la divisione in chunk e il salvataggio degli embedding correlati nel Vector DB.
- [ ] **Task 2.4: Motore RAG e JSON Function Calling**
  - Sviluppare la logica RAG per recuperare i chunk più rilevanti a partire da una richiesta/query.
  - Scrivere System Prompt solidi per istruire l'LLM a rispondere ESCLUSIVAMENTE tramite stringhe JSON.
  - Implementare la decodifica del JSON (via `kotlinx.serialization`) per convertire l'output in data class Kotlin.
  - *Test:* Richiedere all'LLM di generare un quiz testuale a risposta multipla su un testo fornito; verificare il parsing automatico del JSON.
- [ ] **Task 2.5: UI Gestione Testi e Visualizzazione Quiz**
  - Creare la UI per permettere all'utente di incollare blocchi di testo o caricare documenti PDF base.
  - Implementare la UI dinamica del Quiz renderizzata partendo dall'oggetto Kotlin (decodificato dal JSON dell'LLM).
  - Tutte le stringhe e label fisse ("Avvia Quiz", "Carica", "Punteggio") in `strings.xml`.
  - *Test:* Flusso completo end-to-end utente: inserimento testo -> attesa elaborazione -> svolgimento quiz.
