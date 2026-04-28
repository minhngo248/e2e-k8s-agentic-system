# Plan: Enable A2A Streaming from Worker Agents to Orchestrator

## Current State

- Both worker agents (Tourist Agent, Weather Agent) use `streaming(false)` in their `AgentCard` and blocking `.call().content()` in their `AgentExecutor`.
- The orchestrator's A2A client uses `client.sendMessage()` (blocking) and collects the full response before returning.
- This causes long waits and the "waiting" response problem in the UI.

## Goal

Enable A2A streaming end-to-end so the orchestrator progressively receives chunks from worker agents and relays them to the end-user via the existing WebFlux SSE endpoint (`RoutingController`).

## Steps

### 1. Set `streaming(true)` in both `AgentCard` beans

- **WeatherAgentApplication.java** (line 41): `.streaming(false)` → `.streaming(true)`
- **TouristAgentApplication.java** (line 53): `.streaming(false)` → `.streaming(true)`

### 2. Switch `AgentExecutor` to streaming execution in both agents

- Replace the blocking lambda (`.call().content()`) with a streaming variant using `.stream().content()`.
- Investigate `DefaultAgentExecutor`'s API for a streaming callback signature; if unsupported, implement a custom `AgentExecutor` that produces streaming A2A events (partial `Artifact` chunks).

### 3. Update `RemoteAgentConnections.sendMessage()` in orchestrator

- Use `client.sendMessageStreaming()` (or A2A SDK 0.3.3's streaming equivalent) instead of `client.sendMessage()`.
- Handle `TaskArtifactUpdateEvent` (partial chunks) in the `BiConsumer` in addition to `TaskEvent` (final), accumulating text incrementally via `StringBuilder`.

### 4. Keep `sendMessage` return type as `String`

- Since it's a Spring AI `@Tool`, it must return `String` (blocking).
- Streaming at the A2A transport layer still reduces latency even though the tool call collects all chunks before returning to the LLM.

### 5. Verify `JSONRPCTransportConfig` supports SSE streaming

- The A2A SDK 0.3.3 should handle this by default via the existing `JSONRPCTransport`. No config change expected, but verify.

### 6. Test end-to-end

- Start worker agents → orchestrator → Streamlit UI.
- Verify response arrives faster and the "waiting" intermediate response no longer appears.

## Further Considerations

1. **`DefaultAgentExecutor` streaming API**: Check if `DefaultAgentExecutor` from `spring-ai-a2a-server-autoconfigure` supports a streaming callback (e.g. `Flux<String>`). If not, a custom `AgentExecutor` implementation is needed per agent.
2. **Timeout tuning**: The 180s `CompletableFuture.get()` timeout still applies; consider whether a per-chunk idle timeout is more appropriate for streaming.
3. **Backward compatibility**: A2A protocol capability negotiation should handle graceful fallback if streaming is unavailable on a particular agent.

