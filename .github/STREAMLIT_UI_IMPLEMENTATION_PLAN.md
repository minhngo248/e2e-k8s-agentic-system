# Streamlit UI Implementation Plan

**Date:** April 23, 2026  
**Component:** UI Frontend (Streamlit Chat Interface)  
**Status:** ✅ Completed

---

## Objective

Design and implement a professional chat-based user interface using Streamlit that:
1. Displays a welcoming message with examples in the center of the page
2. Shows all conversation messages from the current session
3. Maintains conversation history throughout the session
4. Provides a clean, intuitive user experience

---

## Requirements

### 1. Welcome Screen (Initial State)
- **Display when:** No messages exist in the session yet
- **Location:** Center of the page
- **Content:**
  - Welcoming headline introducing the assistant's purpose
  - Brief description of capabilities (tourist guide + weather information)
  - Example prompts to help users get started
  - Professional, centered layout with proper styling

### 2. Conversation History
- **Display when:** User has started interacting with the assistant
- **Requirements:**
  - Show all messages from the current session
  - Display both user and assistant messages with proper role indicators
  - Maintain chronological order (oldest to newest)
  - Preserve formatting and markdown in responses
  - Messages should persist throughout the session until manually cleared

### 3. Chat Input & Interaction
- **Input field:** Always visible at the bottom
- **Streaming responses:** Show assistant responses character-by-character with cursor indicator
- **Error handling:** Display user-friendly error messages if request fails
- **State management:** Add messages to history after sending/receiving

### 4. Additional Features
- **Page configuration:** Title, icon, and layout settings
- **Sidebar:** Information about capabilities and clear conversation button
- **Clear conversation:** Allow users to reset and start fresh

---

## Implementation Details

### File: `ui/main.py`

#### 1. **Page Configuration** (Lines 5-9)
```python
st.set_page_config(
    page_title="Tourist Guide & Weather Assistant",
    page_icon="🌍",
    layout="wide"
)
```
- Sets page title visible in browser tab
- Adds emoji icon for branding
- Uses wide layout for better space utilization

#### 2. **Streaming Function** (Lines 11-29)
```python
def stream_from_spring(prompt: str):
    timeout = httpx.Timeout(300.0, connect=10.0)
    headers = {"Accept": "text/event-stream", "Content-Type": "application/json"}
    orchestrator_url = os.getenv("ORCHESTRATOR_URL", "http://localhost:8083")
    
    with httpx.stream("POST", f"{orchestrator_url}/chat", ...) as r:
        for line in r.iter_lines():
            if line.startswith("data:"):
                chunk = line[5:]  # Remove "data:" prefix
                if chunk:
                    yield chunk
```
- Connects to orchestrator service via environment variable
- Handles Server-Sent Events (SSE) streaming
- Yields chunks for real-time display
- Extended timeout (5 minutes) for long responses

#### 3. **Session State Initialization** (Lines 31-32)
```python
if "messages" not in st.session_state:
    st.session_state.messages = []
```
- Initializes conversation history storage
- Uses Streamlit's session state for persistence across reruns
- Structure: List of dictionaries with "role" and "content" keys

#### 4. **Welcome Message** (Lines 38-54)
```python
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
```
- Only displays when session is empty (no messages)
- Centered layout with custom HTML/CSS styling
- Provides clear introduction and example use cases
- Guides users on what they can ask

#### 5. **Conversation Display** (Lines 55-59)
```python
else:
    # Display all previous messages in the conversation
    for message in st.session_state.messages:
        with st.chat_message(message["role"]):
            st.markdown(message["content"])
```
- Iterates through all messages in session history
- Uses Streamlit's `chat_message` component for proper role display
- Renders markdown content for rich formatting
- Automatically handles user/assistant avatars and styling

#### 6. **Chat Input Handler** (Lines 61-88)
```python
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
            
            response_placeholder.markdown(full_response)
            st.session_state.messages.append({"role": "assistant", "content": full_response})
        
        except Exception as e:
            error_message = f"⚠️ Error: {str(e)}"
            response_placeholder.markdown(error_message)
            st.session_state.messages.append({"role": "assistant", "content": error_message})
```
- Captures user input and adds to history immediately
- Displays user message in chat
- Streams assistant response with cursor indicator (▌)
- Removes cursor and shows final response
- Adds assistant response to history
- Handles errors gracefully and adds to history

#### 7. **Sidebar** (Lines 90-108)
```python
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
```
- Provides context about the system
- Clear conversation button resets session state
- Triggers rerun to show welcome message again

---

## Data Flow

```
User Input → Add to history → Display user message
           ↓
Send to orchestrator (SSE stream)
           ↓
Receive chunks → Accumulate + Display with cursor
           ↓
Final response → Add to history → Remove cursor
           ↓
Session persists until cleared
```

---

## Key Features Achieved

✅ **Welcome Screen**
- Centered, professional welcome message
- Clear introduction of capabilities
- Helpful example prompts
- Only shows when no conversation exists

✅ **Conversation History**
- All messages displayed in chronological order
- Proper role indicators (user/assistant)
- Markdown rendering for rich content
- Preserved throughout session

✅ **Streaming Responses**
- Real-time character-by-character display
- Cursor indicator during streaming
- Clean final display without cursor

✅ **Error Handling**
- Try-catch for network/timeout errors
- User-friendly error messages
- Errors added to conversation history

✅ **Session Management**
- Conversation persists across interactions
- Clear button to reset session
- Rerun triggers welcome screen

✅ **User Experience**
- Intuitive chat interface
- Helpful sidebar information
- Responsive layout
- Professional styling

---

## Testing Checklist

- [x] Welcome message displays on first load
- [x] Example prompts are visible and clear
- [x] User messages are added to history
- [x] Assistant responses stream correctly
- [x] All messages persist in session
- [x] Clear conversation button works
- [x] Error handling displays properly
- [x] Markdown rendering works in responses
- [x] Layout is responsive and clean
- [x] Environment variable for orchestrator URL works

---

## Future Enhancements (Not Implemented)

- [ ] Export conversation history to file
- [ ] Syntax highlighting for code blocks
- [ ] Copy button for assistant responses
- [ ] Conversation search/filter
- [ ] Save/load conversation sessions
- [ ] Multiple conversation threads
- [ ] User preferences (theme, font size)
- [ ] Response rating/feedback

---

## Dependencies

- `streamlit`: Web UI framework
- `httpx`: HTTP client with streaming support
- `os`: Environment variable access

## Configuration

- **Environment Variable:** `ORCHESTRATOR_URL` (default: `http://localhost:8083`)
- **Timeout:** 300 seconds read, 10 seconds connect
- **Layout:** Wide mode
- **Page Title:** "Tourist Guide & Weather Assistant"
- **Icon:** 🌍

---

**Implementation Status:** Complete and functional ✅

