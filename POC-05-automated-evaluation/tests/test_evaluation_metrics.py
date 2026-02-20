"""
Unit tests for evaluation metrics and Bedrock API response parsing.
No AWS credentials needed -- all Bedrock calls are mocked.

Tests validate:
- ROUGE score computation and interpretation
- BLEU score computation and interpretation
- Ground truth dataset format validation
- Bedrock evaluation job response parsing
- Evaluation job lifecycle (create, in-progress, completed, failed)
"""

import json
import os

from rouge_score import rouge_scorer
from nltk.translate.bleu_score import sentence_bleu, SmoothingFunction


DATASET_PATH = os.path.join(
    os.path.dirname(__file__), "..", "datasets", "ground_truth.json"
)


class TestRougeScores:
    """Tests for ROUGE metric computation."""

    def test_identical_texts_return_perfect_score(self):
        """Identical reference and generated text should score 1.0."""
        scorer = rouge_scorer.RougeScorer(["rouge1", "rougeL"], use_stemmer=True)
        text = "Amazon Bedrock is a managed service for foundation models."
        scores = scorer.score(text, text)

        assert scores["rouge1"].fmeasure == 1.0
        assert scores["rougeL"].fmeasure == 1.0

    def test_completely_different_texts_score_low(self):
        """Completely different texts should have low ROUGE scores."""
        scorer = rouge_scorer.RougeScorer(["rouge1"], use_stemmer=True)
        reference = "The cat sat on the mat."
        generated = "Purple elephants dance freely."
        scores = scorer.score(reference, generated)

        assert scores["rouge1"].fmeasure < 0.2

    def test_partial_overlap_scores_moderate(self):
        """Partial overlap should produce moderate ROUGE scores."""
        scorer = rouge_scorer.RougeScorer(["rouge1"], use_stemmer=True)
        reference = "Amazon Bedrock provides access to foundation models."
        generated = "Amazon Bedrock is a service for AI models."
        scores = scorer.score(reference, generated)

        assert 0.2 < scores["rouge1"].fmeasure < 0.9

    def test_rouge_returns_precision_recall_fmeasure(self):
        """ROUGE scorer should return all three components."""
        scorer = rouge_scorer.RougeScorer(["rouge1"], use_stemmer=True)
        scores = scorer.score("the quick brown fox", "the quick red fox")

        score = scores["rouge1"]
        assert hasattr(score, "precision")
        assert hasattr(score, "recall")
        assert hasattr(score, "fmeasure")
        assert 0.0 <= score.precision <= 1.0
        assert 0.0 <= score.recall <= 1.0
        assert 0.0 <= score.fmeasure <= 1.0

    def test_rouge_recall_higher_when_generated_is_superset(self):
        """When generated contains all reference words plus more, recall should be high."""
        scorer = rouge_scorer.RougeScorer(["rouge1"], use_stemmer=True)
        reference = "Bedrock provides models."
        generated = "Amazon Bedrock provides foundation models and fine-tuning."
        scores = scorer.score(reference, generated)

        assert scores["rouge1"].recall > scores["rouge1"].precision


class TestBleuScores:
    """Tests for BLEU metric computation."""

    def test_identical_texts_return_high_score(self):
        """Identical texts should produce a high BLEU score."""
        ref = "the cat sat on the mat".split()
        gen = "the cat sat on the mat".split()
        score = sentence_bleu([ref], gen)

        assert score > 0.9

    def test_completely_different_texts_score_near_zero(self):
        """Completely different texts should score near zero."""
        ref = "the cat sat on the mat".split()
        gen = "purple elephants dance freely".split()
        smoothing = SmoothingFunction().method1
        score = sentence_bleu([ref], gen, smoothing_function=smoothing)

        assert score < 0.1

    def test_bleu_penalizes_short_translations(self):
        """BLEU brevity penalty should reduce score for short outputs."""
        ref = "the quick brown fox jumps over the lazy dog".split()
        gen_short = "the quick fox".split()
        gen_full = "the quick brown fox jumps over the lazy dog".split()
        smoothing = SmoothingFunction().method1

        score_short = sentence_bleu([ref], gen_short, smoothing_function=smoothing)
        score_full = sentence_bleu([ref], gen_full, smoothing_function=smoothing)

        assert score_full > score_short

    def test_bleu_individual_ngram_precision(self):
        """Individual n-gram BLEU scores should be computable."""
        ref = "the cat sat on the mat".split()
        gen = "the cat sat on a mat".split()
        smoothing = SmoothingFunction().method1

        bleu_1 = sentence_bleu([ref], gen, weights=(1, 0, 0, 0), smoothing_function=smoothing)
        bleu_2 = sentence_bleu([ref], gen, weights=(0, 1, 0, 0), smoothing_function=smoothing)

        # Unigram precision should be higher than bigram for near-match
        assert bleu_1 > bleu_2


class TestGroundTruthDataset:
    """Tests for ground truth dataset format and completeness."""

    def test_dataset_file_exists(self):
        """Ground truth dataset file should exist."""
        assert os.path.exists(DATASET_PATH), f"Dataset not found at {DATASET_PATH}"

    def test_dataset_is_valid_json(self):
        """Dataset should be valid JSON."""
        with open(DATASET_PATH, "r", encoding="utf-8") as f:
            data = json.load(f)
        assert isinstance(data, list)

    def test_dataset_has_required_fields(self):
        """Each example should have all required fields."""
        with open(DATASET_PATH, "r", encoding="utf-8") as f:
            data = json.load(f)

        required_fields = {"id", "task_type", "prompt", "reference_answer", "generated_answer"}
        for item in data:
            missing = required_fields - set(item.keys())
            assert not missing, f"Item {item.get('id', '?')} missing fields: {missing}"

    def test_dataset_has_multiple_task_types(self):
        """Dataset should cover at least 3 different task types."""
        with open(DATASET_PATH, "r", encoding="utf-8") as f:
            data = json.load(f)

        task_types = set(item["task_type"] for item in data)
        assert len(task_types) >= 3, f"Only {len(task_types)} task types: {task_types}"

    def test_dataset_has_minimum_examples(self):
        """Dataset should have at least 10 examples."""
        with open(DATASET_PATH, "r", encoding="utf-8") as f:
            data = json.load(f)

        assert len(data) >= 10, f"Only {len(data)} examples, need at least 10"

    def test_dataset_ids_are_unique(self):
        """All example IDs should be unique."""
        with open(DATASET_PATH, "r", encoding="utf-8") as f:
            data = json.load(f)

        ids = [item["id"] for item in data]
        assert len(ids) == len(set(ids)), f"Duplicate IDs found"

    def test_dataset_has_summarization_examples(self):
        """Dataset should include summarization examples for ROUGE evaluation."""
        with open(DATASET_PATH, "r", encoding="utf-8") as f:
            data = json.load(f)

        summarization = [item for item in data if item["task_type"] == "summarization"]
        assert len(summarization) >= 2, "Need at least 2 summarization examples"

    def test_dataset_has_translation_examples(self):
        """Dataset should include translation examples for BLEU evaluation."""
        with open(DATASET_PATH, "r", encoding="utf-8") as f:
            data = json.load(f)

        translation = [item for item in data if item["task_type"] == "translation"]
        assert len(translation) >= 2, "Need at least 2 translation examples"


class TestBedrockEvaluationJobParsing:
    """Tests for Bedrock Model Evaluation Job API response parsing."""

    def test_create_job_returns_arn(self, mock_bedrock_client, create_job_response):
        """CreateModelEvaluationJob should return a job ARN."""
        mock_bedrock_client.create_model_evaluation_job.return_value = create_job_response

        response = mock_bedrock_client.create_model_evaluation_job(
            jobName="test-eval",
            roleArn="arn:aws:iam::123456789012:role/test-role",
            evaluationConfig={"automated": {}},
            inferenceConfig={"models": []},
            outputDataConfig={"s3Uri": "s3://test-bucket/results/"},
        )

        assert "jobArn" in response
        assert response["jobArn"].startswith("arn:aws:bedrock:")

    def test_get_completed_job_has_status(self, mock_bedrock_client, get_job_completed_response):
        """Completed evaluation job should have status=Completed."""
        mock_bedrock_client.get_model_evaluation_job.return_value = get_job_completed_response

        response = mock_bedrock_client.get_model_evaluation_job(
            jobIdentifier="arn:aws:bedrock:us-east-1:123456789012:evaluation-job/abc"
        )

        assert response["status"] == "Completed"
        assert "outputDataConfig" in response
        assert response["outputDataConfig"]["s3Uri"].startswith("s3://")

    def test_in_progress_job_has_no_results(self, mock_bedrock_client, get_job_in_progress_response):
        """In-progress job should not have evaluation results."""
        mock_bedrock_client.get_model_evaluation_job.return_value = get_job_in_progress_response

        response = mock_bedrock_client.get_model_evaluation_job(
            jobIdentifier="arn:aws:bedrock:us-east-1:123456789012:evaluation-job/abc"
        )

        assert response["status"] == "InProgress"
        assert "evaluationResults" not in response

    def test_failed_job_has_failure_messages(self, mock_bedrock_client, get_job_failed_response):
        """Failed evaluation job should include failure messages."""
        mock_bedrock_client.get_model_evaluation_job.return_value = get_job_failed_response

        response = mock_bedrock_client.get_model_evaluation_job(
            jobIdentifier="arn:aws:bedrock:us-east-1:123456789012:evaluation-job/abc"
        )

        assert response["status"] == "Failed"
        assert "failureMessages" in response
        assert len(response["failureMessages"]) > 0

    def test_list_jobs_returns_summaries(self, mock_bedrock_client, list_jobs_response):
        """ListModelEvaluationJobs should return job summaries."""
        mock_bedrock_client.list_model_evaluation_jobs.return_value = list_jobs_response

        response = mock_bedrock_client.list_model_evaluation_jobs()

        assert "jobSummaries" in response
        assert len(response["jobSummaries"]) == 2

        first_job = response["jobSummaries"][0]
        assert "jobName" in first_job
        assert "status" in first_job
        assert "jobType" in first_job

    def test_evaluation_job_lifecycle(self, mock_bedrock_client, create_job_response, get_job_in_progress_response, get_job_completed_response):
        """Evaluation job should follow lifecycle: Created -> InProgress -> Completed."""
        # Step 1: Create
        mock_bedrock_client.create_model_evaluation_job.return_value = create_job_response
        create_resp = mock_bedrock_client.create_model_evaluation_job(
            jobName="lifecycle-test",
            roleArn="arn:aws:iam::123456789012:role/test",
            evaluationConfig={"automated": {}},
            inferenceConfig={"models": []},
            outputDataConfig={"s3Uri": "s3://bucket/results/"},
        )
        assert "jobArn" in create_resp

        # Step 2: Check status (InProgress)
        mock_bedrock_client.get_model_evaluation_job.return_value = get_job_in_progress_response
        status_resp = mock_bedrock_client.get_model_evaluation_job(
            jobIdentifier=create_resp["jobArn"]
        )
        assert status_resp["status"] == "InProgress"

        # Step 3: Check status (Completed)
        mock_bedrock_client.get_model_evaluation_job.return_value = get_job_completed_response
        final_resp = mock_bedrock_client.get_model_evaluation_job(
            jobIdentifier=create_resp["jobArn"]
        )
        assert final_resp["status"] == "Completed"
