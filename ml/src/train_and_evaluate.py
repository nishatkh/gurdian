"""Reproducible ML training, participant-aware validation, evaluation, and XAI reporting for Guardian AI."""

from __future__ import annotations

import json
import time
from datetime import datetime, timezone
from pathlib import Path
from typing import Any

import joblib
import numpy as np
import pandas as pd
from sklearn.ensemble import HistGradientBoostingClassifier, IsolationForest, RandomForestClassifier
from sklearn.inspection import permutation_importance
from sklearn.linear_model import LogisticRegression
from sklearn.metrics import (
    accuracy_score,
    classification_report,
    confusion_matrix,
    f1_score,
    precision_score,
    recall_score,
    roc_auc_score,
)
from sklearn.model_selection import GroupShuffleSplit
from sklearn.neighbors import LocalOutlierFactor
from sklearn.preprocessing import StandardScaler
from sklearn.svm import LinearSVC, OneClassSVM
from sklearn.tree import DecisionTreeClassifier

from download_datasets import download_uci_har


def load_uci_har(dataset_root: Path) -> dict[str, Any]:
    features_df = pd.read_csv(
        dataset_root / "features.txt",
        sep=r"\s+",
        header=None,
        names=["idx", "name"],
    )
    # Disambiguate duplicate feature names cleanly
    seen: dict[str, int] = {}
    unique_names: list[str] = []
    for raw_name in features_df["name"].tolist():
        count = seen.get(raw_name, 0)
        seen[raw_name] = count + 1
        unique_names.append(raw_name if count == 0 else f"{raw_name}__{count}")

    labels_df = pd.read_csv(
        dataset_root / "activity_labels.txt",
        sep=r"\s+",
        header=None,
        names=["id", "label"],
    )
    label_map = dict(zip(labels_df["id"], labels_df["label"]))

    X_train = np.loadtxt(dataset_root / "train" / "X_train.txt")
    y_train = np.loadtxt(dataset_root / "train" / "y_train.txt", dtype=int)
    s_train = np.loadtxt(dataset_root / "train" / "subject_train.txt", dtype=int)

    X_test = np.loadtxt(dataset_root / "test" / "X_test.txt")
    y_test = np.loadtxt(dataset_root / "test" / "y_test.txt", dtype=int)
    s_test = np.loadtxt(dataset_root / "test" / "subject_test.txt", dtype=int)

    # Data validation & participant leakage check
    assert X_train.shape[1] == len(unique_names) == 561
    assert X_test.shape[1] == 561
    assert not np.isnan(X_train).any(), "NaNs detected in X_train"
    assert not np.isnan(X_test).any(), "NaNs detected in X_test"
    train_subjects = set(s_train.tolist())
    test_subjects = set(s_test.tolist())
    overlap = train_subjects.intersection(test_subjects)
    assert len(overlap) == 0, f"Subject leakage detected between train and test: {overlap}"

    return {
        "feature_names": unique_names,
        "label_map": label_map,
        "X_train_full": X_train,
        "y_train_full": y_train,
        "s_train_full": s_train,
        "X_test": X_test,
        "y_test": y_test,
        "s_test": s_test,
    }


def evaluate_task_a(data: dict[str, Any], seed: int, models_dir: Path) -> dict[str, Any]:
    X_train_full = data["X_train_full"]
    y_train_full = data["y_train_full"]
    s_train_full = data["s_train_full"]
    X_test = data["X_test"]
    y_test = data["y_test"]
    label_map = data["label_map"]
    feature_names = data["feature_names"]

    # Participant-aware train/validation split
    gss = GroupShuffleSplit(n_splits=1, test_size=0.2, random_state=seed)
    tr_idx, val_idx = next(gss.split(X_train_full, y_train_full, groups=s_train_full))

    scaler = StandardScaler()
    X_tr = scaler.fit_transform(X_train_full[tr_idx])
    y_tr = y_train_full[tr_idx]
    X_val = scaler.transform(X_train_full[val_idx])
    y_val = y_train_full[val_idx]

    candidates = {
        "logistic_regression": LogisticRegression(max_iter=1000, C=1.0, random_state=seed),
        "decision_tree": DecisionTreeClassifier(max_depth=12, random_state=seed),
        "random_forest": RandomForestClassifier(
            n_estimators=150, max_depth=18, random_state=seed, n_jobs=-1
        ),
        "linear_svc": LinearSVC(C=0.5, max_iter=5000, random_state=seed),
        "hist_gradient_boosting": HistGradientBoostingClassifier(
            max_iter=100, max_depth=6, random_state=seed
        ),
    }

    val_results: dict[str, Any] = {}
    best_name = ""
    best_val_f1 = -1.0

    for name, clf in candidates.items():
        t0 = time.perf_counter()
        clf.fit(X_tr, y_tr)
        train_sec = time.perf_counter() - t0

        t1 = time.perf_counter()
        val_pred = clf.predict(X_val)
        infer_ms = (time.perf_counter() - t1) * 1000.0 / len(X_val)

        val_acc = float(accuracy_score(y_val, val_pred))
        val_f1 = float(f1_score(y_val, val_pred, average="macro"))
        val_results[name] = {
            "val_accuracy": round(val_acc, 4),
            "val_macro_f1": round(val_f1, 4),
            "train_seconds": round(train_sec, 3),
            "val_inference_ms_per_sample": round(infer_ms, 5),
        }
        if val_f1 > best_val_f1:
            best_val_f1 = val_f1
            best_name = name

    # Retrain all candidates on full 21-subject train set & evaluate on held-out 9-subject test set
    full_scaler = StandardScaler()
    X_train_scaled = full_scaler.fit_transform(X_train_full)
    X_test_scaled = full_scaler.transform(X_test)

    test_results: dict[str, Any] = {}
    trained_models: dict[str, Any] = {}

    for name, clf in candidates.items():
        clf.fit(X_train_scaled, y_train_full)
        t0 = time.perf_counter()
        y_pred = clf.predict(X_test_scaled)
        infer_ms = (time.perf_counter() - t0) * 1000.0 / len(X_test_scaled)

        acc = float(accuracy_score(y_test, y_pred))
        prec = float(precision_score(y_test, y_pred, average="macro"))
        rec = float(recall_score(y_test, y_pred, average="macro"))
        f1 = float(f1_score(y_test, y_pred, average="macro"))
        cm = confusion_matrix(y_test, y_pred).tolist()
        per_class = classification_report(
            y_test,
            y_pred,
            target_names=[label_map[i] for i in sorted(label_map.keys())],
            output_dict=True,
        )

        test_results[name] = {
            "test_accuracy": round(acc, 4),
            "test_macro_precision": round(prec, 4),
            "test_macro_recall": round(rec, 4),
            "test_macro_f1": round(f1, 4),
            "test_inference_ms_per_sample": round(infer_ms, 5),
            "confusion_matrix": cm,
            "per_class": per_class,
        }
        trained_models[name] = clf

    # Explainable AI: Logistic Regression coefficients + Random Forest Gini importance + Permutation importance
    lr_model = trained_models["logistic_regression"]
    rf_model = trained_models["random_forest"]

    lr_mean_abs_coef = np.mean(np.abs(lr_model.coef_), axis=0)
    top_lr_idx = np.argsort(lr_mean_abs_coef)[::-1][:15]
    lr_top_features = [
        {"feature": feature_names[i], "mean_abs_coef": round(float(lr_mean_abs_coef[i]), 4)}
        for i in top_lr_idx
    ]

    rf_imp = rf_model.feature_importances_
    top_rf_idx = np.argsort(rf_imp)[::-1][:15]
    rf_top_features = [
        {"feature": feature_names[i], "gini_importance": round(float(rf_imp[i]), 5)}
        for i in top_rf_idx
    ]

    best_model = trained_models[best_name]
    perm = permutation_importance(
        best_model, X_test_scaled[:400], y_test[:400], n_repeats=3, random_state=seed, n_jobs=-1
    )
    top_perm_idx = np.argsort(perm.importances_mean)[::-1][:15]
    perm_top_features = [
        {
            "feature": feature_names[i],
            "importance_mean": round(float(perm.importances_mean[i]), 5),
            "importance_std": round(float(perm.importances_std[i]), 5),
        }
        for i in top_perm_idx
    ]

    # Save artifacts
    models_dir.mkdir(parents=True, exist_ok=True)
    joblib.dump(full_scaler, models_dir / "har_scaler.joblib")
    joblib.dump(best_model, models_dir / "activity_classifier.joblib")
    joblib.dump(lr_model, models_dir / "activity_logreg_xai.joblib")
    joblib.dump(rf_model, models_dir / "activity_rf_xai.joblib")

    return {
        "best_model_by_val_f1": best_name,
        "validation_comparison": val_results,
        "held_out_test_comparison": test_results,
        "explainability": {
            "logistic_regression_top_features": lr_top_features,
            "random_forest_top_features": rf_top_features,
            "best_model_permutation_importance": perm_top_features,
        },
    }


def evaluate_task_b(data: dict[str, Any], seed: int, models_dir: Path) -> dict[str, Any]:
    """Task B: Anomaly detection (stationary baseline vs unusual dynamic motion).
    Strictly represents unusual motion/activity context, NEVER confirmed theft.
    """
    X_train_full = data["X_train_full"]
    y_train_full = data["y_train_full"]
    X_test = data["X_test"]
    y_test = data["y_test"]

    # Normal baseline = stationary states (4=SITTING, 5=STANDING, 6=LAYING)
    normal_mask_train = np.isin(y_train_full, [4, 5, 6])
    X_train_norm = X_train_full[normal_mask_train]

    scaler = StandardScaler()
    X_tr_scaled = scaler.fit_transform(X_train_norm)
    X_te_scaled = scaler.transform(X_test)
    # 1 = unusual dynamic motion (WALKING/STAIRS), 0 = stationary baseline
    y_test_unusual = np.isin(y_test, [1, 2, 3]).astype(int)

    detectors = {
        "isolation_forest": IsolationForest(
            n_estimators=150, contamination=0.05, random_state=seed, n_jobs=-1
        ),
        "local_outlier_factor": LocalOutlierFactor(
            n_neighbors=25, contamination=0.05, novelty=True, n_jobs=-1
        ),
        "one_class_svm": OneClassSVM(nu=0.05, kernel="rbf", gamma="scale"),
    }

    results: dict[str, Any] = {}
    best_anom_name = "isolation_forest"
    best_auc = -1.0
    fitted_detectors: dict[str, Any] = {}

    for name, det in detectors.items():
        det.fit(X_tr_scaled)
        t0 = time.perf_counter()
        raw_pred = det.predict(X_te_scaled)  # -1 = outlier, 1 = inlier
        infer_ms = (time.perf_counter() - t0) * 1000.0 / len(X_te_scaled)
        y_pred = (raw_pred == -1).astype(int)
        # Higher anomaly score => more unusual
        scores = -det.decision_function(X_te_scaled)
        auc = float(roc_auc_score(y_test_unusual, scores))
        f1 = float(f1_score(y_test_unusual, y_pred))
        prec = float(precision_score(y_test_unusual, y_pred))
        rec = float(recall_score(y_test_unusual, y_pred))
        results[name] = {
            "roc_auc": round(auc, 4),
            "precision": round(prec, 4),
            "recall": round(rec, 4),
            "f1_score": round(f1, 4),
            "inference_ms_per_sample": round(infer_ms, 5),
        }
        fitted_detectors[name] = det
        if auc > best_auc:
            best_auc = auc
            best_anom_name = name

    # Statistical Z-score baseline (mean max-abs z-score across energy/jerk features)
    z_scores = np.mean(np.abs(X_te_scaled), axis=1)
    z_thresh = np.percentile(np.mean(np.abs(X_tr_scaled), axis=1), 95)
    z_pred = (z_scores > z_thresh).astype(int)
    results["zscore_baseline"] = {
        "roc_auc": round(float(roc_auc_score(y_test_unusual, z_scores)), 4),
        "precision": round(float(precision_score(y_test_unusual, z_pred)), 4),
        "recall": round(float(recall_score(y_test_unusual, z_pred)), 4),
        "f1_score": round(float(f1_score(y_test_unusual, z_pred)), 4),
        "threshold_z": round(float(z_thresh), 4),
    }

    joblib.dump(scaler, models_dir / "anomaly_scaler.joblib")
    joblib.dump(fitted_detectors[best_anom_name], models_dir / "anomaly_detector.joblib")

    return {
        "task_definition": "Stationary context baseline (Sitting/Standing/Laying) vs Unusual Dynamic Motion (Walking/Stairs). Never interpreted as theft proof.",
        "best_detector_by_roc_auc": best_anom_name,
        "detector_comparison": results,
    }


def main() -> None:
    config = json.loads(Path("ml/configs/training_config.json").read_text())
    seed = config["random_seed"]
    dataset_root = download_uci_har(Path(config["raw_dir"]))
    data = load_uci_har(dataset_root)

    # Class imbalance analysis
    unique_tr, counts_tr = np.unique(data["y_train_full"], return_counts=True)
    unique_te, counts_te = np.unique(data["y_test"], return_counts=True)
    class_balance = {
        "train": {data["label_map"][int(k)]: int(v) for k, v in zip(unique_tr, counts_tr)},
        "test": {data["label_map"][int(k)]: int(v) for k, v in zip(unique_te, counts_te)},
    }

    models_dir = Path(config["models_dir"])
    reports_dir = Path(config["reports_dir"])
    reports_dir.mkdir(parents=True, exist_ok=True)

    task_a = evaluate_task_a(data, seed, models_dir)
    task_b = evaluate_task_b(data, seed, models_dir)

    metadata = {
        "model_version": config["model_version"],
        "trained_at_utc": datetime.now(timezone.utc).isoformat(),
        "random_seed": seed,
        "dataset": config["dataset_name"],
        "train_samples": int(data["X_train_full"].shape[0]),
        "test_samples": int(data["X_test"].shape[0]),
        "num_features": int(data["X_train_full"].shape[1]),
        "train_subjects": sorted(set(data["s_train_full"].tolist())),
        "test_subjects": sorted(set(data["s_test"].tolist())),
        "label_map": {str(k): v for k, v in data["label_map"].items()},
        "best_activity_classifier": task_a["best_model_by_val_f1"],
        "best_anomaly_detector": task_b["best_detector_by_roc_auc"],
        "feature_names": data["feature_names"],
        "limitations": [
            "Trained on UCI HAR smartphone waist-mounted activity data (30 subjects).",
            "Does NOT predict or confirm mobile phone theft.",
            "Used strictly for device motion/context classification and supplementary anomaly scoring.",
        ],
    }
    (models_dir / "model_metadata.json").write_text(json.dumps(metadata, indent=2))

    report = {
        "metadata": {k: v for k, v in metadata.items() if k != "feature_names"},
        "class_balance": class_balance,
        "task_a_activity_classification": task_a,
        "task_b_anomaly_detection": task_b,
    }
    (reports_dir / "evaluation_report.json").write_text(json.dumps(report, indent=2))

    # Write Markdown evaluation report & Model Card
    best_a = task_a["best_model_by_val_f1"]
    best_a_metrics = task_a["held_out_test_comparison"][best_a]
    best_b = task_b["best_detector_by_roc_auc"]
    best_b_metrics = task_b["detector_comparison"][best_b]

    md_lines = [
        "# Guardian AI — ML Training & Evaluation Report",
        "",
        f"- **Generated (UTC):** {metadata['trained_at_utc']}",
        f"- **Dataset:** {metadata['dataset']} (21 train subjects: {len(metadata['train_subjects'])}, 9 held-out test subjects: {len(metadata['test_subjects'])})",
        f"- **Train/Test Samples:** {metadata['train_samples']} / {metadata['test_samples']} ({metadata['num_features']} features, 0 subject overlap)",
        "",
        "## Critical Scope Notice",
        "UCI HAR contains human-activity sensor labels only. **No phone-theft labels exist in this dataset.** Guardian AI uses these models exclusively for **Task A (Device Activity/Context Classification)** and **Task B (Unusual Motion Context Anomaly Detection)**.",
        "",
        "## Task A — Device Activity/Context Classification (Held-out 9-Subject Test Set)",
        "",
        "| Model | Val F1 | Test Accuracy | Test Precision | Test Recall | Test Macro F1 | Latency (ms/sample) |",
        "|---|---:|---:|---:|---:|---:|---:|",
    ]
    for m_name, m_val in task_a["validation_comparison"].items():
        t_val = task_a["held_out_test_comparison"][m_name]
        md_lines.append(
            f"| {m_name} | {m_val['val_macro_f1']:.4f} | {t_val['test_accuracy']:.4f} | "
            f"{t_val['test_macro_precision']:.4f} | {t_val['test_macro_recall']:.4f} | "
            f"{t_val['test_macro_f1']:.4f} | {t_val['test_inference_ms_per_sample']:.4f} |"
        )

    md_lines += [
        "",
        f"**Selected Classifier:** `{best_a}` (Test Accuracy: {best_a_metrics['test_accuracy']:.4f}, Macro F1: {best_a_metrics['test_macro_f1']:.4f})",
        "",
        "## Task B — Motion Anomaly Detection (Stationary Baseline vs Dynamic Motion)",
        "",
        "| Detector | ROC-AUC | Precision | Recall | F1-Score |",
        "|---|---:|---:|---:|---:|",
    ]
    for d_name, d_val in task_b["detector_comparison"].items():
        md_lines.append(
            f"| {d_name} | {d_val['roc_auc']:.4f} | {d_val['precision']:.4f} | "
            f"{d_val['recall']:.4f} | {d_val['f1_score']:.4f} |"
        )

    md_lines += [
        "",
        f"**Selected Anomaly Detector:** `{best_b}` (ROC-AUC: {best_b_metrics['roc_auc']:.4f})",
        "",
        "## Top Explainability Signals (Top 10 Permutation Importance)",
    ]
    for item in task_a["explainability"]["best_model_permutation_importance"][:10]:
        md_lines.append(f"- `{item['feature']}`: mean={item['importance_mean']} (±{item['importance_std']})")

    (reports_dir / "evaluation_report.md").write_text("\n".join(md_lines) + "\n")

    model_card = f"""# Model Card — Guardian AI Activity & Motion Anomaly Models ({config['model_version']})

## Intended Use
- **Primary Use:** Classify smartphone motion/activity context (`WALKING`, `WALKING_UPSTAIRS`, `WALKING_DOWNSTAIRS`, `SITTING`, `STANDING`, `LAYING`) and detect unusual dynamic motion relative to a stationary baseline as a *supplementary context signal* in user-confirmed incident workflows.
- **Out-of-Scope / Prohibited Use:** Do **NOT** use this model to declare a phone stolen, accuse individuals, or automatically trigger emergency authorities (999/police).

## Training Data & Provenance
- **Dataset:** UCI Human Activity Recognition Using Smartphones (Anguita et al., 30 participants, 10,299 windowed samples, 561 sensor features).
- **Splitting:** Participant-aware split (21 training participants vs 9 completely disjoint test participants; internal 20% GroupShuffleSplit validation).

## Measured Performance (Held-Out Test Set)
- **Task A (`{best_a}`):** Accuracy = `{best_a_metrics['test_accuracy']:.4f}`, Macro F1 = `{best_a_metrics['test_macro_f1']:.4f}`
- **Task B (`{best_b}`):** ROC-AUC = `{best_b_metrics['roc_auc']:.4f}`, F1 = `{best_b_metrics['f1_score']:.4f}`

## Limitations
1. Trained on waist-mounted smartphone lab recordings; real-world handbag/pocket placement and snatching events differ from lab walking/stairs.
2. Contains zero theft labels; anomaly score only measures deviation from stationary sensor baseline.
"""
    (reports_dir / "model_card.md").write_text(model_card)
    print(f"Training complete! Best Task A={best_a} (acc={best_a_metrics['test_accuracy']}), Best Task B={best_b} (auc={best_b_metrics['roc_auc']})")


if __name__ == "__main__":
    main()
