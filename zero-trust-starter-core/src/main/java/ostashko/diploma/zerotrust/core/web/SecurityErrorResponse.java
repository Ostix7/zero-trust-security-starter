package ostashko.diploma.zerotrust.core.web;

public record SecurityErrorResponse(
        String timestamp,
        int status,
        String error,
        String message,
        String path,
        String traceId
) {
}
