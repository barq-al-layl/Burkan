package io.github.barqallayl.burkan.feature.connection.model

import io.github.barqallayl.burkan.core.model.AppError

/** Why the app could not get a shell on the phone. */
sealed interface ConnectionError : AppError {
    /** Wireless debugging needs Wi-Fi, and the phone has none. */
    data object NoWifi : ConnectionError

    /** Wireless debugging is off and the app cannot switch it on, because it lacks the permission. */
    data object WirelessDebuggingOff : ConnectionError

    /** The app switched wireless debugging on and the system switched it straight back off. */
    data object WirelessDebuggingRefused : ConnectionError

    /** No debugging service was advertised on this phone in time. */
    data object DaemonNotFound : ConnectionError

    /** The daemon answered but does not accept this app's key: pairing is needed again. */
    data object NotAuthorised : ConnectionError

    /** The daemon could not be reached on either address. */
    data object ConnectFailed : ConnectionError
}

/** Why pairing did not succeed. Each has its own message. */
sealed interface PairingError : AppError {
    /** What was typed is not a six-digit code. */
    data object InvalidCode : PairingError

    /** The pairing dialog is not open: nothing is listening for a code. */
    data object DialogClosed : PairingError

    /** The dialog was open and the code did not match. */
    data object WrongCode : PairingError

    /** No code arrived in time, or the exchange did not finish. */
    data object TimedOut : PairingError

    /** Anything else; the details are in the log. */
    data object Failed : PairingError
}
