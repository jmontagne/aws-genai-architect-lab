# -----------------------------------------------------------------------------
# Guardrail A: MASK strategy — PII is replaced with identifiers ({SSN}, {EMAIL})
# Use when you need to preserve response structure but redact sensitive values.
# -----------------------------------------------------------------------------
resource "aws_bedrock_guardrail" "mask" {
  name                      = "${var.project_name}-mask"
  description               = "Guardrail with MASK strategy — replaces PII with identifiers"
  blocked_input_messaging   = "Your request contains content that is not allowed."
  blocked_outputs_messaging = "The model response was filtered by security guardrails."

  # --- Content Filters (hate, insults, sexual, violence, misconduct, prompt attack) ---
  content_policy_config {
    filters_config {
      type            = "HATE"
      input_strength  = "HIGH"
      output_strength = "HIGH"
    }
    filters_config {
      type            = "INSULTS"
      input_strength  = "HIGH"
      output_strength = "HIGH"
    }
    filters_config {
      type            = "SEXUAL"
      input_strength  = "HIGH"
      output_strength = "HIGH"
    }
    filters_config {
      type            = "VIOLENCE"
      input_strength  = "HIGH"
      output_strength = "HIGH"
    }
    filters_config {
      type            = "MISCONDUCT"
      input_strength  = "HIGH"
      output_strength = "HIGH"
    }
    filters_config {
      type           = "PROMPT_ATTACK"
      input_strength = "HIGH"
      # PROMPT_ATTACK only applies to input
      output_strength = "NONE"
    }
  }

  # --- Sensitive Information (PII) — MASK strategy ---
  sensitive_information_policy_config {
    pii_entities_config {
      type   = "US_SOCIAL_SECURITY_NUMBER"
      action = "ANONYMIZE"
    }
    pii_entities_config {
      type   = "EMAIL"
      action = "ANONYMIZE"
    }
    pii_entities_config {
      type   = "PHONE"
      action = "ANONYMIZE"
    }
    pii_entities_config {
      type   = "CREDIT_DEBIT_CARD_NUMBER"
      action = "ANONYMIZE"
    }

    # Custom regex pattern for Polish PESEL number (11 digits)
    regexes_config {
      name        = "PESEL"
      description = "Polish national identification number (11 digits)"
      pattern     = "\\b\\d{11}\\b"
      action      = "ANONYMIZE"
    }
  }

  # --- Denied Topics ---
  topic_policy_config {
    topics_config {
      name       = "Investment Advice"
      definition = "Providing specific investment recommendations, stock picks, portfolio allocation advice, or financial planning guidance."
      type       = "DENY"
      examples   = [
        "Should I buy NVIDIA stock?",
        "What percentage of my portfolio should be in bonds?",
        "Is now a good time to invest in crypto?"
      ]
    }
    topics_config {
      name       = "Medical Diagnosis"
      definition = "Providing specific medical diagnoses, treatment recommendations, or medication advice based on symptoms."
      type       = "DENY"
      examples   = [
        "I have a headache and fever, what disease do I have?",
        "Should I take ibuprofen or acetaminophen for my pain?",
        "Based on my symptoms, do I have diabetes?"
      ]
    }
  }

  # --- Word Filters ---
  word_policy_config {
    words_config {
      text = "CompetitorCorp"
    }
    words_config {
      text = "RivalAI"
    }
    words_config {
      text = "BetterCloud"
    }

    managed_word_lists_config {
      type = "PROFANITY"
    }
  }

  # --- Contextual Grounding ---
  contextual_grounding_policy_config {
    filters_config {
      type      = "GROUNDING"
      threshold = 0.7
    }
    filters_config {
      type      = "RELEVANCE"
      threshold = 0.7
    }
  }
}

# Create a version of the MASK guardrail
resource "aws_bedrock_guardrail_version" "mask" {
  guardrail_arn = aws_bedrock_guardrail.mask.guardrail_arn
  description   = "Version 1 — all six filter types with MASK PII strategy"
}

# -----------------------------------------------------------------------------
# Guardrail B: BLOCK strategy — entire response is blocked if PII is detected
# Use when PII must NEVER appear in output, even partially.
# -----------------------------------------------------------------------------
resource "aws_bedrock_guardrail" "block" {
  name                      = "${var.project_name}-block"
  description               = "Guardrail with BLOCK strategy — blocks response entirely if PII detected"
  blocked_input_messaging   = "Your request contains content that is not allowed."
  blocked_outputs_messaging = "The model response was filtered by security guardrails."

  # Same content filters as MASK variant
  content_policy_config {
    filters_config {
      type            = "HATE"
      input_strength  = "HIGH"
      output_strength = "HIGH"
    }
    filters_config {
      type            = "INSULTS"
      input_strength  = "HIGH"
      output_strength = "HIGH"
    }
    filters_config {
      type            = "SEXUAL"
      input_strength  = "HIGH"
      output_strength = "HIGH"
    }
    filters_config {
      type            = "VIOLENCE"
      input_strength  = "HIGH"
      output_strength = "HIGH"
    }
    filters_config {
      type            = "MISCONDUCT"
      input_strength  = "HIGH"
      output_strength = "HIGH"
    }
    filters_config {
      type           = "PROMPT_ATTACK"
      input_strength = "HIGH"
      output_strength = "NONE"
    }
  }

  # --- Sensitive Information (PII) — BLOCK strategy ---
  sensitive_information_policy_config {
    pii_entities_config {
      type   = "US_SOCIAL_SECURITY_NUMBER"
      action = "BLOCK"
    }
    pii_entities_config {
      type   = "EMAIL"
      action = "BLOCK"
    }
    pii_entities_config {
      type   = "PHONE"
      action = "BLOCK"
    }
    pii_entities_config {
      type   = "CREDIT_DEBIT_CARD_NUMBER"
      action = "BLOCK"
    }

    regexes_config {
      name        = "PESEL"
      description = "Polish national identification number (11 digits)"
      pattern     = "\\b\\d{11}\\b"
      action      = "BLOCK"
    }
  }

  # Same denied topics
  topic_policy_config {
    topics_config {
      name       = "Investment Advice"
      definition = "Providing specific investment recommendations, stock picks, portfolio allocation advice, or financial planning guidance."
      type       = "DENY"
      examples   = [
        "Should I buy NVIDIA stock?",
        "What percentage of my portfolio should be in bonds?",
        "Is now a good time to invest in crypto?"
      ]
    }
    topics_config {
      name       = "Medical Diagnosis"
      definition = "Providing specific medical diagnoses, treatment recommendations, or medication advice based on symptoms."
      type       = "DENY"
      examples   = [
        "I have a headache and fever, what disease do I have?",
        "Should I take ibuprofen or acetaminophen for my pain?",
        "Based on my symptoms, do I have diabetes?"
      ]
    }
  }

  # Same word filters
  word_policy_config {
    words_config {
      text = "CompetitorCorp"
    }
    words_config {
      text = "RivalAI"
    }
    words_config {
      text = "BetterCloud"
    }

    managed_word_lists_config {
      type = "PROFANITY"
    }
  }

  # Same contextual grounding
  contextual_grounding_policy_config {
    filters_config {
      type      = "GROUNDING"
      threshold = 0.7
    }
    filters_config {
      type      = "RELEVANCE"
      threshold = 0.7
    }
  }
}

# Create a version of the BLOCK guardrail
resource "aws_bedrock_guardrail_version" "block" {
  guardrail_arn = aws_bedrock_guardrail.block.guardrail_arn
  description   = "Version 1 — all six filter types with BLOCK PII strategy"
}
