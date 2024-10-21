package org.hibernate.omm.id;

import java.util.EnumSet;
import org.bson.types.ObjectId;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.generator.BeforeExecutionGenerator;
import org.hibernate.generator.EventType;

public class ObjectIdGenerator implements BeforeExecutionGenerator {

    @Override
    public ObjectId generate(
            final SharedSessionContractImplementor session,
            final Object owner,
            final Object currentValue,
            final EventType eventType) {
        return ObjectId.get();
    }

    @Override
    public EnumSet<EventType> getEventTypes() {
        return EnumSet.of(EventType.INSERT);
    }
}
