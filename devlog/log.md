---
header-includes: |
    \usepackage{amsmath}
...

# AreaComp DevLog

## Naive Sequitur-inspirierte Implementation (V1)

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

Ist $len > 1$, so muss noch darauf geachtet werden, dass sich überlappende Vorkommen nicht gezählt werden. Das Entfernen überlappender Vorkommen wird hier durch Sortieren und Durchlaufen des Arrays bewältigt.

#### Faktorisierung

Ist das getan, so kann mit diesen Daten der Substring faktorisiert werden.  
Sei $x = x_0, \dots, x_{n - 1} \in (N \cup T)^*$ der die Regel beschreibende String mit $n := |x|$ und  
$s = s_0, \cdot, s_{k - 1} \in (N \cup T)^*$ der zu faktorisierende Substring mit $k := |s|$.

##### Erstellen der neuen Regel

Der Algorithmus läuft durch die verkettete Datenstruktur der Regel zum ersten Vorkommen von $s$ in $x$. Es wird ein neues Nichtterminal $Y \in N$ erstellt. Das Vorkommen von $s$ wird durch $Y$ ersetzt und eine neue Produktionsregel $Y \rightarrow s_0, \dots, s_{k-1}$ erstellt.  

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

Ein damit Verbundenes Problem ist, dass selbst Vorkommen, die mehrmals innerhalb einer Regel vorkommen, nur durch einen Scan der gesamten Grammatik entfernt werden können.

#### Laufzeit

Auch ein großes Problem ist die Laufzeit dieser Implementierung.

In jeder Iteration wird für jede Regel $X$ mit Länge $k := |X|$ ihre $k^2$ Teilintervalle berechnet und für jedes Dieser Intervalle $A$ berechnet. Da $A$ eine lineare Laufzeit in der Länge des Intervalls hat und die Intervalle $\mathcal{O}(k)$ Speicher benötigen, so hat das allein eine $\mathcal{O}(k^3)$ Laufzeit. Diese Laufzeit geschieht für jede Regel in jeder Iteration. Jede Regel hat im worst-case $\mathcal{O}(n)$ Speicherverbrauch.

Auf einem Text von nur 662 Zeichen, hatte dieser Algorithmus bereits eine Laufzeit von etwa 1.5 Sekunden.

##### Verbesserungen

Eine offensichtliche Verbesserung ist, die Prioritätswarteschlange durch eine normale Maximumsuche zu ersetzen, da hier nur eines der Intervalle aus der Warteschlange genutzt wird.
Hierdurch verbessert sich die Laufzeit schon auf etwa 700 Millisekunden.

Aber auch hier ist die Bestimmung des besten Intervalls noch das größte Bottleneck.

## Version 2

In dieser Version ist das Vorgehen etwas anders:

### Datenstrukturen

Das Konzept des V2 Algorithmus bringt einige Schwierigkeiten mit sich. Da das Suffix Array, LCP Array etc. nur einmal global für den gesamten Eingabestring berechnet wird, sind neue Datenstrukturen vonnöten.  
Diese müssen es erlauben, festzustellen, welche Bereiche des Eingabestrings von durch welche Regeln faktorisiert werden.  
Dazu dienen die Datenstrukturen $\texttt{ruleRanges}$ und $\texttt{ruleRangeStarts}$.

#### $\texttt{ruleRanges}$

$\texttt{ruleRanges}$ ist eine Liste von Integerlisten. Jede Regel hat eine einzigartige ID.
Die Liste an Index $i$ in $\texttt{ruleRanges}$ enthält die IDs aller Regeln, die "durchlaufen" werden müssen, um zu dem Zeichen $S[i]$ zu gelangen in dieser Reihenfolge.  
Beispielsweise, betrachte den String $aabaacaabaa$ und die zugehörige Grammatik:  
$R0 \rightarrow R2\ c\ R2$  
$R1 \rightarrow aa$  
$R2 \rightarrow R1\ b\ R1$

Um in der Grammatik zu dem Zeichen $S[3] = 'a'$ zu gelangen, muss von der Wurzel (der Startregel $R0$), die Regeln $R0 \rightarrow R2 \rightarrow R1$ durchlaufen werden, um zu diesem Zeichen zu gelangen. $\texttt{ruleRanges}[i]$ ist also folglich $[0, 2, 1]$. Die Datenstruktur würde dann für diesen String und diese Grammatik folgendermaßen Aussehen (die Listen sind hier senkrecht aufgeschrieben):

 a | a | b | a | a | c | a | a | b | a | a
---|---|---|---|---|---|---|---|---|---|---
 0 | 0 | 0 | 0 | 0 | 0 | 0 | 0 | 0 | 0 | 0
 2 | 2 | 2 | 2 | 2 |   | 2 | 2 | 2 | 2 | 2
 1 | 1 |   | 1 | 1 |   | 1 | 1 |   | 1 | 1

#### $\texttt{ruleRangeStarts}$

$\texttt{ruleRanges}$ erlaubt uns also für jeden Index, die am tiefsten verschachtelte Regel zu finden, die diesen Bereich einnimmt. Allerdings gibt es dabei ein Problem. Betrachtet man zum Beispiel für den String $aaaabaaaa$ die Grammatik:

$R0 \rightarrow R1\ R1\ b\ R1\ R1$  
$R1 \rightarrow aa$

Dann sieht die $\texttt{ruleRanges}$ Datenstruktur folgendermaßen aus:

 a | a | a | a | b | a | a | a | a
---|---|---|---|---|---|---|---|---
 0 | 0 | 0 | 0 | 0 | 0 | 0 | 0 | 0
 1 | 1 | 1 | 1 |   | 1 | 1 | 1 | 1

Wir können nicht feststellen, an welchen Positionen einige Bereiche Anfangen, beziehungsweise enden. Ist der Bereich $[0, 3]$ eine zusammenhängender Bereich, oder besteht dieser aus mehreren Bereichen?
Diese Datenstruktur könnte auch genauso die Grammatik

$R0 \rightarrow R1\ b\ R1$  
$R1 \rightarrow aaaa$

beschreiben.

Um dies eindeutig zu beschreiben kommt die $\texttt{ruleRangeStarts}$ Datenstruktur ins Spiel.
Diese ist eine Liste von Mengen. Die Menge an Index $i$ ist genau die Menge aller Regel-IDs,
sodass für jede dieser Regeln ein Bereich dieser Regel an diesem Index beginnt. Betrachten wir wieder die erste Grammatik, so sieht die Datenstruktur folgendermaßen aus:

 a | a | a | a | b | a | a | a | a
---|---|---|---|---|---|---|---|---
 0 |   | 1 |   |   | 1 |   | 1 |  
 1 |   |   |   |   |   |   |   |

Diese Mengen müssen dabei nicht sortiert sein. So kann eindeutig entschieden werden, an welchem Index, die Bereiche welcher Regeln beginnen (und enden).

### Vorgehen
#### Suffix Array, LCP Array und Prioritätswarteschlange

Anders als bei Version 1, wird hier nicht für jede Regel in jedem Durchlauf einzeln das Suffix-Array und LCP-Array, sowie die Prioritätswarteschlange berechnet. Stattdessen wird hier zu Anfang einmal das Suffix-Array, LCP-Array und die Prioritätswarteschlange über die Intervalle im LCP Array für den gesamten Eingabestring berechnet.

#### Bestimmung des Intervalls und Berechung der relevanten Daten

In jedem Durchlauf wird das Intervall des LCP Arrays mit dem besten Wert der Flächenfunktion aus der Prioritätswarteschlange entnommen.  
Mithilfe des Suffix Arrays werden dann die Positionen im Suffix Array berechnet, an denen das Pattern vorkommt.

Aus diesem Intervall wird auch die Länge des zu ersetzenden Pattern, durch eine Minimumsuche bestimmt.

Ist diese Länge $\leq 1$, wird der Algorithmus abgebrochen. Die Area-Funktion ist so geschrieben, dass sie $0$ zurückgibt, wenn die Länge $\leq 1$ ist. In diesem Fall ist die Länge also, $> 1$, wenn das Intervall zur Kompression brauchbar ist.

Die Positionen werden nun sortiert und nicht-überlappende Vorkommen, wie beim V1 Algorithmus, entfernt. 


