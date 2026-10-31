package com.ilya.homewatch

/**
 * Данные OAuth-приложения Яндекса. Сами значения лежат в local.properties
 * (yandex.clientId и yandex.clientSecret) и попадают сюда при сборке через
 * BuildConfig, поэтому в репозиторий ключи не уходят. Как их получить, см. README.md.
 */
object Config {
    val CLIENT_ID: String = BuildConfig.YANDEX_CLIENT_ID
    val CLIENT_SECRET: String = BuildConfig.YANDEX_CLIENT_SECRET
}
