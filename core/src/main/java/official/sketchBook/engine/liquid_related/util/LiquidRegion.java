package official.sketchBook.engine.liquid_related.util;

public class LiquidRegion{
    /// Condensação de dados de coordenadas de dimensões
    private final float
        x,
        y,
        width,
        height;

    public LiquidRegion(float x, float y, float width, float height) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
    }

    public float getX() {
        return x;
    }

    public float getY() {
        return y;
    }

    public float getWidth() {
        return width;
    }

    public float getHeight() {
        return height;
    }
}
