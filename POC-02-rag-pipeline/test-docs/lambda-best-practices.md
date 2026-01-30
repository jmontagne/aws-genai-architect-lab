# AWS Lambda Best Practices

## Overview

AWS Lambda is a serverless compute service that runs your code in response to events. This guide covers best practices for building efficient, reliable, and cost-effective Lambda functions.

## Cold Start Optimization

### Understanding Cold Starts

A cold start occurs when Lambda needs to create a new execution environment. This includes:
1. Downloading your code
2. Starting the runtime
3. Running initialization code

Cold starts typically add 100ms to several seconds of latency.

### Strategies to Reduce Cold Starts

**1. Provisioned Concurrency**
- Pre-warms execution environments
- Eliminates cold starts for configured capacity
- Best for latency-sensitive applications
- Additional cost: $0.000004463 per GB-second

**2. Optimize Package Size**
- Keep deployment packages small
- Remove unused dependencies
- Use Lambda Layers for shared libraries
- Target: Under 50MB zipped

**3. Choose Appropriate Memory**
- More memory = more CPU
- Often faster initialization
- Test different memory settings
- Use AWS Lambda Power Tuning

**4. Minimize Initialization Code**
- Move initialization outside handler
- Lazy load dependencies
- Use connection pooling
- Cache frequently used data

### Runtime-Specific Tips

**Java:**
- Use GraalVM native images
- Consider SnapStart for Java 11+
- Minimize reflection usage
- Pre-warm JIT compilation

**Python:**
- Import only needed modules
- Use slim base images
- Consider compiled extensions

**Node.js:**
- Tree-shake unused code
- Use ES modules
- Minimize package.json dependencies

## Memory and Performance

### Memory Configuration

- Range: 128 MB to 10,240 MB
- CPU scales linearly with memory
- Network bandwidth scales with memory

### Finding Optimal Memory

1. Start with 512 MB
2. Run AWS Lambda Power Tuning
3. Balance cost vs. latency
4. Monitor with CloudWatch

### Example Memory Impact

| Memory | Duration | Cost |
|--------|----------|------|
| 128 MB | 3000 ms | $0.0000625 |
| 512 MB | 800 ms | $0.0000667 |
| 1024 MB | 400 ms | $0.0000667 |
| 2048 MB | 200 ms | $0.0000667 |

*Often, more memory is faster AND cheaper!*

## Error Handling and Resilience

### Implement Retry Logic

```java
// Exponential backoff example
int maxRetries = 3;
int baseDelay = 100;

for (int i = 0; i < maxRetries; i++) {
    try {
        return callExternalService();
    } catch (TransientException e) {
        if (i == maxRetries - 1) throw e;
        Thread.sleep(baseDelay * (long) Math.pow(2, i));
    }
}
```

### Dead Letter Queues (DLQ)

- Configure DLQ for async invocations
- Use SQS or SNS as targets
- Monitor DLQ for failures
- Implement reprocessing logic

### Circuit Breaker Pattern

- Prevent cascading failures
- Fast-fail when dependencies are down
- Gradual recovery
- Use libraries like Resilience4j

## Security Best Practices

### IAM Permissions

- Follow least privilege principle
- Use resource-based policies
- Avoid wildcards in permissions
- Regular permission audits

### Secrets Management

- Use AWS Secrets Manager
- Or AWS Systems Manager Parameter Store
- Never hardcode credentials
- Rotate secrets regularly

### VPC Considerations

- Only use VPC when necessary
- VPC adds cold start latency
- Use VPC endpoints for AWS services
- Consider PrivateLink

## Monitoring and Observability

### CloudWatch Metrics

Key metrics to monitor:
- Invocations
- Duration
- Errors
- Throttles
- ConcurrentExecutions
- IteratorAge (for streams)

### Structured Logging

```java
// Good logging practice
logger.info(Map.of(
    "event", "processing_complete",
    "requestId", context.getAwsRequestId(),
    "duration", processingTime,
    "itemsProcessed", count
));
```

### X-Ray Tracing

- Enable active tracing
- Add custom subsegments
- Track downstream calls
- Identify bottlenecks

## Cost Optimization

### Right-Size Functions

- Use AWS Lambda Power Tuning
- Analyze invocation patterns
- Consider reserved concurrency
- Review unused functions

### Efficient Invocation Patterns

- Batch processing where possible
- Use SQS batch size optimization
- Implement request coalescing
- Consider Step Functions for orchestration

### Arm64 Architecture

- Up to 34% better price-performance
- Available for most runtimes
- Test compatibility first
- Use Graviton2 processors

## Deployment Best Practices

### Versioning and Aliases

- Use versions for immutable deployments
- Create aliases for environments (dev, prod)
- Enable gradual deployments
- Implement canary releases

### Infrastructure as Code

- Use AWS SAM or CDK
- Version control your infrastructure
- Implement CI/CD pipelines
- Test infrastructure changes

### Environment Variables

- Use for configuration
- Encrypt sensitive values
- Different values per alias
- Avoid large configurations
