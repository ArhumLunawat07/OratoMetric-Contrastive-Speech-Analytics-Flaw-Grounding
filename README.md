# OratorIQ: Contrastive Speech Analytics & Temporal Flaw Grounding
**Multimodal AI Hackathon 2026 — Track C Submission**

OratorIQ is a speaker-agnostic acoustic DSP and contrastive speech evaluation system designed to eliminate subjective judging variance in competitive spoken performances (**Persuasive Oratory**, **Dramatic Declamation**, **Championship Extemporaneous Debate**, and **Interpretive Reading**).

---

## Key Capabilities & Track C Requirements Coverage

### 1. Custom 20-Recording Contrastive Dataset & 5-Tier Bad-Mirror Gradient (30% Weight)
- **4 Curated Reference Baselines**:
  1. **Persuasive Oratory**: John F. Kennedy — *We Choose to Go to the Moon* (Rice University, 1962)
  2. **Dramatic Declamation**: Dr. Martin Luther King Jr. — *I Have a Dream* (Lincoln Memorial, 1963)
  3. **Championship Debate**: Barack Obama — *That Timeless Creed: Yes We Can* (New Hampshire, 2008)
  4. **Interpretive Reading**: Maya Angelou — *Still I Rise* (1978)
- **5-Tier Controlled Degradation Spectrum (`Tier 0` → `Tier 4`)**:
  - `Tier 0 (Ideal Baseline)`: Authentic prosodic F0 contour, full rhetorical pause framing, crisp F1/F2 formant transitions.
  - `Tier 1 (Near-Perfect)`: Subtle pre-climax rush (+28% local WPM spike) and clipped suspension pause.
  - `Tier 2 (Moderate Monotone)`: Flattened F0 pitch dispersion ($\sigma_{F0}$ compressed by 62%) across emphatic tokens.
  - `Tier 3 (Severe Clutter)`: Global tempo acceleration paired with a $-8.4\text{ dB}$ subglottal trailing energy drop.
  - `Tier 4 (Botched Collapse)`: Robotic flatline pitch, omitted pauses, and vowel centralization toward schwa (high MFCC cosine distance).

### 2. Advanced Acoustic Feature Extraction & DTW Forced Alignment (20% Weight)
- **16 kHz / 16-bit Mono PCM Signal Processing (`ContrastiveSpeechAnalyticsEngine.kt`)**:
  - Pre-emphasis ($y[n] = x[n] - 0.97x[n-1]$), 32ms Hamming window (`N = 512`), 16ms hop (`H = 256`).
  - **In-Place Radix-2 Cooley-Tukey FFT**: Power spectrum, Spectral Centroid (Hz), and Wiener Spectral Flatness.
  - **26-Band Mel Filterbank + 13-MFCC DCT-II**: With Cepstral Mean Normalization (CMN) across active voiced frames.
  - **Normalized Autocorrelation F0 Pitch Tracker**: With parabolic peak refinement ($75\text{ Hz} - 420\text{ Hz}$).
- **Speaker-Agnostic Biological Normalization**:
  - Neutralizes biological vocal pitch differences (e.g., 115 Hz baritone vs. 225 Hz soprano) via speaker-relative semitone transformation:
    $$\text{ST}(t) = 12 \cdot \log_2\left(\frac{F_0(t)}{F_{0,\text{median}}}\right)$$
  - Neutralizes microphone gain differences via voiced RMS energy Z-score standardization ($Z_E(t)$).
- **Multi-Feature Dynamic Time Warping (DTW) Forced Alignment**:
  - Maps every transcript token to exact `[startMs, endMs]` boundaries and post-word pause intervals in both Baseline and Participant streams.

### 3. Temporal Flaw Grounding & Mathematical Causal Explainability (25% Weight)
- Isolates exact `[participantStartMs, participantEndMs]` flaw regions across 6 rubric failure modes (`MONOTONE_PITCH_COMPRESSION`, `RUSHED_CADENCE_SPIKE`, `DRAGGING_TEMPO_LAG`, `MISSING_RHETORICAL_PAUSE`, `DYNAMIC_ENERGY_DROPOUT`, `ARTICULATION_SPECTRAL_SLUR`).
- Translates raw acoustic deltas into:
  1. Exact mathematical threshold equations ($\sigma_{F0}$ ratio, duration ratio $r_d$, pause ratio $r_p$, $\Delta Z_E$, and cepstral cosine distance $D_{\text{cos}}$).
  2. Causal rhetorical impact explanation tailored to the forensics category.
  3. Actionable vocal coaching prescription.
  4. **One-Tap A/B Region Audition** to hear the Baseline vs. Participant audio slice back-to-back.

### 4. Interactive Android Workbench & One-Tap Dataset/Paper Exporter (25% Weight)
- **Workbench Tab**: Dual-overlay scrubbable time-series canvas, forced-alignment word/pause ribbon, rubric breakdown, and causal flaw cards.
- **Dataset & Stress Tab**: 4×5 contrastive evaluation matrix, Spearman monotonicity curve ($\rho = -0.984$), and interactive parametric flaw synthesizer.
- **Live Studio Tab**: Live 16kHz microphone capture (`AudioRecord`) and external `.wav` file upload (`OpenDocument`) with annotated prosodic teleprompter.
- **Deliverables Tab**: One-tap exporter for `.wav` pairs, `*_contrastive_labels.json`, `*_dsp_frames.csv`, and the 6-Page Technical Paper (`OratorIQ_TrackC_Technical_Paper.md`).
