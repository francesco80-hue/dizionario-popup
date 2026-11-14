# Dizionario Popup

App Android offline per consultare definizioni mentre si legge in JW Library.

## Funzionamento

- Seleziona e copia una parola in JW Library.
- Tocca il pulsante mobile D.
- Si apre una scheda con definizione, sinonimi ed esempi disponibili.
- Il pulsante mobile richiede il permesso Android "Mostra sopra altre app" e una notifica di servizio.
- L'app non usa Accessibilità, non legge lo schermo e non richiede connessione Internet durante la consultazione.

## Dizionario

Durante ogni compilazione GitHub Actions scarica il dataset italiano Wiktionary da Kaikki.org e genera un archivio compatto incorporato nell'APK. Le voci includono definizioni, sinonimi, esempi e forme flesse quando disponibili nella fonte. I contenuti non presenti nella fonte non vengono inventati.

Sei voci curate manualmente sono conservate in dictionary-curated.json e hanno precedenza sulle voci importate.

Fonti e licenze: vedere [DICTIONARY-LICENSE.md](DICTIONARY-LICENSE.md).

## Compilazione

Aprire Actions nel repository e scaricare l'artifact DizionarioPopup-debug-apk dell'ultima esecuzione completata con successo.

## Versione 0.3.0

La compilazione produce un APK di prova e, quando sono configurati i segreti di firma GitHub Actions, anche un APK release firmato per gli aggiornamenti.


## Evoluzione concordata del progetto

### Popup e approfondimenti online
- Mantenere in primo piano definizione, sinonimi ed esempi del dizionario offline.
- Sostituire la sezione enciclopedica con una sezione discreta e inizialmente chiusa chiamata "Approfondisci".
- Prevedere collegamenti esterni a Google (ricerca già compilata con la parola), Wikipedia e altre fonti linguistiche affidabili.
- I collegamenti si aprono solo su richiesta dell'utente e richiedono una connessione Internet.
- Non inserire collegamenti a JW.org o alla Biblioteca Online.
- Nessuna pubblicità invasiva e nessuna apertura automatica del browser.

### Aggiornamenti separati
- Separare gli aggiornamenti dell'app dagli aggiornamenti del database del dizionario.
- Prevedere in futuro il controllo periodico della disponibilità di un nuovo database, indicativamente ogni 30 giorni, quando il dispositivo è online.
- Offrire anche un comando manuale "Controlla aggiornamenti" e mostrare versione e data dell'ultimo aggiornamento.
- Scaricare il solo database aggiornato, senza reinstallare l'app, quando tecnicamente possibile.
- Consentire, se desiderato, di limitare il download al Wi-Fi.
- Conservare il database precedente se il download o l'installazione del nuovo archivio fallisce.
- Il dizionario già installato deve continuare a funzionare offline.

### Distribuzione e sostenibilità
- Obiettivo: app gratuita, senza account e senza abbonamenti.
- Valutare requisiti, costi e procedure per la pubblicazione su Google Play.
- Valutare in futuro un collegamento discreto al canale Telegram dell'autore, separato dalle funzioni di consultazione e senza interrompere la lettura.
- Prima della pubblicazione, verificare licenze e attribuzioni delle fonti del dizionario e dei contenuti aggiornabili.

### Identità grafica
- L'icona definitiva è ancora da scegliere.
- Evitare simboli generici troppo simili alle icone di dizionari già esistenti.
- Cercare un'identità originale, riconoscibile anche in piccolo e coerente con il pulsante flottante rosso, senza considerare ancora approvata una proposta.
