package org.hibernate.omm.service;

import java.util.ArrayList;
import java.util.List;
import org.bson.BsonDocument;

/**
 * A simple {@link CommandRecorder} implementation. Not thread-safe.
 *
 * @author Nathan Xu
 * @since 1.0.0
 */
public class DefaultCommandRecorderImpl implements CommandRecorder {

    public static final DefaultCommandRecorderImpl INSTANCE = new DefaultCommandRecorderImpl();

    private DefaultCommandRecorderImpl() {}

    private final List<BsonDocument> records = new ArrayList<>();

    @Override
    public void record(final BsonDocument command) {
        records.add(command);
    }

    @Override
    public List<BsonDocument> getCommandsRecorded() {
        return records;
    }

    @Override
    public void clearCommandsRecorded() {
        this.records.clear();
    }
}
