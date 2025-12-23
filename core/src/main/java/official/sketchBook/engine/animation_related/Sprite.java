package official.sketchBook.engine.animation_related;

public class Sprite {

    private final int indexX, indexY;
    private final float duration;

    public Sprite(int indexX, int indexY, float duration) {
        this.indexX = indexX;
        this.indexY = indexY;
        this.duration = duration;
    }

    public Sprite(int indexX, int indexY) {
        this.indexX = indexX;
        this.indexY = indexY;
        this.duration = -1;
    }

    public int getIndexX() {
        return indexX;
    }

    public int getIndexY() {
        return indexY;
    }

    public float getDuration() {
        return duration;
    }

}
