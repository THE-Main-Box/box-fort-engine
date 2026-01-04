package official.sketchBook.game.components_related;

import official.sketchBook.engine.components_related.base_components.KeyBoundControllerComponent;
import official.sketchBook.game.gameObject_related.Player;
import official.sketchBook.game.util_related.values.ControlKeys;

public class PlayerControllerComponent extends KeyBoundControllerComponent {

    private final Player player;

    private final float speed = 10;

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
        if (pressed) {
            player.getTransformC().setY(
                player.getTransformC().getY() - speed
            );
        }
    }

    public void left(boolean pressed) {
        if (pressed) {
            player.getMoveC().setCanAccelerateX(true);
            player.getTransformC().setxAxisInverted(true);
            player.getMoveC().setxAccelInMeters(-speed);
        } else {
            player.getMoveC().setCanAccelerateX(false);
            player.getMoveC().setxAccelInMeters(0);
        }

    }

    public void right(boolean pressed) {
        if (pressed) {
            player.getMoveC().setCanAccelerateX(true);
            player.getTransformC().setxAxisInverted(false);
            player.getMoveC().setxAccelInMeters(speed);
        } else {
            player.getMoveC().setCanAccelerateX(false);
            player.getMoveC().setxAccelInMeters(0);
        }
    }

}
