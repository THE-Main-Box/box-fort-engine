package official.sketchBook.engine.components_related.objects;

import official.sketchBook.engine.components_related.intefaces.base_interfaces.Component;

public class TransformComponent implements Component {

    /// Valores da posição em seus eixos relativos em pixel
    private float x, y, z;

    /// Valores de dimensão em pixels
    private float width, height;

    /// Inversão de percepção do objeto em relação ao eixo
    private boolean xAxisInverted, yAxisInverted;

    /// Rotação atual do sprite em graus
    private float radians;

    private boolean disposed = false;

    public TransformComponent(
        float x,
        float y,
        float z,
        float radians,
        float width,
        float height,
        boolean xAxisInverted,
        boolean yAxisInverted
    ) {
        this.x = x;
        this.y = y;
        this.z = z;
        this.radians = radians;
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

    public void setRadians(float radians) {
        this.radians = radians;
    }

    public float getRadians() {
        return radians;
    }

    public float getX() {
        return x;
    }

    public void setX(float x) {
        this.x = x;
    }

    public float getY() {
        return y;
    }

    public void setY(float y) {
        this.y = y;
    }

    public float getZ() {
        return z;
    }

    public void setZ(float z) {
        this.z = z;
    }

    public float getWidth() {
        return width;
    }

    public void setWidth(float width) {
        this.width = width;
    }

    public float getHeight() {
        return height;
    }

    public void setHeight(float height) {
        this.height = height;
    }

    public boolean isxAxisInverted() {
        return xAxisInverted;
    }

    public void setxAxisInverted(boolean xAxisInverted) {
        this.xAxisInverted = xAxisInverted;
    }

    public boolean isyAxisInverted() {
        return yAxisInverted;
    }

    public void setyAxisInverted(boolean yAxisInverted) {
        this.yAxisInverted = yAxisInverted;
    }

    @Override
    public boolean isDisposed() {
        return disposed;
    }
}
