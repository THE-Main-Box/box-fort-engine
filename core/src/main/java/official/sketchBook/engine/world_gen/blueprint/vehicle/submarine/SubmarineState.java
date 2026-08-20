package official.sketchBook.engine.world_gen.blueprint.vehicle.submarine;

public class SubmarineState {
    public final SubmarineBlueprint submarine;
    public final float spawnX, spawnY;

    public SubmarineState(SubmarineBlueprint submarine, float spawnX, float spawnY) {
        this.submarine = submarine;
        this.spawnX = spawnX;
        this.spawnY = spawnY;
    }
}
