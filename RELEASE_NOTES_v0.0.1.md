# SimEngine v0.0.1

Premiere release publique de SimEngine.

## Apercu

Cette version pose les bases du jeu avec :

- un moteur de rendu 2D
- une gestion des objets du monde (protagoniste, ennemis, projectiles)
- un systeme de tuiles et de generation de niveau
- des assets graphiques et sons integres

## Contenu principal

- boucle de jeu via `main.GamePanel`
- gestion des controles clavier
- systeme d'armes et projectiles
- logique d'IA de base (`object/ai/BotBrain`)
- packaging applicatif macOS (`SimEngine.app`) via `jpackage`

## Artefacts de release

- `SimEngine.zip`
- `release/SimEngine.app`

## Installation / Execution (macOS)

1. Telecharger `SimEngine.zip` depuis la release.
2. Decompresser l'archive.
3. Ouvrir `SimEngine.app`.

## Build local

```bash
bash compile.bash
```

Build complet + packaging :

```bash
bash factor.bash
```

## Notes

- Version initiale : des ajustements d'equilibrage et de performance sont prevus pour les prochaines versions.
- En cas de blocage au lancement sur macOS, verifier les autorisations de l'application dans les reglages de securite.