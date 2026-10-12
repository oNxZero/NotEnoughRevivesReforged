package com.termi.notenoughrevives.config;

import com.google.gson.*;
import com.termi.notenoughrevives.util.AtomicFiles;
import java.lang.reflect.Modifier;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.function.Consumer;
import java.util.function.Supplier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Preserves unknown settings and never overwrites an unreadable/invalid user file. */
public final class JsonConfigStore<T> {
    private static final Logger LOGGER = LoggerFactory.getLogger("not_enough_revives");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private final Path path;
    private final Class<T> type;
    private final Supplier<T> defaults;
    private final Consumer<T> validator;
    private JsonObject document = new JsonObject();
    private boolean writable = true;

    public JsonConfigStore(Path path, Class<T> type, Supplier<T> defaults, Consumer<T> validator) {
        this.path = path;
        this.type = type;
        this.defaults = defaults;
        this.validator = validator;
    }

    public T load() {
        T result = defaults.get();
        if (Files.exists(path)) {
            try {
                if (Files.size(path) > 1024 * 1024) throw new JsonParseException("Config exceeds 1 MiB");
                JsonElement parsed = JsonParser.parseString(Files.readString(path));
                if (!parsed.isJsonObject()) throw new JsonParseException("Expected a JSON object");
                document = parsed.getAsJsonObject();
                for (var field : type.getFields()) {
                    if (Modifier.isStatic(field.getModifiers()) || !document.has(field.getName())) continue;
                    JsonElement value = document.get(field.getName());
                    if (value.isJsonNull()) continue; // validator supplies safe string defaults
                    if (!value.isJsonPrimitive()) throw new JsonParseException("Invalid setting: " + field.getName());
                    JsonPrimitive primitive = value.getAsJsonPrimitive();
                    Class<?> fieldType = field.getType();
                    boolean valid = fieldType == boolean.class ? primitive.isBoolean()
                            : fieldType == String.class ? primitive.isString() : primitive.isNumber();
                    if (!valid) throw new JsonParseException("Wrong setting type: " + field.getName());
                    if (fieldType == int.class) primitive.getAsBigDecimal().intValueExact();
                }
                result = GSON.fromJson(document, type);
            } catch (Exception error) {
                writable = false;
                LOGGER.error("Cannot read {}. Using defaults without overwriting the file; correct it and restart.", path, error);
                try {
                    Path backup = path.resolveSibling(path.getFileName() + ".invalid-" + System.currentTimeMillis());
                    Files.copy(path, backup);
                    LOGGER.warn("Preserved invalid config at {}", backup);
                } catch (Exception backupError) {
                    LOGGER.error("Could not back up {}; original file is still untouched", path, backupError);
                }
                result = defaults.get();
            }
        }
        validator.accept(result);
        if (writable) save(result);
        return result;
    }

    public void save(T config) {
        if (!writable) return;
        validator.accept(config);
        if (type == NERConfig.class) {
            for (String obsolete : new String[]{"allowCustomSoundUploads", "customSoundUploadCooldownSeconds", "allowPitchChange", "maxHelpSoundDistance", "selfReviveItem"}) document.remove(obsolete);
        }
        JsonObject values = GSON.toJsonTree(config).getAsJsonObject();
        values.entrySet().forEach(entry -> document.add(entry.getKey(), entry.getValue()));
        try {
            AtomicFiles.write(path, (GSON.toJson(document) + "\n").getBytes(StandardCharsets.UTF_8));
        } catch (Exception error) {
            LOGGER.error("Could not save config {}", path, error);
        }
    }
}
