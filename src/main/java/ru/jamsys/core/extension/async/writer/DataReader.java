package ru.jamsys.core.extension.async.writer;

public interface DataReader {

    void add(DataReadWrite dataReadWrite);

    default DataReadWrite add(long position, byte[] bytes, Object object) {
        DataReadWrite dataReadWrite = new DataReadWrite(position, bytes, object);
        add(dataReadWrite);
        return dataReadWrite;
    }

    void setFinishState(boolean finishState);

    void setError(boolean error);

}
