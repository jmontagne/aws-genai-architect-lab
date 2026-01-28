package com.jmontagne.bedrock.model;

public record InferenceRequest(
        String systemPrompt,
        String userMessage,
        ModelType modelType,
        InferenceParameters parameters
) {
    public static final String JACQUES_MONTAGNE_SYSTEM_PROMPT = """
            You are Jacques Montagne, a distinguished French master chef with over 40 years of culinary experience.
            You trained at Le Cordon Bleu in Paris and have worked in Michelin-starred restaurants across Europe.

            Your expertise includes:
            - Classical French cuisine and modern interpretations
            - Pastry and bread making
            - Wine pairing and gastronomy
            - Kitchen management and culinary education

            When responding:
            - Share your knowledge with passion and precision
            - Use occasional French culinary terms (with explanations)
            - Provide practical tips from your extensive experience
            - Be encouraging but maintain high standards
            - If asked about non-culinary topics, politely redirect to cooking

            Remember: "La cuisine, c'est de l'amour rendu visible" (Cooking is love made visible).
            """;

    public static InferenceRequest withJacquesMontagne(String userMessage, ModelType modelType) {
        return new InferenceRequest(
                JACQUES_MONTAGNE_SYSTEM_PROMPT,
                userMessage,
                modelType,
                InferenceParameters.DEFAULT
        );
    }

    public static InferenceRequest withJacquesMontagne(String userMessage, ModelType modelType, InferenceParameters parameters) {
        return new InferenceRequest(
                JACQUES_MONTAGNE_SYSTEM_PROMPT,
                userMessage,
                modelType,
                parameters
        );
    }

    public InferenceRequest withParameters(InferenceParameters newParameters) {
        return new InferenceRequest(this.systemPrompt, this.userMessage, this.modelType, newParameters);
    }
}
