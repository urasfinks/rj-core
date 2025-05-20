package ru.jamsys.core.extension.expiration;

import lombok.Getter;
import lombok.Setter;
import org.jetbrains.annotations.NotNull;
import ru.jamsys.core.component.Core;
import ru.jamsys.core.component.manager.item.log.DataHeader;
import ru.jamsys.core.extension.ManagerElement;
import ru.jamsys.core.statistic.expiration.immutable.DisposableExpirationMsImmutableEnvelope;
import ru.jamsys.core.statistic.expiration.mutable.ExpirationMsMutableImplAbstractLifeCycle;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Function;

// Временная карта - хранит элементы keepAliveOnInactivityMs, далее удаляет
// если в течении этого времени не было обращений к ним.
// В случае обращения таймер сбрасывается опять на keepAliveOnInactivityMs и объект продолжает жить в карте
// Нельзя устанавливать произвольное время для разных элементов, у всех время жизни одно

@Getter
public class ExpirationMap<K, V>
        extends ExpirationMsMutableImplAbstractLifeCycle
        implements
        Map<K, V>,
        ManagerElement {

    private final String key;

    @Getter
    @Setter
    public static class Envelope implements MapRemover {

        private final Object key;

        private Object value;

        private final Map<?, ?> map;

        private DisposableExpirationMsImmutableEnvelope<MapRemover> expiration;

        public Envelope(Object key, Map<?, ?> map) {
            this.key = key;
            this.map = map;
        }

        @Override
        public void remove() {
            map.remove(key);
        }

    }

    private final Map<K, Envelope> mainMap = new ConcurrentHashMap<>(); // Основная карта, в которой хранятся сессионные данные

    public ExpirationMap(String key, int keepAliveOnInactivityMs) {
        this.key = key;
        setKeepAliveOnInactivityMs(keepAliveOnInactivityMs);
    }

    public int size() {
        return mainMap.size();
    }

    @Override
    public V put(K key, V value) {
        Envelope envelope = mainMap.computeIfAbsent(key, k -> new Envelope(k, mainMap));
        if (envelope.getExpiration() != null) {
            envelope.getExpiration().doNeutralized();
        }
        envelope.setValue(value);
        envelope.setExpiration(Core.expirationMapConfiguration.get().add(envelope, getKeepAliveOnInactivityMs()));
        return value;
    }

    @Override
    public V get(Object key) {
        Envelope envelope = mainMap.get(key);
        if (envelope == null) {
            return null;
        }
        if (envelope.getExpiration() != null) {
            envelope.getExpiration().doNeutralized();
        }
        envelope.setExpiration(Core.expirationMapConfiguration.get().add(envelope, getKeepAliveOnInactivityMs()));
        @SuppressWarnings("unchecked")
        V value = (V) envelope.getValue();
        return value;
    }

    public V computeIfAbsent(K key, @NotNull Function<? super K, ? extends V> mappingFunction) {
        Envelope envelope = mainMap.computeIfAbsent(key, k -> {
            Envelope envelope1 = new Envelope(k, mainMap);
            envelope1.setValue(mappingFunction.apply(k));
            return envelope1;
        });
        if (envelope.getExpiration() != null) {
            envelope.getExpiration().doNeutralized();
        }
        envelope.setExpiration(Core.expirationMapConfiguration.get().add(envelope, getKeepAliveOnInactivityMs()));
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
        Envelope envelope = mainMap.remove(key);
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
        Collection<Envelope> values = mainMap.values();
        for (Envelope envelope : values) {
            DisposableExpirationMsImmutableEnvelope<MapRemover> expiration = envelope.getExpiration();
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
                Iterator<Entry<K, Envelope>> internalIterator = mainMap.entrySet().iterator();

                return new Iterator<>() {
                    private Entry<K, Envelope> current;

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
                Iterator<Entry<K, Envelope>> internalIterator = mainMap.entrySet().iterator();

                return new Iterator<>() {
                    private Entry<K, Envelope> current;

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
                for (Envelope envelope : mainMap.values()) {
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
                for (Iterator<Entry<K, Envelope>> it = mainMap.entrySet().iterator(); it.hasNext(); ) {
                    Entry<K, Envelope> entry = it.next();
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
                Iterator<Entry<K, Envelope>> internalIterator = mainMap.entrySet().iterator();

                return new Iterator<>() {
                    private Entry<K, Envelope> current;

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
                                Envelope envelope = current.getValue();
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
                Envelope envelope = mainMap.get(entry.getKey());
                if (envelope == null) return false;
                return Objects.equals(envelope.getValue(), entry.getValue());
            }

            @SuppressWarnings("all")
            @Override
            public boolean remove(Object o) {
                if (!(o instanceof Entry<?, ?> entry)) return false;
                Envelope envelope = mainMap.get(entry.getKey());
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
        for (Envelope envelope : mainMap.values()) {
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
        Envelope envelope = mainMap.get(key);
        return envelope != null ? (V) envelope.getValue() : null;
    }

    public boolean expireNow(K key) {
        return mainMap.remove(key) != null;
    }

}
