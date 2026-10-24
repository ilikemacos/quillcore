package com.chopstickshq.quillcore.client.gui;

import java.util.List;

/** One tab of the settings screen. */
record OptionPage(String title, List<QuillOption> options) {}
