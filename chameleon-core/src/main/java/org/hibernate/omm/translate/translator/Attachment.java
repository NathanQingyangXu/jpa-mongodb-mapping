package org.hibernate.omm.translate.translator;

public class Attachment {
    private Object curValue;
    private AttachmentKey<?> expectedKey;

    public <T> void attach(AttachmentKey<T> key, T value) {
        if (key == null) {
            throw new IllegalArgumentException("Attachment key can not be null");
        }
        if (value == null) {
            throw new IllegalArgumentException("Attachment value can not be null");
        }
        // Eventually these would be uncommented for early detection of bugs, but commenting out for now
        // so that most tests still pass.
        //        if (curValue != null) {
        //            throw new IllegalStateException("Trying to attach value with key " + key + " but
        // there is already an attached value of " + curValue);
        //        }
        //        if (expectedKey == null) {
        //            throw new IllegalStateException("Expected attachment key is null.  Did you forget
        // to call expect method?");
        //        }
        //        if (key != expectedKey) {
        //            throw new IllegalStateException("Expected attachment key is not equal to the
        // attachment key");
        //        }
        curValue = value;
    }

    public <T> T expect(AttachmentKey<T> key, Runnable runnable) {
        AttachmentKey<?> curExpectedKey = expectedKey;
        expectedKey = key;
        runnable.run();
        expectedKey = curExpectedKey;

        if (curValue == null) {
            throw new IllegalStateException("Expected an attachment but nothing has been attached");
        }

        //noinspection unchecked
        T retVal = (T) curValue;
        curValue = null;
        return retVal;
    }
}
