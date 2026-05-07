package official.sketchBook.game.util_related.values;

import com.badlogic.gdx.Input;

public class ControlKeys {

    public static int dir_up = Input.Keys.UP;
    public static int dir_down = Input.Keys.DOWN;

    public static int dir_left = Input.Keys.LEFT;
    public static int dir_right = Input.Keys.RIGHT;

    public static int jump = Input.Keys.SPACE;
    public static int recharge = Input.Keys.D;

    public static int use = Input.Keys.Z;
    public static int secondaryUse = Input.Keys.X;

    public static int interact = Input.Keys.S;

    // redefinir uma tecla
    public static void setKey(String action, int newKey) {
        switch (action.toLowerCase()) {
            case "move_left":
                dir_left = newKey;
                break;
            case "move_right":
                dir_right = newKey;
                break;
            case "jump":
                jump = newKey;
                break;
        }
    }

}
