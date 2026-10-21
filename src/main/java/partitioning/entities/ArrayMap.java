package partitioning.entities;
import java.util.*;

public class ArrayMap<K, V> extends AbstractMap<K, V> {
    private Object[] keys;
    private Object[] values;
    private int size = 0;
    private static final int DEFAULT_CAPACITY = 4;

    public ArrayMap() {
        keys = new Object[DEFAULT_CAPACITY];
        values = new Object[DEFAULT_CAPACITY];
    }

    @Override
    public V put(K key, V value) {
        // Проверяем, есть ли уже такой ключ (линейный поиск)
        for (int i = 0; i < size; i++) {
            if (Objects.equals(keys[i], key)) {
                V oldValue = (V) values[i];
                values[i] = value;
                return oldValue;
            }
        }
        
        // Если массив заполнен, увеличиваем его
        if (size == keys.length) {
            keys = Arrays.copyOf(keys, keys.length * 2);
            values = Arrays.copyOf(values, values.length * 2);
        }

        keys[size] = key;
        values[size] = value;
        size++;
        return null;
    }

    @Override
    public V get(Object key) {
        for (int i = 0; i < size; i++) {
            if (Objects.equals(keys[i], key)) {
                return (V) values[i];
            }
        }
        return null;
    }

    @Override 
    public V remove(Object key) {
        for (int i = 0; i < size; i++) {
            if (Objects.equals(keys[i], key)) {
                V oldValue = (V) values[i];
                
                // Берем последний элемент и ставим его на место удаленного
                keys[i] = keys[size - 1];
                values[i] = values[size - 1];
                
                // Очищаем бывшее место последнего элемента
                size--;
                keys[size] = null;
                values[size] = null;
                
                return oldValue;
            }
        }
        return null;
    }


    @Override
    public int size() {
        return size;
    }

    @Override
    public Set<Entry<K, V>> entrySet() {
        // Необходимый метод для реализации AbstractMap
        Set<Entry<K, V>> set = new LinkedHashSet<>();
        for (int i = 0; i < size; i++) {
            set.add(new SimpleEntry<>((K)keys[i], (V)values[i]));
        }
        return set;
    }
}
