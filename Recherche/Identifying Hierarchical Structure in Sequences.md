# Identifying Hierarchical Structure in Sequences: A linear-time algorithm

## Einleitung

### $\texttt{Sequitur}$ Algorithmus

- leitet eine hierarchische Struktur aus einem Text ab
- Substrings, die öfter als 1 mal vorkommen, werden von einer grammatischen Regel ersetzt, die diesen Substring generiert
- rekursive fortführung
- arbeitet inkrementell 

## Der $\texttt{Sequitur}$ Algorithmus

bildet basierend auf sich wiederholenden Substrings eine Grammatik. Jede Wiederholung führt zu einer neuen Regel der Grammatik. Der wiederholte Substring wird durch ein Nichtterminal ersetzt und komprimiert damit den Input.  
Als Nebenprodukt entsteht dabei einer hierarchische Struktur.

Die von $\texttt{Sequitur}$ erzeugten Grammatiken haben folgende Eigenschaften:

- Kein Paar nebeneinander stehender Symbole kommt öfter als einmal in der Grammatik vor. ($p_1:$ Digramm-Einzigartigkeit)
- Jede Produktionsregel der Grammatik wird mehr als einmal verwendet. ($p_2:$ Regel-Nützlichkeit)

Wird während der Konstruktion $p_1$ verletzt, so wird eine neue Regel erzeugt. Wenn $p_2$ verletzt wird, wird die nutzlose Regel gelöscht.

### Digramm Einzigartigkeit 

Wird ein neues Symbol gelesen, so wird es zur $S$ Regel hinzugefügt. Dieses Symbol formt mit seinem Vorgänger ein neues Digramm. Kommt dieses Digramm an anderer Stelle in der Grammatik vor, so ist $p_1$ verletzt. Es wird nun eine neue Regel erzeugt, die ein neues Nichtterminal auf dieses Digramm abbildet. Die beiden ursprünglichen Digramme, werden durch das neue Nichtterminal ersetzt.

### Regel-Nützlichkeit

Es kann sein, dass durch Substitutionen ein Nichtterminal nur einmal vorkommt. Dieses wird dann durch die rechte Seite ihrer Produktionsregel ersetzt. So können längere Regeln entstehen.

## Implementation

Der Algorithmus funktioniert durch das Aufrechterhalten von Digramm-Einzigartigkeit und Regel-Nützlichkeit. Verletzungen dieser Regel müssen effizient erkannt werden können.

Die optimale Datenstruktur, hängt davon ab, welche Operation auf die Grammatik ausgeführt werden können soll. Im Fall von $\texttt{Sequitur}$ sind das:

- ein Symbol an $S$ anzuhängen
- Eine existierende Regel zu benutzen
- Eine neue Regel zu schaffen
- Eine Regel zu löschen 

### Datenstruktur für Regeln und Index

Eine Art Doubly-Linked-List. Der Start und das Ende der Liste sind mit einer einzigen Guard Node verbunden. Jedes Nichtterminal zeigt auf den Startpunkt der Regel, die das Nichtterminal repräsentiert.
Jede Regel hat einen reference counter. Die Regel wird gelöscht, wenn die Regel nur noch eine Referenz besitzt.

Für Digramm-Einzigartigkeit wird eine Index-Struktur angewandt um nach Digrammen zu suchen. Diese soll in konstanter Laufzeit Digramme suchen können und effizient Einträge hinzufügen und löschen können. Hierzu wird eine Hashtabelle benutzt.





