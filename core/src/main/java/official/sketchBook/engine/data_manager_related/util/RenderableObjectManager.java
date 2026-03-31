package official.sketchBook.engine.data_manager_related.util;

import official.sketchBook.engine.components_related.intefaces.integration_interfaces.util_related.MultiRenderableObjectII;
import official.sketchBook.engine.components_related.intefaces.integration_interfaces.util_related.RenderableObjectII;
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
    public void add(RenderableObjectII obj) {
        int renderIndex = obj.getRenderIndex();
        ObjectBucket bucket = renderTree.computeIfAbsent(renderIndex, k -> new ObjectBucket());
        bucket.add(obj);
    }

    /// Removemos um objeto da tree
    public void remove(RenderableObjectII obj) {
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
    public void updateRenderIndex(RenderableObjectII obj) {
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
    public void forEachObject(Consumer<RenderableObjectII> action) {
        for (ObjectBucket bucket : renderTree.values()) {
            bucket.forEach(action);
        }
    }

    /// Executa um código para cada objeto renderizável,
    ///  desde que este esteja dentro dos limites passados e cacheados,
    ///  deve ser usado para chamar o render e atualização de visuais
    public void forEachObject(
        Consumer<RenderableObjectII> action,
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

                //Verificamos se podemos renderizar o objeto, por validar se está dentro da tela e se pode renderizar,
                // de acordo com sua lógica interna
                if (obj.isInScreen() && obj.canRender()) {
                    action.accept(obj);
                }

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

                if (obj instanceof MultiRenderableObjectII) {
                    obj.setInScreen(
                        isInBounds(
                            (MultiRenderableObjectII) obj,
                            bounds
                        )
                    );
                } else {
                    //Seta o valor do objeto para mostrar que ele está dentro da tela
                    obj.setInScreen(
                        isInBounds(
                            obj,
                            bounds
                        )
                    );
                }

                //Se o objeto puder ser renderizado e estiver dentro da tela
                if (obj.isInScreen() && obj.canRender()) {
                    action.accept(obj);
                }

            }
        }

        /// Verifica se o objeto que pode ser renderizado está com todas suas seções dentro da tela
        private boolean isInBounds(MultiRenderableObjectII object, CullBounds bounds) {
            if(object.getRenderableObjList() == null) return true;

            RenderableObjectII currentObj;

            int renderableCount = object.getRenderableObjList().size();
            int isInsideScreenCount = 0;

            for (int i = 0; i < renderableCount; i++) {
                currentObj = object.getRenderableObjList().get(i);

                currentObj.setInScreen(
                    isInBounds(
                        currentObj,
                        bounds
                    )
                );

                if (currentObj.isInScreen()) isInsideScreenCount++;
            }

            return isInsideScreenCount >= renderableCount;
        }

        /// Verificamos se o objeto está dentro dos limites da tela
        /// Verificamos se o objeto está dentro dos limites da tela
        private boolean isInBounds(RenderableObjectII obj, CullBounds bounds) {

            TransformComponent transformC = obj.getTransformC();

            if (transformC == null) return true;

            float x = transformC.x;
            float y = transformC.y;
            float width = transformC.width;
            float height = transformC.height;

            // --- NOVO: calcular AABB rotacionado ---
            float halfW = width * 0.5f;
            float halfH = height * 0.5f;

            float centerX = x + halfW;
            float centerY = y + halfH;

            float rad = (float) Math.toRadians(transformC.rotation);
            float cos = Math.abs((float) Math.cos(rad));
            float sin = Math.abs((float) Math.sin(rad));

            float rotatedHalfW = halfW * cos + halfH * sin;
            float rotatedHalfH = halfW * sin + halfH * cos;

            float rotatedWidth = rotatedHalfW * 2f;
            float rotatedHeight = rotatedHalfH * 2f;

            // --- mantém exatamente seu comportamento de padding ---
            float paddingX = rotatedWidth * 1.5f;
            float paddingY = rotatedHeight * 1.5f;

            return !(
                centerX + rotatedHalfW + paddingX < bounds.minX ||
                    centerX - rotatedHalfW - paddingX > bounds.maxX ||
                    centerY + rotatedHalfH + paddingY < bounds.minY ||
                    centerY - rotatedHalfH - paddingY > bounds.maxY
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
