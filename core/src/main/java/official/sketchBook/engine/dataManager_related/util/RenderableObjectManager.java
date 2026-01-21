package official.sketchBook.engine.dataManager_related.util;

import official.sketchBook.engine.components_related.intefaces.integration_interfaces.util_related.RenderAbleObjectII;

import java.util.*;
import java.util.function.Consumer;

/**
 * Gerenciador de renderização otimizado usando TreeMap (Red-Black Tree).
 * Mantém objetos automaticamente ordenados por renderIndex sem necessidade de sort.
 *
 * Inserção/Remoção: O(log n)
 * Iteração: O(n)
 * Mudança de índice: O(log n) + O(log n)
 */
public class RenderableObjectManager {

    /// Árvore que mantém objetos ordenados por renderIndex
    /// Chave = renderIndex, Valor = lista de objetos com mesmo índice
    private final TreeMap<Integer, List<RenderAbleObjectII>> renderTree;

    /// Mapa auxiliar para rastrear o renderIndex atual de cada objeto
    private final HashMap<RenderAbleObjectII, Integer> objectIndexMap;

    /// Buffer reutilizável para atualização visual (evita alocação nova toda frame)
    private final List<RenderAbleObjectII> updateBuffer;

    public RenderableObjectManager() {
        this.renderTree = new TreeMap<>();
        this.objectIndexMap = new HashMap<>();
        this.updateBuffer = new ArrayList<>();
    }

    /// Adiciona um objeto renderizável à árvore
    public void add(RenderAbleObjectII obj) {
        //Obtém o índice de renderização
        int renderIndex = obj.getRenderIndex();

        //Obtém ou cria a lista para esse índice
        List<RenderAbleObjectII> list = renderTree.computeIfAbsent(renderIndex, k -> new ArrayList<>());

        //Adiciona o objeto à lista (O(1))
        list.add(obj);

        //Rastreia o índice atual do objeto
        objectIndexMap.put(obj, renderIndex);
    }

    /// Remove um objeto renderizável da árvore
    public void remove(RenderAbleObjectII obj) {
        //Obtém o índice anterior do objeto
        Integer previousIndex = objectIndexMap.remove(obj);
        if (previousIndex == null) return;

        //Obtém a lista para esse índice
        List<RenderAbleObjectII> list = renderTree.get(previousIndex);
        if (list != null) {
            //Remove da lista
            list.remove(obj);

            //Se a lista ficou vazia, remove a entrada da árvore
            if (list.isEmpty()) {
                renderTree.remove(previousIndex);
            }
        }
    }

    /// Atualiza o índice de renderização de um objeto
    /// Chamamos quando queremos aplicar a nova render index de um objeto renderizável nele
    public void updateRenderIndex(RenderAbleObjectII obj) {
        //Obtém o índice antigo
        Integer oldIndex = objectIndexMap.get(obj);
        //Obtém o novo índice
        int newIndex = obj.getRenderIndex();

        //Se não mudou, não faz nada
        if (oldIndex != null && oldIndex == newIndex) {
            return;
        }

        //Remove com índice antigo
        remove(obj);
        //Adiciona com novo índice
        add(obj);
    }

    /// Itera sobre todos os objetos em ordem de renderIndex para atualização visual
    /// Usa buffer reutilizável para evitar alocação nova
    public void forEachForUpdate(Consumer<RenderAbleObjectII> action) {
        for (List<RenderAbleObjectII> list : renderTree.values()) {
            for (int i = list.size() - 1; i >= 0; i--) {
                RenderAbleObjectII obj = list.get(i);
                if (obj.canRender()) {
                    action.accept(obj);
                }
            }
        }
    }


    /// Itera sobre todos os objetos em ordem de renderIndex (menor para maior) para renderização
    public void forEachForRender(Consumer<RenderAbleObjectII> action) {
        for (List<RenderAbleObjectII> list : renderTree.values()) {
            for (int i = list.size() - 1; i >= 0; i--) {
                RenderAbleObjectII obj = list.get(i);
                if (obj.canRender()) {
                    action.accept(obj);
                }
            }
        }
    }


    /// Retorna o número total de objetos na árvore
    public int size() {
        int count = 0;
        for (List<RenderAbleObjectII> list : renderTree.values()) {
            count += list.size();
        }
        return count;
    }

    /// Limpa todos os objetos
    public void clear() {
        renderTree.clear();
        objectIndexMap.clear();
        updateBuffer.clear();
    }
}
