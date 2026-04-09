package official.sketchBook.engine.components_related.objects;

public class MovementDataComponent {

    public AxisData
        xAxis,
        yAxis,
        rAxis;

    /// Valor de escala de gravidade padrão
    public float gravityScale;

    public boolean gravityAffected;

    private boolean disposed = false;


    public MovementDataComponent() {
        xAxis = new AxisData();
        yAxis = new AxisData();
        rAxis = new AxisData();
    }

    public MovementDataComponent(
        float xMaxMoveSpeed,
        float yMaxMoveSpeed,
        float rMaxMoveSpeed,
        float xMaxSpeed,
        float yMaxSpeed,
        float rMaxSpeed,
        float xDeceleration,
        float yDeceleration,
        float rDeceleration,
        boolean canMoveX,
        boolean canMoveY,
        boolean canRotate,
        boolean canAccelerateX,
        boolean canAccelerateY,
        boolean canAccelerateR,
        boolean canDecelerateX,
        boolean canDecelerateY,
        boolean canDecelerateR,
        boolean gravityAffected
    ) {

        this.xAxis = new AxisData(
            0,
            0,
            xDeceleration,
            0,
            xMaxSpeed,
            xMaxMoveSpeed,
            canAccelerateX,
            canDecelerateX,
            canMoveX
        );

        this.yAxis = new AxisData(
            0,
            0,
            yDeceleration,
            0,
            yMaxSpeed,
            yMaxMoveSpeed,
            canAccelerateY,
            canDecelerateY,
            canMoveY
        );

        this.rAxis = new AxisData(
            0,
            0,
            rDeceleration,
            0,
            rMaxSpeed,
            rMaxMoveSpeed,
            canAccelerateR,
            canDecelerateR,
            canRotate
        );

        this.gravityAffected = gravityAffected;
        this.gravityScale = 1;
    }

    public void updateAndConstraintAllAxis(float delta) {

        xAxis.updateAxis(delta);

        yAxis.updateAxis(delta);

        rAxis.updateAxis(delta);

    }

    public void set(MovementDataComponent dataComponent) {

        this.xAxis.set(dataComponent.xAxis);
        this.yAxis.set(dataComponent.yAxis);
        this.rAxis.set(dataComponent.rAxis);

        this.gravityAffected = dataComponent.gravityAffected;

        this.gravityScale = dataComponent.gravityScale;
    }

    public MovementDataComponent cpy() {
        MovementDataComponent toReturn = new MovementDataComponent();
        toReturn.set(this);
        return toReturn;
    }

    public void reset() {
        this.xAxis.resetMovement();
        this.yAxis.resetMovement();
        this.rAxis.resetMovement();
    }

    public void dispose() {
        if (disposed) return;

        xAxis = null;
        yAxis = null;
        rAxis = null;

        disposed = true;
    }
}
