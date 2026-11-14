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
