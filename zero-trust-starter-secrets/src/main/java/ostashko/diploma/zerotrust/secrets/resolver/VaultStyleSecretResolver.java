package ostashko.diploma.zerotrust.secrets.resolver;

import java.util.Optional;
import java.util.function.Supplier;
import ostashko.diploma.zerotrust.secrets.backend.VaultStyleSecretBackend;
import org.springframework.util.StringUtils;

public class VaultStyleSecretResolver {

    private final Supplier<VaultStyleSecretBackend> backendSupplier;

    public VaultStyleSecretResolver(VaultStyleSecretBackend backend) {
        this(() -> backend);
    }

    public VaultStyleSecretResolver(Supplier<VaultStyleSecretBackend> backendSupplier) {
        this.backendSupplier = backendSupplier;
    }

    public Optional<String> resolve(String path, String key) {
        VaultStyleSecretBackend backend = backendSupplier.get();
        if (backend == null || !StringUtils.hasText(path) || !StringUtils.hasText(key)) {
            return Optional.empty();
        }
        return backend.read(path).map(values -> values.get(key)).filter(StringUtils::hasText);
    }
}
