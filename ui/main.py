import streamlit as st
import httpx
import os

# Page configuration
st.set_page_config(
    page_title="Tourist Guide & Weather Assistant",
    page_icon="🌍",
    layout="wide"
)

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
        data_lines = []
        for line in r.iter_lines():
            if line.startswith("data:"):
                data_lines.append(line[5:])
            elif line == "" and data_lines:
                # Blank line = end of SSE event; join accumulated data lines with \n
                yield "\n".join(data_lines)
                data_lines = []
        # Flush any remaining data
        if data_lines:
            yield "\n".join(data_lines)

# Initialize session state for chat history
if "messages" not in st.session_state:
    st.session_state.messages = []

# Header
st.title("🌍 Tourist Guide & Weather Assistant")

# Welcome message in the center
if len(st.session_state.messages) == 0:
    st.markdown("""
    <div style="text-align: center; padding: 2rem; margin: 2rem 0;">
        <h3>Welcome! I am a specialized agent for tourist guides and weather information.</h3>
        <p style="font-size: 1.1rem; color: #666;">
            I can help you discover amazing destinations and provide weather forecasts for your travels.
        </p>
        <p style="font-size: 1rem; margin-top: 1rem;">
            <strong>Try asking me:</strong><br/>
            • "Show me cultural destinations in Hanoi"<br/>
            • "What's the weather like in Berlin?"<br/>
            • "Recommend tourist attractions in Paris"<br/>
            • "Give me the 7-day weather forecast for Tokyo"
        </p>
    </div>
    """, unsafe_allow_html=True)
else:
    # Display all previous messages in the conversation
    for message in st.session_state.messages:
        with st.chat_message(message["role"]):
            st.markdown(message["content"])

# Chat input
if prompt := st.chat_input("Ask me about destinations or weather..."):
    # Add user message to chat history
    st.session_state.messages.append({"role": "user", "content": prompt})

    # Display user message
    with st.chat_message("user"):
        st.markdown(prompt)

    # Display assistant response
    with st.chat_message("assistant"):
        response_placeholder = st.empty()
        full_response = ""

        try:
            for chunk in stream_from_spring(prompt):
                full_response += chunk
                response_placeholder.markdown(full_response + "▌")

            # Final response without cursor
            response_placeholder.markdown(full_response)

            # Add assistant message to chat history
            st.session_state.messages.append({"role": "assistant", "content": full_response})

        except Exception as e:
            error_message = f"⚠️ Error: {str(e)}"
            response_placeholder.markdown(error_message)
            st.session_state.messages.append({"role": "assistant", "content": error_message})

# Sidebar with additional information
with st.sidebar:
    st.header("ℹ️ About")
    st.markdown("""
    This assistant combines:
    - **Tourist Agent**: Provides destination recommendations and travel information
    - **Weather Agent**: Delivers weather forecasts and climate data
    
    All powered by AI agents working together!
    """)

    if st.button("🗑️ Clear Conversation"):
        st.session_state.messages = []
        st.rerun()

