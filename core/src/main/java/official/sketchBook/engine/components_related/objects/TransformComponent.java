package official.sketchBook.engine.components_related.objects;

import official.sketchBook.engine.components_related.intefaces.base_interfaces.Component;

public class TransformComponent implements Component {

    /// Valores da posição em seus eixos relativos em pixel
    public float x, y, z;

    /// Valores de dimensão em pixels
    private final float width, height;

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
    }

    @Override
    public void update(float delta) {
    }

    @Override
    public void postUpdate() {

    }

    @Override
    public void dispose() {
        disposed = true;
    }

    public float getWidth() {
        return width;
    }

    public float getHeight() {
        return height;
    }

    @Override
    public boolean isDisposed() {
        return disposed;
    }
}
