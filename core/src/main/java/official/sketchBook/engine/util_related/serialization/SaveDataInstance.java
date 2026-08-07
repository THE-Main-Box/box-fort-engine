package official.sketchBook.engine.util_related.serialization;
/**
 * Classe base de todo DTO/intermedi�rio do sistema de save.
 * <p>
 * Padr�o validado externamente (ver OlegDzhuraev/SaveSystem, Unity):
 * cada objeto "salv�vel" do jogo (Player, SubmarineNode, ...) tem um
 * DTO correspondente 1:1, sem tipos complexos dentro ? s� dados
 * planos. Esse DTO � o que de fato passa pelo registry/JSON, NUNCA a
 * classe de jogo em si.
 * <p>
 * A parte que n�o vem de nenhuma refer�ncia externa, e resolve o
 * problema real do projeto (Player/SubmarineNode precisam de
 * World/room pra existir, um DTO puro n�o): {@link #newInstance}
 * recebe um contexto {@code C} (o que for necess�rio pra construir o
 * objeto de verdade) e delega pra {@link #executeInstantiation}, que �
 * o �nico m�todo que cada subclasse precisa implementar de fato. O
 * try/catch fica centralizado AQUI, uma vez s�, ent�o nenhuma
 * subclasse esquece de tratar falha de constru��o.
 * <p>
 * T = a classe de jogo final (Player, SubmarineNode).
 * C = o contexto necess�rio pra constru�-la (pode ser um record
 * pr�prio, ou {@code Void} quando a classe n�o precisa de nada
 * externo).
 */
public abstract class SaveDataInstance<T, C> {

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
     * Ponto de extens�o real: como construir T a partir do estado
     * atual do DTO (j� populado via loadFields) mais o contexto
     * externo recebido. Pode lan�ar livremente ? newInstance cerca
     * isso com try/catch.
     */
    protected abstract T executeInstantiation(C context);

    /**
     * Ponto de entrada usado por quem carrega um save: tenta construir
     * T a partir do DTO + contexto. Nunca deixa uma exce��o de
     * executeInstantiation vazar crua ? sempre reempacotada como
     * SaveDataException, com o nome da classe do DTO como contexto,
     * pra quem l� o erro saber exatamente qual objeto falhou sem
     * vasculhar stacktrace.
     */
    public final T newInstance(C context) {
        try {
            return executeInstantiation(context);
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
