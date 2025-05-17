package ru.jamsys.core.extension.async.writer;

public interface DataFromFile {

    void add(DataPayload dataPayload);

    default DataPayload add(long position, byte[] bytes, Object object) {
        DataPayload dataPayload = new DataPayload(position, bytes, object);
        add(dataPayload);
        return dataPayload;
    }

    void setFinishState(boolean finishState);

    void setError(boolean error);

}
