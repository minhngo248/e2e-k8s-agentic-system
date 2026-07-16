from dataclasses import dataclass
import json
from typing import Any
from uuid import uuid4


@dataclass(frozen=True)
class AgentPayload:
    answer: str
    image_urls: list[str]


def normalize_agent_url(agent_url: str) -> str:
    return agent_url.rstrip("/")


def agent_card_url(agent_url: str) -> str:
    return f"{normalize_agent_url(agent_url)}/.well-known/agent-card.json"


def build_message_send_request(
    prompt: str,
    request_id: str | None = None,
    message_id: str | None = None,
) -> dict[str, Any]:
    return {
        "jsonrpc": "2.0",
        "id": request_id or f"request-{uuid4()}",
        "method": "message/send",
        "params": {
            "message": {
                "kind": "message",
                "role": "user",
                "parts": [
                    {
                        "kind": "text",
                        "text": prompt,
                    }
                ],
                "messageId": message_id or f"msg-{uuid4()}",
            }
        },
    }


def extract_agent_payload(response: dict[str, Any]) -> AgentPayload:
    if "error" in response:
        error = response["error"]
        if isinstance(error, dict):
            message = error.get("message") or json.dumps(error)
        else:
            message = str(error)
        raise ValueError(f"A2A error: {message}")

    text_parts = _artifact_text_parts(response)
    answers: list[str] = []
    image_urls: list[str] = []

    for text in text_parts:
        try:
            decoded = json.loads(text)
        except json.JSONDecodeError:
            answers.append(text)
            continue

        if not isinstance(decoded, dict):
            answers.append(text)
            continue

        answer = decoded.get("answer")
        if isinstance(answer, str) and answer.strip():
            answers.append(answer)

        images = decoded.get("images", [])
        if isinstance(images, list):
            image_urls.extend(_image_urls_from_images(images))

    return AgentPayload(answer="\n\n".join(answers).strip(), image_urls=image_urls)


def fetch_agent_card(agent_url: str) -> dict[str, Any]:
    import httpx

    timeout = httpx.Timeout(20.0, connect=5.0)
    response = httpx.get(agent_card_url(agent_url), timeout=timeout)
    response.raise_for_status()
    return response.json()


def send_message(agent_url: str, prompt: str) -> AgentPayload:
    import httpx

    timeout = httpx.Timeout(300.0, connect=10.0)
    response = httpx.post(
        f"{normalize_agent_url(agent_url)}/",
        json=build_message_send_request(prompt),
        timeout=timeout,
        headers={"Content-Type": "application/json", "Accept": "application/json"},
    )
    response.raise_for_status()
    return extract_agent_payload(response.json())


def _artifact_text_parts(response: dict[str, Any]) -> list[str]:
    result = response.get("result", {})
    if not isinstance(result, dict):
        return []

    artifacts = result.get("artifacts", [])
    if not isinstance(artifacts, list):
        return []

    text_parts: list[str] = []
    for artifact in artifacts:
        if not isinstance(artifact, dict):
            continue
        parts = artifact.get("parts", [])
        if not isinstance(parts, list):
            continue
        for part in parts:
            if isinstance(part, dict) and isinstance(part.get("text"), str):
                text_parts.append(part["text"])

    return text_parts


def _image_urls_from_images(images: list[Any]) -> list[str]:
    image_urls: list[str] = []
    for image in images:
        if not isinstance(image, dict):
            continue
        urls = image.get("imageUrls", [])
        if not isinstance(urls, list):
            continue
        image_urls.extend(url for url in urls if isinstance(url, str) and url)
    return image_urls
