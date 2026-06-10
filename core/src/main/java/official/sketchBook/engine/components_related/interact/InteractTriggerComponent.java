package official.sketchBook.engine.components_related.interact;

import com.badlogic.gdx.math.Vector2;
import official.sketchBook.engine.components_related.intefaces.base_interfaces.Component;
import official.sketchBook.engine.components_related.intefaces.integration_interfaces.object_tree.interaction.HoldInteractableObjectII;
import official.sketchBook.engine.components_related.intefaces.integration_interfaces.object_tree.interaction.InteractableObjectII;
import official.sketchBook.engine.components_related.intefaces.integration_interfaces.object_tree.interaction.InteractionTriggerer;
import official.sketchBook.engine.components_related.intefaces.integration_interfaces.object_tree.interaction.ProximityInteractableObjectII;
import official.sketchBook.engine.components_related.objects.TimerComponent;

import java.util.ArrayList;
import java.util.List;

public class InteractTriggerComponent implements Component {

    /// Objeto dono do componente
    private InteractionTriggerer triggererObject;

    /// Lista de objetos capazes de interagirmos
    private List<InteractableObjectII> interactableList;

    /// Cache do objeto interativo mais próximo
    private InteractableObjectII cachedNearest;

    /// Dirty flag para auxiliar na otimização de search
    private boolean nearestDirty = true;

    /// Estado de hold do timer
    private final TimerComponent holdTimer;

    /// Flag para input
    private boolean holding = false;

    /// Buffer de dado de posição antiga
    private final Vector2 lastTriggererPos = new Vector2();

    public InteractTriggerComponent(InteractionTriggerer triggererObject) {
        this.triggererObject = triggererObject;

        this.interactableList = new ArrayList<>();
        this.holdTimer = new TimerComponent();

        initObject();
    }

    @Override
    public void initObject() {

    }

    // --- Lista ---

    /// Adiciona um objeto e notifica uma atualização
    public void addInteractable(InteractableObjectII interactable) {
        //Se a lista não possuir o objeto a ser mandado
        if (interactableList.contains(interactable)) return;
        //Adicionamos
        interactableList.add(interactable);
        //Marcamos para atualizar a pipeline
        nearestDirty = true;

        //Se for um objeto marcado para interagir com base na proximidade com um triggerer
        if (interactable instanceof ProximityInteractableObjectII)
            //Chamamos para lidar com a entrada na area de interação
            ((ProximityInteractableObjectII) interactable).onTriggererEnter(triggererObject);
    }

    /// Remove um objeto e notifica uma atualização
    public void removeInteractable(InteractableObjectII interactable) {
        //Se o objeto existir e foi removido
        if (!interactableList.remove(interactable)) return;
        //Marcamos para atualizar a pipeline
        nearestDirty = true;
        //Se o que foi removido foi o que estava mais próximo e estávamos segurando para interagir,
        // cancelamos a interação
        if (holding && cachedNearest == interactable) cancelHold();

        //Se for um objeto que pode interagir com base na proximidade, com um triggerer
        if (interactable instanceof ProximityInteractableObjectII)
            //Chamamos para lidar com a saída da area de interação
            ((ProximityInteractableObjectII) interactable).onTriggererExit(triggererObject);
    }

    // --- Input ---

    /// Quando tentamos marcar para interagir com um objeto
    public void onInteractPress() {
        if (holding) return; // já está segurando, ignora

        //Atualizamos o objeto mais próximo
        InteractableObjectII nearest = getNearestInteractable();
        //Se não tivermos um objeto interativo próximo, early exit
        if (nearest == null) return;

        //Se o objeto for um que usa hold para interagir
        if (nearest instanceof HoldInteractableObjectII) {
            //Obtemos o objeto interativo por segurar o botão de interação
            HoldInteractableObjectII hold = (HoldInteractableObjectII) nearest;
            //Se o hold está marcado para também interagir ao apertar para interagir
            //(Além da interação por tempo)
            if (hold.isTriggerInteract()) nearest.interact();
            //Atualizamos o tempo alvo do temporizador
            holdTimer.setTargetTime(hold.getHoldTime());
            //Resetamos e iniciamos o próprio
            holdTimer.reset();
            holdTimer.start();
            //Marcamos a flag de input
            holding = true;
        } else {//Caso o objet for um interativo simples, interagimos logo
            nearest.interact();
        }
    }

    /// Chamamos para quando soltamos o input
    public void onInteractRelease() {
        cancelHold();
    }

    // --- Update ---

    @Override
    public void update(float delta) {
        //Tentamos obter um dado de coordenadas
        Vector2 currentPos = triggererObject.getCoordinatesInMeters();
        //Validamos para tentar descobrir se mudamos de posição
        //Usamos um epsilon para evitar mudanças fracas demais
        if (!currentPos.epsilonEquals(lastTriggererPos, 0.01f)) {
            //Marca para re-validação
            nearestDirty = true;
            //Atualiza o buffer da posição atual
            lastTriggererPos.set(currentPos);
        }

        //Se já estivermos segurando o input, não será necessário passar por essa seção novamente
        if (!holding) return;

        //Tentamos obter o objeto interativo mais próximo
        InteractableObjectII nearest = getNearestInteractable();

        //Se o objeto não for uma isntancia válida necessária para usar o temporizador
        if (!(nearest instanceof HoldInteractableObjectII)) {
            //Cancelamos o hold
            cancelHold();
            //Retornamos para evitar atualização indevída
            return;
        }

        //Atualizamos o temporizador
        holdTimer.update(delta);

        //Caso o tempo para interagir já tiver passado
        if (holdTimer.isFinished()) {
            //Chamamos para interagir
            (
                (HoldInteractableObjectII) nearest
            ).interactOnHold();
            //Cancelamos o hold, já que este cumpriu seu papel
            cancelHold();
        }
    }

    @Override
    public void postUpdate() {
    }

    // --- Nearest ---

    public InteractableObjectII getNearestInteractable() {
        //Se tivemos uma necessidade de recálculo
        if (nearestDirty) {
            //Caso o dirty esteja true tentamos obter uma nova referencia e atualizar ela no buffer
            cachedNearest = computeNearest();
            //Resetamos o dirty pois acabamos se encontrar um válido
            nearestDirty = false;
        }

        //Retornamos o buffer atualizado ou não, dependendo da situação
        return cachedNearest;
    }

    ///Cálcula o objeto interativo próximo
    private static InteractableObjectII computeNearest(
        List<InteractableObjectII> list,
        Vector2 origin
    ) {
        //Early exit para lidar com listas vázias, e evitar alocação de dados real durante runtime
        if(list.isEmpty()) return null;

        //Seta como null para início
        InteractableObjectII nearest = null;
        //Obtemos o valor float mais alto, já que precisamos buscar qual é o mais próximo,
        // sem correr o risco de causar um problema de overflow de bits
        float nearestDist = Float.MAX_VALUE;

        //Para cada objeto da lista de interativos passada
        for (int i = 0; i < list.size(); i++) {

            InteractableObjectII obj = list.get(i);
            //Se o objeto não puder interagir ignoramos ele
            if (!obj.canInteract()) continue;
            //Tentamos obter a distancia usando o "Vector2" presente para obter as coordenadas
            float dist = origin.dst2(obj.getCoordinatesInMeters());

            //Se a distância atual for menor que a distancia do menor fora da lista
            if (dist < nearestDist) {
                //Atualizamos a distancia do menor e o objeto menor
                nearestDist = dist;
                nearest = obj;
            }

        }

        //Retornamos o que pudermos
        return nearest;
    }

    ///Tenta buscar o objeto interativo mais próximo
    private InteractableObjectII computeNearest() {
        return computeNearest(interactableList, triggererObject.getCoordinatesInMeters());
    }

    // --- Helpers ---

    /// Cancelamos o hold state
    private void cancelHold() {
        //Marcamos a flag de input como false
        holding = false;

        //Paramos e resetamos o temporizador
        holdTimer.stop();
        holdTimer.reset();
    }

    @Override
    public void dispose() {
        interactableList.clear();
        interactableList = null;
        triggererObject = null;
        cachedNearest = null;
    }
}
