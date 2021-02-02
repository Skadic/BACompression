# Off-line compression by greedy textual substitution

## Einleitung

### Datenkompression durch Textersetzung

- Mehrfach auftretende Substrings werden durch Menge von Pointern auf eine einzigartige Kopie ersetzt
  - z.B. Pointer bestehend aus Textposition und Länge des Substrings
- Relative Performance solcher Methoden hängt von vielen Faktoren ab z.B.
  - Pointerlänge
  - Parameter des Wörterbuchs
    - Anzahl Einträge
    - Durchschnittliche Länge der Einträge
- Optimale Implementierung solcher Methoden oft NP-vollständig
  - Ausnahme: LZ-Faktorisierung

### Dieses Paper

- Konzentriert sich auf die Implementierung ungefährer Off-Line Methoden
- Wiederholte Identifizierung eines bestimmten Substrings in der jetzigen Version des Texts
  - Ersetzen aller dieser Substrings (außer Einem) durch Pointer

## Substring Statistik ohne Überlappung

- Funktion $f_w$, die für einen Substring $w$ die Anzahl nichtüberlappender Vorkommen beschreibt
- Ist $f_w$ bekannt, kann die maximale Kontraktion aus $f_w$ und $|w|$ berechnet werden.
- Hier ist Suffixbaum (und damit auch Suffix Array nutzbar)
  - Datenstruktur ist vonnöten, die nicht-überlappende Vorkommen effizient ermitteln kann

## Implementierung der Datenstrukturen

Sei $x$ der String.

- Minimaler Augmentierter Suffix-Baum
  - Blätter enthalten Anfang des Suffix in $x$
  - Knoten $\langle w \rangle$, der Knoten in dem der Substring $w$ im Baum endet
    - 2 Indizes [i, j], so dass $w = x[i, j]$
    - 1 Pointer zur Liste von Kindknoten und Einen zur Liste von Geschwisterknoten
    - 1 Counter, der die Anzahl nicht-überlappender Vorkommen von $w$ in $x$ beschreibt
  
Die Datenstruktur, die $x$ beinhaltet sollte z.B. effiziente String-Suchen und wiederholte Substring-Löschungen erlauben. Deswegen wird diese als Linked-List dynamischer Arrays implementiert:

- Zu Anfang: Ein Array der Länge $n := |x|$.
- Entfernung eines Substring $w$ partitioniert das Array in verkettete Teile.
  - In Refresh-Cycles werden diese verketteten Teile zu einem Array zusammengesetzt.
- Wenn die Datenstruktur jedes mal neu konstruiert werden würde: sehr ineffizient
  - Dynamische Datenstruktur wäre gut
  - Hier wird der Baum aber immer neu konstruiert (aber mit Heuristiken)
  
## Wählen und Berechnen einer Gewinnfunktion

- Gewinnfunktion: Funktion $G$, die die Wahl des zu ersetzenden Substrings $w$ steuert
Sei $w$ der Substring der $G$ maximiert.

### 3 Mögliche $G$-Funktionen:

Sei $l(i) = \lceil \log i \rceil$ die Anzahl Bits, die zum Speichern von $i$ nötig sind.

#### Methode 1

Alle $f_w$ Vorkommen von $w$ werden aus dem Text entfernt. $w$ wird dabei in einer zusätzlichen Datenstruktur gespeichert.  
Diese enthält:
- den String $w$, der $Bm_w$ Bits lang ist, wobei $m_w = |w|$ und $B = \log |\Sigma|$
- die Länge $m_w$ von $w$, mit Kosten von $l(m_w)$ Bits
- den Wert von $f_w$, mit Kosten von $l(f_w)$ Bits
- die $f_w$ Positionen von $w$ in x, mit globalen Kosten maximal $f_w l(n)$

Es folgt also, dass die Vorkommen von $w$ in $x$ ursprünglich $Bf_wm_w$ Bits Speicher eingenommen haben.  
Durch die zusätzliche Datenstruktur belegen die Informationen über die ersetzten Vorkommen dann $Bm_w + l(m_w) + l(f_w) + f_w l(n)$ Bits. Demnach ist die Differenz:

$$
G_1(w) = B f_w m_w - Bm_w - l(m_w) - l(f_w) - f_w l(n)\\
= (f_w - 1) Bm_w - l(m_w) - l(f_w) - f_w l(n)
$$

#### Methode 2

Eine der $f_w$ Kopien von $w$ bleibt im Originaltext, markiert von einem "Literal-Identifikationsbit", während die restlichen $f_w - 1$ Kopien durch Pointer kodiert werden.  
Jedem Pointer geht ein entsprechendes Identifikationsbit vorher.

Wegen dieses extra-Bit nimmt die Klartext Repräsentation von $w$ $(B+1)f_wm_w$ Bits ein.  
Die Kosten der Pointer-basierten Repräsentation sind folgende:

- $(B + 1)m_w$ Bits für die Originalkopie von $w$
- $(f_w - 1) (l(n) + l(m_w) + 1)$ Bits für die $f_w - 1$ Pointer

Die Differenz für dieses Schema ergibt dann:

$$
G_2(w) = (B + 1) f_w m_w - (B + 1) m_w - (f_w - 1) \cdot (l(n) + l(m_w) + 1)\\
= (f_w - 1)((B + 1) m_w - l(n) - l(m_w) - 1)
$$


#### Methode 3

Substrings in $x$ werden durch Pointer in ein Wörterbuch ersetzt. Nachdem also ein Substring $w$ ausgewählt wird, wird dieser als neuer Eintrag in das Wörterbuch eingefügt.  
In diesem Fall wird ein zusätzlicher Bit-Vektor benötigt um zwischen Pointern und Literalen zu unterscheiden, sowohl im Text, also auch im Wörterbuch.  
Falls aber Pointer-Rekursion verboten ist, ist dieser Bit-Vektor für das Wöterbuch nicht nötig.

Die Kosten der Pointer-gestützten Repräsentation von $w$ ist dann:
- $Bm_w$ Bits um $w$ im Wörterbuch zu speichern
- $l(m_w)$ um die Länge $m_w$ zu speichern
- $l(d) f_w$ für die $f_w$ Pointer im tex, wobei $d$ die Größe des Wörterbuchs ist.
  
Daraus folgt:
$$
G_3(w) = (B + 1)f_w m_w - Bm_w - (l(d) + 1) f_w - l(m_w)\\
= B(f_w - 1) m_w + f_w m_w - (l(d) + 1) f_w - l(m_w)
$$

---

Jede dieser $G$ ist eine isotone Funktion in $m_w$. Da die maximale Anzahl nicht-überlappender Vorkommen von $w$ in $x$ sich nicht in der Mitte einer Kante im Suffix-Baum ändert, endet das Wort, dass $G$ maximiert, *immer* in einem Knoten im Baum. 

## Ergebnisse

Der Meistens beste Algorithmus der Dreien war Off-Line 3, gefolgt von 1 und mit etwas Abstand 2. Diese Algorithmen können mit GZip mithalten und Outperformen GZip auch zum Teil.

Während BZip2 auf dem Calgary Corpus bessere Kompression bietet, ist Off-line 3 besser auf Gensequenzen. 

Allgemein wird die relative Kompressionsrate von Off-line3 besser, je größer der Input ist.

## Mögliche Verbesserungen

Der zeitraubendste Teil des Algorithmus, ist das wiederholte Konstruieren des Suffixbaumes nach jedem Auswählen eines Wortes. Eine Möglichkeit ist, pro Konstruktion des Baumes mehrere Substitutionen durchzuführen.  
Dazu kann eine Prioritätswarteschlange mit $Q$ Elementen genutzt werden um die vielversprechendsten Substrings zu speichern und nacheinander abzuarbeiten, bevor ein neuer Baum erstellt wird. Dabei kann es natürlich vorkommen, dass ein Substring nicht mehr zu finden ist.

Wenn es unwahrscheinlich ist, dass es sehr lange sich wiederholende Substrings gibt, dann kann der Baum auch nur bis zu einer gewissen Tiefe erstellt werden.

