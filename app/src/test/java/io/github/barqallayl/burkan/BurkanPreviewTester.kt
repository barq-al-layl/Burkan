package io.github.barqallayl.burkan

import com.dropbox.differ.SimpleImageComparator
import com.github.takahirom.roborazzi.AndroidComposePreviewTester
import com.github.takahirom.roborazzi.ComposePreviewTester
import com.github.takahirom.roborazzi.ComposePreviewTester.TestParameter.JUnit4TestParameter.AndroidPreviewJUnit4TestParameter
import com.github.takahirom.roborazzi.ExperimentalRoborazziApi

/**
 * Runs the screenshot tests generated from the previews, comparing each pixel a little more loosely than Roborazzi
 * does by default. The screenshots are recorded on a Mac and checked on Linux too, by GitHub Actions, where blurs
 * and gradients round a few levels differently: up to 4 of 255 in a channel, over as much as a tenth of an image.
 * The default lets through about 2; this lets through 4 in every channel at once. A change anyone could see, a
 * colour or a word, is far beyond it.
 */
@OptIn(ExperimentalRoborazziApi::class)
class BurkanPreviewTester : ComposePreviewTester<AndroidPreviewJUnit4TestParameter> by AndroidComposePreviewTester(
    capturer = { parameter ->
        val options = parameter.roborazziOptions
        AndroidComposePreviewTester.DefaultCapturer().capture(
            parameter.copy(
                roborazziOptions = options.copy(
                    compareOptions = options.compareOptions.copy(
                        imageComparator = SimpleImageComparator(maxDistance = MAX_DISTANCE),
                    ),
                ),
            ),
        )
    },
)

/** How far apart two pixels' colours may be, as the straight-line distance between them with channels from 0 to 1. */
private const val MAX_DISTANCE = 0.028f
