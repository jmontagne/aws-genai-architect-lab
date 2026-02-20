"""
BERTScore Evaluation -- Semantic Similarity via Contextual Embeddings

Demonstrates BERTScore which uses pre-trained BERT embeddings to compute
semantic similarity. Unlike BLEU/ROUGE, BERTScore captures meaning even
when different words are used (paraphrasing, synonyms).

Cost: $0 (runs locally using pre-trained model on CPU)

Exam relevance (AIF-C01):
- BERTScore measures SEMANTIC ALIGNMENT between texts
- Robust to paraphrasing (unlike BLEU which needs exact n-gram matches)
- Uses cosine similarity of contextual embeddings
- Available as a programmatic metric in Bedrock Model Evaluation

Usage:
    python scripts/evaluate_bertscore.py

Note: First run downloads the model (~400MB). Subsequent runs use cache.
"""

import json
import os

from bert_score import score as bert_score


DATASET_PATH = os.path.join(
    os.path.dirname(__file__), "..", "datasets", "ground_truth.json"
)


def load_dataset() -> list[dict]:
    """Load all ground truth examples (BERTScore works across all task types)."""
    with open(DATASET_PATH, "r", encoding="utf-8") as f:
        return json.load(f)


def compute_bertscore(references: list[str], generated: list[str]) -> dict:
    """Compute BERTScore for a batch of reference/generated pairs.

    Returns per-example and average precision, recall, F1.
    """
    P, R, F1 = bert_score(
        cands=generated,
        refs=references,
        lang="en",
        verbose=False,
    )
    return {
        "precision": [round(p.item(), 4) for p in P],
        "recall": [round(r.item(), 4) for r in R],
        "f1": [round(f.item(), 4) for f in F1],
    }


def main():
    dataset = load_dataset()

    print("=" * 70)
    print("POC-05: BERTScore Evaluation (Semantic Similarity)")
    print(f"Dataset: {len(dataset)} examples across all task types")
    print("Cost: $0 (local computation with pre-trained model)")
    print("=" * 70)

    references = [item["reference_answer"] for item in dataset]
    generated = [item["generated_answer"] for item in dataset]

    print("\nComputing BERTScore (first run downloads model ~400MB)...")
    scores = compute_bertscore(references, generated)

    # Print per-example scores
    for i, item in enumerate(dataset):
        print(f"\n{'='*70}")
        print(f"ID: {item['id']} ({item['task_type']})")
        print(f"PROMPT: {item['prompt'][:80]}...")
        print(f"{'--'*35}")
        print(f"  Precision: {scores['precision'][i]:.4f}")
        print(f"  Recall:    {scores['recall'][i]:.4f}")
        print(f"  F1:        {scores['f1'][i]:.4f}")

    # Print averages by task type
    task_types = set(item["task_type"] for item in dataset)
    print(f"\n{'='*70}")
    print("AVERAGE SCORES BY TASK TYPE")
    print(f"{'--'*35}")
    for task_type in sorted(task_types):
        indices = [i for i, item in enumerate(dataset) if item["task_type"] == task_type]
        avg_f1 = sum(scores["f1"][i] for i in indices) / len(indices)
        avg_p = sum(scores["precision"][i] for i in indices) / len(indices)
        avg_r = sum(scores["recall"][i] for i in indices) / len(indices)
        print(f"  {task_type:<25} P={avg_p:.4f}  R={avg_r:.4f}  F1={avg_f1:.4f}")

    # Overall average
    avg_f1 = sum(scores["f1"]) / len(scores["f1"])
    print(f"\n  {'OVERALL':<25} F1={avg_f1:.4f}")

    print(f"\n{'='*70}")
    print("INTERPRETATION GUIDE")
    print("  BERTScore uses contextual embeddings (not exact word matching)")
    print("  Captures SEMANTIC similarity -- paraphrases score high")
    print("  Precision: How much of generated content is semantically in reference")
    print("  Recall:    How much of reference content is semantically in generated")
    print("  F1:        Harmonic mean -- the primary metric to report")
    print("  Typical F1 ranges: >0.85 = good, >0.90 = very good, >0.95 = excellent")
    print(f"{'='*70}")


if __name__ == "__main__":
    main()
