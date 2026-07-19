package official.sketchBook.engine.util_related.contact_listener.listeners;

import com.badlogic.gdx.physics.box2d.Body;
import com.badlogic.gdx.physics.box2d.Contact;
import com.badlogic.gdx.physics.box2d.ContactImpulse;
import com.badlogic.gdx.physics.box2d.Manifold;
import com.badlogic.gdx.utils.Array;
import official.sketchBook.engine.components_related.intefaces.integration_interfaces.object_tree.vehicle.VehiclePassenger;
import official.sketchBook.engine.game_object_related.vehicle_related.Vehicle;
import official.sketchBook.engine.game_object_related.vehicle_related.VehicleSection;
import official.sketchBook.engine.util_related.contact_listener.MultiContactListener;
import official.sketchBook.engine.util_related.enumerators.ObjectType;
import official.sketchBook.engine.util_related.helper.GameObjectTag;

import static official.sketchBook.engine.util_related.helper.body.BodyTagHelper.getFixtureTagFromContact;

public class VehicleContactListener implements MultiContactListener.SubContactListener {

    @Override
    public void beginContact(Contact contact, GameObjectTag tagA, GameObjectTag tagB) {
        handle(contact, tagA, tagB, true);
    }

    @Override
    public void endContact(Contact contact, GameObjectTag tagA, GameObjectTag tagB) {
        handle(contact, tagA, tagB, false);
    }

    @Override
    public void preSolve(Contact contact, Manifold oldManifold, GameObjectTag tagA, GameObjectTag tagB) {
    }

    @Override
    public void postSolve(Contact contact, ContactImpulse impulse, GameObjectTag tagA, GameObjectTag tagB) {
    }

    private void handle(Contact contact, GameObjectTag tagA, GameObjectTag tagB, boolean entering) {
        if (!tryHandle(contact, tagA, tagB, entering)) {
            tryHandle(contact, tagB, tagA, entering);
        }
    }

    private boolean tryHandle(Contact contact, GameObjectTag sectionTag, GameObjectTag passengerTag, boolean entering) {
        if (sectionTag == null || passengerTag == null) return false;

        // Validamos o tipo da tag pra garantir o tipo correto
        if (sectionTag.type != ObjectType.VEHICLE) return false;

        // Se não for uma seção de um veiculo
        if (!(sectionTag.owner instanceof VehicleSection)) return false;

        // Se ele tiver area interna
        if (!((VehicleSection) sectionTag.owner).hasInternalArea()) return false;

        // O passageiro precisa ser um VehiclePassenger
        if (!(passengerTag.owner instanceof VehiclePassenger)) return false;

        // Verificamos se a fixture que colidiu é o sensor de área seca
        GameObjectTag fixtureTag = getFixtureTagFromContact(contact);
        if (fixtureTag == null || fixtureTag.type != ObjectType.DYNAMIC_INTERNAL_AREA) return false;

        VehiclePassenger passenger = (VehiclePassenger) passengerTag.owner;
        VehicleSection section = (VehicleSection) sectionTag.owner;

        if (entering) {
            VehicleSection previousSection = passenger.getVehiclePassengerPhysicsC().getCurrentSection();

            passenger.onVehicleEnter(section);

            // onSectionChanged cobre tanto entrada real no veículo (previousSection
            // == null) quanto troca de node dentro do mesmo veículo, cujo beginContact
            // do node novo às vezes chega antes do endContact do node antigo.
            passenger.onSectionChanged(previousSection, section);
            return true;
        }

        // --- Saída ---
        //
        // Trocar de node dentro do MESMO veículo dispara endContact do sensor antigo
        // (às vezes antes, às vezes depois do beginContact do sensor novo, sem ordem
        // garantida entre fixtures adjacentes do mesmo Body). Se disparássemos
        // onVehicleExit incondicionalmente aqui, o passageiro seria tratado como tendo
        // saído do veículo inteiro só por atravessar a fronteira entre duas seções.
        //
        // Antes de confirmar a saída, verificamos se o Body do passageiro ainda tem
        // ALGUM outro contato tocando um sensor DYNAMIC_INTERNAL_AREA pertencente ao
        // MESMO veículo (não necessariamente o mesmo node) — se tiver, ele só trocou
        // de seção internamente. onVehicleExit/onVehicleEnter do veículo como um todo
        // não disparam nesse caso, mas onSectionChanged já foi disparado no
        // beginContact do node novo acima (se esse já tiver ocorrido) — então aqui só
        // absorvemos o endContact silenciosamente, sem nada a fazer.
        if (isStillInsideSameVehicle(passenger, section.getVehicle(), contact)) {
            return true;
        }

        VehicleSection currentSection = passenger.getVehiclePassengerPhysicsC().getCurrentSection();

        passenger.onVehicleExit(section);

        // Saída real do veículo: newSection = null.
        passenger.onSectionChanged(currentSection, null);
        return true;
    }

    /// Verifica se o Body do passageiro ainda está tocando algum sensor de área interna
    /// de QUALQUER seção do mesmo veículo, exceto o contato que está sendo finalizado
    /// agora.
    ///
    /// NOTA: Body.getContactList() (com ContactEdge) não existe na API pública padrão
    /// do LibGDX Box2D (com.badlogic.gdx.physics.box2d.*) — isso é da variante
    /// GWT/jbox2d. Na API padrão (bindings nativos), o único jeito de listar contatos
    /// é via World.getContactList(), que retorna TODOS os contatos ativos do mundo —
    /// por isso filtramos manualmente pelos que envolvem o Body do passageiro.
    private boolean isStillInsideSameVehicle(VehiclePassenger passenger, Vehicle vehicle, Contact endingContact) {
        if (vehicle == null) return false;

        Body passengerBody = passenger.getBody();
        if (passengerBody == null) return false;

        Array<Contact> allContacts = passengerBody.getWorld().getContactList();

        for (int i = 0; i < allContacts.size; i++) {
            Contact otherContact = allContacts.get(i);

            if (otherContact == null || otherContact == endingContact) continue;
            if (!otherContact.isTouching()) continue;

            Body bodyA = otherContact.getFixtureA().getBody();
            Body bodyB = otherContact.getFixtureB().getBody();

            // Só nos interessam contatos que envolvem o Body do passageiro
            if (bodyA != passengerBody && bodyB != passengerBody) continue;

            if (belongsToSameVehicleInternalArea(otherContact, vehicle)) {
                return true;
            }
        }

        return false;
    }

    /// Confere se um contato ativo é entre o sensor DYNAMIC_INTERNAL_AREA de uma seção
    /// do veículo informado e qualquer outro corpo (o passageiro, presumidamente, já
    /// que estamos iterando a partir do Body dele).
    private boolean belongsToSameVehicleInternalArea(Contact contact, Vehicle vehicle) {
        GameObjectTag fixtureTag = getFixtureTagFromContact(contact);
        if (fixtureTag == null || fixtureTag.type != ObjectType.DYNAMIC_INTERNAL_AREA) return false;

        Object fixtureBodyTagAOwner = contact.getFixtureA().getBody().getUserData();
        Object fixtureBodyTagBOwner = contact.getFixtureB().getBody().getUserData();

        return isSameVehicleSectionOwner(fixtureBodyTagAOwner, vehicle)
            || isSameVehicleSectionOwner(fixtureBodyTagBOwner, vehicle);
    }

    private boolean isSameVehicleSectionOwner(Object bodyUserData, Vehicle vehicle) {
        if (!(bodyUserData instanceof GameObjectTag)) return false;

        GameObjectTag tag = (GameObjectTag) bodyUserData;
        if (tag.type != ObjectType.VEHICLE) return false;
        if (!(tag.owner instanceof VehicleSection)) return false;

        VehicleSection section = (VehicleSection) tag.owner;
        return section.getVehicle() == vehicle;
    }
}
