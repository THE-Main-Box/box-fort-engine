package official.sketchBook.engine.util_related.enumerators;

public enum Direction {
    UP,
    LEFT,
    DOWN,
    RIGHT,
    STILL,
    UP_LEFT,
    UP_RIGHT,
    DOWN_LEFT,
    DOWN_RIGHT,
    UP_DOWN,
    LEFT_RIGHT;

    public boolean isYAxis(){
        return isDown() || isUp();
    }

    public boolean isXAxis(){
        return isLeft() || isRight();
    }

    /// Verifica se tem a direção baixo
    public boolean isDown() {
        return this == DOWN || this == DOWN_LEFT || this == DOWN_RIGHT;
    }

    /// Verifica se tem a direção cima
    public boolean isUp() {
        return this == UP || this == UP_LEFT || this == UP_RIGHT;
    }

    /// Verifica se tem a direção esquerda
    public boolean isLeft() {
        return this == LEFT || this == DOWN_LEFT || this == UP_LEFT;
    }

    /// Verifica se possui a direção direita
    public boolean isRight() {
        return this == RIGHT || this == DOWN_RIGHT || this == UP_RIGHT;
    }

    /// Verifica se é uma diagonal
    public boolean isDiagonal() {
        return this == DOWN_LEFT || this == DOWN_RIGHT || this == UP_RIGHT || this == UP_LEFT;
    }

    public Direction getOpposite() {
        switch (this) {
            case UP:
                return DOWN;
            case DOWN:
                return UP;
            case LEFT:
                return RIGHT;
            case RIGHT:
                return LEFT;
            case UP_LEFT:
                return DOWN_RIGHT;
            case UP_RIGHT:
                return DOWN_LEFT;
            case DOWN_LEFT:
                return UP_RIGHT;
            case DOWN_RIGHT:
                return UP_LEFT;
            case UP_DOWN:
                return LEFT_RIGHT;
            case LEFT_RIGHT:
                return UP_DOWN;
            case STILL:
            default:
                return STILL;
        }
    }

}
