package main;

// L'ordre des valeurs est envoye sur le reseau (ordinal) : ne pas le modifier.
public enum DifficultyProfile {
    EASY("Facile"),
    NORMAL("Normal"),
    CUSTOM("Personnaliser");

    private final String label;

    DifficultyProfile(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
