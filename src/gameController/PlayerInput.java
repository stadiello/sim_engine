package gameController;

public interface PlayerInput {
    boolean isUp();
    boolean isDown();
    boolean isLeft();
    boolean isRight();
    boolean isSprint();
    boolean isLeftClickPressed();
    boolean consumeLeftClickPressed();
    boolean consumeRightClickTriggered();
    int getRightClickX();
    int getRightClickY();
    boolean consumePauseToggleTriggered();
    boolean consumeInteractTriggered();
    boolean consumeReloadTriggered();
    int consumeWeaponScrollDelta();
    int getMouseX();
    int getMouseY();
}
