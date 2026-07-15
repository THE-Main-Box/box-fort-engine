package official.sketchBook.engine.components_related.system_utils;


/// Controla a frequ�ncia de atualiza��o de um sistema, limitando execu��es por segundo.
/// Uso: chame shouldUpdate(delta) no in�cio do update — s� executa a l�gica se retornar true.
public class UpdateRateLimiter {

    private float timer = 0f;
    private float interval;
    private float lastElapsed = 0f; // tempo decorrido desde o último update
    private boolean active;

    public UpdateRateLimiter(float updatesPerSecond, boolean active) {
        setRate(updatesPerSecond);
        this.active = active;
    }

    /// Retorna true se já passou tempo suficiente para uma nova atualização
    public boolean shouldUpdate(float delta) {
        if (!active) {
            lastElapsed = delta;
            return true;
        }

        timer += delta;

        if (timer < interval) return false;

        lastElapsed = timer; // tempo real decorrido, não necessariamente igual ao interval
        timer = 0f;
        return true;
    }

    /// Tempo decorrido desde o último update — use como delta na simulação
    public float getElapsed() {
        return lastElapsed;
    }

    public void setRate(float updatesPerSecond) {
        if (updatesPerSecond <= 0f)
            throw new IllegalArgumentException("updatesPerSecond deve ser maior que 0");
        this.interval = 1f / updatesPerSecond;
        this.timer = 0f;
    }

    public void setActive(boolean active) { this.active = active; }
    public void reset() { this.timer = 0f; }
    public boolean isActive() { return active; }
    public float getInterval() { return interval; }
}
