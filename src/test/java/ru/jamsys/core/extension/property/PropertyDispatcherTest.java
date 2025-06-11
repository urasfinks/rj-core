package ru.jamsys.core.extension.property;

import lombok.Getter;
import lombok.experimental.FieldNameConstants;
import org.junit.jupiter.api.*;
import org.mockito.MockedStatic;
import ru.jamsys.core.App;
import ru.jamsys.core.component.ServicePromise;
import ru.jamsys.core.component.ServiceProperty;
import ru.jamsys.core.extension.annotation.PropertyDescription;
import ru.jamsys.core.extension.annotation.PropertyKey;
import ru.jamsys.core.extension.annotation.PropertyNotNull;
import ru.jamsys.core.extension.annotation.PropertyValueRegexp;
import ru.jamsys.core.extension.exception.ForwardException;
import ru.jamsys.core.extension.property.repository.AbstractRepositoryProperty;
import ru.jamsys.core.extension.property.repository.RepositoryPropertyAnnotationField;
import ru.jamsys.core.flat.util.UtilLog;

import static org.mockito.Mockito.*;

class PropertyDispatcherTest {

    public static ServicePromise servicePromise;

    static long start;

    @BeforeAll
    static void beforeAll() {
        start = System.currentTimeMillis();
        App.getRunBuilder().addTestArguments().runCore();
        servicePromise = App.get(ServicePromise.class);
    }

    @AfterAll
    static void shutdown() {
        App.shutdown();
        UtilLog.printInfo("Test time: " + (System.currentTimeMillis() - start));
    }

    @SuppressWarnings("all")
    @FieldNameConstants
    @Getter
    public static class Property extends RepositoryPropertyAnnotationField<Object> {

        @PropertyNotNull
        @PropertyKey("method")
        @PropertyDescription("Http method")
        @PropertyValueRegexp("^(GET|POST|PUT|DELETE|HEAD|OPTIONS|PATCH|TRACE|CONNECT)$")
        private volatile String method = "GET";

    }

    @Test
    public void regexpPreinstall() {
        // Пока нет подписчиков это можно сделать
        try {
            App.get(ServiceProperty.class).set("test.method", "GETS");
        } catch (Exception e) {
            Assertions.fail();
        }
        Property property = new Property();
        Assertions.assertThrows(ForwardException.class, () -> new PropertyDispatcher<>(
                null,
                property,
                "test"
        ));
    }

    @Test
    public void updateRegexpRuntime() {
        Property property = new Property();
        PropertyDispatcher<Object> tPropertyDispatcher = new PropertyDispatcher<>(
                null,
                property,
                "test"
        );
        tPropertyDispatcher.run();
        Assertions.assertEquals("GET", property.getMethod());
        tPropertyDispatcher.set(Property.Fields.method, "POST");
        Assertions.assertEquals("POST", property.getMethod());
        Assertions.assertThrows(ForwardException.class, () -> tPropertyDispatcher.set(Property.Fields.method, null));
        Assertions.assertThrows(ForwardException.class, () -> tPropertyDispatcher.set(Property.Fields.method, "GETS"));
        tPropertyDispatcher.shutdown();
    }

    private AbstractRepositoryProperty<String> repositoryProperty;
    private PropertyListener propertyListener;
    private ServiceProperty serviceProperty;
    private PropertyDispatcher<String> dispatcher;

    @BeforeEach
    public void setup() {
        repositoryProperty = mock(AbstractRepositoryProperty.class);
        propertyListener = mock(PropertyListener.class);
        serviceProperty = mock(ServiceProperty.class);

        try (MockedStatic<App> mockedApp = mockStatic(App.class)) {
            mockedApp.when(() -> App.get(ServiceProperty.class)).thenReturn(serviceProperty);
            dispatcher = new PropertyDispatcher<>(propertyListener, repositoryProperty, "ns");
        }
    }

    @Test
    public void testOnPropertyUpdate_updatesRepositoryAndNotifiesListener() {
        // Given
        String propertyKey = "ns.testKey";
        String oldValue = "old";
        String newValue = "new";

        // When
        dispatcher.onPropertyUpdate(propertyKey, oldValue, newValue);

        // Then
        verify(repositoryProperty).updateRepository("testKey", dispatcher);
        verify(propertyListener).onPropertyUpdate("testKey", oldValue, newValue);
    }

    @Test
    public void testGetPropertyKey_withNonEmptyKey() {
        String fullKey = dispatcher.getPropertyKey("key");
        assert (fullKey.equals("ns.key"));
    }

    @Test
    public void testGetRepositoryPropertyKey_withFullKey() {
        String repoKey = dispatcher.getRepositoryPropertyKey("ns.key");
        assert (repoKey.equals("key"));
    }

    @Test
    public void testShutdownOperation_clearsSubscriptions() {
        dispatcher.addSubscriptionRegexp("some.*");
        dispatcher.shutdownOperation();

        verify(serviceProperty, atLeastOnce()).removeSubscription(any());
    }

}