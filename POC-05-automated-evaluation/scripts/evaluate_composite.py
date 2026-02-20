"""
Composite Evaluation Dashboard -- All Metrics in One View

Runs ROUGE, BLEU, and BERTScore across the full dataset and produces
a summary dashboard showing which metric is best suited for which task type.

Cost: $0 (all local computation)

Exam relevance (AIF-C01):
- Demonstrates choosing the RIGHT metric for the RIGHT task
- ROUGE -> summarization, BLEU -> translation, BERTScore -> all (semantic)
- Shows how different metrics rank the same outputs differently

Usage:
    python scripts/evaluate_composite.py
"""

import json
import os

from rouge_score import rouge_scorer
from nltk.translate.bleu_score import sentence_bleu, SmoothingFunction
from bert_score import score as bert_score


DATASET_PATH = os.path.join(
    os.path.dirname(__file__), "..", "datasets", "ground_truth.json"
)


def load_dataset() -> list[dict]:
    with open(DATASET_PATH, "r", encoding="utf-8") as f:
        return json.load(f)


def compute_rouge_f1(reference: str, generated: str) -> float:
    """Compute ROUGE-L F-measure."""
    scorer = rouge_scorer.RougeScorer(["rougeL"], use_stemmer=True)
    scores = scorer.score(reference, generated)
    return round(scores["rougeL"].fmeasure, 4)


def compute_bleu(reference: str, generated: str) -> float:
    """Compute BLEU score."""
    ref_tokens = reference.lower().split()
    gen_tokens = generated.lower().split()
    smoothing = SmoothingFunction().method1
    return round(
        sentence_bleu([ref_tokens], gen_tokens, smoothing_function=smoothing), 4
    )


def compute_bertscore_batch(references: list[str], generated: list[str]) -> list[float]:
    """Compute BERTScore F1 for a batch."""
    _, _, F1 = bert_score(cands=generated, refs=references, lang="en", verbose=False)
    return [round(f.item(), 4) for f in F1]


def main():
    dataset = load_dataset()

    print("=" * 70)
    print("POC-05: Composite Evaluation Dashboard")
    print(f"Dataset: {len(dataset)} examples")
    print("Metrics: ROUGE-L, BLEU, BERTScore")
    print("Cost: $0 (all local computation)")
    print("=" * 70)

    # Compute ROUGE and BLEU per example
    rouge_scores = []
    bleu_scores = []
    for item in dataset:
        rouge_scores.append(compute_rouge_f1(item["reference_answer"], item["generated_answer"]))
        bleu_scores.append(compute_bleu(item["reference_answer"], item["generated_answer"]))

    # Compute BERTScore in batch
    print("\nComputing BERTScore...")
    references = [item["reference_answer"] for item in dataset]
    generated = [item["generated_answer"] for item in dataset]
    bertscore_f1s = compute_bertscore_batch(references, generated)

    # Per-example table
    print(f"\n{'='*70}")
    print(f"  {'ID':<10} {'Task':<25} {'ROUGE-L':>8} {'BLEU':>8} {'BERT-F1':>8}")
    print(f"  {'--'*35}")
    for i, item in enumerate(dataset):
        print(
            f"  {item['id']:<10} {item['task_type']:<25} "
            f"{rouge_scores[i]:>8.4f} {bleu_scores[i]:>8.4f} {bertscore_f1s[i]:>8.4f}"
        )

    # Averages by task type
    task_types = sorted(set(item["task_type"] for item in dataset))
    print(f"\n{'='*70}")
    print("AVERAGES BY TASK TYPE")
    print(f"  {'Task':<25} {'ROUGE-L':>8} {'BLEU':>8} {'BERT-F1':>8} {'Best Metric':<15}")
    print(f"  {'--'*40}")

    metric_recommendation = {
        "summarization": "ROUGE",
        "question_answering": "BERTScore",
        "hallucination_detection": "BERTScore",
        "translation": "BLEU",
    }

    for task_type in task_types:
        indices = [i for i, item in enumerate(dataset) if item["task_type"] == task_type]
        avg_rouge = sum(rouge_scores[i] for i in indices) / len(indices)
        avg_bleu = sum(bleu_scores[i] for i in indices) / len(indices)
        avg_bert = sum(bertscore_f1s[i] for i in indices) / len(indices)
        best = metric_recommendation.get(task_type, "BERTScore")
        print(
            f"  {task_type:<25} {avg_rouge:>8.4f} {avg_bleu:>8.4f} "
            f"{avg_bert:>8.4f} {best:<15}"
        )

    # Overall
    avg_rouge = sum(rouge_scores) / len(rouge_scores)
    avg_bleu = sum(bleu_scores) / len(bleu_scores)
    avg_bert = sum(bertscore_f1s) / len(bertscore_f1s)
    print(f"  {'--'*40}")
    print(f"  {'OVERALL':<25} {avg_rouge:>8.4f} {avg_bleu:>8.4f} {avg_bert:>8.4f}")

    print(f"\n{'='*70}")
    print("METRIC SELECTION GUIDE (Exam Key Knowledge)")
    print(f"{'--'*35}")
    print("  Task Type               -> Recommended Metric")
    print("  Summarization           -> ROUGE (recall-oriented, captures coverage)")
    print("  Translation             -> BLEU (precision-oriented, n-gram matching)")
    print("  Q&A / General           -> BERTScore (semantic similarity, paraphrase-robust)")
    print("  Hallucination detection -> BERTScore + manual review")
    print("  All tasks (baseline)    -> BERTScore (most robust, but slower)")
    print(f"{'='*70}")


if __name__ == "__main__":
    main()
