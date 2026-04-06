package ostashko.diploma.zerotrust.secrets.backend;

import java.util.Map;
import java.util.Optional;

public interface VaultStyleSecretBackend {

    Optional<Map<String, String>> read(String path);
}
