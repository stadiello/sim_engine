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