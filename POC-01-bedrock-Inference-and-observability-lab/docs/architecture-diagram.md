# POC-01 Architecture Diagrams

Copy each code block into [app.eraser.io](https://app.eraser.io) to generate the diagrams.

---

## 1. Cloud Architecture Diagram

```eraser-cloud-architecture
// POC-01: Bedrock Inference & Observability Lab

direction right

Client [icon: user]

Spring Boot App [icon: spring, color: green] {
  WebFlux Controller [icon: server]
  Inference Service [icon: gears]
  Streaming Client [icon: code]
  AWS SDK v2 Async [icon: aws]
}

AWS [icon: aws] {
  Bedrock [icon: aws-bedrock, color: orange] {
    Claude 3.5 Sonnet [icon: aws-bedrock]
    Claude 3 Haiku [icon: aws-bedrock]
  }

  Logging [icon: aws-cloudwatch, color: blue] {
    CloudWatch [icon: aws-cloudwatch]
    S3 [icon: aws-s3]
  }

  KMS [icon: aws-kms]
  IAM Role [icon: aws-iam]
}

Terraform [icon: terraform, color: purple]

Client > WebFlux Controller: SSE Stream
WebFlux Controller > Inference Service
Inference Service > Streaming Client
Streaming Client > AWS SDK v2 Async
AWS SDK v2 Async > Bedrock: ConverseStream API
Bedrock > CloudWatch: Invocation Logs
Bedrock > S3: Persistent Logs
CloudWatch > KMS: Encrypted
S3 > KMS: Encrypted
IAM Role > CloudWatch: Write
IAM Role > S3: Write
Terraform > Logging: Provisions
Terraform > KMS: Provisions
Terraform > IAM Role: Provisions
```

---

## 2. Sequence Diagram - Streaming Flow

```eraser-sequence
title Bedrock Streaming Inference Flow

Client [icon: user]
Controller [icon: server, label: InferenceController]
Service [icon: gears, label: InferenceService]
StreamingClient [icon: code, label: BedrockStreamingClient]
BedrockSDK [icon: aws, label: AWS SDK v2]
Bedrock [icon: aws-bedrock, label: Amazon Bedrock]
CloudWatch [icon: aws-cloudwatch]

Client > Controller: GET /api/v1/inference/stream/CLAUDE_3_5_SONNET?message=Hello
activate Controller

Controller > Service: streamWithJacquesMontagne(message, model, params)
activate Service

Service > StreamingClient: streamConverse(InferenceRequest)
activate StreamingClient

StreamingClient > StreamingClient: Create Reactor Sink
StreamingClient > BedrockSDK: converseStream(ConverseStreamRequest, handler)
activate BedrockSDK

BedrockSDK > Bedrock: HTTPS POST /converse-stream
activate Bedrock

Bedrock > CloudWatch: Log invocation (async)

loop For each token
  Bedrock --> BedrockSDK: ContentBlockDeltaEvent
  BedrockSDK --> StreamingClient: onEventStream callback
  StreamingClient --> StreamingClient: sink.tryEmitNext(token)
  StreamingClient --> Service: Flux emits token
  Service --> Controller: Flux emits token
  Controller --> Client: data: token\n
end

Bedrock --> BedrockSDK: MessageStopEvent
deactivate Bedrock

BedrockSDK --> StreamingClient: onComplete
deactivate BedrockSDK

StreamingClient --> StreamingClient: sink.tryEmitComplete()
deactivate StreamingClient

Service --> Controller: Flux completes
deactivate Service

Controller --> Client: Stream ends
deactivate Controller
```

---

## 3. Component Diagram - Application Structure

```eraser-component
title Spring Boot Application Components

direction down

External [icon: globe] {
  HTTP Client [icon: user]
}

Presentation [icon: layout, color: blue] {
  InferenceController [icon: server, label: "@RestController\nWebFlux Endpoints\nSSE MediaType"]
  ExceptionHandler [icon: alert-triangle, label: "@RestControllerAdvice\nThrottling → 429\nModelNotReady → 503"]
}

Business [icon: briefcase, color: green] {
  InferenceService [icon: gears, label: "@Service\nJacques Montagne Persona\nModel Comparison"]
}

Integration [icon: plug, color: orange] {
  BedrockStreamingClient [icon: code, label: "@Component\nSinks.Many → Flux\nTTFT Tracking"]
  BedrockClientConfig [icon: settings, label: "@Configuration\nAsync Client\nRetry Policy"]
}

AWS [icon: aws, color: yellow] {
  BedrockRuntimeAsyncClient [icon: aws-bedrock, label: "AWS SDK v2\nNetty NIO\nAdaptive Retry"]
}

Domain [icon: box, color: purple] {
  ModelType [icon: list, label: "enum\nCLAUDE_3_5_SONNET\nCLAUDE_3_HAIKU"]
  InferenceRequest [icon: file-text, label: "record\nsystemPrompt\nuserMessage\nparameters"]
  InferenceResponse [icon: file-text, label: "record\ncontent\nmetrics"]
  PerformanceMetrics [icon: activity, label: "record\nTTFT\ntotalTime\ntokens"]
}

HTTP Client > InferenceController
InferenceController > InferenceService
InferenceController > ExceptionHandler
InferenceService > BedrockStreamingClient
InferenceService > ModelType
InferenceService > InferenceRequest
BedrockStreamingClient > BedrockClientConfig
BedrockClientConfig > BedrockRuntimeAsyncClient
BedrockStreamingClient > PerformanceMetrics
InferenceController > InferenceResponse
```

---

## 4. Infrastructure Diagram - Terraform Resources

```eraser-cloud-architecture
title Terraform Infrastructure - Bedrock Logging Module

direction down

Terraform Module [icon: terraform, color: purple] {
  bedrock-logging [icon: folder]
}

AWS Resources [icon: aws] {
  Encryption [icon: lock, color: red] {
    KMS Key [icon: aws-kms, label: "aws_kms_key\nAuto-rotation\n7-day deletion window"]
    KMS Alias [icon: aws-kms, label: "aws_kms_alias\nalias/bedrock-logging"]
  }

  Storage [icon: database, color: blue] {
    S3 Bucket [icon: aws-s3, label: "aws_s3_bucket\nVersioned\nEncrypted"]
    Lifecycle [icon: refresh-cw, label: "aws_s3_bucket_lifecycle\n30d → Standard-IA\n60d → Glacier\n90d → Expire"]
    Public Block [icon: shield, label: "aws_s3_bucket_public_access_block\nBlock ALL public access"]
  }

  Logging [icon: file-text, color: green] {
    CloudWatch Log Group [icon: aws-cloudwatch, label: "aws_cloudwatch_log_group\n/aws/bedrock/*/invocations\n30-day retention"]
  }

  IAM [icon: aws-iam, color: orange] {
    Bedrock Role [icon: aws-iam, label: "aws_iam_role\nAssumed by bedrock.amazonaws.com"]
    S3 Policy [icon: file, label: "aws_iam_role_policy\ns3:PutObject\nkms:Encrypt"]
    CW Policy [icon: file, label: "aws_iam_role_policy\nlogs:PutLogEvents"]
  }

  Bedrock Config [icon: aws-bedrock, color: orange] {
    Logging Config [icon: settings, label: "aws_bedrock_model_invocation_logging_configuration\ntext_data_delivery_enabled"]
  }
}

bedrock-logging > KMS Key: creates
bedrock-logging > S3 Bucket: creates
bedrock-logging > CloudWatch Log Group: creates
bedrock-logging > Bedrock Role: creates
bedrock-logging > Logging Config: creates

S3 Bucket > KMS Key: encrypted by
CloudWatch Log Group > KMS Key: encrypted by
Logging Config > S3 Bucket: writes to
Logging Config > CloudWatch Log Group: writes to
Logging Config > Bedrock Role: uses
Bedrock Role > S3 Policy: has
Bedrock Role > CW Policy: has
S3 Bucket > Lifecycle: managed by
S3 Bucket > Public Block: protected by
```

---

## 5. Data Flow Diagram

```eraser-flowchart
title Request/Response Data Flow

start [shape: oval, label: "Client Request"]
controller [shape: rectangle, label: "Parse Request\nValidate ModelType\nExtract Parameters"]
service [shape: rectangle, label: "Build InferenceRequest\nAttach System Prompt\nSet Inference Config"]
client [shape: rectangle, label: "Create Reactor Sink\nBuild ConverseStreamRequest"]
bedrock [shape: diamond, label: "Amazon Bedrock\nModel Inference"]
stream [shape: parallelogram, label: "Token Stream\nContentBlockDeltaEvent"]
metrics [shape: rectangle, label: "Calculate TTFT\nTrack Token Count"]
response [shape: parallelogram, label: "SSE Response\ndata: token"]
complete [shape: oval, label: "Stream Complete"]
error [shape: rectangle, label: "Error Handler\nRetry or Return Error"]

start > controller
controller > service
service > client
client > bedrock
bedrock > stream: Success
bedrock > error: Failure
stream > metrics
metrics > response
response > complete
error > complete
```

---

## How to Use

1. Go to [app.eraser.io](https://app.eraser.io)
2. Create a new diagram
3. Select the diagram type (Cloud Architecture, Sequence, etc.)
4. Copy the content between the triple backticks
5. Paste into the Eraser editor
6. The diagram will render automatically

## Export Options

- PNG (high resolution)
- SVG (vector, scalable)
- PDF (print-ready)
- Embed link (for documentation)
