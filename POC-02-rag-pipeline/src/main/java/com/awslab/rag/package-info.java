/**
 * POC-02: RAG Pipeline with Amazon Bedrock Knowledge Bases.
 *
 * <p>A complete Retrieval-Augmented Generation implementation that solves the LLM
 * hallucination problem by grounding answers in source documents. Implements two
 * retrieval patterns and an LLM-as-Judge evaluation framework.</p>
 *
 * <h2>Two Retrieval Patterns</h2>
 * <ul>
 *   <li><b>Retrieve (raw chunks):</b> {@link com.awslab.rag.service.RetrievalService} —
 *       returns document chunks with relevance scores. Use when you need custom prompts
 *       or post-processing.</li>
 *   <li><b>RetrieveAndGenerate (end-to-end):</b> {@link com.awslab.rag.service.RagService} —
 *       returns a generated answer with source citations and text span mapping.
 *       Use when you need an out-of-the-box RAG answer with attribution.</li>
 * </ul>
 *
 * <h2>Key Findings</h2>
 * <table>
 *   <tr><th>Aspect</th><th>Detail</th></tr>
 *   <tr><td>Vector store</td><td>S3 Vectors (~$0/month) vs OpenSearch Serverless (~$700/month baseline)</td></tr>
 *   <tr><td>Chunking</td><td>Fixed Size, 300 tokens, 20% overlap — optimal for structured AWS docs</td></tr>
 *   <tr><td>Embedding</td><td>Titan Embeddings V2 (1024 dim, 8K context, $0.00002/1K tokens)</td></tr>
 *   <tr><td>Generation</td><td>Claude 3 Haiku ($0.00025/$0.00125 per 1K tokens)</td></tr>
 *   <tr><td>Total POC cost</td><td>&lt; $0.50/month</td></tr>
 * </table>
 *
 * <h2>Evaluation: LLM-as-Judge</h2>
 * <p>{@link com.awslab.rag.service.EvaluationService} scores RAG quality on two axes:</p>
 * <ul>
 *   <li><b>Relevance (0.0–1.0):</b> Are the retrieved chunks relevant to the query?</li>
 *   <li><b>Groundedness (0.0–1.0):</b> Is the generated answer supported by the chunks?</li>
 * </ul>
 *
 * <h2>Tech Stack</h2>
 * <ul>
 *   <li>Java 21, Spring Boot 3.4, AWS SDK v2 (async)</li>
 *   <li>Bedrock Knowledge Bases, S3 Vectors (cosine similarity, 1024 dim)</li>
 *   <li>Claude 3 Haiku (generation + evaluation), Titan Embeddings V2</li>
 *   <li>Terraform IaC (18 resources, pure IaC — zero scripts)</li>
 *   <li>62 unit tests, all mocked (zero AWS credentials needed)</li>
 * </ul>
 *
 * <h2>AIP-C01 Exam Relevance</h2>
 * <p>Covers ~15-20% of exam: Knowledge Base architecture, chunking strategies,
 * vector store selection, Retrieve vs RetrieveAndGenerate trade-offs,
 * metadata filtering, source attribution, and RAG evaluation metrics.</p>
 *
 * @see com.awslab.rag.service.RetrievalService Pattern: raw chunk retrieval
 * @see com.awslab.rag.service.RagService Pattern: end-to-end RAG with citations
 * @see com.awslab.rag.service.EvaluationService LLM-as-Judge evaluation
 */
package com.awslab.rag;
