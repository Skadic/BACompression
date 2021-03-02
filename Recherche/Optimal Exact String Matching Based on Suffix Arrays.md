---
header-includes:
    - \usepackage{amsmath}
...


# Optimal Exact String Matching Based on Suffix Arrays

String $S$ mit Länge $|S| = n$ auf geordnetem Alphabet $\Sigma$ konstanter größe und $n < 2^{32}$.  
Sei $\$ \in \Sigma$, das größer ist, als alle anderen Elemente in $\Sigma$ aber $\$ \notin \Sigma$. $S[i]$ ist das Zeichen an Stelle $i$ mit $0 \leq i < n$. Für $i \leq j$ ist $S[i..j]$ der Substring, der aus den Zeichen an den Stellen $i$ bis $j$ besteht.

Suffix Array $\texttt{suftab}$

LCP Array $\texttt{lcptab}$ mit $\texttt{lcptab}[0] := 0$

## LCP Intervalle

### Definition LCP Intervall

Ein Intervall $[i..j]$, $0 \leq i < j \leq n$ heißt LCP-Intervall mit LCP-Wert $l$ wenn gilt:

- $\texttt{lcptab}[i] < l$
- $\texttt{lcptab}[k] \geq l, \text{ für alle } k \in [i+1..j]$
- $\texttt{lcptab}[k] = l \text{ für mindestens ein } k \in [i+1..j]$
- $\texttt{lcptab}[j + 1] < l$

Ein LCP-Intervall $[i..j]$ mit LCP-Wert $l$ heißt auch $l$-Intervall oder $l$-$[i..j]$.
Jeder Index $i + 1 \leq k \leq j$ mit $\texttt{lcptab}[k] = l$ heißt $l$-Index. Die Menge aller $l$-Indizes dieses Intervalls ist $lIndices(i, j)$.

## Child Table

Die Hierarchie der LCP Intervalle wird durch einen LCP-Intervallbaum dargestellt.
Dieser Baum wird nicht direkt implementiert, sondern durch ein weiteres Array dargestellt, der Child-Table $\texttt{cldtab}$.  
Das Array besitzt $n + 1$ Einträge an Indizes von $0$ bis $n$ (inklusive).  
Jeder Eintrag in diesem Array hat 3 Komponenten. $\textit{up}$, $\textit{down}$ und $\textit{nextlIndex}$ und sie sind folgendermaßen definiert:

- $\texttt{cldtab}[i].up = \min\{q \in [0..i-1]\ |\ \texttt{lcptab}[q] > \texttt{lcptab}[i] \text{ and } \forall k \in [q+1..i-1] : \texttt{lcptab}[k] \geq \texttt{lcptab}[q]\}$
- $\texttt{cldtab}[i].down = \max\{q \in [i+1..n]\ |\ \texttt{lcptab}[q] > \texttt{lcptab}[i] \text{ and } \forall k \in [i+1..q-1] : \texttt{lcptab}[k] > \texttt{lcptab}[q]\}$
- $\texttt{cldtab}[i].nextlIndex = \min\{q \in [i+1..n]\ |\ \texttt{lcptab}[q] = \texttt{lcptab}[i] \text{ and } \forall k \in [i+1..q-1] : \texttt{lcptab}[k] > \texttt{lcptab}[i]\}$

$up$ ist also der kleinste Index $q < i$, dessen LCP Wert größer ist als der von $i$ und für alle Indizes $k$ zwischen $q$ und $i$ (beides exklusive) der LCP Wert mindestestens so groß wie der an Index $q$ ist. $q$ ist also der erste $l$-Index des eingebetteten LCP Intervalls, das am Index $i-1$ endet.

$down$ ist der größte Index $q > i$, für den der LCP Wert größer ist als der bei Index $i$ und für alle Indizes $k$ zwischen $i$ und $q$ (beides exklusive) der LCP Wert größer als der bei $q$ ist. $q$ ist also der erste $l$-Index des eigebetteten LCP Intervalls, das am Index $i+1$ beginnt.

$nextlIndex$ ist der kleinste Index $q > i$, so dass die LCP Werte von $q$ und $i$ gleich sind, und alle Indizes dazwischen, einen größeren LCP Wert haben. $q$ ist dann also der nächstgrößere Index, mit selbem Wert in demselben LCP Intervall.  

### Konstrkuktion

Zwei Algorithmen: Einer für $up$ und $down$, und einer für $nextlIndex$.
