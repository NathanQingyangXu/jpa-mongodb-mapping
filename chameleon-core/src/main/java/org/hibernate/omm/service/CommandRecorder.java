package org.hibernate.omm.service;

import java.util.List;
import org.bson.BsonDocument;
import org.hibernate.generator.BeforeExecutionGenerator;
import org.hibernate.service.Service;

/**
 * A {@link Service} mainly used for testing purpose.
 * <p/>
 * Sometimes we need to verify on real command issued to Mongo,
 * e.g. verifying {@link BeforeExecutionGenerator}.
 *
 * @author Nathan Xu
 * @since 1.0.0
 */
public interface CommandRecorder extends Service {

    /**
     * Record a complete Bson command issued to Mongo
     *
     * @param command a de-parameterized BSON command
     */
    void record(BsonDocument command);

    /**
     * Return all recorded commands.
     * @return previous accumulated commands
     */
    List<BsonDocument> getCommandsRecorded();

    /**
     * empty recorded commands
     */
    void clearCommandsRecorded();
}
