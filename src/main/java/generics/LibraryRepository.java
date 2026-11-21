package generics;

import java.io.Serializable;
import java.util.*;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * Generic In-Memory Repository (Lectures 47-48 Generics).
 * Demonstrates:
 * - Generic class with bounded type parameter <T extends Serializable>
 * - Type safety
 * - Diamond operator
 * - Generic methods
 * - Upper-bounded wildcards (<? extends T>)
 */
public class LibraryRepository<T extends Serializable> {

    private final Map<Integer, T> storage = new HashMap<>();

    public LibraryRepository() {
    }

    public void save(int id, T item) {
        if (item == null) {
            throw new IllegalArgumentException("Cannot save null item into generic repository.");
        }
        storage.put(id, item);
    }

    public Optional<T> findById(int id) {
        return Optional.ofNullable(storage.get(id));
    }

    public List<T> findAll() {
        return new ArrayList<>(storage.values());
    }

    public boolean deleteById(int id) {
        return storage.remove(id) != null;
    }

    public int count() {
        return storage.size();
    }

    public void clear() {
        storage.clear();
    }

    /**
     * Demonstrates Upper-Bounded Wildcard (<? extends T>).
     * Accepts a list of any type that extends T and stores them.
     */
    public void addAll(List<? extends T> items, Function<? super T, Integer> idExtractor) {
        for (T item : items) {
            Integer id = idExtractor.apply(item);
            if (id != null) {
                storage.put(id, item);
            }
        }
    }

    /**
     * Demonstrates a Generic Method with custom type parameter <R>.
     */
    public <R> List<R> transform(Function<T, R> mapper) {
        return storage.values().stream()
                .map(mapper)
                .collect(Collectors.toList());
    }

    /**
     * Demonstrates filtering with Predicate.
     */
    public List<T> filter(Predicate<T> condition) {
        return storage.values().stream()
                .filter(condition)
                .collect(Collectors.toList());
    }
}
