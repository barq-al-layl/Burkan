package io.github.barqallayl.burkan.core.model

import androidx.annotation.StringRes
import io.github.barqallayl.burkan.R

/** One entry per message a user can be shown about a failure. */
enum class AppErrorType(@StringRes val resource: Int) {
    Unexpected(R.string.error_unexpected),
}

/** The message for this error. State holds the [AppError]; the UI calls `stringResource(error.messageRes())`. */
@StringRes
fun AppError.messageRes(): Int = when (this) {
    else -> AppErrorType.Unexpected.resource
}
