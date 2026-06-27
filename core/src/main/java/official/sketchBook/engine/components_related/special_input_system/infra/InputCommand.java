package official.sketchBook.engine.components_related.special_input_system.infra;

public interface InputCommand <T>{
    ///Nome para contexto
    String getName();
    ///Descrição para uso futuro
    String getDescription();

    ///Obtém o valor de input da classe
    T getValue();

    ///Determina o valor de input passado
    void setValue(T newValue);

    InputCommandType getType();
}
