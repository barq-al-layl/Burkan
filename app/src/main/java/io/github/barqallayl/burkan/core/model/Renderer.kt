package io.github.barqallayl.burkan.core.model

/** The renderer a running process reports. */
enum class Renderer {
    Vulkan,
    OpenGL,

    /**
     * Running, but it has drawn nothing yet, so it has no renderer to report. The keyboard is like this from its
     * restart until it is first shown.
     */
    NotDrawn,

    /** Not running, not read, or reporting a pipeline that is neither of the two. */
    Unknown,
}
