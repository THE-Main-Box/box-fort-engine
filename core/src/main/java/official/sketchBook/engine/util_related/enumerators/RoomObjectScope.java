package official.sketchBook.engine.util_related.enumerators;

public enum RoomObjectScope {
    /// É removido do gerenciador de objetos ao mudar de sala, pois é local a sala apenas
    LOCAL,
    /// Por ser global, poderá ser transferido de sala em sala
    GLOBAL
}
