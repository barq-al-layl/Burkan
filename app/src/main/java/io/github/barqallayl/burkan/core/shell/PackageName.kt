package io.github.barqallayl.burkan.core.shell

/** An Android package name that is safe to put in a shell command unquoted. */
@JvmInline
value class PackageName private constructor(val value: String) {

    override fun toString(): String = value

    companion object {
        private val pattern = Regex("^[A-Za-z0-9_.]+$")

        /** The name, or null when [raw] contains anything a package name cannot. */
        fun parse(raw: String): PackageName? = if (pattern.matches(raw)) PackageName(raw) else null

        /** A name written into the app's source. Fails at once if the constant is wrong. */
        fun known(raw: String): PackageName = requireNotNull(parse(raw)) { "Not a package name: $raw" }
    }
}
