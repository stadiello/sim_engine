// il s'agit de l'IA de base qui est utilisée pour effectuer les actions décidées par les LLMs.
package object.ai;

public class BotBrain {

    private String action;
    private int clock = 0;

    public BotBrain(String action) {
        if (clock % 10 == 0) {
            
        }
        
    }

    public void decideAction() {
        switch (action) {
            case "ATTACK" -> enemy.attack(player);
            case "FLEE" -> enemy.flee();
            case "HIDE" -> enemy.hide();
            case "CALL_BACKUP" -> enemy.callBackup();
        }
    }
}
