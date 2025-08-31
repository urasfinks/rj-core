package ru.jamsys.core.extension.expiration;

import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import ru.jamsys.core.App;
import ru.jamsys.core.component.manager.ManagerConfiguration;
import ru.jamsys.core.extension.AbstractManagerElement;
import ru.jamsys.core.extension.builder.HashMapBuilder;
import ru.jamsys.core.extension.statistic.StatisticDataHeader;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Function;

// Временная карта - хранит элементы keepAliveOnInactivityMs, далее удаляет
// если в течении этого времени не было обращений к ним.
// В случае обращения таймер сбрасывается опять на keepAliveOnInactivityMs и объект продолжает жить в карте
// Нельзя устанавливать произвольное время для разных элементов, у всех время жизни одно

@Getter
public class ExpirationMap<K, V> extends AbstractManagerElement implements Map<K, V> {

    private final String ns;

    private final String key;

    public ManagerConfiguration<ExpirationList<EnvelopeObject>> expirationMapConfiguration;

    private final Map<K, EnvelopeObject> mainMap = new ConcurrentHashMap<>(); // Основная карта, в которой хранятся сессионные данные

    private int timeoutElementExpirationMs = 6_000;

    public ExpirationMap(String ns, String key) {
        this.ns = ns;
        this.key = key;
        expirationMapConfiguration = ManagerConfiguration.getInstance(
                ExpirationList.class,
                App.getUniqueClassName(ExpirationMap.class), // Это общий ExpirationList для всех экземпляров ExpirationMap
                App.getUniqueClassName(ExpirationMap.class), // Это общий ExpirationList для всех экземпляров ExpirationMap
                expirationMapExpirationObjectExpirationList -> expirationMapExpirationObjectExpirationList
                        .setupOnExpired(EnvelopeObject::remove)
        );
    }

    public void setupTimeoutElementExpirationMs(int timeoutElementExpirationMs) {
        this.timeoutElementExpirationMs = timeoutElementExpirationMs;
    }

    @JsonValue
    public Object getJsonValue() {
        return new HashMapBuilder<String, Object>()
                .append("hashCode", Integer.toHexString(hashCode()))
                .append("cls", getClass())
                .append("ns", ns)
                .append("key", key)
                .append("size", size())
                ;
    }

    public int size() {
        return mainMap.size();
    }

    @Override
    public V put(K key, V value) {
        V remove = remove(key);
        EnvelopeObject envelope = new EnvelopeObject(key, value, mainMap);
        envelope.updateExpiration(expirationMapConfiguration.get(), timeoutElementExpirationMs);
        mainMap.put(key, envelope);
        return remove;
    }

    @Override
    public V get(Object key) {
        return get(key, true);
    }

    public V get(Object key, boolean updateExpiration) {
        EnvelopeObject envelope = mainMap.get(key);
        if (envelope == null) {
            return null;
        }
        if (updateExpiration) {
            envelope.updateExpiration(expirationMapConfiguration.get(), timeoutElementExpirationMs);
        }
        @SuppressWarnings("unchecked")
        V value = (V) envelope.getValue();
        return value;
    }

    public V computeIfAbsent(K key, @NotNull Function<? super K, ? extends V> mappingFunction) {
        EnvelopeObject envelope = mainMap.computeIfAbsent(
                key,
                k -> new EnvelopeObject(k, mappingFunction.apply(k), mainMap)
        );
        envelope.updateExpiration(expirationMapConfiguration.get(), timeoutElementExpirationMs);
        @SuppressWarnings("unchecked")
        V value = (V) envelope.getValue();
        return value;
    }

    // Смысл remove ExpirationMap и ExpirationList совершенно разный, если ExpirationList.remove() создан для того,
    // что бы не вызвался onDrop, так как операция была выполнена (к примеру),
    // то тут ExpirationMap.remove() нет никакой логики onDrop, потому что удаление по времени это просто очистка
    // памяти, не связанная с бизнес-логикой.
    @Override
    public V remove(Object key) {
        EnvelopeObject envelope = mainMap.remove(key);
        if (envelope == null) {
            return null;
        }
        if (envelope.getExpiration() != null) {
            envelope.getExpiration().doNeutralized();
        }
        envelope.remove();
        @SuppressWarnings("unchecked")
        V value = (V) envelope.getValue();
        return value;
    }

    @Override
    public void putAll(@NotNull Map<? extends K, ? extends V> m) {
        m.forEach(this::put);
    }

    @Override
    public void clear() {
        Collection<EnvelopeObject> values = mainMap.values();
        for (EnvelopeObject envelope : values) {
            if (envelope.getExpiration() != null) {
                envelope.getExpiration().doNeutralized();
            }
            envelope.remove();
        }
        mainMap.clear();
    }

    @NotNull
    @Override
    public Set<K> keySet() {
        return new AbstractSet<>() {
            @NotNull
            @Override
            public Iterator<K> iterator() {
                Iterator<Entry<K, EnvelopeObject>> internalIterator = mainMap.entrySet().iterator();

                return new Iterator<>() {
                    private Entry<K, EnvelopeObject> current;

                    @Override
                    public boolean hasNext() {
                        return internalIterator.hasNext();
                    }

                    @Override
                    public K next() {
                        current = internalIterator.next();
                        return current.getKey();
                    }

                    @Override
                    public void remove() {
                        if (current == null) {
                            throw new IllegalStateException("next() must be called before remove()");
                        }
                        internalIterator.remove();
                        current = null;
                    }
                };
            }

            @Override
            public int size() {
                return mainMap.size();
            }

            @SuppressWarnings("all")
            @Override
            public boolean contains(Object o) {
                return mainMap.containsKey(o);
            }

            @Override
            public boolean remove(Object o) {
                return ExpirationMap.this.remove(o) != null;
            }

            @Override
            public void clear() {
                ExpirationMap.this.clear();
            }
        };
    }


    @NotNull
    @Override
    public Collection<V> values() {
        return new AbstractCollection<>() {
            @NotNull
            @Override
            public Iterator<V> iterator() {
                Iterator<Entry<K, EnvelopeObject>> internalIterator = mainMap.entrySet().iterator();

                return new Iterator<>() {
                    private Entry<K, EnvelopeObject> current;

                    @Override
                    public boolean hasNext() {
                        return internalIterator.hasNext();
                    }

                    @SuppressWarnings("unchecked")
                    @Override
                    public V next() {
                        current = internalIterator.next();
                        return (V) current.getValue().getValue();
                    }

                    @Override
                    public void remove() {
                        if (current == null) {
                            throw new IllegalStateException("next() must be called before remove()");
                        }
                        internalIterator.remove();
                        current = null;
                    }
                };
            }

            @Override
            public int size() {
                return mainMap.size();
            }

            @Override
            public boolean contains(Object o) {
                for (EnvelopeObject envelope : mainMap.values()) {
                    if (Objects.equals(envelope.getValue(), o)) {
                        return true;
                    }
                }
                return false;
            }

            @Override
            public void clear() {
                ExpirationMap.this.clear();
            }

            @Override
            public boolean remove(Object o) {
                for (Iterator<Entry<K, EnvelopeObject>> it = mainMap.entrySet().iterator(); it.hasNext(); ) {
                    Entry<K, EnvelopeObject> entry = it.next();
                    if (Objects.equals(entry.getValue().getValue(), o)) {
                        it.remove();
                        return true;
                    }
                }
                return false;
            }
        };
    }


    @NotNull
    @Override
    public Set<Entry<K, V>> entrySet() {
        return new AbstractSet<>() {

            @NotNull
            @Override
            public Iterator<Entry<K, V>> iterator() {
                Iterator<Entry<K, EnvelopeObject>> internalIterator = mainMap.entrySet().iterator();

                return new Iterator<>() {
                    private Entry<K, EnvelopeObject> current;

                    @Override
                    public boolean hasNext() {
                        return internalIterator.hasNext();
                    }

                    @Override
                    public Entry<K, V> next() {
                        current = internalIterator.next();
                        return new Map.Entry<>() {

                            @Override
                            public K getKey() {
                                return current.getKey();
                            }

                            @SuppressWarnings("unchecked")
                            @Override
                            public V getValue() {
                                return (V) current.getValue().getValue();
                            }

                            @SuppressWarnings("unchecked")
                            @Override
                            public V setValue(V value) {
                                EnvelopeObject envelope = current.getValue();
                                V old = (V) envelope.getValue();
                                envelope.setValue(value);
                                return old;
                            }

                            @Override
                            public boolean equals(Object o) {
                                if (!(o instanceof Entry<?, ?> other)) return false;
                                return Objects.equals(getKey(), other.getKey()) &&
                                        Objects.equals(getValue(), other.getValue());
                            }

                            @Override
                            public int hashCode() {
                                return Objects.hashCode(getKey()) ^ Objects.hashCode(getValue());
                            }
                        };
                    }

                    @Override
                    public void remove() {
                        if (current == null) {
                            throw new IllegalStateException("next() must be called before remove()");
                        }
                        internalIterator.remove();
                        current = null;
                    }
                };
            }

            @Override
            public int size() {
                return mainMap.size();
            }

            @Override
            public boolean isEmpty() {
                return mainMap.isEmpty();
            }

            @Override
            public void clear() {
                ExpirationMap.this.clear();
            }

            @SuppressWarnings("all")
            @Override
            public boolean contains(Object o) {
                if (!(o instanceof Entry<?, ?> entry)) return false;
                EnvelopeObject envelope = mainMap.get(entry.getKey());
                if (envelope == null) return false;
                return Objects.equals(envelope.getValue(), entry.getValue());
            }

            @SuppressWarnings("all")
            @Override
            public boolean remove(Object o) {
                if (!(o instanceof Entry<?, ?> entry)) return false;
                EnvelopeObject envelope = mainMap.get(entry.getKey());
                if (envelope == null) return false;
                if (Objects.equals(envelope.getValue(), entry.getValue())) {
                    ExpirationMap.this.remove(entry.getKey());
                    return true;
                }
                return false;
            }
        };
    }

    @Override
    public boolean isEmpty() {
        return mainMap.isEmpty();
    }

    @Override
    public boolean containsKey(Object key) {
        return mainMap.containsKey(key);
    }

    @Override
    public boolean containsValue(Object value) {
        for (EnvelopeObject envelope : mainMap.values()) {
            if (Objects.equals(envelope.getValue(), value)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void runOperation() {

    }

    @Override
    public void shutdownOperation() {
        clear();
    }

    public List<StatisticDataHeader> flushAndGetStatistic(AtomicBoolean threadRun) {
        List<StatisticDataHeader> result = new ArrayList<>();
        result.add(new StatisticDataHeader(getClass(), ns)
                .addHeader("size", size())
        );
        return result;
    }

    @Override
    public void helper() {
        if (!mainMap.isEmpty()) {
            markActive();
        }
    }

}
