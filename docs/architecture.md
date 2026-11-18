# NNLS Architecture & High-Level Class Contracts

![Class diagram](class-diagram.puml)

## Matrice des Composants & Responsabilités

| Package | Classe | Rôle & Responsabilité principale |
| :--- | :--- | :--- |
| **`common`** | **`NetworkDelta`** | **DTO Réseau** : Record immuable modélisant une opération de diff élémentaire (`INSERT`, `DELETE`, `CHANGE`) selon `PROTOCOL.md`. |
| **`common`** | **`DiffEngine`** | **Moteur de Diff & Hashing** : Façade statique centralisant le calcul de diffs Myers et patchs (`java-diff-utils`), la sérialisation JSON (`Gson`), et le calcul SHA-256. |
| **`server`** | **`Room`** | **État atomique partagé** : Conteneur synchronisé (`synchronized`) garantissant l'atomicité de l'application de patchs et de la mise à jour du hash face aux écritures concurrentes. |
| **`server`** | **`RoomManager`** | **Registre des salles** : Gestion concurrente (`ConcurrentHashMap`) de la création, recherche et suppression des salles virtuelles. |
| **`server`** | **`ServerSession`** | **Machine à états du protocole** : Parse les lignes de requête, applique les règles métier et formate les réponses (`OK`, `ERROR`), 100 % isolée du réseau. |
| **`server`** | **`TcpServer`** | **Serveur TCP Réseau** : Écoute sur le port d'entrée et répartit les connexions entrantes dans le pool de threads (`ExecutorService`). |
| **`server`** | **`ClientHandler`** | **Gestionnaire de connexion client** : Tâche de travail (`Runnable`) lisant et écrivant sur la socket TCP d'un client connecté. |
| **`client`** | **`TcpClient`** | **Client TCP Réseau** : Gère la connexion cliente, l'envoi de requêtes et la lecture de réponses sur le flux TCP. |
| **`client`** | **`ClientState`** | **Gestionnaire d'état local** : Regroupe la persistance disque du fichier local, l'instantané synchronisé (`syncedLines`, `currentChecksum`), et la réconciliation de conflits (`acceptRemote` vs `keepLocalAndRebase`). |
| **`client`** | **`ClientRepl`** | **Interface utilisateur REPL** : Boucle de commande console, saisie utilisateur et affichage formaté. |
| **`ch.heigvd`** | **`Main` / CLI** | **Lanceur Picocli** : Parsing des options de ligne de commande (`server` vs `client`). |
