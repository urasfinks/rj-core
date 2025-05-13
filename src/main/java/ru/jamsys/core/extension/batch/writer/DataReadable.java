package ru.jamsys.core.extension.batch.writer;

public interface DataReadable {

    void add(DataPayload dataPayload);

    default DataPayload add(long position, byte[] bytes, Object object) {
        DataPayload dataPayload = new DataPayload(position, bytes, object);
        add(dataPayload);
        return dataPayload;
    }

    void setFinishState(boolean finishState);

    void setError(boolean error);

}
