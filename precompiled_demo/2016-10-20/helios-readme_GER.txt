HELIOS Sandbox

"Mini-Handbuch"

Stand: 2016-07-06


1. Systemvoraussetzungen
========================

HELIOS Sandbox benötigt eine Java-Laufzeitumgebung (1.8 oder höher) und OpenGL-Unterstützung

2. Programmstart
================

Im HELIS-Ordner Kommandozeile öffnen und folgenden Befehl eingeben:

"java -jar helios.jar <survey-file>"

wobei <survey-file> ein (absoluter oder relativer) Pfad zu einer Survey-XML-Definition sein muss.

Einige im Paket enthaltene Demos:

data/surveys/tls_arbaro.xml        - TLS-Demo mit einem Arbaro-Baum
data/surveys/tls_outcrop.xml       - TLS-Demo mit einer Felswand
data/surveys/uav_terrain2.xml      - Quadrocopter-Demo
data/surveys/als_terrain2.xml      - ALS-Demo (Cirrus SR-22)

Beispiel:

Der vollständige Starbefehl für die Outcrop-Demo ist

java -jar helios.jar data/surveys/tls_outcrop.xml


3. Steuerung der Kamera:
========================

3.1 Wechseln zwischen Kamera-Modi:

Taste <v> oder Klick auf entsrprechenden Button im Sidebar


3.2 Kamerasteuerung im Freiflug-Modus:

Bewegung:

links:  <a>
rechts: <d>
vor:    <w>
zurück: <s>

Drehen: Maustaste gedrückt halten + Maus bewegen


3.3 Kamerasteuerung im Verfolgungsmodus:
 
Näher an den Scanner:   Mausrad nach vorne
Weiter weg vom Scanner: Mausrad nach hinten

Drehen: Maustaste gedrückt halten + Maus bewegen


4. Verschieben einer Scanposition:
==================================

Zunächst Aktivierung des Verschiebemodus durch Klick auf "Waypoint -> Move".

Nun kann die Scanposition mit den Pfeiltasten der Tastatur bewegt werden.

Automatische Anpassung der Höhe auf das Bodenniveau kann per Klick auf
"Auto-Ground" ein- und ausgeschaltet werden


5. Einstellung des Scannfelds (Start- und Endwinkel)
====================================================

Zunächst Aktiverung der Scanfeld-Bearbeitung durch Klick auf
"Scan Settings -> Edit Scan Field".

Nun sind folgende Tasten aktiv:

<Pfeil nach oben>   - Vergrößerung des Scanfelds
<Pfeil nach hinten> - Verkleinerung des Scanfelds
<Pfeil nach links>  - Drehen des Scanfelds nach links
<Pfeil nach rechts> - Drehen des Scanfelds nach rechts


ANMERKUNG:

Die Usability der Scanfeld-Bearbeitung ist noch nicht optimal. Die
visuelle Rückmeldung ist uneindeutig, wodurch manchmal unerwartetes,
scheinbar falsches Verhalten auftritt (Scanner dreht sich weiter,
obwohl scheinbar der Stop-Scanwinkel überschritten wurde). Ich habe
Ideen, wie man das verbessern könnte. Falls ich es noch schaffe, bau
ich das bis zur Konferenz noch ein.


6. Sonstiges
============
Alle weiteren Funktionen können über die Sidebar bedient werden und
sollten "selbsterklärend" sein ;).

Bei Fragen bitte einfach melden!

Viel Spaß!
