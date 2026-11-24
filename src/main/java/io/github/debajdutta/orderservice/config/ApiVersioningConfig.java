package io.github.debajdutta.orderservice.config;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.accept.ApiVersionParser;
import org.springframework.web.accept.SemanticApiVersionParser;
import org.springframework.web.servlet.config.annotation.ApiVersionConfigurer;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Spring Framework 7 built-in API versioning, using the URL path:
 *
 *   /api/v1/orders  ->  the segment after /api/ ("v1") holds the version
 *
 * Controllers declare their path as "/api/{version}/..." and each handler method
 * says which version it serves, e.g. @GetMapping(path = "/{id}", version = "1").
 * A v2 endpoint can later sit next to the v1 one in the same controller.
 *
 * Behaviour:
 *   /api/v1/...  -> handled
 *   /api/v2/...  -> 400 (unsupported version) until a v2 mapping exists
 */
@Configuration
public class ApiVersioningConfig implements WebMvcConfigurer {

    private static final SemanticApiVersionParser SEMANTIC = new SemanticApiVersionParser();

    /** Accept "v1" as well as "1": strip an optional leading v, then parse 1 -> 1.0.0 */
    private static final ApiVersionParser<SemanticApiVersionParser.Version> V_PREFIX_PARSER =
            version -> SEMANTIC.parseVersion(
                    version.startsWith("v") || version.startsWith("V") ? version.substring(1) : version);

    @Override
    public void configureApiVersioning(ApiVersionConfigurer configurer) {
        configurer
                // Read the version only from /api/{version}/... so other URLs
                // (/actuator/health, /error) are never treated as versioned.
                .useVersionResolver(ApiVersioningConfig::versionFromApiPath)
                .setVersionRequired(false)
                .setVersionParser(V_PREFIX_PARSER)
                .addSupportedVersions("1");
    }

    /** "/api/v1/orders/5" -> "v1"; anything not under /api/ -> null (no version). */
    static String versionFromApiPath(HttpServletRequest request) {
        String path = request.getRequestURI().substring(request.getContextPath().length());
        String[] segments = path.split("/"); // ["", "api", "v1", "orders", ...]
        return (segments.length > 2 && "api".equals(segments[1])) ? segments[2] : null;
    }
}
