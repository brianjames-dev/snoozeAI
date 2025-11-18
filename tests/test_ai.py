import asyncio

from backend.app.ai import AIConfig, classify, summarize


def test_summarize_offline_prefix_and_trim():
    text = "This is a pretty long body " * 10
    summary = asyncio.run(summarize(text, max_tokens=20, config=AIConfig(use_openai=False)))
    assert summary.startswith("Summary: ")
    # Ensure we trim based on token budget multiplier
    assert len(summary) <= len("Summary: ") + 20 * 4 + 3
    assert "…" in summary


def test_classify_offline_keywords_raise_score():
    payload = "Reminder: standup in 15 minutes. ASAP join zoom."
    result = asyncio.run(classify(payload, config=AIConfig(use_openai=False)))
    assert result["label"] == "urgent"
    assert 0.6 <= result["urgency"] <= 1.0


def test_classify_hints_increase_score():
    payload = "Weekly update from Product team"
    result = asyncio.run(
        classify(payload, hints=["product team"], config=AIConfig(use_openai=False))
    )
    assert result["label"] == "urgent"
