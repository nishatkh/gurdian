from __future__ import annotations

import json
from pathlib import Path
from typing import Any, Optional

import joblib
import numpy as np

from app.core.config import get_settings


class MLInferenceService:
    """Loads real trained UCI HAR models and executes Task A (activity), Task B (anomaly), and Task C (rule assessment + XAI)."""

    def __init__(self, models_dir: Optional[Path] = None) -> None:
        self.models_dir = models_dir or Path(get_settings().ml_models_dir)
        self._loaded = False
        self.scaler = None
        self.activity_clf = None
        self.anom_scaler = None
        self.anomaly_det = None
        self.rf_xai = None
        self.metadata: dict[str, Any] = {}
        self.try_load()

    def try_load(self) -> bool:
        meta_path = self.models_dir / "model_metadata.json"
        clf_path = self.models_dir / "activity_classifier.joblib"
        scaler_path = self.models_dir / "har_scaler.joblib"
        anom_path = self.models_dir / "anomaly_detector.joblib"
        anom_scaler_path = self.models_dir / "anomaly_scaler.joblib"
        rf_path = self.models_dir / "activity_rf_xai.joblib"

        if not (meta_path.exists() and clf_path.exists() and scaler_path.exists()):
            self._loaded = False
            return False

        self.metadata = json.loads(meta_path.read_text(encoding="utf-8"))
        self.scaler = joblib.load(scaler_path)
        self.activity_clf = joblib.load(clf_path)
        if anom_path.exists() and anom_scaler_path.exists():
            self.anom_scaler = joblib.load(anom_scaler_path)
            self.anomaly_det = joblib.load(anom_path)
        if rf_path.exists():
            self.rf_xai = joblib.load(rf_path)
        self._loaded = True
        return True

    @property
    def is_ready(self) -> bool:
        return self._loaded

    def predict_sensor_context(self, features: list[float]) -> dict[str, Any]:
        if not self._loaded:
            if not self.try_load():
                raise RuntimeError("Trained ML artifacts are not available in ml/models/")

        expected_dim = int(self.metadata.get("num_features", 561))
        if len(features) != expected_dim:
            raise ValueError(f"Expected {expected_dim} sensor features, received {len(features)}")

        arr = np.asarray(features, dtype=float).reshape(1, -1)
        if np.isnan(arr).any() or np.isinf(arr).any():
            raise ValueError("Sensor feature vector contains NaN or Inf values")

        x_scaled = self.scaler.transform(arr)
        pred_id = int(self.activity_clf.predict(x_scaled)[0])
        label_map = self.metadata.get("label_map", {})
        pred_label = label_map.get(str(pred_id), f"CLASS_{pred_id}")

        confidence = 0.0
        if hasattr(self.activity_clf, "predict_proba"):
            probs = self.activity_clf.predict_proba(x_scaled)[0]
            confidence = float(np.max(probs))
        elif hasattr(self.activity_clf, "decision_function"):
            scores = self.activity_clf.decision_function(x_scaled)[0]
            exp_s = np.exp(scores - np.max(scores))
            confidence = float(np.max(exp_s / np.sum(exp_s)))

        anomaly_flag = False
        anomaly_score = 0.0
        if self.anomaly_det is not None and self.anom_scaler is not None:
            xa = self.anom_scaler.transform(arr)
            anomaly_flag = bool(int(self.anomaly_det.predict(xa)[0]) == -1)
            anomaly_score = round(float(-self.anomaly_det.decision_function(xa)[0]), 4)

        # Explainability: top contributing active standardized signals weighted by RF importance
        feature_names = self.metadata.get("feature_names", [])
        top_signals = []
        if self.rf_xai is not None and len(feature_names) == expected_dim:
            importances = self.rf_xai.feature_importances_
            weighted = np.abs(x_scaled[0]) * importances
            top_idx = np.argsort(weighted)[::-1][:5]
            for idx in top_idx:
                top_signals.append(
                    {
                        "feature": feature_names[idx],
                        "z_value": round(float(x_scaled[0][idx]), 3),
                        "model_importance": round(float(importances[idx]), 5),
                    }
                )

        return {
            "model_version": self.metadata.get("model_version", "guardian-har-v1.0.0"),
            "task": "device_activity_context_classification",
            "predicted_activity": pred_label,
            "confidence": round(confidence, 4),
            "unusual_motion_signal": anomaly_flag,
            "motion_anomaly_score": anomaly_score,
            "xai_top_signals": top_signals,
            "uncertainty_notice": (
                "Low classification confidence; treat context signal as uncertain."
                if confidence < 0.65
                else "Sensor context only — NOT proof of phone theft."
            ),
        }

    def assess_incident_rules(
        self,
        *,
        category: str,
        description: str,
        has_imei: bool,
        has_device_info: bool,
        has_location: bool,
        completed_checklist_count: int,
        total_checklist_count: int,
        timeline_count: int,
    ) -> dict[str, Any]:
        """Task C: Transparent deterministic rule-based incident assessment & Explainable AI trace."""
        observed: list[str] = []
        missing: list[str] = []
        rule_trace: list[dict[str, Any]] = []

        if description and len(description.strip()) >= 20:
            observed.append("Detailed incident narrative provided by user")
            rule_trace.append({"rule": "narrative_length >= 20", "passed": True, "weight": 20})
        else:
            missing.append("Detailed narrative describing how/where the incident occurred")
            rule_trace.append({"rule": "narrative_length >= 20", "passed": False, "weight": 20})

        if has_imei:
            observed.append("15-digit IMEI stored securely for GD & BTRC/NEIR reference")
            rule_trace.append({"rule": "imei_present", "passed": True, "weight": 25})
        else:
            missing.append("15-digit IMEI number (check box, invoice, or Google Find Hub)")
            rule_trace.append({"rule": "imei_present", "passed": False, "weight": 25})

        if has_device_info:
            observed.append("Device brand and model recorded")
            rule_trace.append({"rule": "device_info_present", "passed": True, "weight": 15})
        else:
            missing.append("Device brand, model, and color")
            rule_trace.append({"rule": "device_info_present", "passed": False, "weight": 15})

        if has_location:
            observed.append("Last known location or area recorded")
            rule_trace.append({"rule": "location_present", "passed": True, "weight": 15})
        else:
            missing.append("Approximate location and Thana/area name")
            rule_trace.append({"rule": "location_present", "passed": False, "weight": 15})

        if completed_checklist_count >= 2:
            observed.append(f"{completed_checklist_count}/{total_checklist_count} account/SIM protection steps completed")
            rule_trace.append({"rule": "checklist_progress >= 2", "passed": True, "weight": 15})
        else:
            missing.append("Immediate SIM blocking and MFS (bKash/Nagad/Rocket) protection steps")
            rule_trace.append({"rule": "checklist_progress >= 2", "passed": False, "weight": 15})

        if timeline_count >= 1:
            observed.append(f"{timeline_count} timeline event(s) logged")
            rule_trace.append({"rule": "timeline_logged", "passed": True, "weight": 10})
        else:
            missing.append("Initial event timeline entry")
            rule_trace.append({"rule": "timeline_logged", "passed": False, "weight": 10})

        readiness_score = sum(r["weight"] for r in rule_trace if r["passed"])
        urgent_recommendation = (
            "Contact 999 or nearest Thana immediately if violent snatching occurred; then block SIM and MFS."
            if category == "snatching"
            else "Block SIM & MFS wallets first, secure Google/Apple account, then prepare Online GD draft."
        )

        return {
            "assessment_type": "deterministic_rule_baseline",
            "gd_readiness_score": readiness_score,
            "observed_signals": observed,
            "missing_information": missing,
            "rule_trace": rule_trace,
            "recommended_next_action": urgent_recommendation,
            "uncertainty_notice": (
                "Readiness score measures documentation completeness only. "
                "Only the user can confirm whether a device was lost or stolen."
            ),
        }
