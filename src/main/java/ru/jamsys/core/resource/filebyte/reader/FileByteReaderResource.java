package ru.jamsys.core.resource.filebyte.reader;

import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import ru.jamsys.core.App;
import ru.jamsys.core.component.ExceptionHandler;
import ru.jamsys.core.extension.ByteItem;
import ru.jamsys.core.flat.util.UtilByte;
import ru.jamsys.core.resource.Resource;
import ru.jamsys.core.resource.balancer.algorithm.BalancerAlgorithm;
import ru.jamsys.core.statistic.expiration.mutable.ExpirationMsMutableImpl;

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.List;

@Component
@Scope("prototype")
public class FileByteReaderResource
        extends ExpirationMsMutableImpl
        implements Resource<Void, FileByteReaderRequest, List<ByteItem>> {

    @Override
    public void constructor(Void constructor) throws Throwable {

    }

    @Override
    public List<ByteItem> execute(FileByteReaderRequest arguments) {
        List<ByteItem> result = new ArrayList<>();
        try (BufferedInputStream fis = new BufferedInputStream(new FileInputStream(arguments.getFilePath()))) {
            while (fis.available() > 0) {
                ByteItem item = arguments.getClsItem().getConstructor().newInstance();
                int lenData = UtilByte.bytesToShort(fis.readNBytes(4));
                item.instanceFromByte(fis.readNBytes(lenData));
                result.add(item);
            }
        } catch (Exception e) {
            App.context.getBean(ExceptionHandler.class).handler(e);
        }
        return result;
    }

    @Override
    public void close() {

    }

    @Override
    public int getWeight(BalancerAlgorithm balancerAlgorithm) {
        return 0;
    }

}
