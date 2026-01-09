package official.sketchBook.engine.components_related.objects;

import official.sketchBook.engine.components_related.intefaces.base_interfaces.Component;

public class TransformComponent implements Component {

    /// Valores da posição em seus eixos relativos em pixel
    public float
        x,
        y,
        z;

    /// Valores de dimensão em pixels
    public float
        width,
        height;

    private float
        scaleX,
        scaleY;

    /// Inversão de percepção do objeto em relação ao eixo
    public boolean xAxisInverted, yAxisInverted;

    /// Rotação atual do sprite em graus
    public float rotation;

    private boolean disposed = false;

    public TransformComponent(
        float x,
        float y,
        float z,
        float rotation,
        float width,
        float height,
        float scaleX,
        float scaleY,
        boolean xAxisInverted,
        boolean yAxisInverted
    ) {
        this.x = x;
        this.y = y;
        this.z = z;
        this.rotation = rotation;
        this.width = width;
        this.height = height;
        this.xAxisInverted = xAxisInverted;
        this.yAxisInverted = yAxisInverted;

        setScale(
            scaleX,
            scaleY
        );
    }

    @Override
    public void update(float delta) {
    }

    @Override
    public void postUpdate() {

    }

    public void setScale(
        float scaleX,
        float scaleY
    ) {
        if (scaleX <= 0 || scaleY <= 0) {
            throw new IllegalArgumentException("Escala deve ser maior que 0");
        }

        this.scaleX = scaleX;
        this.scaleY = scaleY;

        this.width *= scaleX;
        this.height *= scaleY;
    }

    public float getScaleY() {
        return scaleY;
    }

    public float getScaleX() {
        return scaleX;
    }

    public float getHalfWidth() {
        return width / 2;
    }

    public float getHalfHeight() {
        return height / 2;
    }

    public float getCenterX() {
        return x + getHalfWidth();
    }

    public float getCenterY() {
        return y + getHalfHeight();
    }

    @Override
    public void dispose() {
        disposed = true;
    }

    @Override
    public boolean isDisposed() {
        return disposed;
    }
}
