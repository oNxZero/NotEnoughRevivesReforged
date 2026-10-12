import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.zip.ZipFile;

public final class PatchVerification {
    public static void main(String[] args) throws Exception {
        Set<String> replaced = new HashSet<>(Set.of(
            "com/termi/notenoughrevives/NotEnoughRevives.class",
            "com/termi/notenoughrevives/NotEnoughRevivesClient.class",
            "com/termi/notenoughrevives/manager/KnockedManager.class",
            "com/termi/notenoughrevives/config/NERConfig.class",
            "com/termi/notenoughrevives/client/hud/NERHudRenderer.class",
            "com/termi/notenoughrevives/mixin/ServerPlayerEntityMixin.class",
            "not_enough_revives.mixins.json", "META-INF/MANIFEST.MF", "META-INF/neoforge.mods.toml", "pack.mcmeta"));
        try (var sources = java.nio.file.Files.walk(Path.of(args[3]))) {
            sources.filter(path -> path.toString().endsWith(".java")).forEach(path -> replaced.add(
                    Path.of(args[3]).relativize(path).toString().replace(java.io.File.separatorChar, '/').replace(".java", ".class")));
        }
        Set<String> removed = new HashSet<>(java.nio.file.Files.readAllLines(Path.of("gradle/removed-legacy-entries.txt")));
        replaced.add("assets/not_enough_revives/sounds.json");
        String recipe = "data/not_enough_revives/recipe/cross_of_revival.json";
        try (ZipFile original = new ZipFile(args[0]); ZipFile patched = new ZipFile(args[1])) {
            Set<String> names = new HashSet<>();
            var entries = patched.entries();
            while (entries.hasMoreElements()) {
                var entry = entries.nextElement();
                if (!names.add(entry.getName())) throw new AssertionError("Duplicate: " + entry.getName());
            }
            for (String name : replaced) {
                if (name.endsWith(".class") && patched.getEntry(name) == null) throw new AssertionError("Missing replacement class: " + name);
            }
            for (String removedEntry : removed) if (patched.getEntry(removedEntry) != null) throw new AssertionError("Legacy sound/upload entry remains: " + removedEntry);
            if (patched.getEntry(recipe) != null) throw new AssertionError("Self-revive recipe remains");
            var originals = original.entries();
            int preserved = 0;
            while (originals.hasMoreElements()) {
                var entry = originals.nextElement();
                if (entry.isDirectory() || removed.contains(entry.getName()) || replaced.contains(entry.getName()) || recipe.equals(entry.getName())) continue;
                var copy = patched.getEntry(entry.getName());
                if (copy == null || !Arrays.equals(original.getInputStream(entry).readAllBytes(), patched.getInputStream(copy).readAllBytes())) {
                    throw new AssertionError("Unexpected original change: " + entry.getName());
                }
                preserved++;
            }
            System.out.println("Verified " + preserved + " original entries preserved byte-for-byte; no recipe or duplicates.");
        }
        URL[] urls = Arrays.stream(args[2].split(java.io.File.pathSeparator)).filter(s -> !Path.of(s).equals(Path.of(args[0])))
            .map(s -> { try { return Path.of(s).toUri().toURL(); } catch (Exception e) { throw new RuntimeException(e); } }).toArray(URL[]::new);
        try (URLClassLoader serverLoader = new URLClassLoader(urls, ClassLoader.getPlatformClassLoader()) {
            @Override protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
                if (name.startsWith("net.minecraft.client.") || name.startsWith("com.termi.notenoughrevives.client.")
                        || name.equals("com.termi.notenoughrevives.NotEnoughRevivesClient")) {
                    throw new ClassNotFoundException("Client class forbidden: " + name);
                }
                return super.loadClass(name, resolve);
            }
        }) {
            for (String name : new String[]{"NotEnoughRevives", "manager.KnockedManager", "config.NERConfig",
                    "DownedInteractionEvents", "config.JsonConfigStore", "data.DownedStateSavedData", "mixin.DownedInteractionMixin", "mixin.DownedFinishMixin", "mixin.DownedServerInputMixin", "mixin.ServerPlayerEntityMixin"}) {
                Class<?> type = Class.forName("com.termi.notenoughrevives." + name, false, serverLoader);
                type.getDeclaredMethods();
                type.getDeclaredFields();
            }
            for (String[] target : new String[][]{
                    {"net.minecraft.server.level.ServerPlayerGameMode", "destroyBlock", "useItem", "useItemOn"},
                    {"net.minecraft.server.level.ServerPlayer", "hurt", "die"},
                    {"net.minecraft.server.network.ServerGamePacketListenerImpl", "handlePlayerAction", "handleContainerClick", "handleContainerButtonClick"}}) {
                Class<?> type = Class.forName(target[0], false, serverLoader);
                Set<String> methods = new HashSet<>();
                for (var method : type.getDeclaredMethods()) methods.add(method.getName());
                for (int index = 1; index < target.length; index++) {
                    if (!methods.contains(target[index])) throw new AssertionError("Missing mixin target: " + target[0] + "." + target[index]);
                }
            }
            System.out.println("Verified all new mixin target methods exist in Minecraft 1.21.1.");
            System.out.println("Common class linkage passed with client classes forbidden (not a full server launch).");
        }
    }
}
