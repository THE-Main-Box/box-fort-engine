package official.sketchBook.engine.components_related.system_utils;

public class SubmersibleVolume {

    private float
        centerXToBody,
        centerYToBody;
    private float
        width,
        height;

    private boolean
        dimensionsDataDirty,
        posDataDirty;

    /// Última fração de submersão calculada para este volume especificamente (0 = seco, 1 = totalmente submerso)
    private float lastSubmersionFraction = 0f;

    /// Marcado quando a fração de submersão muda além do epsilon tolerado.
    /// Ainda não usado para short-circuit — reservado para uma otimização futura.
    private boolean submersionDirty = true;

    public SubmersibleVolume(float centerXToBody, float centerYToBody, float width, float height) {
        this.centerXToBody = centerXToBody;
        this.centerYToBody = centerYToBody;
        this.width = width;
        this.height = height;
        this.dimensionsDataDirty = true;
        this.posDataDirty = true;
    }

    public void setCenterXToBody(float centerXToBody) {
        this.centerXToBody = centerXToBody;
        this.posDataDirty = true;
    }

    public void setCenterYToBody(float centerYToBody) {
        this.centerYToBody = centerYToBody;
        this.posDataDirty = true;
    }

    public void setWidth(float width) {
        this.width = width;
        this.dimensionsDataDirty = true;
    }

    public void setHeight(float height) {
        this.height = height;
        this.dimensionsDataDirty = true;
    }

    public float getCenterXToBody() {
        return centerXToBody;
    }

    public float getCenterYToBody() {
        return centerYToBody;
    }

    public float getWidth() {
        return width;
    }

    public float getHeight() {
        return height;
    }

    public boolean isDimensionsDataDirty() {
        return dimensionsDataDirty;
    }

    public boolean isPosDataDirty() {
        return posDataDirty;
    }

    public float getLastSubmersionFraction() {
        return lastSubmersionFraction;
    }

    public boolean isSubmersionDirty() {
        return submersionDirty;
    }

    /// Atualiza a fração cacheada e marca dirty se a mudança superar o epsilon informado.
    /// Quem chama decide o epsilon (fica no componente de física, junto da constante).
    public void updateSubmersionFraction(float newFraction, float epsilon) {
        float delta = Math.abs(newFraction - lastSubmersionFraction);
        this.submersionDirty = delta > epsilon;
        this.lastSubmersionFraction = newFraction;
    }

    public float getArea() {
        return height * width;
    }
}
