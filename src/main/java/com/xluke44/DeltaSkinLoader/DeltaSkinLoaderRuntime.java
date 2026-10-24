package com.xluke44.DeltaSkinLoader;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.AbstractClientPlayer;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.client.renderer.texture.ITextureObject;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.client.resources.IReloadableResourceManager;
import net.minecraft.client.resources.IResourceManager;
import net.minecraft.client.resources.IResourceManagerReloadListener;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.FileTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

@SideOnly(Side.CLIENT)
public final class DeltaSkinLoaderRuntime {

    private static final Object LOCK = new Object();
    private static final long MAX_PNG_BYTES = 16L * 1024L * 1024L;
    private static final String RESOURCE_NAMESPACE = "deltaskinloader";
    private static final ResourceLocation DEFAULT_STEVE = new ResourceLocation("minecraft", "textures/entity/steve.png");
    private static final ResourceLocation DEFAULT_ALEX = new ResourceLocation("minecraft", "textures/entity/alex.png");

    private static final Map<String, PlayerData> PLAYER_DATA_CACHE = new HashMap<String, PlayerData>();
    private static final Map<String, FileSnapshot> PLAYER_DATA_FILE_SNAPSHOTS = new HashMap<String, FileSnapshot>();
    private static final Map<String, TextureEntry> TEXTURES = new HashMap<String, TextureEntry>();
    private static final Set<String> INVALID_ASSET_WARNINGS = new HashSet<String>();

    private static Path rootDirectory;
    private static Path configFile;
    private static ModConfig config = ModConfig.defaults();
    private static FileSnapshot configFileSnapshot;
    private static boolean initialized;
    private static boolean reloadListenerRegistered;
    private static boolean lifecycleListenerRegistered;

    private DeltaSkinLoaderRuntime() {
    }

    public static void initialize(Path forgeConfigDirectory) {
        synchronized (LOCK) {
            rootDirectory = forgeConfigDirectory.resolve("DeltaSkinLoader");
            configFile = rootDirectory.resolve("config.json");
            try {
                Files.createDirectories(rootDirectory);
                Files.createDirectories(rootDirectory.resolve("playerdata"));
                Files.createDirectories(rootDirectory.resolve("skins"));
                Files.createDirectories(rootDirectory.resolve("capes"));
                Files.createDirectories(rootDirectory.resolve("elytras"));
                installBundledConfigIfMissing();
                config = readMainConfig();
                configFileSnapshot = snapshot(configFile);
                initialized = true;
            } catch (IOException exception) {
                config = ModConfig.defaults();
                configFileSnapshot = snapshot(configFile);
                initialized = true;
            }
        }
    }

    public static void registerReloadListener() {
        synchronized (LOCK) {
            if (reloadListenerRegistered) {
                return;
            }
            try {
                IResourceManager manager = Minecraft.getMinecraft().getResourceManager();
                if (manager instanceof IReloadableResourceManager) {
                    ((IReloadableResourceManager) manager).registerReloadListener(new IResourceManagerReloadListener() {
                        @Override
                        public void onResourceManagerReload(IResourceManager resourceManager) {
                            reloadActiveTextures();
                        }
                    });
                    reloadListenerRegistered = true;
                }
            } catch (RuntimeException ignored) {
            }
        }
    }

    public static void registerWorldLifecycleListener() {
        synchronized (LOCK) {
            if (!lifecycleListenerRegistered) {
                MinecraftForge.EVENT_BUS.register(new LifecycleListener());
                lifecycleListenerRegistered = true;
            }
        }
    }

    public static ResourceLocation getSkinOverride(AbstractClientPlayer player) {
        if (player == null || !initialized || rootDirectory == null) {
            return null;
        }
        refreshConfigIfChanged();
        String playerName = getPlayerName(player);
        if (!isUsablePlayerName(playerName)) {
            return null;
        }

        PlayerData data = getPlayerData(playerName);
        Path localSkin = getAssetPath("skins", playerName);
        boolean skinFilePresent = Files.isRegularFile(localSkin);

        if (skinFilePresent) {
            if (data != null && data.valid && data.enabled && config.enableSkinOverride) {
                return loadLocalTexture("skin", playerName, localSkin, false);
            }
            return null;
        }

        removeTexture(getTextureLocation("skin", playerName));
        if (!config.enableSkinOverride) {
            return null;
        }
        NoSkinSettings settings = settingsFor(playerName);
        if (!settings.enabled) {
            return null;
        }
        String skinPreset = effectiveNoSkinSkin(playerName, settings, data);
        return getPresetSkin(skinPreset);
    }

    public static ResourceLocation getSkinReturnOverride(AbstractClientPlayer player, ResourceLocation skin) {
        if (player == null || skin == null || !initialized || rootDirectory == null) {
            return skin;
        }
        refreshConfigIfChanged();
        if (!config.enableSkinOverride || !config.customDefaultSkins) {
            return skin;
        }

        String defaultPreset;
        if (DEFAULT_ALEX.equals(skin)) {
            defaultPreset = "alex";
        } else if (DEFAULT_STEVE.equals(skin)) {
            defaultPreset = "steve";
        } else {
            return skin;
        }

        String playerName = getPlayerName(player);
        if (!isUsablePlayerName(playerName)) {
            return skin;
        }

        if (Files.isRegularFile(getAssetPath("skins", playerName))) {
            return skin;
        }

        NoSkinSettings settings = settingsFor(playerName);
        if (settings.enabled) {
            PlayerData data = getPlayerData(playerName);
            String skinPreset = effectiveNoSkinSkin(playerName, settings, data);
            if ("none".equals(canonicalSkin(skinPreset, "none"))) {
                return skin;
            }
        }

        return getPresetSkin(defaultPreset);
    }

    public static ResourceLocation getCapeOverride(AbstractClientPlayer player) {
        if (player == null || !initialized || rootDirectory == null) {
            return null;
        }
        refreshConfigIfChanged();
        if (!config.enableCape) {
            return null;
        }
        String playerName = getPlayerName(player);
        if (!isUsablePlayerName(playerName)) {
            return null;
        }
        PlayerData data = getPlayerData(playerName);
        if (data == null || !data.valid || !data.enabled) {
            return null;
        }
        return loadLocalTexture("cape", playerName, getAssetPath("capes", playerName), false);
    }

    public static ResourceLocation getElytraOverride(AbstractClientPlayer player) {
        if (player == null || !initialized || rootDirectory == null) {
            return null;
        }
        refreshConfigIfChanged();
        if (!config.enableCustomElytra) {
            return null;
        }
        String playerName = getPlayerName(player);
        if (!isUsablePlayerName(playerName)) {
            return null;
        }
        PlayerData data = getPlayerData(playerName);
        if (data == null || !data.valid || !data.enabled) {
            return null;
        }
        return loadLocalTexture("elytra", playerName, getAssetPath("elytras", playerName), false);
    }

    public static String getModelOverride(AbstractClientPlayer player) {
        if (player == null || !initialized || rootDirectory == null) {
            return null;
        }
        refreshConfigIfChanged();
        String playerName = getPlayerName(player);
        if (!isUsablePlayerName(playerName)) {
            return null;
        }

        PlayerData data = getPlayerData(playerName);
        boolean skinFilePresent = Files.isRegularFile(getAssetPath("skins", playerName));
        if (skinFilePresent) {
            if (data != null && data.valid && data.enabled) {
                return toMinecraftModelKey(data.model);
            }
            return null;
        }

        NoSkinSettings settings = settingsFor(playerName);
        if (!settings.enabled) {
            return null;
        }
        String model = data != null && data.valid && data.enabled ? data.model : settings.model;
        return toMinecraftModelKey(model);
    }

    private static ResourceLocation getPresetSkin(String skinPreset) {
        String canonical = canonicalSkin(skinPreset, "none");
        if ("none".equals(canonical)) {
            return null;
        }

        boolean slim = "alex".equals(canonical);
        ResourceLocation vanilla = slim ? DEFAULT_ALEX : DEFAULT_STEVE;
        if (!config.customDefaultSkins || rootDirectory == null) {
            return vanilla;
        }

        String customName = slim ? "custom_s" : "custom_c";
        Path customFile = rootDirectory.resolve("skins").resolve(customName + ".png");
        if (!Files.isRegularFile(customFile)) {
            return vanilla;
        }

        ResourceLocation customTexture = loadLocalTexture("defaultskin", customName, customFile, false);
        return customTexture != null ? customTexture : vanilla;
    }

    private static String getPlayerName(AbstractClientPlayer player) {
        try {
            return player.getGameProfile() == null ? null : player.getGameProfile().getName();
        } catch (RuntimeException exception) {
            return null;
        }
    }

    private static boolean isUsablePlayerName(String playerName) {
        if (playerName == null || playerName.length() < 1 || playerName.length() > 16) {
            return false;
        }
        for (int i = 0; i < playerName.length(); i++) {
            char value = playerName.charAt(i);
            if (!((value >= 'a' && value <= 'z') || (value >= 'A' && value <= 'Z')
                    || (value >= '0' && value <= '9') || value == '_')) {
                return false;
            }
        }
        return true;
    }

    private static boolean isSelfPlayer(String playerName) {
        try {
            String sessionName = Minecraft.getMinecraft().getSession().getUsername();
            return sessionName != null && sessionName.equalsIgnoreCase(playerName);
        } catch (RuntimeException exception) {
            return false;
        }
    }

    private static NoSkinSettings settingsFor(String playerName) {
        return isSelfPlayer(playerName) ? config.noSkinSelf : config.noSkinOthers;
    }

    private static String effectiveNoSkinSkin(String playerName, NoSkinSettings settings, PlayerData data) {
        if (!isSelfPlayer(playerName) && settings.enabled) {
            return settings.skin;
        }
        return data != null && data.valid && data.enabled ? data.skin : settings.skin;
    }

    private static Path getAssetPath(String directory, String playerName) {
        return rootDirectory.resolve(directory).resolve(playerName + ".png");
    }

    private static PlayerData getPlayerData(String playerName) {
        synchronized (LOCK) {
            PlayerData cached = PLAYER_DATA_CACHE.get(playerName);
            if (cached != null) {
                return cached;
            }
            if (rootDirectory == null) {
                return null;
            }

            Path directory = rootDirectory.resolve("playerdata");
            Path file = directory.resolve(playerName + ".json");
            try {
                Files.createDirectories(directory);
                if (!Files.exists(file)) {
                    createPlayerDataOnce(file, playerName);
                }
                FileSnapshot loadedSnapshot = snapshot(file);
                PlayerData loaded = readPlayerDataFile(file, playerName);
                PLAYER_DATA_CACHE.put(playerName, loaded);
                PLAYER_DATA_FILE_SNAPSHOTS.put(playerName, loadedSnapshot);
                return loaded;
            } catch (IOException exception) {
                PlayerData unavailable = PlayerData.invalid(settingsFor(playerName));
                PLAYER_DATA_CACHE.put(playerName, unavailable);
                PLAYER_DATA_FILE_SNAPSHOTS.put(playerName, snapshot(file));
                return unavailable;
            }
        }
    }

    private static void refreshCachedPlayerData() {
        synchronized (LOCK) {
            if (rootDirectory == null || PLAYER_DATA_CACHE.isEmpty()) {
                return;
            }
            List<String> names = new ArrayList<String>(PLAYER_DATA_CACHE.keySet());
            for (String playerName : names) {
                Path file = rootDirectory.resolve("playerdata").resolve(playerName + ".json");
                FileSnapshot currentSnapshot = snapshot(file);
                FileSnapshot cachedSnapshot = PLAYER_DATA_FILE_SNAPSHOTS.get(playerName);
                if (!currentSnapshot.sameAs(cachedSnapshot)) {
                    PLAYER_DATA_CACHE.remove(playerName);
                    PLAYER_DATA_FILE_SNAPSHOTS.remove(playerName);
                    getPlayerData(playerName);
                }
            }
        }
    }


    private static void createPlayerDataOnce(Path file, String playerName) throws IOException {
        NoSkinSettings seed = settingsFor(playerName);
        String json = "{\n"
                + "  \"enabled\": " + config.enableByDefault + ",\n"
                + "  \"playername\": \"" + playerName + "\",\n"
                + "  \"model\": \"" + seed.model + "\",\n"
                + "  \"skin\": \"" + seed.skin + "\"\n"
                + "}\n";
        try (OutputStream output = Files.newOutputStream(file, StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE)) {
            output.write(json.getBytes(StandardCharsets.UTF_8));
        } catch (FileAlreadyExistsException ignored) {

        }
    }

    private static PlayerData readPlayerDataFile(Path file, String expectedName) {
        NoSkinSettings fallback = settingsFor(expectedName);
        if (!Files.isRegularFile(file)) {
            return PlayerData.invalid(fallback);
        }
        try (BufferedReader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            JsonElement parsed = new JsonParser().parse(reader);
            if (parsed == null || !parsed.isJsonObject()) {
                return PlayerData.invalid(fallback);
            }
            JsonObject object = parsed.getAsJsonObject();
            JsonElement enabledElement = object.get("enabled");
            if (enabledElement == null || !enabledElement.isJsonPrimitive()
                    || !enabledElement.getAsJsonPrimitive().isBoolean()) {
                return PlayerData.invalid(fallback);
            }

            JsonElement nameElement = object.get("playername");
            if (nameElement != null && nameElement.isJsonPrimitive() && nameElement.getAsJsonPrimitive().isString()
                    && !expectedName.equals(nameElement.getAsString())) {
            }

            String model = readModelField(object, "model", fallback.model, expectedName);
            String skin = readSkinField(object, "skin", fallback.skin, expectedName);
            return new PlayerData(true, enabledElement.getAsBoolean(), model, skin);
        } catch (Exception exception) {
            return PlayerData.invalid(fallback);
        }
    }

    private static String readModelField(JsonObject object, String key, String defaultValue, String playerName) {
        JsonElement element = object.get(key);
        if (element != null && element.isJsonPrimitive() && element.getAsJsonPrimitive().isString()) {
            String raw = element.getAsString().toLowerCase(Locale.ROOT);
            String model = canonicalModelOrNull(raw);
            if (model != null) {
                return model;
            }
        } else {
        }
        return defaultValue;
    }

    private static String readSkinField(JsonObject object, String key, String defaultValue, String playerName) {
        JsonElement element = object.get(key);
        if (element != null && element.isJsonPrimitive() && element.getAsJsonPrimitive().isString()) {
            String raw = element.getAsString().toLowerCase(Locale.ROOT);
            String skin = canonicalSkinOrNull(raw);
            if (skin != null) {
                return skin;
            }
        } else {
        }
        return defaultValue;
    }

    private static void refreshConfigIfChanged() {
        synchronized (LOCK) {
            if (configFile == null) {
                return;
            }
            FileSnapshot currentSnapshot = snapshot(configFile);
            if (currentSnapshot.sameAs(configFileSnapshot)) {
                return;
            }

            configFileSnapshot = currentSnapshot;
            config = readMainConfig();
            reloadActiveTextures();
        }
    }

    private static FileSnapshot snapshot(Path file) {
        if (file == null || !Files.isRegularFile(file)) {
            return new FileSnapshot(false, 0L, null);
        }
        try {
            return new FileSnapshot(true, Files.size(file), Files.getLastModifiedTime(file));
        } catch (IOException exception) {
            return new FileSnapshot(false, 0L, null);
        }
    }

    private static ResourceLocation getTextureLocation(String type, String playerName) {
        return new ResourceLocation(RESOURCE_NAMESPACE, "local/" + type + "/" + encodeName(playerName));
    }

    private static void installBundledConfigIfMissing() throws IOException {
        if (Files.exists(configFile)) {
            return;
        }
        try (InputStream input = DeltaSkinLoaderRuntime.class.getResourceAsStream("/config.json")) {
            if (input == null) {
                return;
            }
            try {
                Files.copy(input, configFile);
            } catch (FileAlreadyExistsException ignored) {

            }
        }
    }

    private static ModConfig readMainConfig() {
        if (configFile == null || !Files.isRegularFile(configFile)) {
            return ModConfig.defaults();
        }
        try (BufferedReader reader = Files.newBufferedReader(configFile, StandardCharsets.UTF_8)) {
            JsonElement parsed = new JsonParser().parse(reader);
            if (parsed == null || !parsed.isJsonObject()) {
                return ModConfig.defaults();
            }
            JsonObject object = parsed.getAsJsonObject();
            ModConfig defaults = ModConfig.defaults();
            JsonObject noSkinObject = readObject(object, "noskinfile");
            NoSkinSettings self = readNoSkinSettings(noSkinObject, "self", defaults.noSkinSelf);
            NoSkinSettings others = readNoSkinSettings(noSkinObject, "others", defaults.noSkinOthers);
            boolean enableByDefault = readBooleanSetting(object, "enableByDefault", defaults.enableByDefault);
            boolean customDefaultSkins = readBooleanSetting(object, "customDefaultSkins", defaults.customDefaultSkins);
            boolean enableSkinOverride = readBooleanSetting(object, "enableSkinOverride", defaults.enableSkinOverride);
            boolean enableCape = readBooleanSetting(object, "enableCape", defaults.enableCape);
            boolean enableCustomElytra = readBooleanSetting(object, "enableCustomElytra", defaults.enableCustomElytra);
            boolean enableSkinTransparency = readBooleanSetting(object, "enableSkinTransparency", defaults.enableSkinTransparency);
            return new ModConfig(self, others, enableByDefault, customDefaultSkins,
                    enableSkinOverride, enableCape, enableCustomElytra, enableSkinTransparency);
        } catch (Exception exception) {
            return ModConfig.defaults();
        }
    }

    private static JsonObject readObject(JsonObject object, String key) {
        JsonElement element = object.get(key);
        if (element != null && element.isJsonObject()) {
            return element.getAsJsonObject();
        }
        return new JsonObject();
    }

    private static NoSkinSettings readNoSkinSettings(JsonObject parent, String key, NoSkinSettings defaults) {
        JsonObject object = readObject(parent, key);
        boolean enabled = readBooleanSetting(object, "enabled", defaults.enabled);
        String model = readModelSetting(object, "model", defaults.model);
        String skin = readSkinSetting(object, "skin", defaults.skin);
        return new NoSkinSettings(enabled, model, skin);
    }

    private static String readModelSetting(JsonObject object, String key, String defaultValue) {
        JsonElement element = object.get(key);
        if (element != null && element.isJsonPrimitive() && element.getAsJsonPrimitive().isString()) {
            String model = canonicalModelOrNull(element.getAsString().toLowerCase(Locale.ROOT));
            if (model != null) {
                return model;
            }
        }
        return defaultValue;
    }

    private static String readSkinSetting(JsonObject object, String key, String defaultValue) {
        JsonElement element = object.get(key);
        if (element != null && element.isJsonPrimitive() && element.getAsJsonPrimitive().isString()) {
            String skin = canonicalSkinOrNull(element.getAsString().toLowerCase(Locale.ROOT));
            if (skin != null) {
                return skin;
            }
        }
        return defaultValue;
    }

    private static boolean readBooleanSetting(JsonObject object, String key, boolean defaultValue) {
        JsonElement element = object.get(key);
        if (element != null && element.isJsonPrimitive() && element.getAsJsonPrimitive().isBoolean()) {
            return element.getAsBoolean();
        }
        return defaultValue;
    }

    private static String canonicalModelOrNull(String raw) {
        if ("classic".equals(raw) || "steve".equals(raw)) {
            return "classic";
        }
        if ("slim".equals(raw) || "alex".equals(raw)) {
            return "slim";
        }
        return null;
    }

    private static String canonicalSkinOrNull(String raw) {
        if ("none".equals(raw)) {
            return "none";
        }
        if ("classic".equals(raw) || "steve".equals(raw)) {
            return "steve";
        }
        if ("alex".equals(raw) || "slim".equals(raw)) {
            return "alex";
        }
        return null;
    }

    private static String canonicalSkin(String raw, String fallback) {
        String value = raw == null ? null : canonicalSkinOrNull(raw.toLowerCase(Locale.ROOT));
        return value == null ? fallback : value;
    }

    private static String toMinecraftModelKey(String canonicalModel) {
        return "slim".equals(canonicalModel) ? "slim" : "default";
    }

    private static ResourceLocation loadLocalTexture(String type, String playerName, Path file, boolean forceReload) {
        if (rootDirectory == null) {
            return null;
        }
        ResourceLocation location = getTextureLocation(type, playerName);
        synchronized (LOCK) {
            if (!Files.exists(file)) {
                removeTexture(location);
                return null;
            }
            if (!Files.isRegularFile(file) || !Files.isReadable(file)) {
                warnInvalidAsset(file, "asset path is not a readable regular file", null);
                removeTexture(location);
                return null;
            }

            FileSnapshot assetSnapshot = snapshot(file);
            if (!assetSnapshot.exists) {
                removeTexture(location);
                return null;
            }
            TextureEntry currentEntry = TEXTURES.get(location.toString());
            TextureManager manager;
            try {
                manager = Minecraft.getMinecraft().getTextureManager();
            } catch (RuntimeException exception) {
                return null;
            }
            if (manager == null) {
                return null;
            }
            ITextureObject registered = manager.getTexture(location);
            if (!forceReload && currentEntry != null && registered == currentEntry.texture
                    && currentEntry.file.equals(file) && assetSnapshot.sameAs(currentEntry.fileSnapshot)) {
                return location;
            }

            if (isAssetKnownInvalid(file)) {
                removeTexture(location);
                return null;
            }

            BufferedImage image = readAndValidatePng(file, type);
            if (image == null) {
                removeTexture(location);
                return null;
            }
            if (isSkinLike(type) && !config.enableSkinTransparency) {
                image = makeSkinBaseOpaque(image);
            }

            DynamicTexture dynamicTexture;
            try {
                dynamicTexture = new DynamicTexture(image);
            } catch (RuntimeException exception) {
                removeTexture(location);
                return null;
            }

            if (currentEntry != null && currentEntry.texture != registered) {
                try {
                    currentEntry.texture.deleteGlTexture();
                } catch (RuntimeException ignored) {

                }
            }
            if (registered != null) {
                manager.deleteTexture(location);
            }

            boolean loaded;
            try {
                loaded = manager.loadTexture(location, dynamicTexture);
            } catch (RuntimeException exception) {
                dynamicTexture.deleteGlTexture();
                TEXTURES.remove(location.toString());
                return null;
            }
            if (!loaded) {
                dynamicTexture.deleteGlTexture();
                TEXTURES.remove(location.toString());
                return null;
            }

            TEXTURES.put(location.toString(), new TextureEntry(location, type, playerName, file,
                    dynamicTexture, assetSnapshot));
            removeWarningsFor(file);
            return location;
        }
    }

    private static boolean isSkinLike(String type) {
        return "skin".equals(type) || "defaultskin".equals(type);
    }

    private static BufferedImage readAndValidatePng(Path file, String type) {
        long size;
        try {
            size = Files.size(file);
        } catch (IOException exception) {
            warnInvalidAsset(file, "could not determine file size", exception);
            return null;
        }
        if (size <= 8L || size > MAX_PNG_BYTES) {
            warnInvalidAsset(file, "file size is outside the supported range", null);
            return null;
        }

        byte[] signature = new byte[8];
        try (InputStream input = Files.newInputStream(file)) {
            int offset = 0;
            while (offset < signature.length) {
                int count = input.read(signature, offset, signature.length - offset);
                if (count < 0) {
                    warnInvalidAsset(file, "truncated PNG signature", null);
                    return null;
                }
                offset += count;
            }
        } catch (IOException exception) {
            warnInvalidAsset(file, "could not read the PNG signature", exception);
            return null;
        }
        byte[] expected = new byte[] {(byte) 137, 80, 78, 71, 13, 10, 26, 10};
        if (!Arrays.equals(signature, expected)) {
            warnInvalidAsset(file, "file is not a PNG image", null);
            return null;
        }

        try (ImageInputStream imageStream = ImageIO.createImageInputStream(file.toFile())) {
            if (imageStream == null) {
                warnInvalidAsset(file, "ImageIO could not open the file", null);
                return null;
            }
            Iterator<ImageReader> readers = ImageIO.getImageReaders(imageStream);
            if (!readers.hasNext()) {
                warnInvalidAsset(file, "PNG decoder is unavailable", null);
                return null;
            }
            ImageReader reader = readers.next();
            try {
                reader.setInput(imageStream, true, true);
                int width = reader.getWidth(0);
                int height = reader.getHeight(0);
                boolean skinLike = isSkinLike(type);
                boolean supported = skinLike
                        ? width == 64 && (height == 64 || height == 32)
                        : width == 64 && height == 32;
                if (!supported) {
                    warnInvalidAsset(file, "unsupported dimensions " + width + "x" + height + " for " + type, null);
                    return null;
                }
                BufferedImage image = reader.read(0);
                if (image == null) {
                    warnInvalidAsset(file, "PNG decoder returned no image", null);
                }
                return image;
            } finally {
                reader.dispose();
            }
        } catch (Exception exception) {
            warnInvalidAsset(file, "PNG decoding failed", exception);
            return null;
        }
    }

    private static BufferedImage makeSkinBaseOpaque(BufferedImage source) {
        BufferedImage result = new BufferedImage(source.getWidth(), source.getHeight(), BufferedImage.TYPE_INT_ARGB);
        int[] pixels = source.getRGB(0, 0, source.getWidth(), source.getHeight(), null, 0, source.getWidth());
        result.setRGB(0, 0, source.getWidth(), source.getHeight(), pixels, 0, source.getWidth());

        int[][] baseRegions;
        if (source.getHeight() == 64) {
            baseRegions = new int[][] {
                    {0, 0, 32, 16},
                    {0, 16, 16, 16},
                    {16, 16, 24, 16},
                    {40, 16, 16, 16},
                    {16, 48, 16, 16},
                    {32, 48, 16, 16}
            };
        } else {

            baseRegions = new int[][] {
                    {0, 0, 32, 16},
                    {0, 16, 16, 16},
                    {16, 16, 24, 16},
                    {40, 16, 16, 16}
            };
        }

        for (int[] region : baseRegions) {
            int x0 = region[0];
            int y0 = region[1];
            int width = region[2];
            int height = region[3];
            for (int y = y0; y < y0 + height; y++) {
                for (int x = x0; x < x0 + width; x++) {
                    int pixel = result.getRGB(x, y);
                    int alpha = (pixel >>> 24) & 0xFF;
                    if (alpha < 255) {
                        result.setRGB(x, y, 0xFF000000);
                    }
                }
            }
        }
        return result;
    }

    private static boolean isAssetKnownInvalid(Path file) {
        try {
            String prefix = file.toAbsolutePath().toString() + "|" + Files.size(file) + "|"
                    + Files.getLastModifiedTime(file).toMillis() + "|";
            synchronized (LOCK) {
                for (String warning : INVALID_ASSET_WARNINGS) {
                    if (warning.startsWith(prefix)) {
                        return true;
                    }
                }
            }
        } catch (IOException ignored) {
            return false;
        }
        return false;
    }

    private static void warnInvalidAsset(Path file, String reason, Exception exception) {
        String key;
        try {
            key = file.toAbsolutePath().toString() + "|" + Files.size(file) + "|"
                    + Files.getLastModifiedTime(file).toMillis() + "|" + reason;
        } catch (IOException ignored) {
            key = file.toAbsolutePath().toString() + "|" + reason;
        }
        synchronized (LOCK) {
            INVALID_ASSET_WARNINGS.add(key);
        }
    }

    private static void removeWarningsFor(Path file) {
        String prefix = file.toAbsolutePath().toString() + "|";
        Iterator<String> iterator = INVALID_ASSET_WARNINGS.iterator();
        while (iterator.hasNext()) {
            if (iterator.next().startsWith(prefix)) {
                iterator.remove();
            }
        }
    }

    private static void removeTexture(ResourceLocation location) {
        synchronized (LOCK) {
            TextureEntry entry = TEXTURES.remove(location.toString());
            if (entry == null) {
                return;
            }
            try {
                TextureManager manager = Minecraft.getMinecraft().getTextureManager();
                ITextureObject actual = manager == null ? null : manager.getTexture(location);
                if (manager != null && actual == entry.texture) {
                    manager.deleteTexture(location);
                } else {
                    try {
                        entry.texture.deleteGlTexture();
                    } catch (RuntimeException ignored) {

                    }
                }
            } catch (RuntimeException exception) {
                try {
                    entry.texture.deleteGlTexture();
                } catch (RuntimeException ignored) {

                }
            }
        }
    }

    private static void reloadActiveTextures() {
        synchronized (LOCK) {
            List<TextureEntry> entries = new ArrayList<TextureEntry>(TEXTURES.values());
            INVALID_ASSET_WARNINGS.clear();
            for (TextureEntry entry : entries) {
                if (!isLocalAssetEnabled(entry.type, entry.playerName) || !Files.isRegularFile(entry.file)) {
                    removeTexture(entry.location);
                } else {
                    loadLocalTexture(entry.type, entry.playerName, entry.file, true);
                }
            }
        }
    }

    private static boolean isLocalAssetEnabled(String type, String playerName) {
        if ("defaultskin".equals(type)) {
            return config.customDefaultSkins;
        }
        if ("skin".equals(type) && !config.enableSkinOverride) {
            return false;
        }
        if ("cape".equals(type) && !config.enableCape) {
            return false;
        }
        if ("elytra".equals(type) && !config.enableCustomElytra) {
            return false;
        }
        PlayerData data = getPlayerData(playerName);
        return data != null && data.valid && data.enabled;
    }

    private static void clearClientWorldState() {
        synchronized (LOCK) {
            List<TextureEntry> entries = new ArrayList<TextureEntry>(TEXTURES.values());
            for (TextureEntry entry : entries) {
                removeTexture(entry.location);
            }
            PLAYER_DATA_CACHE.clear();
            PLAYER_DATA_FILE_SNAPSHOTS.clear();
            INVALID_ASSET_WARNINGS.clear();

            if (configFile != null) {
                try {
                    installBundledConfigIfMissing();
                } catch (IOException exception) {
                }
                config = readMainConfig();
                configFileSnapshot = snapshot(configFile);
            }
        }
    }

    private static String encodeName(String playerName) {
        byte[] bytes = playerName.getBytes(StandardCharsets.UTF_8);
        StringBuilder result = new StringBuilder(bytes.length * 2);
        final char[] digits = "0123456789abcdef".toCharArray();
        for (byte value : bytes) {
            int unsigned = value & 0xFF;
            result.append(digits[(unsigned >>> 4) & 0x0F]);
            result.append(digits[unsigned & 0x0F]);
        }
        return result.toString();
    }

    public static final class LifecycleListener {
        private int cleanupTicks;

        @SubscribeEvent
        public void onWorldUnload(WorldEvent.Unload event) {
            if (event.getWorld() != null && event.getWorld().isRemote) {
                clearClientWorldState();
            }
        }

        @SubscribeEvent
        public void onClientTick(TickEvent.ClientTickEvent event) {
            if (event.phase != TickEvent.Phase.END) {
                return;
            }
            refreshConfigIfChanged();
            refreshCachedPlayerData();
            if (++cleanupTicks < 100) {
                return;
            }
            cleanupTicks = 0;
            Minecraft minecraft = Minecraft.getMinecraft();
            if (minecraft.world == null) {
                return;
            }

            Set<String> activeNames = new HashSet<String>();
            for (net.minecraft.entity.player.EntityPlayer entityPlayer : minecraft.world.playerEntities) {
                if (entityPlayer instanceof AbstractClientPlayer) {
                    String name = getPlayerName((AbstractClientPlayer) entityPlayer);
                    if (isUsablePlayerName(name)) {
                        activeNames.add(name);
                    }
                }
            }
            synchronized (LOCK) {
                List<TextureEntry> entries = new ArrayList<TextureEntry>(TEXTURES.values());
                for (TextureEntry entry : entries) {

                    if (!activeNames.contains(entry.playerName) && !"defaultskin".equals(entry.type)) {
                        removeTexture(entry.location);
                    }
                }
                Iterator<String> names = PLAYER_DATA_CACHE.keySet().iterator();
                while (names.hasNext()) {
                    String name = names.next();
                    if (!activeNames.contains(name)) {
                        names.remove();
                        PLAYER_DATA_FILE_SNAPSHOTS.remove(name);
                    }
                }
            }
        }
    }

    private static final class PlayerData {
        private final boolean valid;
        private final boolean enabled;
        private final String model;
        private final String skin;

        private PlayerData(boolean valid, boolean enabled, String model, String skin) {
            this.valid = valid;
            this.enabled = enabled;
            this.model = model;
            this.skin = skin;
        }

        private static PlayerData invalid(NoSkinSettings fallback) {
            return new PlayerData(false, false, fallback.model, fallback.skin);
        }
    }

    private static final class FileSnapshot {
        private final boolean exists;
        private final long size;
        private final FileTime lastModified;

        private FileSnapshot(boolean exists, long size, FileTime lastModified) {
            this.exists = exists;
            this.size = size;
            this.lastModified = lastModified;
        }

        private boolean sameAs(FileSnapshot other) {
            if (other == null || exists != other.exists || size != other.size) {
                return false;
            }
            return lastModified == null ? other.lastModified == null : lastModified.equals(other.lastModified);
        }
    }

    private static final class TextureEntry {
        private final ResourceLocation location;
        private final String type;
        private final String playerName;
        private final Path file;
        private final DynamicTexture texture;
        private final FileSnapshot fileSnapshot;

        private TextureEntry(ResourceLocation location, String type, String playerName, Path file,
                             DynamicTexture texture, FileSnapshot fileSnapshot) {
            this.location = location;
            this.type = type;
            this.playerName = playerName;
            this.file = file;
            this.texture = texture;
            this.fileSnapshot = fileSnapshot;
        }
    }

    private static final class NoSkinSettings {
        private final boolean enabled;
        private final String model;
        private final String skin;

        private NoSkinSettings(boolean enabled, String model, String skin) {
            this.enabled = enabled;
            this.model = model;
            this.skin = skin;
        }
    }

    private static final class ModConfig {
        private final NoSkinSettings noSkinSelf;
        private final NoSkinSettings noSkinOthers;
        private final boolean enableByDefault;
        private final boolean customDefaultSkins;
        private final boolean enableSkinOverride;
        private final boolean enableCape;
        private final boolean enableCustomElytra;
        private final boolean enableSkinTransparency;

        private ModConfig(NoSkinSettings noSkinSelf, NoSkinSettings noSkinOthers,
                          boolean enableByDefault, boolean customDefaultSkins, boolean enableSkinOverride,
                          boolean enableCape, boolean enableCustomElytra, boolean enableSkinTransparency) {
            this.noSkinSelf = noSkinSelf;
            this.noSkinOthers = noSkinOthers;
            this.enableByDefault = enableByDefault;
            this.customDefaultSkins = customDefaultSkins;
            this.enableSkinOverride = enableSkinOverride;
            this.enableCape = enableCape;
            this.enableCustomElytra = enableCustomElytra;
            this.enableSkinTransparency = enableSkinTransparency;
        }

        private static ModConfig defaults() {
            return new ModConfig(
                    new NoSkinSettings(true, "classic", "steve"),
                    new NoSkinSettings(false, "classic", "steve"),
                    true, false, true, true, true, true);
        }
    }
}
