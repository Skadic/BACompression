---
header-includes: 
...

# Off-Line Dictionary-Based Compression

## Introduction

Usually dictionary-based compressors operate on-line, incrementalliy inferring its dictionary of available phrases from previous parts of the message.  
In that case, the dictionary can be transmitted implicitly.

Meistens arbeiten Wörterbuch-basierte Kompressoren on-line und leiten ihr Wörterbuch imkrementell aus bereits vorhandenen Substrings der früheren Teile der Nachricht ab.
In diesem fall kann das Wörterbuch implizit übertragen werden.

Alternativer Ansatz: off-line.  
Dieser Ansatz nutzt die gesamte Nachricht (oder einen großen Teil) um ein gesamtes Wörterbuch abzuleiten. Der Vorteil dabei ist dabei, dass mit Zugang zu der ganzen Nachricht es möglich sein sollte die zu ersetzenden Substrings so zu wählen, dass die maximale Kompression möglich ist.

## Re-Pair

Der Re-Pair Algorithmus wählt wiederholt, das am häufigsten auftretende Symbolpaar in der Eingabe und ersetzt dieses durch eine neue Regel.

## Implementation

$\mathcal{O}(n)$ Zeit und Platz.  

### Datenstrukturen

3 Datenstrukturen:

- Ein Array, das Symbolnummern speichert. Zu Anfang sind das die Zeichen der Eingabenachricht. Jeder Eintrag im Array enthält 3 Teile. Einer ist die Symbolnummer selbst, und die anderen zwei sind Pointer.
- Ein Hash-Table mit einem Eintrag für jedes aktive Paar. Ein aktives Paar ist ein Symbolpaar, das zur Ersetzung in Betracht gezogen wird, sowie ein Pointer auf das erste Vorkommen dieses Paars im Symbol-Array
- Eine besondere Prioritätswarteschlange, implementiert als ein Array von ungefähr $\sqrt{n}$ Linked-Lists, die die Anzahl aktiver Paare festhält, die weniger als diese Zahl oft vorkommen.


