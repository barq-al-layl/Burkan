package io.github.barqallayl.burkan.feature.settings.ui

import com.mikepenz.aboutlibraries.entity.License
import org.junit.Test
import kotlin.test.assertEquals

class LicenceTextTest {

    private fun licence(name: String, spdxId: String?) = License(name = name, url = null, spdxId = spdxId, hash = name)

    @Test
    fun `a licence with an SPDX identifier is named by it, read as words`() {
        assertEquals("Apache 2.0", licence("The Apache Software License, Version 2.0", "Apache-2.0").shortName)
        assertEquals("BSD 3-Clause", licence("BSD-3-Clause", "BSD-3-Clause").shortName)
        assertEquals("MIT", licence("MIT License", "MIT").shortName)
    }

    @Test
    fun `a licence without one keeps its full name`() {
        assertEquals("Bouncy Castle Licence", licence("Bouncy Castle Licence", null).shortName)
    }

    @Test
    fun `lines broken for a fixed-width page are joined within each paragraph`() {
        val text = """
                  Apache License
              Version 2.0, January 2004

           1. Definitions.

              "License" shall mean the terms and conditions for use,
              reproduction, and distribution.
        """

        assertEquals(
            "Apache License Version 2.0, January 2004\n\n1. Definitions.\n\n" +
                "\"License\" shall mean the terms and conditions for use, reproduction, and distribution.",
            readableLicence(text),
        )
    }
}
