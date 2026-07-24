package object;

/**
 * Etat graphique produit par la simulation de l'hote et rejoue par la classe
 * d'origine sur un client réseau, sans réexécuter les dégâts.
 */
public interface NetworkVisualState {
    double[] getNetworkVisualState();
    void applyNetworkVisualState(double[] state);
}
