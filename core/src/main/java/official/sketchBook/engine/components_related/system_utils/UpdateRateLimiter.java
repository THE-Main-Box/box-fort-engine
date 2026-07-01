package official.sketchBook.engine.components_related.system_utils;


/// Controla a frequ�ncia de atualiza��o de um sistema, limitando execu��es por segundo.
/// Uso: chame shouldUpdate(delta) no in�cio do update — s� executa a l�gica se retornar true.
public class UpdateRateLimiter {

    /// Tempo acumulado desde a �ltima atualiza��o
    private float timer = 0f;

    /// Intervalo m�nimo entre atualiza��es em segundos
    private float interval;

    /// Se o limitador est� ativo — quando false, sempre retorna true
    private boolean active;

    public UpdateRateLimiter(float updatesPerSecond, boolean active) {
        setRate(updatesPerSecond);
        this.active = active;
    }

    /// Retorna true se j� passou tempo suficiente para uma nova atualiza��o
    public boolean shouldUpdate(float delta) {
        if (!active) return true;

        timer += delta;

        if (timer < interval) return false;

        timer = 0f;
        return true;
    }

    /// Redefine a taxa de atualiza��o em updates por segundo
    public void setRate(float updatesPerSecond) {
        if (updatesPerSecond <= 0f)
            throw new IllegalArgumentException("updatesPerSecond deve ser maior que 0");
        this.interval = 1f / updatesPerSecond;
        this.timer = 0f;
    }

    /// Ativa ou desativa o limitador
    public void setActive(boolean active) {
        this.active = active;
    }

    /// Reseta o timer, for�ando a pr�xima chamada a retornar true imediatamente
    public void reset() {
        this.timer = 0f;
    }

    public boolean isActive() {
        return active;
    }

    public float getInterval() {
        return interval;
    }
}
