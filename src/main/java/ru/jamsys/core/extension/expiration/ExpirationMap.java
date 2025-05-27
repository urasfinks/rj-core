package ru.jamsys.core.extension.expiration;

import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import ru.jamsys.core.component.manager.ManagerConfiguration;
import ru.jamsys.core.extension.AbstractManagerElement;
import ru.jamsys.core.extension.builder.HashMapBuilder;
import ru.jamsys.core.extension.expiration.immutable.DisposableExpirationMsImmutableEnvelope;
import ru.jamsys.core.extension.log.DataHeader;

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

    private final String key;

    public ManagerConfiguration<ExpirationList<ExpirationMapExpirationObject>> expirationMapConfiguration;

    private final Map<K, ExpirationMapExpirationObject> mainMap = new ConcurrentHashMap<>(); // Основная карта, в которой хранятся сессионные данные

    public ExpirationMap(String key) {
        this.key = key;
        expirationMapConfiguration = ManagerConfiguration.getInstance(
                ExpirationList.class,
                ExpirationMap.class.getName(), // Это общий ExpirationList для всех экземпляров ExpirationMap
                expirationMapExpirationObjectExpirationList -> expirationMapExpirationObjectExpirationList
                        .setupOnExpired(ExpirationMapExpirationObject::remove)
        );
    }

    public void setupTimeoutMs(int keepAliveOnInactivityMs) {
        setKeepAliveOnInactivityMs(keepAliveOnInactivityMs);
    }

    @JsonValue
    public Object getValue() {
        return new HashMapBuilder<String, Object>()
                .append("hashCode", Integer.toHexString(hashCode()))
                .append("cls", getClass())
                .append("key", key)
                .append("size", size())
                ;
    }

    public int size() {
        return mainMap.size();
    }

    @Override
    public V put(K key, V value) {
        ExpirationMapExpirationObject envelope = mainMap.computeIfAbsent(key, k -> new ExpirationMapExpirationObject(k, mainMap));
        if (envelope.getExpiration() != null) {
            envelope.getExpiration().doNeutralized();
        }
        envelope.setValue(value);
        envelope.setExpiration(expirationMapConfiguration.get().add(envelope, getKeepAliveOnInactivityMs()));
        return value;
    }

    @Override
    public V get(Object key) {
        ExpirationMapExpirationObject envelope = mainMap.get(key);
        if (envelope == null) {
            return null;
        }
        if (envelope.getExpiration() != null) {
            envelope.getExpiration().doNeutralized();
        }
        envelope.setExpiration(expirationMapConfiguration.get().add(envelope, getKeepAliveOnInactivityMs()));
        @SuppressWarnings("unchecked")
        V value = (V) envelope.getValue();
        return value;
    }

    public V computeIfAbsent(K key, @NotNull Function<? super K, ? extends V> mappingFunction) {
        ExpirationMapExpirationObject envelope = mainMap.computeIfAbsent(key, k -> {
            ExpirationMapExpirationObject envelope1 = new ExpirationMapExpirationObject(k, mainMap);
            envelope1.setValue(mappingFunction.apply(k));
            return envelope1;
        });
        if (envelope.getExpiration() != null) {
            envelope.getExpiration().doNeutralized();
        }
        envelope.setExpiration(expirationMapConfiguration.get().add(envelope, getKeepAliveOnInactivityMs()));
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
        ExpirationMapExpirationObject envelope = mainMap.remove(key);
        if (envelope == null) {
            return null;
        }
        if (envelope.getExpiration() != null) {
            envelope.getExpiration().doNeutralized();
        }
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
        Collection<ExpirationMapExpirationObject> values = mainMap.values();
        for (ExpirationMapExpirationObject envelope : values) {
            DisposableExpirationMsImmutableEnvelope<?> expiration = envelope.getExpiration();
            if (expiration != null) {
                expiration.doNeutralized();
            }
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
                Iterator<Entry<K, ExpirationMapExpirationObject>> internalIterator = mainMap.entrySet().iterator();

                return new Iterator<>() {
                    private Entry<K, ExpirationMapExpirationObject> current;

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
                Iterator<Entry<K, ExpirationMapExpirationObject>> internalIterator = mainMap.entrySet().iterator();

                return new Iterator<>() {
                    private Entry<K, ExpirationMapExpirationObject> current;

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
                for (ExpirationMapExpirationObject envelope : mainMap.values()) {
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
                for (Iterator<Entry<K, ExpirationMapExpirationObject>> it = mainMap.entrySet().iterator(); it.hasNext(); ) {
                    Entry<K, ExpirationMapExpirationObject> entry = it.next();
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
                Iterator<Entry<K, ExpirationMapExpirationObject>> internalIterator = mainMap.entrySet().iterator();

                return new Iterator<>() {
                    private Entry<K, ExpirationMapExpirationObject> current;

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
                                ExpirationMapExpirationObject envelope = current.getValue();
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
                ExpirationMapExpirationObject envelope = mainMap.get(entry.getKey());
                if (envelope == null) return false;
                return Objects.equals(envelope.getValue(), entry.getValue());
            }

            @SuppressWarnings("all")
            @Override
            public boolean remove(Object o) {
                if (!(o instanceof Entry<?, ?> entry)) return false;
                ExpirationMapExpirationObject envelope = mainMap.get(entry.getKey());
                if (envelope == null) return false;
                if (Objects.equals(envelope.getValue(), entry.getValue())) {
                    mainMap.remove(entry.getKey());
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
        for (ExpirationMapExpirationObject envelope : mainMap.values()) {
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

    public List<DataHeader> flushAndGetStatistic(AtomicBoolean threadRun) {
        List<DataHeader> result = new ArrayList<>();
        result.add(new DataHeader()
                .setBody(key)
                .addHeader("size", size())
        );
        return result;
    }

    @SuppressWarnings("all")
    public V peek(Object key) {
        ExpirationMapExpirationObject envelope = mainMap.get(key);
        return envelope != null ? (V) envelope.getValue() : null;
    }

    public boolean expireNow(K key) {
        return mainMap.remove(key) != null;
    }

}
