package official.sketchBook.game.components_related;

import com.badlogic.gdx.Input;
import official.sketchBook.engine.components_related.base_components.KeyBoundControllerComponent;
import official.sketchBook.engine.components_related.physics.VehiclePassengerPhysicsComponent;
import official.sketchBook.engine.components_related.vehicle.VehicleControllerComponent;
import official.sketchBook.engine.util_related.enumerators.Direction;
import official.sketchBook.game.gameObject_related.Player;
import official.sketchBook.game.util_related.values.ControlKeys;

public class PlayerControllerComponent extends KeyBoundControllerComponent {

    public Player player;

    private float accelToApply = 100;

    private boolean
        upPressed = false,
        downPressed = false,
        leftPressed = false,
        rightPressed = false;

    private Direction lastDirectionPressed = Direction.STILL;

    // Estado anterior armazenado para compara��o
    private Direction lastAppliedDirection = Direction.STILL;
    private float lastAppliedAccel = 0;
    private boolean lastCanAccelerate = false;

    public PlayerControllerComponent(Player player) {
        this.player = player;

        this.bindKey(ControlKeys.dir_up, this::up);
        this.bindKey(ControlKeys.dir_down, this::down);
        this.bindKey(ControlKeys.dir_left, this::left);
        this.bindKey(ControlKeys.dir_right, this::right);
        this.bindKey(ControlKeys.jump, this::jump);
        this.bindKey(ControlKeys.interact, this::interact);

        // TODO: remover quando HUD de controller estiver implementado
        this.bindKey(Input.Keys.NUM_0, pressed -> { if (pressed) VehicleControllerComponent.PendingGroupInput.trigger(0); });
        this.bindKey(Input.Keys.NUM_1, pressed -> { if (pressed) VehicleControllerComponent.PendingGroupInput.trigger(1); });
        this.bindKey(Input.Keys.NUM_2, pressed -> { if (pressed) VehicleControllerComponent.PendingGroupInput.trigger(2); });
    }

    public void interact(boolean pressed) {
        if (pressed) {
            player.getTriggerC().onInteractPress();
        } else {
            player.getTriggerC().onInteractRelease();
        }
    }

    public void up(boolean pressed) {
        this.upPressed = pressed;
    }

    public void down(boolean pressed) {
        this.downPressed = pressed;
    }

    public void jump(boolean pressed) {
        player.jump(!pressed);
    }

    public void left(boolean pressed) {
        if (leftPressed == pressed) return;
        leftPressed = pressed;
        if (pressed) lastDirectionPressed = Direction.LEFT;
    }

    public void right(boolean pressed) {
        if (rightPressed == pressed) return;
        rightPressed = pressed;
        if (pressed) lastDirectionPressed = Direction.RIGHT;
    }

    @Override
    public void update(float delta) {
        super.update(delta);
        if (player == null) return;

        updateMovement();
        testSubMovement();
    }

    @Override
    public void initObject() {
    }

    private void testSubMovement() {
        VehiclePassengerPhysicsComponent VPPC = player.getVehiclePassengerPhysicsC();

        if (VPPC.isInsideVehicle()) {
            float vel = 0;

            if (upPressed && !downPressed) {
                vel = 2f;
            } else if (!upPressed && downPressed) {
                vel = -2f;
            }

            VPPC.getCurrentSection().getPhysicsC().setVelocity(vel, 0);
        }
    }

    private void updateMovement() {
        Direction movementDirection;

        // L�gica de dire��o com early exit
        if (leftPressed && !rightPressed) {
            movementDirection = Direction.LEFT;
        } else if (!leftPressed && rightPressed) {
            movementDirection = Direction.RIGHT;
        } else if (!leftPressed && !rightPressed) {
            movementDirection = Direction.STILL;
        } else {
            movementDirection = lastDirectionPressed;
        }

        // S� atualiza se houve mudan�a de dire��o
        if (movementDirection != lastAppliedDirection) {
            applyDirectionChange(movementDirection);
            lastAppliedDirection = movementDirection;
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
        // S� faz setters se realmente algo mudou
        if (lastCanAccelerate != canAccelerate) {
            player.getMoveC().dataComponent.xAxis.canAccelerate = canAccelerate;
            lastCanAccelerate = canAccelerate;
        }

        if (lastAppliedAccel != accel) {
            player.getMoveC().dataComponent.xAxis.acceleration = accel;
            lastAppliedAccel = accel;
        }
    }

    public void nullifyReferences() {
        player = null;
    }
}
