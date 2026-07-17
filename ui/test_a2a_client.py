import unittest

from a2a_client import extract_agent_payload


class ExtractAgentPayloadTest(unittest.TestCase):
    def test_extracts_answer_only_from_json_text_part(self):
        response = {
            "result": {
                "artifacts": [
                    {
                        "parts": [
                            {
                                "text": '{"answer": "A useful answer", "images": [{"imageUrls": ["https://example.com/image.jpg"]}]}',
                                "kind": "text",
                            }
                        ]
                    }
                ]
            }
        }

        payload = extract_agent_payload(response)

        self.assertEqual(payload.answer, "A useful answer")
        self.assertFalse(hasattr(payload, "image_urls"))


if __name__ == "__main__":
    unittest.main()
