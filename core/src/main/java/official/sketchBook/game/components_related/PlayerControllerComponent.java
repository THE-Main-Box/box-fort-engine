package official.sketchBook.game.components_related;

import official.sketchBook.engine.components_related.base_components.KeyBoundControllerComponent;
import official.sketchBook.engine.util_related.enumerators.Direction;
import official.sketchBook.game.gameObject_related.Player;
import official.sketchBook.game.util_related.values.ControlKeys;

public class PlayerControllerComponent extends KeyBoundControllerComponent {

    public Player player;

    private float accelToApply = 100;

    private boolean leftPressed = false;
    private boolean rightPressed = false;
    private Direction lastDirectionPressed = Direction.STILL;

    // Estado anterior armazenado para comparação
    private Direction lastAppliedDirection = Direction.STILL;
    private float lastAppliedAccel = 0;
    private boolean lastCanAccelerate = false;

    public PlayerControllerComponent(Player player) {
        this.player = player;

        this.bindKey(ControlKeys.dir_up, this::up);
        this.bindKey(ControlKeys.dir_down, this::down);
        this.bindKey(ControlKeys.dir_left, this::left);
        this.bindKey(ControlKeys.dir_right, this::right);
    }

    public void up(boolean pressed) {

    }

    public void down(boolean pressed) {

    }

    public void left(boolean pressed) {
        if (leftPressed == pressed) return; // Early exit se estado não mudou

        leftPressed = pressed;
        if (pressed) {
            lastDirectionPressed = Direction.LEFT;
        }

    }

    public void right(boolean pressed) {
        if (rightPressed == pressed) return; // Early exit se estado não mudou

        rightPressed = pressed;
        if (pressed) {
            lastDirectionPressed = Direction.RIGHT;
        }
    }

    @Override
    public void update(float delta) {
        super.update(delta);
        if (player == null) return;
        updateMovement();
    }

    private void updateMovement() {
        Direction directionToApply;

        // Lógica de direção com early exit
        if (leftPressed && !rightPressed) {
            directionToApply = Direction.LEFT;
        } else if (!leftPressed && rightPressed) {
            directionToApply = Direction.RIGHT;
        } else if (!leftPressed && !rightPressed) {
            directionToApply = Direction.STILL;
        } else {
            directionToApply = lastDirectionPressed;
        }

        // Só atualiza se houve mudança de direção
        if (directionToApply != lastAppliedDirection) {
            applyDirectionChange(directionToApply);
            lastAppliedDirection = directionToApply;
        }
    }

    private void applyDirectionChange(Direction direction) {
        switch (direction) {
            case LEFT:
                player.getTransformC().mirrorX = true;
                setXMovement(true, -accelToApply);
                break;
            case RIGHT:
                player.getTransformC().mirrorX = false;
                setXMovement(true, accelToApply);
                break;
            case STILL:
                setXMovement(false, 0);
                break;
        }
    }

    private void setXMovement(boolean canAccelerate, float accel) {
        // Só faz setters se realmente algo mudou
        if (lastCanAccelerate != canAccelerate) {
            player.getMoveC().canAccelerateX = canAccelerate;
            lastCanAccelerate = canAccelerate;
        }

        if (lastAppliedAccel != accel) {
            player.getMoveC().xAccel = accel;
            lastAppliedAccel = accel;

        }
    }

    @Override
    public void nullifyReferences() {
        player = null;
    }
}
