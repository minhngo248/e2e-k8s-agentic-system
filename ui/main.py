import os

import streamlit as st

from a2a_client import fetch_agent_card, send_message

# Page configuration
st.set_page_config(
    page_title="Tourist Guide & Weather Assistant",
    page_icon="🌍",
    layout="wide"
)

TOURIST_AGENT_URL = os.getenv("TOURIST_AGENT_URL", "http://localhost:8083")


@st.cache_data(ttl=300, show_spinner=False)
def load_agent_card(agent_url: str):
    return fetch_agent_card(agent_url)


def render_assistant_message(content: str):
    st.markdown(content or "_No answer returned._")


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
            if message["role"] == "assistant":
                render_assistant_message(message["content"])
            else:
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

        try:
            with st.spinner("Contacting tourist agent..."):
                payload = send_message(TOURIST_AGENT_URL, prompt)

            response_placeholder.empty()
            render_assistant_message(payload.answer)

            # Add assistant message to chat history
            st.session_state.messages.append({
                "role": "assistant",
                "content": payload.answer,
            })

        except Exception as e:
            error_message = f"⚠️ Error: {str(e)}"
            response_placeholder.markdown(error_message)
            st.session_state.messages.append({"role": "assistant", "content": error_message})

# Sidebar with additional information
with st.sidebar:
    st.header("ℹ️ About")
    st.caption(f"Tourist agent: `{TOURIST_AGENT_URL}`")

    try:
        agent_card = load_agent_card(TOURIST_AGENT_URL)
        st.subheader(agent_card.get("name", "A2A Tourist Agent"))
        description = agent_card.get("description")
        if description:
            st.markdown(description)
    except Exception as e:
        st.warning(f"Agent card unavailable: {e}")

    st.markdown("""
    This assistant combines:
    - **Tourist Agent**: Provides destination recommendations and travel information
    - **Weather Agent**: Delivers weather forecasts and climate data

    All powered by AI agents working together!
    """)

    if st.button("🗑️ Clear Conversation"):
        st.session_state.messages = []
        st.rerun()
