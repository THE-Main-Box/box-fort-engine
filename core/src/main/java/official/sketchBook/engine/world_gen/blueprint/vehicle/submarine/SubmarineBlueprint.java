package official.sketchBook.engine.world_gen.blueprint.vehicle.submarine;

import com.badlogic.gdx.physics.box2d.World;
import official.sketchBook.engine.game_object_related.vehicle_related.SubmarineNode;

import java.util.ArrayList;
import java.util.List;

public class SubmarineBlueprint {
    public String tag;
    public List<SubmarineNodeBlueprint> nodes = new ArrayList<>();

    public SubmarineBlueprint() {
    }

    /**
     * spawnX/spawnY é o ponto de origem do submarino inteiro — cada node
     * soma seu próprio offsetX/Y a este mesmo ponto (não encadeado entre
     * nodes). Não persistido em nenhum lugar: puramente parâmetro de
     * chamada, decidido por quem está spawnando o sub (save state, ou
     * spawn manual no editor/teste).
     */
    public List<SubmarineNode> toSubmarineNodes(
        World physicsWorld,
        float spawnX,
        float spawnY
    ) {
        List<SubmarineNode> result = new ArrayList<>(nodes.size());

        for (int i = 0; i < nodes.size(); i++) {
            result.add(nodes.get(i)
                .toSubmarineNode(
                    physicsWorld,
                    spawnX,
                    spawnY
                )
            );
        }

        return result;
    }
}
