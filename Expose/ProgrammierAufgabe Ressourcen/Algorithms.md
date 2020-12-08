Sei $SA$ das Suffix Array, $ISA$ das inverse Suffix Array und $LCP$ das LCP Array von einem String $S$.

Berechnung des LZ Faktors beginnend bei Index $k$ in $S$:

Sei $i = ISA[k]$, also der Suffix-Array-Index des Suffixes $S_k$, der bei Index $k$ im String anfängt.  

Von $i$ ausgehend, suche Richtung Anfang des Suffix-Arrays nach dem ersten Suffix $S_{psv}$ mit SA-Index $i_{psv}$ und Index in $S$ $psv = SA[i_{psv}]$, der in $S$ vor $S_k$ anfängt, also $SA[i_{psv}] < k$. Insbesondere ist $S_{psv}$ dann lexikographisch *kleiner* als $S_k$, da $S_{psv}$ vor $S_k$ im SA vorkommt.  
Von $i$ ausgehend, suche Richtung Ende des Suffix-Arrays nach dem ersten Suffix $S_{psv}$ mit SA-Index $i_{nsv}$ und Index in $S$ $nsv = SA[i_{nsv}]$, der in $S$ vor $S_k$ anfängt, also $SA[i_{nsv}] < k$. Insbesondere ist $S_{nsv}$ dann lexikographisch *größer* als $S_k$, da $S_{nsv}$ nach $S_k$ im SA vorkommt.  

$S_{psv}$ ist also nun der lexikographisch größte Suffix, der lexik. kleiner als $S_k$ ist und dabei vor $S_k$ in $S$ vorkommt.

Wenn man nun den längsten gemeinsamen Präfix (LCP) von jeweils $(S_k, S_{psv})$ und
$(S_k, S_{nsv})$ berechnet, wähle den Längeren als neuen LZ Faktor