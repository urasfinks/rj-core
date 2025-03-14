package ru.jamsys.core.resource.filebyte.reader;

import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import ru.jamsys.core.App;
import ru.jamsys.core.extension.ByteTransformer;
import ru.jamsys.core.flat.util.UtilByte;
import ru.jamsys.core.resource.Resource;
import ru.jamsys.core.resource.ResourceArguments;
import ru.jamsys.core.resource.ResourceCheckException;
import ru.jamsys.core.statistic.expiration.mutable.ExpirationMsMutableImpl;

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

@Component
@Scope("prototype")
public class FileByteReaderResource
        extends ExpirationMsMutableImpl
        implements
        Resource<FileByteReaderRequest, List<ByteTransformer>>,
        ResourceCheckException {

    @Override
    public void setArguments(ResourceArguments resourceArguments) throws Throwable {

    }

    @Override
    public List<ByteTransformer> execute(FileByteReaderRequest arguments) {
        List<ByteTransformer> result = new ArrayList<>();
        try (BufferedInputStream fis = new BufferedInputStream(new FileInputStream(arguments.getFilePath()))) {
            while (fis.available() > 0) {
                ByteTransformer item = arguments.getClsItem().getConstructor().newInstance();
                int lenData = UtilByte.bytesToInt(fis.readNBytes(4));
                item.instanceFromByte(fis.readNBytes(lenData));
                result.add(item);
            }
        } catch (Exception e) {
            App.error(e);
        }
        return result;
    }

    @Override
    public boolean isValid() {
        return true;
    }

    @Override
    public Function<Throwable, Boolean> getFatalException() {
        return _ -> false;
    }

    @Override
    public void run() {

    }

    @Override
    public void shutdown() {

    }

}
