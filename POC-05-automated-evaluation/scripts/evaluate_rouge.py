"""
ROUGE Evaluation -- Recall-Oriented Understudy for Gisting Evaluation

Demonstrates ROUGE-1, ROUGE-2, and ROUGE-L metrics for summarization tasks.
ROUGE is recall-oriented: it measures how much of the REFERENCE appears in the GENERATED text.

Cost: $0 (runs locally, no API calls)

Exam relevance (AIF-C01):
- ROUGE is the standard metric for SUMMARIZATION evaluation
- ROUGE-1 = unigram overlap, ROUGE-2 = bigram overlap, ROUGE-L = longest common subsequence
- Higher recall = generated text captures more of the reference content

Usage:
    python scripts/evaluate_rouge.py
"""

import json
import os

from rouge_score import rouge_scorer


DATASET_PATH = os.path.join(
    os.path.dirname(__file__), "..", "datasets", "ground_truth.json"
)


def load_dataset(task_type: str = "summarization") -> list[dict]:
    """Load ground truth examples filtered by task type."""
    with open(DATASET_PATH, "r", encoding="utf-8") as f:
        data = json.load(f)
    return [item for item in data if item["task_type"] == task_type]


def compute_rouge(reference: str, generated: str) -> dict:
    """Compute ROUGE-1, ROUGE-2, and ROUGE-L scores."""
    scorer = rouge_scorer.RougeScorer(
        ["rouge1", "rouge2", "rougeL"], use_stemmer=True
    )
    scores = scorer.score(reference, generated)
    return {
        metric: {
            "precision": round(score.precision, 4),
            "recall": round(score.recall, 4),
            "fmeasure": round(score.fmeasure, 4),
        }
        for metric, score in scores.items()
    }


def print_scores(item_id: str, prompt: str, scores: dict):
    """Pretty-print ROUGE scores for a single example."""
    print(f"\n{'='*70}")
    print(f"ID: {item_id}")
    print(f"PROMPT: {prompt[:80]}...")
    print(f"{'--'*35}")
    print(f"  {'Metric':<10} {'Precision':>10} {'Recall':>10} {'F-measure':>10}")
    print(f"  {'--'*20}")
    for metric, values in scores.items():
        print(
            f"  {metric:<10} {values['precision']:>10.4f} "
            f"{values['recall']:>10.4f} {values['fmeasure']:>10.4f}"
        )


def main():
    dataset = load_dataset("summarization")

    print("=" * 70)
    print("POC-05: ROUGE Evaluation (Summarization)")
    print(f"Dataset: {len(dataset)} summarization examples")
    print("Cost: $0 (local computation)")
    print("=" * 70)

    all_scores = []
    for item in dataset:
        scores = compute_rouge(item["reference_answer"], item["generated_answer"])
        print_scores(item["id"], item["prompt"], scores)
        all_scores.append(scores)

    # Compute averages
    print(f"\n{'='*70}")
    print("AVERAGE SCORES")
    print(f"{'--'*35}")
    for metric in ["rouge1", "rouge2", "rougeL"]:
        avg_p = sum(s[metric]["precision"] for s in all_scores) / len(all_scores)
        avg_r = sum(s[metric]["recall"] for s in all_scores) / len(all_scores)
        avg_f = sum(s[metric]["fmeasure"] for s in all_scores) / len(all_scores)
        print(f"  {metric:<10} P={avg_p:.4f}  R={avg_r:.4f}  F={avg_f:.4f}")

    print(f"\n{'='*70}")
    print("INTERPRETATION GUIDE")
    print("  ROUGE-1:  Unigram overlap -- captures key term coverage")
    print("  ROUGE-2:  Bigram overlap  -- captures phrase-level similarity")
    print("  ROUGE-L:  Longest common subsequence -- captures sentence structure")
    print("  Recall:   How much of the REFERENCE is captured in generated text")
    print("  Precision: How much of the GENERATED text matches the reference")
    print("  F-measure: Harmonic mean of precision and recall")
    print(f"{'='*70}")


if __name__ == "__main__":
    main()
