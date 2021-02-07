---
header-includes: |
    \usepackage{amsmath}
...

# AreaComp DevLog

## Naive Sequitur-inspirierte Implementation

- Datenstruktur ähnlich wie bei Sequitur
  - Doppelt verkettete Ringstruktur mit einem speziellen Guard Symbol
    - Kette aus Nichtterminalen und Terminalen, die die Sequenz von Symbolen beschreibt
    - Jedes Nichtterminal hat Referenz auf eine Produktionsregel, die dieses Nichtterminal erzeugt
  - Regel besteht wiederum aus solch einer Datenstruktur

Im Folgende ist $G = (N,T,P,S)$ die Grammatik, mit Nichtterminalen $N$, Terminalen $T$, Produktionsregeln $P$ und Startsymbol $S \in N$.

### Vorgehen

Für einen String $s$ mit $n := |s|$ wird eine Regel $S \rightarrow s_0 \dots s_{n-1}$ erstellt und in $P$ eingefügt.  
Folgendes wird nun wiederholt, bis sich keine Regel in der Regelmenge mehr verändert:

#### Area Funktion

Für jede Regel $X \rightarrow x_0 \dots x_{k - 1} \in P$ berechne Suffix Array $SA$ und zugehöriges LCP Array $LCP$ über $x_0 \dots x_{k - 1} \in (N \cup T)^*$. Sei $A$ eine Funktion, die für ein Intervall $[i, j],\ 1\leq i \leq j < k$ im LCP Array eine "Fläche" $A[i, j]$ berechnet. Ein Intervall im LCP Array beschreibt die Menge gemeinsamer Präfixe, der Suffixe in $SA$.  
Der Wert $A[i, j]$ soll nun ein ungefähres Maß dafür sein, wie groß der Nutzen ist, diese Präfixe als Regel zusammenzufassen.  
In diesem Fall ist 
$$
A[i, j] = \begin{cases}
    \min \{ lcp[x]\ |\ i \leq x \leq j\} \cdot (j - i), &\text{falls }  \min \{ lcp[x]\ |\ i \leq x \leq j\} > 1\\
    0 & \text{sonst}
\end{cases}
$$
gewählt. So wird die Länge des ersetzten Substring und die Anzahl der Vorkommen in Betracht gezogen. Außerdem werden auch unnütze $1$-lange Substrings ignoriert.

#### Wahl des LCP-Intervalls

Der Algorithmus erstellt nun eine Prioritätswarteschlange und fügt alle Möglichen Teilintervalle von $[1, k - 1]$ ein (wichtig: $0$ ausgeschlossen, da $LCP[0]$ undefiniert ist). Diese sind dabei nach ihrem $A[i, j]$ Wert geordnet.

Entnehme nun das beste Intervall $[a, b]$

#### Berechnung der Substring-Indizes und Substring Länge

 und berechne den Index aller Vorkommen des zu ersetzenden Substrings. Das ist einfach $pos = \{SA[i]\ |\ i \in [a - 1, b]\}$. Und die Länge des zu ersetzenden Substrings ist $len = \min \{ lcp[x]\ |\ a \leq x \leq b\}$.

Falls $len \leq 1$ ist, so wird diese Regel nicht weiter beachtet und das Verfahren für die nächste Regel von neuem begonnen, denn aufgrund der Wahl von $A$ bedeutet dies, dass kein Substring mit $len > 1$ gefunden wurde.

Ist $len > 1$, so muss noch darauf geachtet werden, dass sich überlappende Vorkommen nicht gezählt werden. Das Entfernen überlappender Vorkommen wird hier mit einem Linearzeitalgorithmus bewältigt.

Zuletzt werden dann die Indizes aufsteigend sortiert.

#### Faktorisierung

Ist das getan, so kann mit diesen Daten der Substring faktorisiert werden.  
Sei $x = x_0, \dots, x_{n - 1} \in (N \cup T)^*$ der die Regel beschreibende String mit $n := |x|$ und  
$s = s_0, \cdot, s_{k - 1} \in (N \cup T)^*$ der zu faktorisierende Substring mit $k := |s|$.

##### Erstellen der neuen Regel

Der Algorithmus läuft durch die verkettete Datenstruktur der Regel zum ersten Vorkommen von $w$ in $s$. Es wird ein neues Nichtterminal $Y \in N$ erstellt. Das Vorkommen von $w$ wird durch $Y$ ersetzt und eine neue Produktionsregel $Y \rightarrow w_0, \dots, w_{k-1}$ erstellt.  

##### Ersetzen der Vorkommen

Es werden nun der Reihe nach, alle Vorkommen, von $w$ in $x$ durch $Y$ ersetzt.
Dann werden die rechten Seiten aller Regeln in $P$ nach Vorkommen von $w$ durchsucht und diese ebenfalls durch $Y$ ersetzt.

Zuletzt wird $Y \rightarrow w_0, \dots, w_{k-1}$ in $P$ eingefügt.

### Probleme

#### Mehrfach vorkommende Substrings in verschiedenen Regeln nicht entdecht

Angenommen sei die folgende Grammatik:


$$
    A -> aabB
    B -> aac
$$

Der Algoritmus würde die Wiederholung von $aa$ nicht erfassen, da diese Wiederholung regelübergreifend ist.  
Das liegt daran, dass diese Version des Algorithmus jede Regel einzeln als Zeichenfolge interpretiert und nur Wiederholungen innerhalb der Regel sucht.

#### Laufzeit

Auch ein großes Problem ist die Laufzeit dieser Implementierung.

In jeder Iteration wird für jede Regel $X$ mit Länge $k := |X|$ ihre $k^2$ Teilintervalle berechnet und für jedes Dieser Intervalle $A$ berechnet. Da $A$ eine lineare Laufzeit in der Länge des Intervalls hat und die Intervalle $\mathcal{O}(k)$ Speicher benötigen, so hat das allein eine $\mathcal{O}(k^3)$ Laufzeit. Diese Laufzeit geschieht für jede Regel in jeder Iteration. Jede Regel hat im worst-case $\mathcal{O}(n)$ Speicherverbrauch.

