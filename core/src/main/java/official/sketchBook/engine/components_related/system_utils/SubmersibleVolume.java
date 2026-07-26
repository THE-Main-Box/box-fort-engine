package official.sketchBook.engine.components_related.system_utils;

import com.badlogic.gdx.math.MathUtils;

public class SubmersibleVolume {

    private float centerXToBody;
    private float centerYToBody;
    private float radius;

    private boolean dimensionsDataDirty;
    private boolean posDataDirty;

    private float lastSubmersionFraction = 0f;
    private boolean submersionDirty = true;

    public SubmersibleVolume(float centerXToBody, float centerYToBody, float radius) {
        this.centerXToBody = centerXToBody;
        this.centerYToBody = centerYToBody;
        this.radius = radius;
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

    public void setRadius(float radius) {
        this.radius = radius;
        this.dimensionsDataDirty = true;
    }

    public float getCenterXToBody() {
        return centerXToBody;
    }

    public float getCenterYToBody() {
        return centerYToBody;
    }

    public float getRadius() {
        return radius;
    }

    public float getDiameter() {
        return radius * 2f;
    }

    public float getArea() {
        return MathUtils.PI * radius * radius;
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

    public void updateSubmersionFraction(float newFraction, float epsilon) {
        float delta = Math.abs(newFraction - lastSubmersionFraction);
        this.submersionDirty = delta > epsilon;
        this.lastSubmersionFraction = newFraction;
    }
}
