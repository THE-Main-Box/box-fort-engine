package official.sketchBook.engine.dataManager_related.util;

import official.sketchBook.engine.components_related.intefaces.integration_interfaces.util_related.RenderAbleObjectII;
import official.sketchBook.engine.components_related.objects.TransformComponent;

import java.util.*;
import java.util.function.Consumer;

/**
 * Gerenciador de renderização otimizado usando TreeMap (Red-Black Tree).
 * Mantém objetos automaticamente ordenados por renderIndex sem necessidade de sort.
 * <p>
 * Inserção/Remoção: O(log n)
 * Iteração: O(n)
 * Mudança de índice: O(log n) + O(log n)
 */
public class RenderableObjectManager {

    /// Valor padrão para os buckets
    private static final int DEFAULT_BUCKET_SIZE = 32;

    /// Árvore que mantém objetos ordenados por renderIndex
    /// Chave = renderIndex, Valor = classe que encapsula o array e o tamanho atual
    private final TreeMap<Integer, ObjectBucket> renderTree;
    private final CullBounds cachedBounds;

    private boolean disposed = false;

    public RenderableObjectManager() {
        this.renderTree = new TreeMap<>();
        this.cachedBounds = new CullBounds();
    }

    /// Adicionamos um objeto da tree
    public void add(RenderAbleObjectII obj) {
        int renderIndex = obj.getRenderIndex();
        ObjectBucket bucket = renderTree.computeIfAbsent(renderIndex, k -> new ObjectBucket());
        bucket.add(obj);
    }

    /// Removemos um objeto da tree
    public void remove(RenderAbleObjectII obj) {
        int renderIndex = obj.getRenderIndex();
        ObjectBucket bucket = renderTree.get(renderIndex);
        if (bucket != null) {
            bucket.remove(obj);
            if (bucket.isEmpty()) {
                renderTree.remove(renderIndex);
            }
        }
    }

    /// Executa a atualização da mudança de index de renderização de um objeto,
    ///  não basta só mudar no objeto precisamos também aplicar aqui dentro,
    ///  fazemos isso para justamente controlar a quantidade de vezes que mudamos a tree
    public void updateRenderIndex(RenderAbleObjectII obj) {
        int newIndex = obj.getRenderIndex();
        int oldIndex = obj.getRenderIndex();

        if (oldIndex == newIndex) return;

        ObjectBucket oldBucket = renderTree.get(oldIndex);
        if (oldBucket != null) {
            oldBucket.remove(obj);
            if (oldBucket.isEmpty()) {
                renderTree.remove(oldIndex);
            }
        }

        ObjectBucket newBucket = renderTree.computeIfAbsent(newIndex, k -> new ObjectBucket());
        newBucket.add(obj);
    }

    /// Executa um código para cada objeto renderizável,
    ///  deve ser usado para chamar o render e atualização de visuais
    public void forEachObject(Consumer<RenderAbleObjectII> action) {
        for (ObjectBucket bucket : renderTree.values()) {
            bucket.forEach(action);
        }
    }

    /// Executa um código para cada objeto renderizável,
    ///  desde que este esteja dentro dos limites passados e cacheados,
    ///  deve ser usado para chamar o render e atualização de visuais
    public void forEachObject(
        Consumer<RenderAbleObjectII> action,
        float camX,
        float camY,
        float viewWidth,
        float viewHeight
    ) {
        updateCullBounds(camX, camY, viewWidth, viewHeight);

        for (ObjectBucket bucket : renderTree.values()) {
            bucket.forEachCulled(action, cachedBounds);
        }
    }

    /// Atualiza as dimensões em buffer
    private void updateCullBounds(float camX, float camY, float viewWidth, float viewHeight) {
        cachedBounds.minX = camX - viewWidth / 2f;
        cachedBounds.maxX = camX + viewWidth / 2f;
        cachedBounds.minY = camY - viewHeight / 2f;
        cachedBounds.maxY = camY + viewHeight / 2f;
    }

    public int size() {
        int count = 0;
        for (ObjectBucket bucket : renderTree.values()) {
            count += bucket.size();
        }
        return count;
    }

    /// Limpa as referencias armazenadas dentro do sistema
    public void clear() {
        //Limpa todos os buckets dentro de renderTree
        for (ObjectBucket bucket : renderTree.values()) {
            bucket.clear();
        }
        //Limpa a tree
        renderTree.clear();
    }

    /// Buffer de bounds de tela
    private static class CullBounds {
        float minX, maxX, minY, maxY;
    }

    /// Realiza um dispose dos dados gráficos gerais
    public void dispose(){
        if(disposed) return;
        //Realiza a limpeza dos gráficos
        forEachObject(RenderAbleObjectII::disposeGraphics);
        //Limpa a lista existente
        clear();
        disposed = true;
    }

    /// Classe interna que gerencia um array de objetos com tamanho dinâmico
    private static class ObjectBucket {
        private RenderAbleObjectII[] items;
        private int size = 0;

        ObjectBucket() {
            this.items = new RenderAbleObjectII[DEFAULT_BUCKET_SIZE];
        }

        /// Adiciona mais objetos no balde
        void add(RenderAbleObjectII obj) {
            if (size == items.length) {
                // Expande o array quando necessário
                RenderAbleObjectII[] newItems = new RenderAbleObjectII[items.length * 2];
                System.arraycopy(items, 0, newItems, 0, items.length);
                this.items = newItems;
            }
            items[size++] = obj;
        }

        /// Remove um objeto do balde
        void remove(RenderAbleObjectII obj) {
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
        void forEach(Consumer<RenderAbleObjectII> action) {
            for (int i = 0; i < size; i++) {
                RenderAbleObjectII obj = items[i];
                if (!obj.canRender()) continue;
                action.accept(obj);

            }
        }

        /// Código a ser executado para todos os objetos dentro de nossa array,
        ///  desde que estejam dentro dos limites de tela,
        ///  com uma pequena margem de folga
        void forEachCulled(
            Consumer<RenderAbleObjectII> action,
            CullBounds bounds
        ) {
            for (int i = 0; i < size; i++) {
                RenderAbleObjectII obj = items[i];
                if (!obj.canRender() || !isInBounds(obj, bounds)) continue;
                action.accept(obj);

            }
        }

        /// Verificamos se o objeto está dentro dos limites da tela
        private boolean isInBounds(RenderAbleObjectII obj, CullBounds bounds) {

            TransformComponent transformC = obj.getTransformC();

            float x = transformC.x;
            float y = transformC.y;
            float width = transformC.width;
            float height = transformC.height;

            float paddingX = width * 1.5f;
            float paddingY = height * 1.5f;

            return !(x + width + paddingX < bounds.minX ||
                x - paddingX > bounds.maxX ||
                y + height + paddingY < bounds.minY ||
                y - paddingY > bounds.maxY);
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
