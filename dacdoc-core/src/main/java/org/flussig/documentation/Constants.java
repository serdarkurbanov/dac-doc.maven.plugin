package org.flussig.documentation;

/**
 * Constant string values
 * */
public interface Constants {
    // identifying placeholder
    String ANCHOR_FRAMING = "!";
    String ANCHOR_KEYWORD = "DACDOC";

    // extracting parameters from placeholders
    String ANCHOR_PARAMETER_SEPARATOR = ";";
    String ANCHOR_PARAMETER_KEY_VALUE_SEPARATOR = "=";
    String ANCHOR_PARAMETER_IDS_SEPARATOR = ",";

    // names of the parameters
    String ANCHOR_PARAMETER_ID = "id";
    String ANCHOR_PARAMETER_TEST_ID = "test";
    String ANCHOR_PARAMETER_IDS = "ids";

    String DEFAULT_TEST_ID = "dacdoc-url";
}