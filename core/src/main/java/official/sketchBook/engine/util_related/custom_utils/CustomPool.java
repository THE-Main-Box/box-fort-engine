package official.sketchBook.engine.util_related.custom_utils;

import com.badlogic.gdx.utils.Array;

public abstract class CustomPool<T extends CustomPool.Poolable> {

    public final int max;
    public int peak;
    protected final Array<T> freeObjects;

    public CustomPool() {
        this(16, Integer.MAX_VALUE);
    }

    public CustomPool(int initialCapacity) {
        this(initialCapacity, Integer.MAX_VALUE);
    }

    public CustomPool(int initialCapacity, int max) {
        this.freeObjects = new Array<>(false, initialCapacity);
        this.max = max;
    }

    /// Criação personalizada de um objeto
    protected abstract T newObject();

    /// retorna um novo objeto ou um objeto da lista livre
    public final T obtain() {
        return freeObjects.isEmpty() ? newObject() : freeObjects.pop();
    }

    /// Destrói o objeto
    protected void destroy(T object) {
        object.destroy();
    }

    /// Reseta o objeto
    protected void reset(T object) {
        object.reset();
    }

    /// Destrói o objeto e remove ele da lista
    protected void discard(T object) {
        destroy(object);
        this.freeObjects.removeValue(object, true);
    }

    /// Realiza uma liberação dos objetos, ou seja passamos eles para uma lista de objetos para serem reutilizados
    public void free(T object) {
        if (object == null)
            throw new IllegalArgumentException("object cannot be null.");

        if (freeObjects.size < max) {
            freeObjects.add(object);
            peak = Math.max(peak, freeObjects.size);
            reset(object);
        } else {
            this.discard(object);
        }

    }

    /// Preenche todos os espaços livres com objetos novos desejados
    public void fill(int size) {
        for (int i = 0; i < size && freeObjects.size < max; ++i) {
            freeObjects.add(newObject());
        }
        peak = Math.max(peak, freeObjects.size);
    }

    public void freeAll(Array<T> objects) {
        if (objects == null) {
            throw new IllegalArgumentException("objects cannot be null.");
        } else {
            Array<T> freeObjects = this.freeObjects;
            int max = this.max;
            int i = 0;

            for (int n = objects.size; i < n; ++i) {
                T object = objects.get(i);
                if (object != null) {
                    if (freeObjects.size < max) {
                        freeObjects.add(object);
                        reset(object);
                    } else {
                        this.discard(object);
                    }
                }
            }

            this.peak = Math.max(this.peak, freeObjects.size);
        }
    }

    /// Realiza uma limpeza da "pool" e um discard do objeto para remover ele da "pool"
    public void clear() {
        //Percorre a lista de cima pra baixo
        for (int i = freeObjects.size - 1; i >= 0; i--) {
            this.discard(freeObjects.get(i));//realiza um descarte dos objetos
        }
    }

    public Array<T> getFreeObjects() {
        return freeObjects;
    }

    public int getFreeCount() {
        return freeObjects.size;
    }

    public interface Poolable {
        /// Realiza uma limpeza dos dados para preparar pra ser reutilizado
        void reset();

        /// Realiza uma limpeza de dados final, para ser destruído pro completo
        void destroy();

        /// Verifica se estamos limpos e prontos para sermos usados, é mais para uso interno do que da lógica da pool mesmo
        boolean isReset();
    }
}
