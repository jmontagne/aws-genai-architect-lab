"""
BLEU Evaluation -- Bilingual Evaluation Understudy

Demonstrates BLEU score computation for translation tasks.
BLEU is precision-oriented: it measures how much of the GENERATED text appears in the REFERENCE.

Cost: $0 (runs locally, no API calls)

Exam relevance (AIF-C01):
- BLEU is the standard metric for TRANSLATION evaluation
- Measures n-gram precision (1-gram through 4-gram)
- Includes brevity penalty to discourage overly short translations
- Score range: 0.0 (no overlap) to 1.0 (perfect match)

Usage:
    python scripts/evaluate_bleu.py
"""

import json
import os

from nltk.translate.bleu_score import sentence_bleu, SmoothingFunction


DATASET_PATH = os.path.join(
    os.path.dirname(__file__), "..", "datasets", "ground_truth.json"
)


def load_dataset(task_type: str = "translation") -> list[dict]:
    """Load ground truth examples filtered by task type."""
    with open(DATASET_PATH, "r", encoding="utf-8") as f:
        data = json.load(f)
    return [item for item in data if item["task_type"] == task_type]


def compute_bleu(reference: str, generated: str) -> dict:
    """Compute BLEU score with individual n-gram precisions."""
    ref_tokens = reference.lower().split()
    gen_tokens = generated.lower().split()

    smoothing = SmoothingFunction().method1

    # Overall BLEU (weighted 1-4 gram)
    bleu_score = sentence_bleu(
        [ref_tokens], gen_tokens, smoothing_function=smoothing
    )

    # Individual n-gram precisions
    bleu_1 = sentence_bleu(
        [ref_tokens], gen_tokens, weights=(1, 0, 0, 0),
        smoothing_function=smoothing,
    )
    bleu_2 = sentence_bleu(
        [ref_tokens], gen_tokens, weights=(0, 1, 0, 0),
        smoothing_function=smoothing,
    )
    bleu_3 = sentence_bleu(
        [ref_tokens], gen_tokens, weights=(0, 0, 1, 0),
        smoothing_function=smoothing,
    )
    bleu_4 = sentence_bleu(
        [ref_tokens], gen_tokens, weights=(0, 0, 0, 1),
        smoothing_function=smoothing,
    )

    return {
        "bleu": round(bleu_score, 4),
        "bleu_1": round(bleu_1, 4),
        "bleu_2": round(bleu_2, 4),
        "bleu_3": round(bleu_3, 4),
        "bleu_4": round(bleu_4, 4),
    }


def print_scores(item_id: str, prompt: str, scores: dict):
    """Pretty-print BLEU scores for a single example."""
    print(f"\n{'='*70}")
    print(f"ID: {item_id}")
    print(f"PROMPT: {prompt[:80]}...")
    print(f"{'--'*35}")
    print(f"  BLEU (composite): {scores['bleu']:.4f}")
    print(f"  BLEU-1 (unigram): {scores['bleu_1']:.4f}")
    print(f"  BLEU-2 (bigram):  {scores['bleu_2']:.4f}")
    print(f"  BLEU-3 (trigram): {scores['bleu_3']:.4f}")
    print(f"  BLEU-4 (4-gram):  {scores['bleu_4']:.4f}")


def main():
    dataset = load_dataset("translation")

    print("=" * 70)
    print("POC-05: BLEU Evaluation (Translation)")
    print(f"Dataset: {len(dataset)} translation examples")
    print("Cost: $0 (local computation)")
    print("=" * 70)

    all_scores = []
    for item in dataset:
        scores = compute_bleu(item["reference_answer"], item["generated_answer"])
        print_scores(item["id"], item["prompt"], scores)
        all_scores.append(scores)

    # Compute averages
    print(f"\n{'='*70}")
    print("AVERAGE SCORES")
    print(f"{'--'*35}")
    for metric in ["bleu", "bleu_1", "bleu_2", "bleu_3", "bleu_4"]:
        avg = sum(s[metric] for s in all_scores) / len(all_scores)
        print(f"  {metric:<15} {avg:.4f}")

    print(f"\n{'='*70}")
    print("INTERPRETATION GUIDE")
    print("  BLEU measures PRECISION -- how much of generated text matches reference")
    print("  BLEU-1: Word-level overlap (most lenient)")
    print("  BLEU-4: 4-gram overlap (most strict, captures phrase quality)")
    print("  Composite: Geometric mean of BLEU-1 through BLEU-4")
    print("  Brevity penalty applied when generated text is shorter than reference")
    print("  Typical ranges: >0.4 = good, >0.5 = very good, >0.6 = excellent")
    print(f"{'='*70}")


if __name__ == "__main__":
    main()
