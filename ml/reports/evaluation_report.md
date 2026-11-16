# Guardian AI — ML Training & Evaluation Report

- **Generated (UTC):** 2026-11-16T15:04:40.507638+00:00
- **Dataset:** UCI Human Activity Recognition Using Smartphones (21 train subjects: 21, 9 held-out test subjects: 9)
- **Train/Test Samples:** 7352 / 2947 (561 features, 0 subject overlap)

## Critical Scope Notice
UCI HAR contains human-activity sensor labels only. **No phone-theft labels exist in this dataset.** Guardian AI uses these models exclusively for **Task A (Device Activity/Context Classification)** and **Task B (Unusual Motion Context Anomaly Detection)**.

## Task A — Device Activity/Context Classification (Held-out 9-Subject Test Set)

| Model | Val F1 | Test Accuracy | Test Precision | Test Recall | Test Macro F1 | Latency (ms/sample) |
|---|---:|---:|---:|---:|---:|---:|
| logistic_regression | 0.9540 | 0.9559 | 0.9586 | 0.9546 | 0.9557 | 0.0018 |
| decision_tree | 0.8563 | 0.8643 | 0.8640 | 0.8607 | 0.8611 | 0.0014 |
| random_forest | 0.9233 | 0.9277 | 0.9288 | 0.9249 | 0.9261 | 0.0279 |
| linear_svc | 0.9642 | 0.9603 | 0.9631 | 0.9600 | 0.9608 | 0.0012 |
| hist_gradient_boosting | 0.9397 | 0.9365 | 0.9392 | 0.9351 | 0.9362 | 0.0425 |

**Selected Classifier:** `linear_svc` (Test Accuracy: 0.9603, Macro F1: 0.9608)

## Task B — Motion Anomaly Detection (Stationary Baseline vs Dynamic Motion)

| Detector | ROC-AUC | Precision | Recall | F1-Score |
|---|---:|---:|---:|---:|
| isolation_forest | 1.0000 | 0.9448 | 1.0000 | 0.9716 |
| local_outlier_factor | 1.0000 | 0.9222 | 1.0000 | 0.9595 |
| one_class_svm | 1.0000 | 0.9334 | 1.0000 | 0.9655 |
| zscore_baseline | 1.0000 | 0.9481 | 1.0000 | 0.9733 |

**Selected Anomaly Detector:** `one_class_svm` (ROC-AUC: 1.0000)

## Top Explainability Signals (Top 10 Permutation Importance)
- `tBodyGyroJerk-entropy()-X`: mean=0.1825 (±0.0108)
- `fBodyGyro-entropy()-X`: mean=0.0425 (±0.0054)
- `tBodyGyroJerk-arCoeff()-X,3`: mean=0.03167 (±0.00656)
- `tBodyAccJerk-entropy()-Z`: mean=0.03083 (±0.00717)
- `tBodyGyroJerk-entropy()-Y`: mean=0.02833 (±0.00825)
- `fBodyAcc-entropy()-X`: mean=0.0275 (±0.01061)
- `tGravityAcc-mean()-Y`: mean=0.02417 (±0.00312)
- `tBodyGyroJerk-arCoeff()-Z,1`: mean=0.02083 (±0.00425)
- `tGravityAcc-max()-Y`: mean=0.01917 (±0.00471)
- `tBodyAcc-min()-Z`: mean=0.01917 (±0.00514)
