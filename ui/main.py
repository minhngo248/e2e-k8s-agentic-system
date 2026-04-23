import streamlit as st
import httpx
import os

def stream_from_spring(prompt: str):
    timeout = httpx.Timeout(300.0, connect=10.0)  # 5 min read timeout, 10s connect timeout
    headers = {"Accept": "text/event-stream", "Content-Type": "application/json"}

    # Get orchestrator URL from environment variable, default to localhost if not set
    orchestrator_url = os.getenv("ORCHESTRATOR_URL", "http://localhost:8083")

    with httpx.stream(
        "POST",
        f"{orchestrator_url}/chat",
        json={"message": prompt},
        timeout=timeout,
        headers=headers
    ) as r:
        for line in r.iter_lines():
            if line.startswith("data:"):
                # Extract data from SSE format - don't strip to preserve spaces!
                chunk = line[5:]  # Remove "data:" prefix but keep spaces
                if chunk:
                    yield chunk

if prompt := st.chat_input("Ask something..."):
    with st.chat_message("assistant"):
        # Accumulate the streamed response and display as markdown
        response_placeholder = st.empty()
        full_response = ""
        for chunk in stream_from_spring(prompt):
            full_response += chunk
            response_placeholder.markdown(full_response + "▌")
        response_placeholder.markdown(full_response)
