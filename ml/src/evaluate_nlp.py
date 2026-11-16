"""Evaluate bilingual Bangla/English/Banglish NLP extractor against ml/datasets/nlp_eval_bn_en.json."""

from __future__ import annotations

import json
import sys
from pathlib import Path

sys.path.insert(0, str(Path("backend").resolve()))
from app.nlp.extractor import extract_incident_fields  # noqa: E402


def main() -> None:
    dataset = json.loads(Path("ml/datasets/nlp_eval_bn_en.json").read_text(encoding="utf-8"))
    total = len(dataset)
    intent_hits = 0
    brand_hits = 0
    imei_hits = 0
    loc_hits = 0
    time_hits = 0
    per_case = []

    for row in dataset:
        pred = extract_incident_fields(row["text"])
        exp = row["expected"]

        i_ok = pred["intent"] == exp["intent"]
        b_ok = pred["device_brand"] == exp["device_brand"]
        m_ok = pred["imei_detected"] == exp["imei_present"]
        l_ok = (exp["location_mention"] is None and pred["location_mention"] is None) or (
            exp["location_mention"] is not None
            and pred["location_mention"] is not None
            and exp["location_mention"].lower() in pred["location_mention"].lower()
        )
        t_ok = (len(pred["time_mentions"]) > 0) == exp["time_mention"]

        intent_hits += int(i_ok)
        brand_hits += int(b_ok)
        imei_hits += int(m_ok)
        loc_hits += int(l_ok)
        time_hits += int(t_ok)

        per_case.append(
            {
                "id": row["id"],
                "category": row["category"],
                "language": row["language"],
                "intent_match": i_ok,
                "brand_match": b_ok,
                "imei_match": m_ok,
                "location_match": l_ok,
                "time_match": t_ok,
                "predicted": pred,
            }
        )

    summary = {
        "total_cases": total,
        "intent_accuracy": round(intent_hits / total, 4),
        "brand_accuracy": round(brand_hits / total, 4),
        "imei_detection_accuracy": round(imei_hits / total, 4),
        "location_accuracy": round(loc_hits / total, 4),
        "time_mention_accuracy": round(time_hits / total, 4),
        "cases": per_case,
    }
    Path("ml/reports").mkdir(parents=True, exist_ok=True)
    Path("ml/reports/nlp_evaluation.json").write_text(
        json.dumps(summary, indent=2, ensure_ascii=False), encoding="utf-8"
    )
    print(json.dumps({k: v for k, v in summary.items() if k != "cases"}, indent=2))


if __name__ == "__main__":
    main()
