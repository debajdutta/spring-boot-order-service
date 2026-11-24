package io.github.debajdutta.orderservice.common.web;

/**
 * Who is making the current request. Phase 1 returns a fixed value;
 * in phase 2 (Entra ID) the implementation will read the "oid" claim from the JWT.
 * Services depend on this interface, so adding security needs no service changes.
 */
public interface CurrentActorProvider {

    String currentActor();
}
