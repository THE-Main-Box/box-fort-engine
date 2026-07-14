package official.sketchBook.engine.data_manager_related.util;

import official.sketchBook.engine.components_related.intefaces.integration_interfaces.util_related.CompositeRenderableObjectII;
import official.sketchBook.engine.components_related.intefaces.integration_interfaces.util_related.OptmizedRenderableObjectII;
import official.sketchBook.engine.components_related.intefaces.integration_interfaces.util_related.RenderableObjectII;
import official.sketchBook.engine.components_related.objects.TransformComponent;

import java.util.*;
import java.util.function.Consumer;

import static official.sketchBook.game.util_related.constants.RenderingConstants.ZOOM;
import static official.sketchBook.game.util_related.constants.WorldConstants.DEFAULT_BUCKET_SIZE;
import static official.sketchBook.game.util_related.constants.WorldConstants.INITIAL_CAPACITY;

/**
 * Gerenciador de renderização otimizado usando TreeMap (Red-Black Tree).
 * Mantém objetos automaticamente ordenados por renderIndex sem necessidade de sort.
 * <p>
 * Inserção/Remoção: O(log n)
 * Iteração: O(n)
 * Mudança de índice: O(log n) + O(log n)
 */
public class RenderableObjectManager {

    private ObjectBucket[] buckets;
    private int[] bucketKeys;
    private int bucketCount = 0;

    private final CullBounds cachedBounds;

    private boolean disposed = false;

    public RenderableObjectManager() {
        this.cachedBounds = new CullBounds();

        this.buckets = new ObjectBucket[INITIAL_CAPACITY];
        this.bucketKeys = new int[INITIAL_CAPACITY];
    }

    private ObjectBucket getOrCreateBucket(int renderIndex) {
        /// Busca bin�ria pelo renderIndex
        int lo = 0, hi = bucketCount - 1;
        while (lo <= hi) {
            int mid = (lo + hi) >>> 1;
            if (bucketKeys[mid] == renderIndex) return buckets[mid];
            if (bucketKeys[mid] < renderIndex) lo = mid + 1;
            else hi = mid - 1;
        }

        /// N�o encontrado ? insere na posi��o correta
        if (bucketCount == buckets.length) {
            buckets = java.util.Arrays.copyOf(buckets, bucketCount * 2);
            bucketKeys = java.util.Arrays.copyOf(bucketKeys, bucketCount * 2);
        }

        /// Abre espa�o e insere ordenado
        System.arraycopy(buckets, lo, buckets, lo + 1, bucketCount - lo);
        System.arraycopy(bucketKeys, lo, bucketKeys, lo + 1, bucketCount - lo);

        buckets[lo] = new ObjectBucket();
        bucketKeys[lo] = renderIndex;
        bucketCount++;

        return buckets[lo];
    }

    /// Adicionamos um objeto à pipeline
    public void add(RenderableObjectII obj) {
        //Buscamos um bucket ou criamos um
        getOrCreateBucket(      //Passamos o index de renderização
            obj.getRenderIndex()
        ).add(                  //Passamos depois de achar
            obj
        );

    }

    /// Removemos um objeto da tree
    public void remove(RenderableObjectII obj) {
        int idx = findBucketIndex(obj.getRenderIndex());
        if (idx < 0) return;
        buckets[idx].remove(obj);
        if (buckets[idx].isEmpty()) removeBucketAt(idx);
    }

    private int findBucketIndex(int renderIndex) {
        int lo = 0, hi = bucketCount - 1;
        while (lo <= hi) {
            int mid = (lo + hi) >>> 1;
            if (bucketKeys[mid] == renderIndex) return mid;
            if (bucketKeys[mid] < renderIndex) lo = mid + 1;
            else hi = mid - 1;
        }
        return -1;
    }

    private void removeBucketAt(int idx) {
        System.arraycopy(buckets, idx + 1, buckets, idx, bucketCount - idx - 1);
        System.arraycopy(bucketKeys, idx + 1, bucketKeys, idx, bucketCount - idx - 1);
        buckets[--bucketCount] = null;
    }

    /// Executa a atualização da mudança de index de renderização de um objeto,
    ///  não basta só mudar no objeto precisamos também aplicar aqui dentro,
    ///  fazemos isso para justamente controlar a quantidade de vezes que mudamos a tree
    public void updateRenderIndex(RenderableObjectII obj, int oldIndex) {
        int newIndex = obj.getRenderIndex();
        if (oldIndex == newIndex) return;
        int idx = findBucketIndex(oldIndex);
        if (idx >= 0) {
            buckets[idx].remove(obj);
            if (buckets[idx].isEmpty()) removeBucketAt(idx);
        }
        getOrCreateBucket(newIndex).add(obj);
    }

    /// Executa um código para cada objeto renderizável,
    ///  deve ser usado para chamar o render e atualização de visuais
    public void forEachObject(Consumer<RenderableObjectII> action) {
        for (int i = 0; i < bucketCount; i++) {
            buckets[i].forEach(action);
        }
    }

    /// Executa um código para cada objeto renderizável,
    ///  desde que este esteja dentro dos limites passados e cacheados,
    ///  deve ser usado para chamar o render e atualização de visuais
    public void forEachObject(
        Consumer<RenderableObjectII> action,
        float camX, float camY,
        float viewWidth, float viewHeight
    ) {
        cachedBounds.minX = camX - viewWidth * 0.5f;
        cachedBounds.maxX = camX + viewWidth * 0.5f;
        cachedBounds.minY = camY - viewHeight * 0.5f;
        cachedBounds.maxY = camY + viewHeight * 0.5f;

        for (int i = 0; i < bucketCount; i++) {
            buckets[i].forEachCulled(action, cachedBounds);
        }
    }

    /// Atualiza as dimensões em buffer
    private void updateCullBounds(float camX, float camY, float viewWidth, float viewHeight) {
        cachedBounds.minX = camX - viewWidth / 2f;
        cachedBounds.maxX = camX + viewWidth / 2f;
        cachedBounds.minY = camY - viewHeight / 2f;
        cachedBounds.maxY = camY + viewHeight / 2f;
    }

    /// Limpa as referencias armazenadas dentro do sistema
    public void clear() {
        for (int i = 0; i < bucketCount; i++) {
            buckets[i].clear();
            buckets[i] = null;
        }
        bucketCount = 0;
    }

    /// Buffer de bounds de tela
    private static class CullBounds {
        float minX, maxX, minY, maxY;
    }

    /// Realiza um dispose dos dados gráficos gerais
    public void dispose() {
        if (disposed) return;
        //Realiza a limpeza dos gráficos
        forEachObject(RenderableObjectII::disposeGraphics);
        //Limpa a lista existente
        clear();
        disposed = true;
    }

    public static void tryRemoveFromRender(
        RenderableObjectManager manager,
        Object toAdd
    ) {
        if (toAdd instanceof RenderableObjectII) {
            manager.remove(
                (RenderableObjectII) toAdd
            );
        }
    }

    public static void tryAddToRender(
        RenderableObjectManager manager,
        Object toAdd
    ) {
        if (toAdd instanceof RenderableObjectII) {
            manager.add(
                (RenderableObjectII) toAdd
            );
        }

    }

    /// Classe interna que gerencia um array de objetos com tamanho dinâmico
    private static class ObjectBucket {
        private RenderableObjectII[] items;
        private int size = 0;

        ObjectBucket() {
            this.items = new RenderableObjectII[DEFAULT_BUCKET_SIZE];
        }

        /// Adiciona mais objetos no balde
        void add(RenderableObjectII obj) {
            if (size == items.length) {
                // Expande o array quando necessário
                RenderableObjectII[] newItems = new RenderableObjectII[items.length * 2];
                System.arraycopy(items, 0, newItems, 0, items.length);
                this.items = newItems;
            }
            items[size++] = obj;
        }

        /// Remove um objeto do balde
        void remove(RenderableObjectII obj) {
            for (int i = 0; i < size; i++) {
                if (items[i] == obj) {
                    // Move o último elemento para a posição do removido
                    items[i] = items[--size];
                    items[size] = null; // Limpa referência para GC
                    return;
                }
            }
        }

        /// Código a ser executado para todos os objetos dentro de nossa array
        void forEach(Consumer<RenderableObjectII> action) {

            for (int i = 0; i < size; i++) {

                RenderableObjectII obj = items[i];

                if (!obj.canRender())
                    continue;

                if (obj instanceof OptmizedRenderableObjectII) {

                    if (!((OptmizedRenderableObjectII) obj).isInScreen())
                        continue;
                }

                action.accept(obj);
            }
        }

        /// Código a ser executado para todos os objetos dentro de nossa array,
        ///  desde que estejam dentro dos limites de tela,
        ///  com uma pequena margem de folga
        void forEachCulled(
            Consumer<RenderableObjectII> action,
            CullBounds bounds
        ) {

            for (int i = 0; i < size; i++) {

                RenderableObjectII obj = items[i];

                if (!(obj instanceof OptmizedRenderableObjectII)) {

                    if (obj.canRender())
                        action.accept(obj);

                    continue;
                }

                OptmizedRenderableObjectII optimized =
                    (OptmizedRenderableObjectII) obj;

                boolean inScreen;

                if (optimized instanceof CompositeRenderableObjectII) {

                    inScreen = isInBounds(
                        (CompositeRenderableObjectII) optimized,
                        bounds
                    );

                } else {

                    inScreen = isInBounds(
                        optimized,
                        bounds
                    );
                }

                optimized.setInScreen(inScreen);

                if (inScreen && obj.canRender()) {
                    action.accept(obj);
                }
            }
        }

        /// Verifica se o objeto que pode ser renderizado está com todas suas seções dentro da tela
        private boolean isInBounds(
            CompositeRenderableObjectII object,
            CullBounds bounds
        ) {

            List<? extends RenderableObjectII> list =
                object.getRenderableObjList();

            if (list == null || list.isEmpty())
                return true;

            boolean anyVisible = false;

            for (int i = 0; i < list.size(); i++) {

                RenderableObjectII renderable =
                    list.get(i);

                if (!(renderable instanceof OptmizedRenderableObjectII))
                    continue;

                OptmizedRenderableObjectII optimized =
                    (OptmizedRenderableObjectII) renderable;

                boolean visible =
                    isInBounds(
                        optimized,
                        bounds
                    );

                optimized.setInScreen(visible);

                if (visible)
                    anyVisible = true;
            }

            return anyVisible;
        }

        /// Verificamos se o objeto está dentro dos limites da tela
        /// Verificamos se o objeto está dentro dos limites da tela
        private boolean isInBounds(
            OptmizedRenderableObjectII obj,
            CullBounds bounds
        ) {

            TransformComponent t = obj.getTransformC();

            if (t == null)
                return true;

            float centerX = t.getCenterX();
            float centerY = t.getCenterY();

            float halfW = t.getRotatedHalfWidth();
            float halfH = t.getRotatedHalfHeight();

            return !(
                centerX + halfW < bounds.minX ||
                    centerX - halfW > bounds.maxX ||
                    centerY + halfH < bounds.minY ||
                    centerY - halfH > bounds.maxY
            );
        }

        /// Limpa a bucket
        void clear() {
            // Limpa as referências para permitir GC
            for (int i = 0; i < size; i++) {
                items[i] = null;
            }
            size = 0;
        }

        int size() {
            return size;
        }

        boolean isEmpty() {
            return size == 0;
        }
    }
}
