package official.sketchBook.engine.util_related.texture;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import official.sketchBook.engine.animation_rendering_related.Sprite;

public class TextureUtils {
    public static TextureRegion obtainCurrentSpriteImage(
        Sprite sprite,
        int textureWidth,
        int textureHeight,
        Texture spriteSheet,
        boolean flipX,
        boolean flipY
    ) {

        TextureRegion region = new TextureRegion(
            spriteSheet,
            sprite.getIndexX() * textureWidth,
            sprite.getIndexY() * textureHeight,
            textureWidth,
            textureHeight
        );

        region.flip(flipX, flipY);  // Inverte horizontalmente

        return region;
    }

    public static TextureRegion obtainCurrentSpriteImage(
        Sprite sprite,
        int textureWidth,
        int textureHeight,
        int margin,
        int spacing,
        Texture spriteSheet,
        boolean flipX,
        boolean flipY
    ) {
        // Cálculo: Margem + (Índice * (Tamanho + Espaçamento))
        int x = margin + (sprite.getIndexX() * (textureWidth + spacing));
        int y = margin + (sprite.getIndexY() * (textureHeight + spacing));

        TextureRegion region = new TextureRegion(
            spriteSheet,
            x,
            y,
            textureWidth,
            textureHeight
        );

        region.flip(flipX, flipY);

        return region;
    }

    public static float scale(float value, float factor, boolean shouldMultiply) {
        return shouldMultiply ? value * factor : value / factor;
    }
}
