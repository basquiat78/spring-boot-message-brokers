package io.basquiat.global.utils

import io.basquiat.global.provider.ApplicationContextProvider

inline fun <reified T : Any> getBean(): T {
    val applicationContext =
        ApplicationContextProvider.getApplicationContext()
            ?: throw NullPointerException("Not Initialize Spring Container!")
    return applicationContext.getBean(T::class.java)
}