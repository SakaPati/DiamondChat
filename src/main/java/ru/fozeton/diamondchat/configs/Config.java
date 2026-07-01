package ru.fozeton.diamondchat.configs;

public interface Config<T> {
        T createDefault();
        boolean isEmpty(T config);
}
