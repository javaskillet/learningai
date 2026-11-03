---
title: Ollama Model Comparison Matrix
tags: [ai, ollama, llm, models, comparison, benchmarks, local-ai]
created: 2026-09-12 17:36:00
status: active
author: javaskillet@gmail.com
---

# Ollama Local Models Comparison Matrix

Comprehensive technical comparison and operational matrix for local Ollama-compatible models across parameter architectures, disk and memory constraints, primary usage targets, and country of origin.

---

## 1. Master Model Comparison Chart
<table style="width: 100%; border-collapse: collapse; font-family: sans-serif; font-size: 0.88em;">
  <thead>
    <tr style="background-color: #0f172a; color: #f8fafc; border-bottom: 2px solid #38bdf8; text-align: left;">
      <th style="padding: 10px 8px;">Model Identifier</th>
      <th style="padding: 10px 8px; width: 85px; text-align: center;">Disk Size</th>
      <th style="padding: 10px 8px; width: 110px;">Origin & Org</th>
      <th style="padding: 10px 8px; width: 130px;">Architecture</th>
      <th style="padding: 10px 10px;">Hardware & Operational Constraints</th>
      <th style="padding: 10px 10px;">Target Usage & Key Strengths</th>
    </tr>
  </thead>
  <tbody>
    <!-- granite3-moe:3b (Light Green Zebra Row) -->
    <tr style="background-color: #065f46; color: #f1f5f9; border-bottom: 1px solid #047857;">
      <td style="padding: 10px 8px; font-weight: bold; color: #a7f3d0;"><code>granite3-moe:3b</code></td>
      <td style="padding: 10px 8px; text-align: center; font-weight: 600;">2.1 GB</td>
      <td style="padding: 10px 8px;">🇺🇸 USA<br/><span style="color: #d1fae5; font-size: 0.85em;">IBM Research</span></td>
      <td style="padding: 10px 8px;">MoE (Mixture of Experts)<br/><span style="color: #d1fae5; font-size: 0.85em;">3.3B Total / ~800M Active</span></td>
      <td style="padding: 10px 10px;">
        • <strong>RAM/VRAM:</strong> 3.5 – 4.5 GB<br/>
        • <strong>Compute:</strong> Exceptionally fast on CPU; low power draw.<br/>
        • <strong>Limitations:</strong> Focused knowledge; not for long-form creative writing.
      </td>
      <td style="padding: 10px 10px;">
        Enterprise task automation, function calling, tool use, low-latency microservices, API request routing.
      </td>
    </tr>
    <!-- moondream2:latest (Dark Green Zebra Row) -->
    <tr style="background-color: #022c22; color: #f1f5f9; border-bottom: 1px solid #064e3b;">
      <td style="padding: 10px 8px; font-weight: bold; color: #34d399;"><code>moondream2:latest</code></td>
      <td style="padding: 10px 8px; text-align: center; font-weight: 600;">1.7 GB</td>
      <td style="padding: 10px 8px;">🇺🇸 USA<br/><span style="color: #a7f3d0; font-size: 0.85em;">vikhyatk / M87 Labs</span></td>
      <td style="padding: 10px 8px;">Small VLM (Vision + LLM)<br/><span style="color: #a7f3d0; font-size: 0.85em;">~1.9B Parameters (SigLIP)</span></td>
      <td style="padding: 10px 10px;">
        • <strong>RAM/VRAM:</strong> 2.0 – 2.8 GB<br/>
        • <strong>Compute:</strong> Sub-400ms inference on pure CPU; negligible power consumption.<br/>
        • <strong>Limitations:</strong> Concise visual responses; limited multi-step textual reasoning.
      </td>
      <td style="padding: 10px 10px;">
        Fast edge vision, image captioning, visual sanity checks, spatial grounding, CLI/terminal snapshot parsing, camera feeds.
      </td>
    </tr>
    <!-- msft-phi4:3b (Light Slate Zebra Row) -->
    <tr style="background-color: #1e293b; color: #f1f5f9; border-bottom: 1px solid #334155;">
      <td style="padding: 10px 8px; font-weight: bold; color: #38bdf8;"><code>msft-phi4:3b</code></td>
      <td style="padding: 10px 8px; text-align: center; font-weight: 600;">2.5 GB</td>
      <td style="padding: 10px 8px;">🇺🇸 USA<br/><span style="color: #94a3b8; font-size: 0.85em;">Microsoft Research</span></td>
      <td style="padding: 10px 8px;">Dense Transformer<br/><span style="color: #94a3b8; font-size: 0.85em;">3.8B Parameters (128k Context)</span></td>
      <td style="padding: 10px 10px;">
        • <strong>RAM/VRAM:</strong> 3.8 – 4.8 GB<br/>
        • <strong>Compute:</strong> High throughput on CPU; synthetic curriculum trained.<br/>
        • <strong>Limitations:</strong> Compact model; narrower encyclopedic world knowledge.
      </td>
      <td style="padding: 10px 10px;">
        Mathematical logic, code synthesis, strict JSON/schema adherence, deterministic validation gates, STEM reasoning.
      </td>
    </tr>
    <!-- nemotron-3.5-lightning:latest (Dark Slate Zebra Row) -->
    <tr style="background-color: #0f172a; color: #f1f5f9; border-bottom: 1px solid #334155;">
      <td style="padding: 10px 8px; font-weight: bold; color: #a5b4fc;"><code>nemotron-3.5-lightning:latest</code></td>
      <td style="padding: 10px 8px; text-align: center; font-weight: 600;">25 GB</td>
      <td style="padding: 10px 8px;">🇺🇸 USA<br/><span style="color: #94a3b8; font-size: 0.85em;">NVIDIA</span></td>
      <td style="padding: 10px 8px;">Dense / Hybrid Transformer<br/><span style="color: #94a3b8; font-size: 0.85em;">~30B–70B Class Quantized</span></td>
      <td style="padding: 10px 10px;">
        • <strong>RAM/VRAM:</strong> 28 – 34 GB (Requires 24GB+ GPU or 32GB+ Unified RAM).<br/>
        • <strong>Compute:</strong> High GPU intensity; sluggish on pure CPU.<br/>
        • <strong>Limitations:</strong> Large footprint, high power requirement.
      </td>
      <td style="padding: 10px 10px;">
        Complex multi-step reasoning, mathematical deduction, synthetic training data generation, enterprise guardrailing.
      </td>
    </tr>
    <!-- ornith-1.0:latest (Highlighted Green Hue Zebra Row) -->
    <tr style="background-color: #065f46; color: #f1f5f9; border-bottom: 1px solid #047857;">
      <td style="padding: 10px 8px; font-weight: bold; color: #a7f3d0;"><code>ornith-1.0:latest</code></td>
      <td style="padding: 10px 8px; text-align: center; font-weight: 600;">5.5 GB</td>
      <td style="padding: 10px 8px;">🇺🇸 USA<br/><span style="color: #d1fae5; font-size: 0.85em;">DeepReinforce</span></td>
      <td style="padding: 10px 8px;">Dense Transformer<br/><span style="color: #d1fae5; font-size: 0.85em;">9B Parameters (Q4_K_M)</span></td>
      <td style="padding: 10px 10px;">
        • <strong>RAM/VRAM:</strong> 7.0 – 8.5 GB<br/>
        • <strong>Compute:</strong> Highly efficient on 8GB VRAM GPUs or 16GB system RAM.<br/>
        • <strong>Limitations:</strong> Requires steady memory allocation for agentic loop state.
      </td>
      <td style="padding: 10px 10px;">
        Self-scaffolding agentic coding, multi-turn tool harnesses, terminal orchestration, resilient multi-file refactoring.
      </td>
    </tr>
    <!-- llama3.2:1b (Dark Slate Zebra Row) -->
    <tr style="background-color: #0f172a; color: #f1f5f9; border-bottom: 1px solid #334155;">
      <td style="padding: 10px 8px; font-weight: bold; color: #67e8f9;"><code>llama3.2:1b</code></td>
      <td style="padding: 10px 8px; text-align: center; font-weight: 600;">1.3 GB</td>
      <td style="padding: 10px 8px;">🇺🇸 USA<br/><span style="color: #94a3b8; font-size: 0.85em;">Meta AI</span></td>
      <td style="padding: 10px 8px;">Dense Transformer<br/><span style="color: #94a3b8; font-size: 0.85em;">1.23B Parameters (128k Context)</span></td>
      <td style="padding: 10px 10px;">
        • <strong>RAM/VRAM:</strong> 2.0 – 3.0 GB<br/>
        • <strong>Compute:</strong> Ultra-fast CPU execution (>50 tok/s on laptops).<br/>
        • <strong>Limitations:</strong> Shallow reasoning on complex STEM problems.
      </td>
      <td style="padding: 10px 10px;">
        On-device mobile NLP, summarization, email/document drafting, intent classification, multi-turn conversational agents.
      </td>
    </tr>
    <!-- qwen3.5-local:latest (Light Slate Zebra Row) -->
    <tr style="background-color: #1e293b; color: #f1f5f9; border-bottom: 1px solid #334155;">
      <td style="padding: 10px 8px; font-weight: bold; color: #86efac;"><code>qwen3.5-local:latest</code></td>
      <td style="padding: 10px 8px; text-align: center; font-weight: 600;">5.6 GB</td>
      <td style="padding: 10px 8px;">🇨🇳 China<br/><span style="color: #94a3b8; font-size: 0.85em;">Alibaba Cloud</span></td>
      <td style="padding: 10px 8px;">Dense Transformer<br/><span style="color: #94a3b8; font-size: 0.85em;">~7B–8B Parameters</span></td>
      <td style="padding: 10px 10px;">
        • <strong>RAM/VRAM:</strong> 7.5 – 9.0 GB<br/>
        • <strong>Compute:</strong> Runs well on 8GB VRAM GPUs or 16GB system RAM.<br/>
        • <strong>Limitations:</strong> Slower generation on older CPU-only systems.
      </td>
      <td style="padding: 10px 10px;">
        High-accuracy general-purpose reasoning, multilingual translation, structured data extraction, versatile chat.
      </td>
    </tr>
    <!-- qwen2.5-coder:7b (Dark Slate Zebra Row) -->
    <tr style="background-color: #0f172a; color: #f1f5f9; border-bottom: 1px solid #334155;">
      <td style="padding: 10px 8px; font-weight: bold; color: #34d399;"><code>qwen2.5-coder:7b</code></td>
      <td style="padding: 10px 8px; text-align: center; font-weight: 600;">4.7 GB</td>
      <td style="padding: 10px 8px;">🇨🇳 China<br/><span style="color: #94a3b8; font-size: 0.85em;">Alibaba Cloud</span></td>
      <td style="padding: 10px 8px;">Dense Transformer<br/><span style="color: #94a3b8; font-size: 0.85em;">7.61B Parameters (128k Context)</span></td>
      <td style="padding: 10px 10px;">
        • <strong>RAM/VRAM:</strong> 6.5 – 8.0 GB<br/>
        • <strong>Compute:</strong> Fits cleanly inside standard 8GB consumer GPUs.<br/>
        • <strong>Limitations:</strong> Limited multi-file cross-repository architectural planning.
      </td>
      <td style="padding: 10px 10px;">
        Premier local coding engine: algorithmic code generation, unit test writing, bug fixing, SQL query building, and refactoring.
      </td>
    </tr>
    <!-- qwen2.5-coder:1.5b (Light Slate Zebra Row) -->
    <tr style="background-color: #1e293b; color: #f1f5f9; border-bottom: 1px solid #334155;">
      <td style="padding: 10px 8px; font-weight: bold; color: #34d399;"><code>qwen2.5-coder:1.5b</code></td>
      <td style="padding: 10px 8px; text-align: center; font-weight: 600;">986 MB</td>
      <td style="padding: 10px 8px;">🇨🇳 China<br/><span style="color: #94a3b8; font-size: 0.85em;">Alibaba Cloud</span></td>
      <td style="padding: 10px 8px;">Dense Transformer<br/><span style="color: #94a3b8; font-size: 0.85em;">1.54B Parameters (32k Context)</span></td>
      <td style="padding: 10px 10px;">
        • <strong>RAM/VRAM:</strong> 1.5 – 2.2 GB<br/>
        • <strong>Compute:</strong> Blazing-fast inference on basic CPUs; negligible battery impact.<br/>
        • <strong>Limitations:</strong> Not suited for complex multi-step architecture design.
      </td>
      <td style="padding: 10px 10px;">
        Sub-second inline code completion (Copilot style), function documentation, lint error repairs, code snippets.
      </td>
    </tr>
    <!-- nomic-embed-text:latest (Dark Slate Zebra Row) -->
    <tr style="background-color: #0f172a; color: #f1f5f9; border-bottom: 1px solid #334155;">
      <td style="padding: 10px 8px; font-weight: bold; color: #f472b6;"><code>nomic-embed-text:latest</code></td>
      <td style="padding: 10px 8px; text-align: center; font-weight: 600;">274 MB</td>
      <td style="padding: 10px 8px;">🇺🇸 USA<br/><span style="color: #94a3b8; font-size: 0.85em;">Nomic AI</span></td>
      <td style="padding: 10px 8px;">Embedding Encoder<br/><span style="color: #94a3b8; font-size: 0.85em;">137M Parameters (8192 Context)</span></td>
      <td style="padding: 10px 10px;">
        • <strong>RAM/VRAM:</strong> ~0.5 GB<br/>
        • <strong>Compute:</strong> Extremely lightweight embedding generation.<br/>
        • <strong>Limitations:</strong> Non-generative; cannot generate conversational text.
      </td>
      <td style="padding: 10px 10px;">
        Vector search embeddings, RAG pipelines, semantic similarity, document clustering, vector databases (pgvector, Milvus).
      </td>
    </tr>
    <!-- qwen3.5-vision:latest (Light Slate Zebra Row) -->
    <tr style="background-color: #1e293b; color: #f1f5f9; border-bottom: 1px solid #334155;">
      <td style="padding: 10px 8px; font-weight: bold; color: #c084fc;"><code>qwen3.5-vision:latest</code></td>
      <td style="padding: 10px 8px; text-align: center; font-weight: 600;">6.5 GB</td>
      <td style="padding: 10px 8px;">🇨🇳 China<br/><span style="color: #94a3b8; font-size: 0.85em;">Alibaba Cloud</span></td>
      <td style="padding: 10px 8px;">Multimodal VLM<br/><span style="color: #94a3b8; font-size: 0.85em;">Visual Encoder + ~7B LLM</span></td>
      <td style="padding: 10px 10px;">
        • <strong>RAM/VRAM:</strong> 8.5 – 10.5 GB<br/>
        • <strong>Compute:</strong> Heavy image token processing; GPU highly recommended.<br/>
        • <strong>Limitations:</strong> High latency on high-resolution image batches on CPU.
      </td>
      <td style="padding: 10px 10px;">
        Visual document parsing, screenshot understanding, OCR extraction, chart and diagram analysis, visual QA.
      </td>
    </tr>
    <!-- codellama-local:latest (Dark Slate Zebra Row) -->
    <tr style="background-color: #0f172a; color: #f1f5f9; border-bottom: 1px solid #334155;">
      <td style="padding: 10px 8px; font-weight: bold; color: #38bdf8;"><code>codellama-local:latest</code></td>
      <td style="padding: 10px 8px; text-align: center; font-weight: 600;">3.3 GB</td>
      <td style="padding: 10px 8px;">🇺🇸 USA<br/><span style="color: #94a3b8; font-size: 0.85em;">Meta AI</span></td>
      <td style="padding: 10px 8px;">Dense Transformer<br/><span style="color: #94a3b8; font-size: 0.85em;">7B Code Llama (Q3/Q4)</span></td>
      <td style="padding: 10px 10px;">
        • <strong>RAM/VRAM:</strong> 5.0 – 6.5 GB<br/>
        • <strong>Compute:</strong> Stable and mature execution across CPU/GPU.<br/>
        • <strong>Limitations:</strong> Outperformed on newer Python/TypeScript benchmarks by Qwen2.5.
      </td>
      <td style="padding: 10px 10px;">
        Python/C++/Java code generation, legacy code explanation, docstring generation, code completion.
      </td>
    </tr>
    <!-- qwen3-coder:30b (Light Slate Zebra Row) -->
    <tr style="background-color: #1e293b; color: #f1f5f9; border-bottom: 1px solid #334155;">
      <td style="padding: 10px 8px; font-weight: bold; color: #34d399;"><code>qwen3-coder:30b</code></td>
      <td style="padding: 10px 8px; text-align: center; font-weight: 600;">18 GB</td>
      <td style="padding: 10px 8px;">🇨🇳 China<br/><span style="color: #94a3b8; font-size: 0.85em;">Alibaba Cloud</span></td>
      <td style="padding: 10px 8px;">Dense Transformer<br/><span style="color: #94a3b8; font-size: 0.85em;">~32B Parameters (128k Context)</span></td>
      <td style="padding: 10px 10px;">
        • <strong>RAM/VRAM:</strong> 20 – 24 GB (Requires 24GB VRAM GPU or 32GB+ Unified Memory).<br/>
        • <strong>Compute:</strong> Demands modern GPU or Apple M-series Max/Ultra.<br/>
        • <strong>Limitations:</strong> Slow on mid-range consumer laptops.
      </td>
      <td style="padding: 10px 10px;">
        Frontier-grade software development, full architectural refactoring, complex bug diagnosis across multiple repositories, math and logic.
      </td>
    </tr>
  </tbody>
</table>

---

## 2. Functional Categorization & Recommendations

### A. Coding & Software Development
- **Tier 1 (Heavy / Full Project Scope):** [`qwen3-coder:30b`](#) (18 GB)
  - *Best for:* Multi-file refactoring, designing clean architecture, and advanced algorithmic reasoning.
- **Tier 2 (Agentic Orchestration & Resilient Scaffolding):** [`ornith-1.0:latest`](#) (5.5 GB) *(Highlighted)*
  - *Best for:* Self-scaffolding agentic coding, multi-turn tool harnesses, terminal command execution, and co-evolving development scaffolds.
- **Tier 2 (Daily Driver / Best General Coding):** [`qwen2.5-coder:7b`](#) (4.7 GB)
  - *Best for:* Exceptional code quality that fits completely in standard 8GB VRAM GPUs with high token throughput.
- **Tier 3 (Ultra-Fast Inline Autocomplete):** [`qwen2.5-coder:1.5b`](#) (986 MB)
  - *Best for:* Sub-second tab-completion in IDEs without draining system resources.
- **Legacy Fallback:** [`codellama-local:latest`](#) (3.3 GB)
  - *Best for:* Proven stability in older Python/C++ toolchains.

### B. Lightweight, Fast & Edge Models (< 3 GB Disk)
- **Top Low-Latency Choice:** [`granite3-moe:3b`](#) (2.1 GB) *(Highlighted)*
  - *Best for:* Fast microservices, JSON extraction, and tool/agent routing due to sparse active parameters (~800M active).
- **Fast Edge Vision & Snapshot Parsing:** [`moondream2:latest`](#) (1.7 GB) *(Highlighted)*
  - *Best for:* Sub-400ms visual grounding, terminal image OCR, and real-time camera inspection on CPU.
- **Mathematical Logic & Strict Schema Adherence:** [`msft-phi4:3b`](#) (2.5 GB)
  - *Best for:* Deterministic validation gates, STEM reasoning, structured JSON outputs, and fast CPU logic synthesis.
- **Mobile / Minimal Footprint:** [`llama3.2:1b`](#) (1.3 GB)
  - *Best for:* High-speed summaries and edge execution with full 128k token context window support.

### C. Multimodal & Vision
- **Edge Vision & Fast OCR:** [`moondream2:latest`](#) (1.7 GB) *(Highlighted)*
  - *Best for:* Ultra-fast visual inspections, CLI/terminal snapshots, and lightweight image parsing on CPU without GPU overhead.
- **Comprehensive Document & Diagram VLM:** [`qwen3.5-vision:latest`](#) (6.5 GB)
  - *Best for:* Complex multi-page documents, UI layout analysis, intricate chart and diagram interpretation, and visual QA.

### D. Advanced Heavyweight Reasoning (> 20 GB Disk)
- **Frontier Enterprise Reasoning:** [`nemotron-3.5-lightning:latest`](#) (25 GB)
  - *Best for:* Deep chain-of-thought analysis, synthetic training data synthesis, and complex alignment.

### E. Semantic Search & Vector Embeddings
- **RAG & Vector Retrieval:** [`nomic-embed-text:latest`](#) (274 MB)
  - *Best for:* High-performance embeddings across 8192 token documents for vector databases like `PostgreSQL w/ pgvector` or `Milvus`.

---

## 3. Hardware Sizing & Deployment Tier Matrix

| Hardware Tier | Memory / VRAM Capacity | Compatible Models from Inventory | Typical Tokens/Sec |
| :--- | :--- | :--- | :--- |
| **Tier 1: Ultra-Light / CPU Only** | 4 GB – 8 GB System RAM | `nomic-embed-text` (274 MB)<br/>`qwen2.5-coder:1.5b` (986 MB)<br/>`llama3.2:1b` (1.3 GB)<br/>`moondream2` (1.7 GB)<br/>`granite3-moe:3b` (2.1 GB)<br/>`msft-phi4:3b` (2.5 GB) | 35 – 80 tok/s |
| **Tier 2: Standard Workstation** | 8 GB VRAM or 16 GB System RAM | `codellama-local` (3.3 GB)<br/>`qwen2.5-coder:7b` (4.7 GB)<br/>`ornith-1.0` (5.5 GB)<br/>`qwen3.5-local` (5.6 GB)<br/>`qwen3.5-vision` (6.5 GB) | 25 – 60 tok/s |
| **Tier 3: High-End / Enterprise** | 24 GB VRAM or 32 GB+ Unified RAM | `qwen3-coder:30b` (18 GB)<br/>`nemotron-3.5-lightning` (25 GB) | 15 – 35 tok/s |

---

## 4. Quick Ollama Execution Cheatsheet

### Running Models Directly
```bash
# Lightweight fast worker (MoE)
ollama run granite3-moe:3b

# Lightweight fast vision
ollama run moondream2:latest

# Strict schema & math reasoning
ollama run msft-phi4:3b

# Agentic coding & terminal orchestration
ollama run ornith-1.0:latest

# Daily coding assistant
ollama run qwen2.5-coder:7b

# Heavy codebase architect
ollama run qwen3-coder:30b
```

### Generating Embeddings via REST API
```bash
curl http://localhost:11434/api/embeddings -d '{
  "model": "nomic-embed-text:latest",
  "prompt": "The quick brown fox jumps over the lazy dog"
}'
```

### Sending Multimodal Vision Request
```bash
# Moondream2 fast image inspection
curl http://localhost:11434/api/generate -d '{
  "model": "moondream2:latest",
  "prompt": "Describe this image concisely.",
  "images": ["<base64_image_data>"]
}'

# Qwen3.5-Vision detailed analysis
curl http://localhost:11434/api/generate -d '{
  "model": "qwen3.5-vision:latest",
  "prompt": "Extract the text from this screenshot and explain the error message.",
  "images": ["<base64_image_data>"]
}'
```
