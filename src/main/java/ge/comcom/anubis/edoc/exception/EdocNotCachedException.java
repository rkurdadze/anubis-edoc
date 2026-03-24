package ge.comcom.anubis.edoc.exception;

import java.util.UUID;

public class EdocNotCachedException extends RuntimeException {

    public EdocNotCachedException(UUID id) {
        super("Документ " + id + " не найден в локальном кэше. Используйте POST /fetch для загрузки с удалённого сервера.");
    }
}
