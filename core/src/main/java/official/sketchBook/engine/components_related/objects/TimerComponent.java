package official.sketchBook.engine.components_related.objects;

import official.sketchBook.engine.components_related.intefaces.base_interfaces.Component;

public class TimerComponent {

    private float timeElapsed = 0;
    private float targetTime = 0;
    private boolean running = false;

    private static final float EPSILON = 0.0001f; // evita problemas de precisão

    public TimerComponent() {
    }

    public TimerComponent(float targetTime) {
        setTargetTimeSafe(targetTime);
    }

    public void update(float deltaTime) {
        if (running && targetTime > 0) {
            timeElapsed += deltaTime;
            if (timeElapsed >= targetTime) {
                timeElapsed = targetTime; // Garante que não ultrapasse o tempo
            }
        }
    }

    public void resetByFinished() {
        if (isFinished()) {
            stop();
            reset();
        }
    }

    public boolean isFinished() {
        return timeElapsed + EPSILON >= targetTime; // evita erro de ponto flutuante
    }

    public void stop() {
        running = false;
    }

    public void start() {
        running = true;
    }

    public void reset() {
        timeElapsed = 0;
    }

    public float getTimeElapsed() {
        return timeElapsed;
    }

    public float getTargetTime() {
        return targetTime;
    }

    public void setTargetTime(float targetTime) {
        setTargetTimeSafe(targetTime);
    }

    private void setTargetTimeSafe(float newTargetTime) {
        if (newTargetTime < 0) {
            throw new IllegalArgumentException("Target time must be non-negative");
        }
        this.targetTime = newTargetTime;
        if (timeElapsed >= targetTime) {
            stop(); // mantém lógica original
        }
    }

    public boolean isRunning() {
        return running;
    }
}
