# Fase 2: Text & RAG (Retrieval-Augmented Generation)

## Obiettivo
Implementare il database locale per i dati strutturati (es. sistema di ripetizione spaziata, progressi utente) e il vector database per il salvataggio e la ricerca semantica (RAG) dei testi utente, sfruttando l'output strutturato in JSON generato dall'LLM.

## Vincoli Mobile (Design Constraint)
- **Zero modelli aggiuntivi:** gli embedding vengono generati tramite `llama_get_embeddings()` dello stesso modello GGUF già scaricato nella Fase 1. Nessun download extra.
- **Storage overhead < 25MB** per utente pesante: vettori in SQLite (~1.5–4KB per chunk), dati strutturati (<5MB).
- **Context window limitato:** i modelli quantizzati piccoli hanno tipicamente 2048–4096 token disponibili. Il RAG deve restituire solo top-K chunk rilevanti (3–5), non tutti.

## Stack Tecnico
- **Database strutturato:** SQLDelight (Apache 2.0, KMP nativo)
- **Vector storage:** SQLite puro via SQLDelight — vettori serializzati come BLOB, cosine similarity calcolata in Kotlin/native
- **Embeddings:** `llama_get_embeddings()` su modello GGUF esistente (stesso processo di llama.cpp già integrato)
- **Serializzazione JSON:** `kotlinx.serialization`

## Task
- [x] **Task 2.1: Setup Database Strutturato (SQLDelight)**
  - Configurare SQLDelight (licenza Apache 2.0).
  - Progettare e creare le tabelle SQL per Utenti, Sessioni di Studio, Mazzi di Carte e SRS (Spaced Repetition System).
  - Implementare Data Sources e Repository puliti per l'accesso ai dati.
  - *Test:* Eseguire operazioni CRUD su una flashcard e verificarne la persistenza locale.

- [x] **Task 2.2: Setup Vector Storage su SQLite**
  - Aggiungere a SQLDelight una tabella `text_chunks` con colonne: `id`, `source_id`, `content` (testo originale del chunk), `embedding` (BLOB Float array serializzato), `created_at`.
  - Implementare `VectorRepository` con operazioni: `insertChunk`, `getAllChunks`, `deleteBySourceId`.
  - Implementare la funzione di **cosine similarity** in Kotlin puro (operazione vettoriale su FloatArray).
  - Implementare `searchSimilar(queryEmbedding, topK)` che carica i vettori, calcola la similarity in-memory e restituisce i top-K chunk.
  - *Test:* Inserire 10 chunk fittizi con vettori casuali, eseguire una query e verificare che i top-3 risultati abbiano similarity decrescente.

- [ ] **Task 2.3: Text Chunking e Generazione Embeddings**
  - Implementare `TextChunkingUseCase`: suddivisione di testi in chunk da ~200–300 token, rispettando confini di frase (split su `.`, `!`, `?`, `\n`).
  - Esporre `llama_get_embeddings()` nel bridge JNI/CInterop esistente (aggiunta minimale a `LlamaEngine`).
  - Implementare `GenerateEmbeddingUseCase` che richiede l'embedding di un testo al modello on-device tramite `LlamaEngine`.
  - *Test:* Fornire un testo di 3 paragrafi, verificare la divisione in chunk e il salvataggio degli embedding nel Vector Storage. Misurare il tempo di generazione embedding per chunk.

- [ ] **Task 2.4: Motore RAG e JSON Function Calling**
  - Implementare `RagQueryUseCase`: dato un testo di query, genera il suo embedding, recupera i top-K chunk da `VectorRepository`, li concatena come contesto.
  - Scrivere System Prompt solidi per istruire l'LLM a rispondere **esclusivamente tramite JSON** (senza markdown, senza testo libero fuori dal JSON).
  - Implementare la decodifica del JSON via `kotlinx.serialization` in data class Kotlin tipizzate.
  - Gestire il caso di JSON malformato con retry e fallback.
  - *Test:* Richiedere all'LLM di generare un quiz testuale a risposta multipla su un testo fornito; verificare il parsing automatico del JSON e che il contesto RAG sia contenuto nei token disponibili.

- [ ] **Task 2.5: UI Gestione Testi e Visualizzazione Quiz**
  - Creare la UI per permettere all'utente di incollare blocchi di testo o caricare documenti PDF base.
  - Implementare la UI dinamica del Quiz renderizzata partendo dall'oggetto Kotlin decodificato dal JSON dell'LLM.
  - Mostrare feedback visivo durante l'elaborazione (chunking + embedding, che può richiedere alcuni secondi per chunk).
  - Tutte le stringhe e label fisse ("Avvia Quiz", "Carica", "Punteggio", "Elaborazione in corso...") in `strings.xml`.
  - *Test:* Flusso completo end-to-end: inserimento testo → elaborazione chunk/embedding → svolgimento quiz → verifica persistenza punteggio in SQLDelight.
