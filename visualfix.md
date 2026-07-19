Phase 6.6 — Body Map Visual Polish & Interaction Refinement

The Body Localization feature is already complete and working correctly.

Everything below is already implemented and functioning properly:

Body region model
Region enum & IDs
Multi-selection
Follow-up questions
Gemini integration
Prompt generation
Doctor portal visualization
Patient Passport history
Timeline
Demo Mode
Firestore persistence
Navigation
ViewModels
Repositories
AI urgency integration

Do NOT redesign or rewrite the feature.

This phase is UI/UX refinement only.

Primary Objective

I have now added new SVG reference illustrations (Front, Back, Left, Right).

These SVGs are now the single visual source of truth for the body map.

Your task is to make the Compose body map visually follow these SVGs as closely as possible while preserving every existing feature and workflow.

Think of this as replacing the illustration only—not the feature.

1. Preserve ALL Existing Functionality

Do NOT modify:

Body region model
Region enum
Region IDs
Multi-selection logic
Follow-up workflow
Gemini integration
AI prompt generation
Doctor consultation body map
Patient Passport
Timeline
Firestore
Demo Mode
Navigation
ViewModels
Repositories
Connectivity
Existing data structures

The AI currently uses the selected body regions as additional clinical context during symptom analysis.

This behavior is correct and must remain exactly as it is.

Nothing about the medical workflow should change.

2. Use the Uploaded SVGs as the Permanent Visual Reference

The uploaded SVGs are now the official body illustrations.

Do NOT invent new proportions.

Do NOT redesign the human body.

Do NOT stylize it differently.

Instead:

follow the SVG contours
follow the SVG proportions
follow the SVG silhouette
follow the SVG posture

Front, Back, Left and Right should closely resemble their corresponding SVG.

The goal is that someone comparing the app and the SVG immediately recognizes them as the same illustration.

3. Refit Existing Interactive Regions

Every existing region must remain exactly the same logically.

However, visually reposition and reshape each region so it naturally fits inside the new silhouette.

Examples:

head
neck
shoulders
chest
upper abdomen
lower abdomen
pelvis
upper arms
forearms
hands
thighs
knees
calves
feet

There should be:

no floating regions
no oversized blocks
no awkward spacing
no unnatural overlap

Regions should appear embedded into the silhouette.

4. Keep the Existing Hands

The uploaded SVG hands should remain recognizable.

Do NOT simplify them into circles or mitten shapes.

Do NOT redesign the hands.

Follow the SVG closely.

The current SVG is already suitable and should simply be respected.

5. Material 3 Styling

Keep the current Material 3 design system.

Maintain:

animated selection
selection colors
smooth transitions
accessibility labels
existing animation timings
current highlight behavior

Only improve geometry and visual polish.

6. Hover / Press Interaction

Improve discoverability while preserving the existing interaction model.

Before selection:

When the user presses (or hovers in supported environments):

lightly brighten the region
softly increase opacity
show a subtle primary-colored outline
slightly emphasize the selected area

The animation should feel smooth and lightweight.

Avoid exaggerated scaling or flashy effects.

The goal is simply to communicate:

"This area is interactive."

After selection:

Keep the existing animated fill transition.

Do not introduce distracting animations.

7. Improve the Follow-up Bottom Sheet

The current workflow is correct.

Do not change:

questions
options
logic
stored values
AI integration

Only improve presentation.

The sheet should feel cleaner and more professional.

Suggested improvements:

improve spacing
improve typography hierarchy
align chips consistently
make section spacing more comfortable
improve slider placement
improve visual grouping
use better padding
keep buttons consistently aligned
maintain Material 3 styling

Do NOT add additional questions.

Do NOT remove existing questions.

Do NOT alter the workflow.

8. Region Feedback

When a region is selected:

keep the current animation but make the transition feel smoother.

The selected state should clearly communicate:

selected
active
ready for additional symptom details

without changing existing logic.

9. Responsiveness

The body must continue scaling correctly across different phone sizes.

Do not hardcode pixel values.

Maintain responsiveness.

Maintain accessibility.

10. Performance

Remain lightweight.

Do NOT introduce:

OpenGL
Filament
SceneView
Compose 3D
heavy rendering libraries

Continue using lightweight Compose drawing.

11. Absolutely Do NOT

Do NOT redesign the workflow.

Do NOT rename anything.

Do NOT modify Gemini prompts.

Do NOT alter repositories.

Do NOT change ViewModels.

Do NOT touch persistence.

Do NOT change doctor workflow.

Do NOT change Firestore.

Do NOT modify AI logic.

Do NOT change navigation.

Do NOT introduce new features.

Only refine the visual presentation and interaction.

Success Criteria

When comparing the app with the uploaded SVGs:

the body silhouette closely matches the SVGs
front, back, left and right views all resemble their references
every interactive region naturally fits inside the silhouette
hover/press interaction makes the body feel responsive and interactive
the follow-up sheet looks cleaner and more polished
all existing medical functionality continues working exactly as before
Gemini continues receiving the selected body regions exactly as it does today
the feature feels like a production-quality medical body localization interface without changing any behavior