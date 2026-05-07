package official.sketchBook.game.components_related;

import official.sketchBook.engine.animation_rendering_related.ObjectAnimationPlayer;
import official.sketchBook.engine.animation_rendering_related.Sprite;

import java.util.Arrays;
import java.util.Collections;

import static official.sketchBook.game.util_related.values.AnimationKeys.Entities.*;
import static official.sketchBook.game.util_related.values.AnimationKeys.Entities.afterFall;
import static official.sketchBook.game.util_related.values.AnimationKeys.Entities.fall;
import static official.sketchBook.game.util_related.values.AnimationKeys.Entities.idle;

public class PlayerAnimationInitializerComponent {

    public static void initAnimations(ObjectAnimationPlayer aniPlayer) {
        aniPlayer.addAnimation(idle, Arrays.asList(
            new Sprite(0, 0, 0.15f),
            new Sprite(1, 0, 0.15f),
            new Sprite(2, 0, 0.15f),
            new Sprite(3, 0, 0.15f)
        ));

        aniPlayer.addAnimation(run, Arrays.asList(
            new Sprite(4, 0, 0.075f),
            new Sprite(0, 1, 0.075f),
            new Sprite(1, 1, 0.075f),
            new Sprite(2, 1, 0.075f),
            new Sprite(3, 1, 0.075f),
            new Sprite(4, 1, 0.075f),
            new Sprite(0, 2, 0.075f),
            new Sprite(1, 2, 0.075f)
        ));

        aniPlayer.addAnimation(jump, Arrays.asList(
            new Sprite(2, 2),
            new Sprite(3, 2)
        ));

        aniPlayer.addAnimation(fall, Collections.singletonList(
            new Sprite(4, 2)
        ));

        aniPlayer.addAnimation(afterFall, Arrays.asList(
            new Sprite(0, 3, 0.1f),
            new Sprite(1, 3, 0.1f)
        ));

        aniPlayer.playAnimation(idle);
    }

}
