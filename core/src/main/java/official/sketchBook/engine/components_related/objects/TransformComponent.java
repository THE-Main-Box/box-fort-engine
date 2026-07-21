package official.sketchBook.engine.components_related.objects;

public class TransformComponent {

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
    public boolean
        mirrorX,
        mirrorY;

    /// Rotação atual do sprite em graus
    private float rotation;

    private float rotatedHalfWidth;
    private float rotatedHalfHeight;

    private boolean rotationDirty = true;

    public TransformComponent() {
        this(
            0,
            0,
            0,
            0,
            0,
            0,
            1,
            1,
            false,
            false
        );
    }

    public TransformComponent(
        float x,
        float y,
        float z,
        float rotation,
        float width,
        float height,
        float scaleX,
        float scaleY,
        boolean mirrorX,
        boolean mirrorY
    ) {
        this.x = x;
        this.y = y;
        this.z = z;
        this.rotation = rotation;
        this.width = width;
        this.height = height;
        this.mirrorX = mirrorX;
        this.mirrorY = mirrorY;

        setScale(
            scaleX,
            scaleY
        );
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

    public void updateRotationCache() {

        if (!rotationDirty)
            return;

        float halfW = width * 0.5f;
        float halfH = height * 0.5f;

        if (rotation == 0f) {

            rotatedHalfWidth = halfW;
            rotatedHalfHeight = halfH;

        } else {

            float rad = (float)Math.toRadians(rotation);

            float cos = Math.abs((float)Math.cos(rad));
            float sin = Math.abs((float)Math.sin(rad));

            rotatedHalfWidth = halfW * cos + halfH * sin;
            rotatedHalfHeight = halfW * sin + halfH * cos;
        }

        rotationDirty = false;
    }

    public float getRotation() {
        return rotation;
    }

    public void setRotation(float rotation) {
        this.rotation = wrapDegrees(rotation);
    }

    private float wrapDegrees(float degrees) {
        degrees = degrees % 360f;

        if (degrees > 180f) degrees -= 360f;
        else if (degrees < -180f) degrees += 360f;

        return degrees;
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

    public float getRotatedHalfWidth() {
        updateRotationCache();
        return rotatedHalfWidth;
    }

    public float getRotatedHalfHeight() {
        updateRotationCache();
        return rotatedHalfHeight;
    }

    public static TransformComponent initNewTransformComponent(
        float x,
        float y,
        float z,
        float rotation,
        float width,
        float height,
        float scaleX,
        float scaleY,
        boolean mirrorX,
        boolean mirrorY
    ) {
        return new TransformComponent(
            x,
            y,
            z,
            rotation,
            width,
            height,
            scaleX,
            scaleY,
            mirrorX,
            mirrorY
        );
    }
}
