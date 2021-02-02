# AreaComp DevLog

## Naive Sequitur-inspirierte Implementation

- Datenstruktur ähnlich wie bei Sequitur
  - Doppelt verkettete Ringstruktur mit einem speziellen Guard Symbol
    - Kette aus Nichtterminalen und Terminalen, die die Sequenz von Symbolen beschreibt
    - Jedes Nichtterminal hat Referenz auf eine Produktionsregel, die dieses Nichtterminal erzeugt
  - Regel besteht wiederum aus solch einer Datenstruktur

## Vorgehen

Für einen String $s$ mit $n := |s|$ wird eine Regel $S \rightarrow s_0 \dots s_{n-1}$ erstellt und in eine Regelmenge eingefügt.  
Folgendes wird nun wiederholt, bis sich keine Regel in der Regelmenge mehr verändert:

### Area Funktion

Für jede Regel $X \rightarrow x_0 \dots x_{k - 1}$ berechne Suffix Array $SA$ und zugehöriges LCP Array $LCP$. Sei $A$ eine Funktion, die für ein Intervall $[i, j],\ 1\leq i \leq j < k$ im LCP Array eine "Fläche" $A[i, j]$ berechnet. Ein Intervall im LCP Array beschreibt die Menge gemeinsamer Präfixe, der Suffixe in $SA$.  
Der Wert $A[i, j]$ soll nun ein ungefähres Maß dafür sein, wie groß der Nutzen ist, diese Präfixe als Regel zusammenzufassen.  
In diesem Fall ist 
$$
A[i, j] = \begin{cases}
    \min \{ lcp[x]\ |\ i \leq x \leq j\} \cdot (j - i), &\text{falls }  \min \{ lcp[x]\ |\ i \leq x \leq j\} > 1\\
    0 & \text{sonst}
\end{cases}
$$
gewählt. So wird die Länge des ersetzten Substring und die Anzahl der Vorkommen in Betracht gezogen. Außerdem werden auch unnütze $1$-lange Substrings ignoriert.

### Wahl des LCP-Intervalls

Der Algorithmus erstellt nun eine Prioritätswarteschlange und fügt alle Möglichen Teilintervalle von $[1, k - 1]$ ein (wichtig: $0$ ausgeschlossen, da $LCP[0]$ undefiniert ist). Diese sind dabei nach ihrem $A[i, j]$ Wert geordnet.

Entnehme nun das beste Intervall $[a, b]$

### Berechnung der Substring-Indizes und Substring Länge

 und berechne den Index aller Vorkommen des zu ersetzenden Substrings. Das ist einfach $pos = \{SA[i]\ |\ i \in [a - 1, b]\}$. Und die Länge des zu ersetzenden Substrings ist $len = \min \{ lcp[x]\ |\ a \leq x \leq b\}$.

Falls $len \leq 1$ ist, so wird diese Regel nicht weiter beachtet und das Verfahren für die nächste Regel von neuem begonnen, denn aufgrund der Wahl von $A$ bedeutet dies, dass kein Substring mit $len > 1$ gefunden wurde.

Ist $len > 1$, so muss noch darauf geachtet werden, dass sich überlappende Vorkommen nicht gezählt werden. Das Entfernen überlappender Vorkommen wird hier mit einem Linearzeitalgorithmus bewältigt.

### Faktorisierung

Ist das getan, so kann mit diesen Daten der Substring faktorisiert werden.
