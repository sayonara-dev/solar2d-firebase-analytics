# solar2d-firebase-analytics

Firebase Analytics plugin for Solar2D, **Android only**, with Google Consent Mode v2.
Altered fork of [com.solar2d-plugin.googleAnalytics.v2](https://github.com/solar2d/com.solar2d-plugin.googleAnalytics.v2):
Firebase BoM 35.2.0 (firebase-analytics 24.0.0, minSdk 24), `setConsent`, `setCollectionEnabled`,
numeric parameters keep their type.

## build.settings

```lua
android = {
    useGoogleServicesJson = true, -- google-services.json next to main.lua
    applicationChildElements = {
        [[
            <meta-data android:name="firebase_analytics_collection_enabled" android:value="false" />
            <meta-data android:name="google_analytics_default_allow_analytics_storage" android:value="false" />
            <meta-data android:name="google_analytics_default_allow_ad_storage" android:value="false" />
            <meta-data android:name="google_analytics_default_allow_ad_user_data" android:value="false" />
            <meta-data android:name="google_analytics_default_allow_ad_personalization_signals" android:value="false" />
        ]],
    },
},
plugins = {
    ["plugin.firebaseAnalytics"] = {
        publisherId = "com.idleveloping",
        supportedPlatforms = {
            android = { url = "https://github.com/sayonara-dev/solar2d-firebase-analytics/releases/download/v1.0.0/android.tgz" },
        },
    },
},
```

## Lua API

```lua
local ok, fa = pcall(require, "plugin.firebaseAnalytics") -- absent on iOS / simulator
fa.init()                                  -- true when Firebase is ready
fa.setConsent(true)                        -- AD_STORAGE, AD_USER_DATA, AD_PERSONALIZATION, ANALYTICS_STORAGE
fa.setCollectionEnabled(true)
fa.logEvent("purchase", { value = 4.99, currency = "USD", item_id = "gems_1", transaction_id = "GPA.1" })
```

Parameter values: strings, numbers, booleans (sent as 1 / 0). Whole numbers go as long, others as double;
`value`, `price`, `tax`, `shipping` are always double. Firebase errors are logged (`plugin.firebaseAnalytics`)
and never raise a Lua error.

## Build

`bash build.sh` → `dist/android.tgz`. Needs a JDK (javac 17), `android.jar` and Solar2D's `Corona.aar`;
paths are overridable with `JDK_BIN`, `ANDROID_JAR`, `CORONA_AAR`. Upload `dist/android.tgz` to a GitHub Release.
