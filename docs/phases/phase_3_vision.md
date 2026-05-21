# Fase 3: Multimodality - Vision

## Obiettivo
Aggiungere il supporto multimodale per permettere all'app di "leggere" e interpretare i materiali cartacei (o immagini digitali) forniti dall'utente, estraendone testo e concetti senza l'uso di OCR tradizionali esterni.

## Task
- [x] **Task 3.1: Integrazione Permessi e Media Picker**
  - Integrare un modulo per la gestione dei permessi Fotocamera/Galleria multipiattaforma (gestione chiara ed elegante per l'utente, no prompt aggressivi).
  - Implementare il selettore immagine nativo (KMP Camera/Gallery picker).
  - *Test:* Apertura fotocamera o galleria, selezione immagine e caricamento in memoria della Bitmap/ByteArray.
- [x] **Task 3.2: Pre-processamento Immagini**
  - Sviluppare Use Cases dedicati per il ridimensionamento, taglio e conversione formato dell'immagine (per rispettare i limiti di memoria del modello AI mobile).
  - Spostare rigorosamente questa logica pesante fuori dal Main Thread (utilizzo di Coroutine Dispatchers).
  - *Test:* Validazione che un'immagine da 10MB venga compressa/formattata ai requisiti ottimali senza bloccare l'interfaccia utente.
- [x] **Task 3.3: Connessione Vision-to-Text**
  - Estendere il bridge `llama.cpp` per supportare il passaggio di array di pixel all'interno del context del modello.
  - Elaborare i System Prompt multimodali per estrazione OCR ed entità ("Estrai tutto il testo e restituisci la traduzione in JSON").
  - *Test:* Fornire un'immagine di una ricevuta o di una pagina di grammatica, ricevere l'output JSON decodificato correttamente in Kotlin.
- [x] **Task 3.4: UI Acquisizione Visiva**
  - Implementare la UI per lo scatto (Mirino, overlay).
  - Implementare schermate di loading animate e rassicuranti (poiché l'inferenza visiva on-device potrebbe impiegare svariati secondi).
  - Tutte le didascalie e aiuti visivi tradotti tramite file strings.
  - *Test:* Validazione UX del flusso utente: Scatta -> Elabora (Loader) -> Mostra Risultato in MVVM.

