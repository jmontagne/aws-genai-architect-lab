package com.jmontagne.bedrock.model;

public enum ModelType {
    CLAUDE_3_5_SONNET("anthropic.claude-3-5-sonnet-20240620-v1:0", "Claude 3.5 Sonnet", "Anthropic"),
    CLAUDE_3_HAIKU("anthropic.claude-3-haiku-20240307-v1:0", "Claude 3 Haiku", "Anthropic");

    private final String modelId;
    private final String displayName;
    private final String provider;

    ModelType(String modelId, String displayName, String provider) {
        this.modelId = modelId;
        this.displayName = displayName;
        this.provider = provider;
    }

    public String getModelId() {
        return modelId;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getProvider() {
        return provider;
    }

    public static ModelType fromModelId(String modelId) {
        for (ModelType type : values()) {
            if (type.modelId.equals(modelId)) {
                return type;
            }
        }
        throw new IllegalArgumentException("Unknown model ID: " + modelId);
    }
}
