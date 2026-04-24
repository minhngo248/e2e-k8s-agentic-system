# Command
```bash
# Test the agent
http POST http://localhost:8082/ jsonrpc=2.0 method=message/send id=1 params:='{"message":{"role":"user","parts":[{"kind":"text","text":"What is the weather in Paris in the next 2 days?"}],"messageId":"msg-1","kind":"message"}}'
```