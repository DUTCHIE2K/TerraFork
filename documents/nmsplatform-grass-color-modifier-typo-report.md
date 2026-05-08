# NMSPlatform Loader Typo Report

Date: 2026-05-07
Status: Inspection only
Target file: `platforms/bukkit/nms/src/main/java/com/dfsek/terra/bukkit/nms/NMSPlatform.java`

## Summary

`NMSPlatform.register(TypeRegistry registry)` contains a loader registration typo.

The method registers `GrassColorModifier.class` twice:

1. once with a loader that returns `GrassColorModifier`
2. again with a loader that returns `TemperatureModifier`

The second registration should almost certainly target `TemperatureModifier.class`, not `GrassColorModifier.class`.

## Exact Location

Current suspicious block:

```java
registry.registerLoader(GrassColorModifier.class,
    (type, o, loader, depthTracker) -> GrassColorModifier.valueOf(((String) o).toUpperCase(
        Locale.ROOT)))
    .registerLoader(GrassColorModifier.class,
        (type, o, loader, depthTracker) -> TemperatureModifier.valueOf(((String) o).toUpperCase(
            Locale.ROOT)))
```

Observed in:

- `platforms/bukkit/nms/src/main/java/com/dfsek/terra/bukkit/nms/NMSPlatform.java`

## Why This Is a Bug

The second loader returns a `TemperatureModifier` value but is registered against `GrassColorModifier.class`.

That creates a type mismatch at the registration site:

- declared key: `GrassColorModifier.class`
- actual returned type: `TemperatureModifier`

This is structurally inconsistent with the surrounding loader registrations and also inconsistent with the equivalent mod-side platform implementation, where `TemperatureModifier.class` is registered separately.

Reference for comparison:

- `platforms/mixin-common/src/main/java/com/dfsek/terra/mod/ModPlatform.java`

That implementation correctly registers:

- `GrassColorModifier.class -> GrassColorModifier.valueOf(...)`
- `TemperatureModifier.class -> TemperatureModifier.valueOf(...)`

## Likely Cause

This appears to be a copy/paste typo introduced while mirroring biome-related loader registrations from the mod platform into the Bukkit NMS platform.

The pattern strongly suggests the second line was meant to be:

```java
.registerLoader(TemperatureModifier.class, ...)
```

## Expected Impact

Potential effects depend on whether Bukkit NMS pack loading ever asks this registry for `TemperatureModifier`:

- If `TemperatureModifier` is requested, loading may fail because no correct loader is registered for that class.
- If `GrassColorModifier` resolves to the second registration, behavior may become incorrect or fail at runtime due to the wrong enum type being returned.
- If the affected loader path is rarely exercised, the bug can remain latent for a long time.

Because this sits in type-loader registration, the failure mode is likely configuration-load-time rather than generation-time.

## Risk Assessment

Current severity: medium

Reasoning:

- The defect is narrow and isolated to one registration line.
- The affected domain is config/type loading, not hot-path chunk generation.
- If a config pack or biome template relies on `TemperatureModifier`, the bug can produce incorrect parsing or a hard load failure.

## Refactor or Fix Scope

Fix scope should be small:

- change the second registration target from `GrassColorModifier.class` to `TemperatureModifier.class`
- then verify Bukkit NMS config loading for biome templates

No broader refactor should be required for this specific issue.

## Recommended Follow-Up

When this is scheduled:

1. Correct the registration target class.
2. Compare `NMSPlatform.register(...)` against `ModPlatform.register(...)` for any other drift.
3. Add a focused test covering loader registration for both:
   - `GrassColorModifier`
   - `TemperatureModifier`

## Deferral Note

This issue is intentionally being documented and deferred.

No code fix is included with this report.
