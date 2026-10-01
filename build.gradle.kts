import nl.littlerobots.vcu.plugin.resolver.VersionSelectors

// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.kotlin.serialization) apply false
    alias(libs.plugins.metro) apply false
    alias(libs.plugins.roborazzi) apply false
    alias(libs.plugins.versionCatalogUpdate)
}

versionCatalogUpdate {

    versionSelector(VersionSelectors.PREFER_STABLE)

    keep {
        keepUnusedVersions.set(true)
    }
}
