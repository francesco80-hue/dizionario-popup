# Dizionario Popup

Prototipo Android in Kotlin e Jetpack Compose.

- Riceve il testo selezionato tramite ACTION_PROCESS_TEXT.
- Ricerca locale, senza Internet.
- Non richiede Accessibilità, overlay, account o pubblicità.
- Include sei voci dimostrative: non è ancora il dizionario completo.

## Compilazione
Aprire il progetto con Android Studio, JDK 17 e Android SDK 35. Il workflow GitHub Actions compila automaticamente l'APK debug; aprire Actions e scaricare l'artifact DizionarioPopup-debug-apk.

## Test
Installare l'APK, selezionare una parola in un'app compatibile e cercare Dizionario Popup nel menu di condivisione/azioni. La compatibilità con JW Library e HyperOS va verificata sul tablet.

## Dizionario esteso
Il dataset completo non è incluso. È possibile scaricare i dati italiani da https://kaikki.org/itwiktionary/rawdata.html e convertirli con tools/convert_wiktextract.py. Verificare e rispettare licenze e attribuzioni CC BY-SA/GFDL prima di redistribuire i dati.
