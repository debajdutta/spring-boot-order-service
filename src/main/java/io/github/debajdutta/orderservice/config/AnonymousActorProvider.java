package io.github.debajdutta.orderservice.config;

import io.github.debajdutta.orderservice.common.web.CurrentActorProvider;
import org.springframework.stereotype.Component;

/** Phase 1 (no security): every request is attributed to "anonymous". Replaced in phase 2. */
@Component
public class AnonymousActorProvider implements CurrentActorProvider {

    @Override
    public String currentActor() {
        return "anonymous";
    }
}
