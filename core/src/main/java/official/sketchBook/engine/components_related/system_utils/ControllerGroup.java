package official.sketchBook.engine.components_related.system_utils;

import official.sketchBook.engine.components_related.intefaces.integration_interfaces.object_tree.interaction.ControllableObjectII;
import official.sketchBook.engine.components_related.intefaces.integration_interfaces.object_tree.interaction.WirableConfigurable;
import official.sketchBook.engine.components_related.intefaces.integration_interfaces.object_tree.interaction.WiringConfig;

import java.util.ArrayList;
import java.util.IdentityHashMap;

public class ControllerGroup {

    /// Nome do grupo para identifica��o no HUD
    public final String name;

    /// Lista de control�veis do grupo
    private final ArrayList<ControllableObjectII> targets;

    /// Map de configs por objeto — um objeto s� pode ter uma config por grupo
    /// IdentityHashMap usa refer�ncia direta — sem equals(), sem autoboxing
    private final IdentityHashMap<ControllableObjectII, WiringConfig> configMap;

    public ControllerGroup(String name) {
        this.name = name;

        this.targets = new ArrayList<>();
        this.configMap = new IdentityHashMap<>();
    }

    /// Adiciona um objeto ao grupo.
    /// Se for WirableConfigurable, registra sua config padr�o automaticamente.
    public void add(ControllableObjectII object) {
        if (object == null) return; //Se existir um objeto
        targets.add(object);        //Adicionamos na lista de targets

        // Registra a config padr�o caso o objeto seja configur�vel
        if (object instanceof WirableConfigurable) {
            //Obtém a config do objeto atual
            WiringConfig defaultConfig = ((WirableConfigurable) object).getCurrentConfiguration();
            if (defaultConfig != null) {        //Se o retorno for real
                //Adicionamos a config
                configMap.put(object, defaultConfig);
            }
        }
    }

    /// Atualiza a config de um objeto neste grupo.
    /// Usa o map diretamente — O(1) via refer�ncia, sem iterar a lista.
    public void setConfig(ControllableObjectII object, WiringConfig config) {
        //Verificamos se ele pode ser configurado, se sim prosseguimos
        if (object instanceof WirableConfigurable)
            configMap.put(object, config);
    }

    /// Aciona todos os objetos do grupo.
    /// Se configur�vel e houver config no map, aplica antes de acionar.
    public void trigger() {
        //Percorremos a lista de objetos controláveis
        for (int i = 0; i < targets.size(); i++) {
            //obtemos o controlável alvo
            ControllableObjectII target = targets.get(i);

            //Se for um objeto configurável settamos dados presentes aqui
            if (target instanceof WirableConfigurable) {
                //Obtemos a configuração presente
                WiringConfig config = configMap.get(target);
                //Se houver settamos
                if (config != null) {
                    //Atualizamos a config atual conforme desejado
                    ((WirableConfigurable) target).setCurrentConfiguration(config);
                }
            }

            //Chamamos a interação
            target.interactByWiring();
        }
    }

    /// Removemos um objeto controlável
    /// Removemos a config caso exista
    public void remove(ControllableObjectII object) {
        targets.remove(object);
        configMap.remove(object);
    }

    public void clear() {
        targets.clear();
        configMap.clear();
    }

    public boolean isEmpty() {
        return targets.isEmpty();
    }

    public WiringConfig getConfig(ControllableObjectII object) {
        return configMap.get(object);
    }

    public ArrayList<ControllableObjectII> getTargets() {
        return targets;
    }
}
