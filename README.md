# SimEngine

Petit jeu 2D ecrit en Java.

## Prerequis

- macOS (scripts fournis en `.bash`)
- JDK installe (`javac`, `java`, `jar`, `jpackage`)

## Structure

- `src/` : code source Java
- `src/assets/` : images et ressources visuelles
- `src/sound/` : sons
- `compile.bash` : compile et lance le jeu
- `factor.bash` : build complet + package app macOS

## Lancer le jeu en local

```bash
bash compile.bash
```

Ce script :

- nettoie les fichiers `.class`
- copie les assets et sons dans `out/`
- compile `src/main/GamePanel.java`
- lance `main.GamePanel`

Au demarrage, choisis :

- `Solo` pour jouer normalement ;
- `Heberger en LAN` sur l'ordinateur qui simulera la partie ;
- `Rejoindre` sur le second ordinateur.

## Mode coop LAN (2 joueurs)

Les deux ordinateurs doivent utiliser la meme version du jeu et etre connectes au
meme reseau local.

1. Sur l'ordinateur 1, lance le jeu et choisis `Heberger en LAN`.
2. L'adresse a communiquer au joueur 2 est affichee en haut de l'ecran, par
   exemple `192.168.1.20:28765`.
3. Sur l'ordinateur 2, lance le jeu, choisis `Rejoindre`, puis saisis uniquement
   l'adresse IP de l'hote, par exemple `192.168.1.20`.
4. L'hote choisit ensuite la carte et le mode de jeu.

Le port TCP utilise par defaut est `28765`. Le pare-feu de l'ordinateur hote peut
demander d'autoriser les connexions entrantes pour Java ou SimEngine.

Cette premiere version est autoritaire : l'hote execute la simulation et diffuse
l'image au joueur 2, qui lui envoie ses commandes. Les deux joueurs partagent donc
la meme camera et doivent rester relativement proches. Les mouvements, la visee,
les tirs, le rechargement, le changement d'arme et les interactions de mission du
joueur 2 sont transmis a l'hote.

## Marqueurs de morts et depots

- Les morts laissent une silhouette au sol propre a leur faction : allie, hostile,
  alien ou civil.
- L'affichage de ces silhouettes peut etre active ou desactive dans `Options`,
  avec le reglage `Marqueurs des morts`.
- Trois depots de munitions sont places sur chaque carte. Ils restaurent
  automatiquement les reserves de toutes les armes lorsqu'un joueur s'en approche.
- Chaque depot possede un delai de six secondes distinct pour chaque joueur.

Sur la carte `Desert tactique`, la tourelle fortifiee est manuelle : approche-toi,
appuie sur `E`, vise avec la souris et tire avec le clic gauche. Appuie de nouveau
sur `E` pour quitter la tourelle.

Il est aussi possible de choisir directement le role en ligne de commande apres
compilation :

```bash
java -cp out main.GamePanel --host
java -cp out main.GamePanel --join 192.168.1.20
java -cp out main.GamePanel --solo
```

## Generer un package de release

```bash
bash factor.bash
```

Ce script :

- nettoie `out/`, `dist/` et `release/`
- compile tous les fichiers Java
- cree `dist/sim_engine.jar`
- genere `release/SimEngine.app` via `jpackage`
- cree `SimEngine.zip`

## Nettoyage manuel

Si besoin, supprimer les dossiers de build :

```bash
rm -rf out dist release SimEngine.zip
```


## Lancer le jeu sur macOS

Accorder les permissions d'execution sur le fichier `SimEngine.app` :
```bash
xattr -d com.apple.quarantine "SimEngine.app"
```
Ouvrir le jeu :
```bash
open release/SimEngine.app
```
