# Model Card — Guardian AI Activity & Motion Anomaly Models (guardian-har-v1.0.0)

## Intended Use
- **Primary Use:** Classify smartphone motion/activity context (`WALKING`, `WALKING_UPSTAIRS`, `WALKING_DOWNSTAIRS`, `SITTING`, `STANDING`, `LAYING`) and detect unusual dynamic motion relative to a stationary baseline as a *supplementary context signal* in user-confirmed incident workflows.
- **Out-of-Scope / Prohibited Use:** Do **NOT** use this model to declare a phone stolen, accuse individuals, or automatically trigger emergency authorities (999/police).

## Training Data & Provenance
- **Dataset:** UCI Human Activity Recognition Using Smartphones (Anguita et al., 30 participants, 10,299 windowed samples, 561 sensor features).
- **Splitting:** Participant-aware split (21 training participants vs 9 completely disjoint test participants; internal 20% GroupShuffleSplit validation).

## Measured Performance (Held-Out Test Set)
- **Task A (`linear_svc`):** Accuracy = `0.9603`, Macro F1 = `0.9608`
- **Task B (`one_class_svm`):** ROC-AUC = `1.0000`, F1 = `0.9655`

## Limitations
1. Trained on waist-mounted smartphone lab recordings; real-world handbag/pocket placement and snatching events differ from lab walking/stairs.
2. Contains zero theft labels; anomaly score only measures deviation from stationary sensor baseline.
