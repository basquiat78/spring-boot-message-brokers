package io.basquiat.global.utils

@Suppress("UNCHECKED_CAST")
fun <T> convertMessage(
    message: Any,
    targetType: Class<T>,
): T =
    if (targetType.isInstance(message)) {
        message as T
    } else {
        convertToTarget(message, targetType)
    }