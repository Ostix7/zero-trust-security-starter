package ostashko.diploma.zerotrust.secrets.backend;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import ostashko.diploma.zerotrust.secrets.config.ZeroTrustSecretsProperties;
import org.springframework.util.StringUtils;

public class InlineVaultStyleSecretBackend implements VaultStyleSecretBackend {

    private final Map<String, Map<String, String>> secretsByPath;

    public InlineVaultStyleSecretBackend(ZeroTrustSecretsProperties properties) {
        this.secretsByPath = properties.getVault().getEntries().stream()
                .filter(entry -> entry.getPath() != null && !entry.getPath().isBlank())
                .collect(java.util.stream.Collectors.toUnmodifiableMap(
                        ZeroTrustSecretsProperties.Entry::getPath,
                        this::entryValues,
                        (left, right) -> right
                ));
    }

    @Override
    public Optional<Map<String, String>> read(String path) {
        return Optional.ofNullable(secretsByPath.get(path));
    }

    private Map<String, String> entryValues(ZeroTrustSecretsProperties.Entry entry) {
        Map<String, String> values = new LinkedHashMap<>(entry.getValues());
        if (StringUtils.hasText(entry.getKey()) && StringUtils.hasText(entry.getValue())) {
            values.put(entry.getKey(), entry.getValue());
        }
        return Map.copyOf(values);
    }
}
