package de.xbrowniecodez.jbytemod.plugin;

import com.google.gson.Gson;
import com.google.gson.JsonParseException;
import de.xbrowniecodez.jbytemod.utils.Utils;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.IntConsumer;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;
import java.util.zip.ZipFile;

public final class PluginRepositoryService {
    public static final String OFFICIAL_REPOSITORY = "https://github.com/jbytemod/plugin-registry";

    private static final String CUSTOM_REPOSITORIES_KEY = "customRepositories";
    private static final String REPOSITORY_SEPARATOR = "\n";
    private static final long MAX_REGISTRY_SIZE = 2L * 1024 * 1024;
    private static final long MAX_PLUGIN_SIZE = 100L * 1024 * 1024;

    private final Preferences preferences = Preferences.userNodeForPackage(PluginRepositoryService.class);
    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(12))
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build();
    private final Gson gson = new Gson();
    private final Path cacheFolder = new File(Utils.getWorkingDirectory(), "plugin-repository-cache").toPath();

    public record RepositorySource(String url, boolean official) {
        public String displayName() {
            return official ? "Official JByteMod repository" : url;
        }
    }

    public record RepositoryPlugin(String id, String name, String version, String author, String description,
                                   String minimumJByteModVersion, String website, String downloadUrl, String sha256,
                                   String fileName, String repositoryName, String repositoryUrl) {
        public boolean downloadable() {
            return downloadUrl != null && !downloadUrl.isBlank() && sha256 != null && sha256.matches("(?i)[0-9a-f]{64}");
        }
    }

    public record Catalog(List<RepositoryPlugin> plugins, List<String> messages) {
    }

    private record RegistryDocument(int schemaVersion, String name, List<RegistryPlugin> plugins) {
    }

    private record RegistryPlugin(String id, String name, String version, String author, String description,
                                  String minimumJByteModVersion, String website, String downloadUrl, String sha256,
                                  String fileName) {
    }

    public List<RepositorySource> getSources() {
        List<RepositorySource> sources = new ArrayList<>();
        sources.add(new RepositorySource(OFFICIAL_REPOSITORY, true));
        for (String repository : preferences.get(CUSTOM_REPOSITORIES_KEY, "").split(REPOSITORY_SEPARATOR)) {
            String normalized = repository.trim();
            if (!normalized.isEmpty() && !normalized.equalsIgnoreCase(OFFICIAL_REPOSITORY)) {
                sources.add(new RepositorySource(normalized, false));
            }
        }
        return List.copyOf(sources);
    }

    public void addSource(String repository) {
        String normalized = normalizeSource(repository);
        Set<String> repositories = new LinkedHashSet<>();
        for (RepositorySource source : getSources()) {
            if (!source.official()) repositories.add(source.url());
        }
        if (!normalized.equalsIgnoreCase(OFFICIAL_REPOSITORY)) repositories.add(normalized);
        saveCustomRepositories(repositories);
    }

    public void removeSource(RepositorySource source) {
        if (source.official()) throw new IllegalArgumentException("The official plugin repository cannot be removed");
        Set<String> repositories = new LinkedHashSet<>();
        for (RepositorySource existing : getSources()) {
            if (!existing.official() && !existing.url().equals(source.url())) repositories.add(existing.url());
        }
        saveCustomRepositories(repositories);
    }

    public Catalog loadCatalog() {
        Map<String, RepositoryPlugin> plugins = new LinkedHashMap<>();
        List<String> messages = new ArrayList<>();
        for (RepositorySource source : getSources()) {
            try {
                LoadedRegistry registry = loadRegistry(source);
                if (registry.cached()) messages.add(registry.document().name() + ": using cached data");
                for (RegistryPlugin plugin : registry.document().plugins()) {
                    RepositoryPlugin entry = validatePlugin(plugin, registry.document().name(), source.url());
                    RepositoryPlugin previous = plugins.putIfAbsent(entry.id(), entry);
                    if (previous != null) {
                        messages.add("Ignored duplicate plugin " + entry.id() + " from " + registry.document().name());
                    }
                }
            } catch (Exception exception) {
                messages.add(source.displayName() + ": " + conciseMessage(exception));
            }
        }
        return new Catalog(List.copyOf(plugins.values()), List.copyOf(messages));
    }

    public Path downloadPlugin(RepositoryPlugin plugin, IntConsumer progress) throws IOException, InterruptedException {
        if (!plugin.downloadable()) throw new IOException("This plugin does not provide a verified release download");
        URI uri = validHttpUri(plugin.downloadUrl());
        HttpRequest request = HttpRequest.newBuilder(uri).timeout(Duration.ofMinutes(2)).GET().build();
        HttpResponse<InputStream> response = httpClient.send(request, HttpResponse.BodyHandlers.ofInputStream());
        if (response.statusCode() != 200) {
            response.body().close();
            throw new IOException("Download returned HTTP " + response.statusCode());
        }
        long contentLength = response.headers().firstValueAsLong("Content-Length").orElse(-1);
        if (contentLength > MAX_PLUGIN_SIZE) {
            response.body().close();
            throw new IOException("Plugin exceeds the 100 MB download limit");
        }

        Path pluginFolder = new File(Utils.getWorkingDirectory(), "plugins").toPath();
        Files.createDirectories(pluginFolder);
        Path temporary = Files.createTempFile(pluginFolder, ".plugin-download-", ".jar");
        MessageDigest digest = sha256Digest();
        long copied = 0;
        try (InputStream input = response.body();
             var output = Files.newOutputStream(temporary, StandardOpenOption.TRUNCATE_EXISTING)) {
            byte[] buffer = new byte[32 * 1024];
            int read;
            while ((read = input.read(buffer)) >= 0) {
                copied += read;
                if (copied > MAX_PLUGIN_SIZE) throw new IOException("Plugin exceeds the 100 MB download limit");
                output.write(buffer, 0, read);
                digest.update(buffer, 0, read);
                if (contentLength > 0) progress.accept((int) Math.min(99, copied * 100 / contentLength));
            }
            String actualHash = HexFormat.of().formatHex(digest.digest());
            if (!actualHash.equalsIgnoreCase(plugin.sha256())) {
                throw new IOException("SHA-256 mismatch (expected " + plugin.sha256() + ", got " + actualHash + ")");
            }
            validatePluginJar(temporary);
            progress.accept(100);
            return temporary;
        } catch (IOException | RuntimeException exception) {
            Files.deleteIfExists(temporary);
            throw exception;
        }
    }

    private LoadedRegistry loadRegistry(RepositorySource source) throws IOException, InterruptedException {
        Exception lastFailure = null;
        for (URI candidate : registryCandidates(source.url())) {
            try {
                byte[] bytes = downloadRegistry(candidate);
                RegistryDocument document = parseRegistry(bytes, candidate.toString());
                Files.createDirectories(cacheFolder);
                Files.write(cachePath(source.url()), bytes);
                return new LoadedRegistry(document, false);
            } catch (IOException | InterruptedException | RuntimeException exception) {
                lastFailure = exception;
                if (exception instanceof InterruptedException interrupted) throw interrupted;
            }
        }
        Path cached = cachePath(source.url());
        if (Files.isRegularFile(cached)) {
            return new LoadedRegistry(parseRegistry(Files.readAllBytes(cached), source.url()), true);
        }
        if (lastFailure instanceof IOException ioException) throw ioException;
        throw new IOException(lastFailure == null ? "Could not load repository" : conciseMessage(lastFailure), lastFailure);
    }

    private byte[] downloadRegistry(URI uri) throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder(uri).timeout(Duration.ofSeconds(25)).GET().build();
        HttpResponse<byte[]> response = httpClient.send(request, HttpResponse.BodyHandlers.ofByteArray());
        if (response.statusCode() != 200) throw new IOException("HTTP " + response.statusCode() + " from " + uri);
        if (response.body().length > MAX_REGISTRY_SIZE) throw new IOException("Repository index exceeds 2 MB");
        return response.body();
    }

    private RegistryDocument parseRegistry(byte[] bytes, String source) throws IOException {
        try {
            RegistryDocument document = gson.fromJson(new String(bytes, StandardCharsets.UTF_8),
                    RegistryDocument.class);
            if (document == null || document.schemaVersion() != 1) {
                throw new IOException("Unsupported or missing schemaVersion in " + source);
            }
            String name = requireText(document.name(), "repository name");
            List<RegistryPlugin> plugins = document.plugins() == null ? List.of() : document.plugins();
            return new RegistryDocument(1, name, plugins);
        } catch (JsonParseException exception) {
            throw new IOException("Invalid repository JSON from " + source, exception);
        }
    }

    private RepositoryPlugin validatePlugin(RegistryPlugin plugin, String repositoryName, String repositoryUrl) {
        String id = requireText(plugin.id(), "plugin id");
        String name = requireText(plugin.name(), "plugin name");
        String version = requireText(plugin.version(), "plugin version");
        String author = requireText(plugin.author(), "plugin author");
        String minimumVersion = blankToDefault(plugin.minimumJByteModVersion(), "2.11.0");
        return new RepositoryPlugin(id, name, version, author, blankToDefault(plugin.description(), ""), minimumVersion,
                blankToDefault(plugin.website(), ""), blankToDefault(plugin.downloadUrl(), ""),
                blankToDefault(plugin.sha256(), "").toLowerCase(Locale.ROOT), blankToDefault(plugin.fileName(), ""),
                repositoryName, repositoryUrl);
    }

    private List<URI> registryCandidates(String source) {
        URI uri = URI.create(source);
        String host = uri.getHost() == null ? "" : uri.getHost().toLowerCase(Locale.ROOT);
        String path = uri.getPath() == null ? "" : uri.getPath().replaceAll("/+$", "");
        if (host.equals("github.com")) {
            String[] parts = Arrays.stream(path.split("/")).filter(part -> !part.isBlank()).toArray(String[]::new);
            if (parts.length == 2) {
                return List.of(
                        URI.create("https://raw.githubusercontent.com/" + parts[0] + "/" + parts[1] + "/main/plugins.json"),
                        URI.create("https://raw.githubusercontent.com/" + parts[0] + "/" + parts[1] + "/master/plugins.json"));
            }
        }
        if (path.endsWith(".json")) return List.of(validHttpUri(source));
        return List.of(validHttpUri(source.replaceAll("/+$", "") + "/plugins.json"));
    }

    private URI validHttpUri(String value) {
        URI uri = URI.create(value.trim());
        if (!"https".equalsIgnoreCase(uri.getScheme()) && !"http".equalsIgnoreCase(uri.getScheme())) {
            throw new IllegalArgumentException("Repository and download URLs must use HTTP or HTTPS");
        }
        if (uri.getHost() == null) throw new IllegalArgumentException("Invalid URL: " + value);
        return uri;
    }

    private String normalizeSource(String repository) {
        if (repository == null || repository.isBlank()) throw new IllegalArgumentException("Enter a repository URL");
        URI uri = validHttpUri(repository);
        String normalized = uri.toString().replaceAll("/+$", "");
        if (normalized.endsWith(".git")) normalized = normalized.substring(0, normalized.length() - 4);
        return normalized;
    }

    private Path cachePath(String source) {
        MessageDigest digest = sha256Digest();
        byte[] bytes = digest.digest(source.getBytes(StandardCharsets.UTF_8));
        return cacheFolder.resolve(HexFormat.of().formatHex(bytes) + ".json");
    }

    private MessageDigest sha256Digest() {
        try {
            return MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException(exception);
        }
    }

    private void validatePluginJar(Path file) throws IOException {
        boolean hasClass = false;
        try (ZipFile zip = new ZipFile(file.toFile())) {
            var entries = zip.entries();
            while (entries.hasMoreElements()) {
                if (entries.nextElement().getName().endsWith(".class")) {
                    hasClass = true;
                    break;
                }
            }
        }
        if (!hasClass) throw new IOException("Downloaded file is not a plugin JAR");
    }

    private String requireText(String value, String field) {
        if (value == null || value.isBlank()) throw new IllegalArgumentException("Missing " + field);
        return value.trim();
    }

    private String blankToDefault(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value.trim();
    }

    private String conciseMessage(Throwable throwable) {
        String message = throwable.getMessage();
        return message == null || message.isBlank() ? throwable.getClass().getSimpleName() : message;
    }

    private void saveCustomRepositories(Set<String> repositories) {
        preferences.put(CUSTOM_REPOSITORIES_KEY, String.join(REPOSITORY_SEPARATOR, repositories));
        try {
            preferences.flush();
        } catch (BackingStoreException exception) {
            throw new IllegalStateException("Could not save plugin repositories", exception);
        }
    }

    private record LoadedRegistry(RegistryDocument document, boolean cached) {
    }
}
