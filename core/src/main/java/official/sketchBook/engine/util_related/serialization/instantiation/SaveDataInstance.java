package official.sketchBook.engine.util_related.serialization.instantiation;

import official.sketchBook.engine.util_related.exceptions.SaveDataException;

public abstract class SaveDataInstance<T> {

    /**
     * Extrai o estado atual do objeto de jogo J� EXISTENTE como
     * SaveData ? chamado na hora de salvar. Espelha, no sentido
     * inverso, o que {@link #loadFields} faz.
     */
    public abstract SaveData save(T instance);

    /**
     * Popula os campos do PR�PRIO DTO a partir de um SaveData bruto ?
     * chamado ANTES de newInstance, logo ap�s o DTO ser criado a
     * partir do JSON. Separado de executeInstantiation de prop�sito:
     * ler campos (pode falhar por dado corrompido, via
     * getXxxRequired/SaveDataException) � uma preocupa��o diferente de
     * construir o objeto de jogo (pode falhar por motivo de
     * engine/f�sica).
     */
    public abstract void loadFields(SaveData data);

    /**
     * Contexto vem já instanciado com dados padrão
     * Aqui dentro passamos os dados obtidos no {@link #loadFields(SaveData)}
     * já que aqui apenas recebemos dados padrão já passados no context
     */
    protected abstract T executeInstantiation() throws SaveDataException;

    /**
     * Ponto de entrada usado por quem carrega um save: tenta construir
     * T a partir do DTO + contexto. Nunca deixa uma exce��o de
     * executeInstantiation vazar crua ? sempre reempacotada como
     * SaveDataException, com o nome da classe do DTO como contexto,
     * pra quem l� o erro saber exatamente qual objeto falhou sem
     * vasculhar stacktrace.
     */
    public final T newInstance() {
        try {
            return executeInstantiation();
        } catch (SaveDataException e) {
            // j� � um erro de dado estruturado ? s� acumula mais um
            // n�vel de contexto antes de repassar.
            throw e.withContext(getClass().getSimpleName());
        } catch (Exception e) {
            // qualquer outra falha (NPE de contexto incompleto, erro
            // de engine/f�sica etc) vira SaveDataException tamb�m ?
            // quem chama s� precisa tratar UM tipo de exce��o.
            throw new SaveDataException(
                "instantiation",
                "falha ao instanciar via " + getClass().getSimpleName() + ": " + e.getMessage()
            );
        }
    }
}
